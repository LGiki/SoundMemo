package net.lgiki.soundmemo.data.storage

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.data.model.RecordingStorageType
import net.lgiki.soundmemo.data.settings.RecordingStorageLocation

class RecordingStorage internal constructor(
    private val context: Context,
    uriOperations: RecordingUriOperations = AndroidRecordingUriOperations(context),
) {
    private val uriDeleter = RecordingUriDeleter(uriOperations)
    private val pendingPublications = PendingPublicationJournal(
        File(context.filesDir, "pending_recording_publications"),
    )
    private val appRecordingsDir: File
        get() = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir,
            APP_RECORDINGS_DIR,
        ).apply { mkdirs() }

    private val tempRecordingsDir: File
        get() = File(context.cacheDir, "recordings").apply { mkdirs() }

    fun createTempOutputFile(generatedName: GeneratedRecordingName): File =
        File(tempRecordingsDir, generatedName.fileName)

    fun abandonedTempRecordings(): AbandonedRecordingFiles {
        return abandonedRecordingFilesIn(tempRecordingsDir)
    }

    fun deleteAbandonedTempRecordings(files: AbandonedRecordingFiles): StagingCleanupResult {
        return deleteAbandonedRecordingFilesIn(tempRecordingsDir, files)
    }

    fun publishRecording(
        tempFile: File,
        generatedName: GeneratedRecordingName,
        location: RecordingStorageLocation,
        format: String,
        customFolderUri: String?,
    ): RecordingSaveResult =
        when (location) {
            RecordingStorageLocation.DeviceMusic -> {
                if (canWriteDeviceMusic()) {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            publishToMediaStore(tempFile, generatedName, format)
                        } else {
                            publishToPublicMusicFile(tempFile, generatedName)
                        }
                    }.getOrElse {
                        publishToAppFile(tempFile, generatedName, fellBackToAppFiles = true)
                    }
                } else {
                    publishToAppFile(tempFile, generatedName, fellBackToAppFiles = true)
                }
            }
            RecordingStorageLocation.CustomFolder -> {
                val treeUri = customFolderUri?.let(Uri::parse)
                if (treeUri != null && hasPersistedWritePermission(treeUri)) {
                    runCatching {
                        publishToCustomFolder(tempFile, generatedName, format, treeUri)
                    }.getOrElse {
                        publishToAppFile(tempFile, generatedName, fellBackToAppFiles = true)
                    }
                } else {
                    publishToAppFile(tempFile, generatedName, fellBackToAppFiles = true)
                }
            }
            RecordingStorageLocation.AppFiles -> publishToAppFile(
                tempFile = tempFile,
                generatedName = generatedName,
                fellBackToAppFiles = false,
            )
        }

    fun displayNameFor(file: File): String {
        val name = file.nameWithoutExtension
        val displayName = if (name.matches(GENERATED_RECORDING_NAME)) {
            name.substringBeforeLast('_')
        } else {
            name
        }
        return displayName.replace('_', ' ')
    }

    fun playbackUri(recording: Recording): Uri? =
        when (RecordingStorageType.fromStorageValue(recording.storageType)) {
            RecordingStorageType.MediaStore -> recording.storageUri?.let(Uri::parse)
            RecordingStorageType.ContentUri -> recording.storageUri?.let(Uri::parse)
            RecordingStorageType.File -> File(recording.filePath).takeIf { it.exists() }?.let(Uri::fromFile)
        }

    fun shareUri(recording: Recording): Uri? =
        when (RecordingStorageType.fromStorageValue(recording.storageType)) {
            RecordingStorageType.MediaStore -> recording.storageUri?.let(Uri::parse)
            RecordingStorageType.ContentUri -> recording.storageUri?.let(Uri::parse)
            RecordingStorageType.File -> File(recording.filePath)
                .takeIf { it.exists() }
                ?.let {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        it,
                    )
                }
        }

    fun recordingExists(recording: Recording): Boolean =
        when (RecordingStorageType.fromStorageValue(recording.storageType)) {
            RecordingStorageType.MediaStore,
            RecordingStorageType.ContentUri -> recording.storageUri
                ?.let(Uri::parse)
                ?.let { uri -> runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { true } }.getOrDefault(false) }
                ?: false
            RecordingStorageType.File -> File(recording.filePath).exists()
        }

    fun deleteRecording(recording: Recording): Boolean =
        when (RecordingStorageType.fromStorageValue(recording.storageType)) {
            RecordingStorageType.MediaStore -> {
                val uri = recording.storageUri?.let(Uri::parse) ?: return true
                uriDeleter.delete(uri, documentUri = false)
            }
            RecordingStorageType.ContentUri -> {
                val uri = recording.storageUri?.let(Uri::parse) ?: return true
                uriDeleter.delete(uri, documentUri = true)
            }
            RecordingStorageType.File -> deleteFile(recording.filePath)
        }

    fun deletePublishedRecording(saveResult: RecordingSaveResult): Boolean =
        when (RecordingStorageType.fromStorageValue(saveResult.storageType)) {
            RecordingStorageType.MediaStore -> {
                val uri = saveResult.storageUri?.let(Uri::parse) ?: return true
                uriDeleter.delete(uri, documentUri = false)
            }
            RecordingStorageType.ContentUri -> {
                val uri = saveResult.storageUri?.let(Uri::parse) ?: return true
                uriDeleter.delete(uri, documentUri = true)
            }
            RecordingStorageType.File -> deleteFile(saveResult.filePath)
        }

    /**
     * Records a published file until its Room metadata has been committed. This lets the next
     * launch remove untracked files if the process dies while a recording is being saved.
     */
    fun markPublicationPending(saveResult: RecordingSaveResult) {
        pendingPublications.add(saveResult)
    }

    fun pendingPublications(): List<RecordingSaveResult> = pendingPublications.read()

    fun removePendingPublications(saveResults: Collection<RecordingSaveResult>) {
        pendingPublications.remove(saveResults)
    }

    fun deleteFile(path: String): Boolean {
        val file = File(path)
        return !file.exists() || file.delete()
    }

    private fun canWriteDeviceMusic(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun hasPersistedWritePermission(uri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }

    private fun publishToAppFile(
        tempFile: File,
        generatedName: GeneratedRecordingName,
        fellBackToAppFiles: Boolean,
    ): RecordingSaveResult {
        val file = File(appRecordingsDir, generatedName.fileName)
        tempFile.moveTo(file)
        return RecordingSaveResult(
            storageType = RecordingStorageType.File.storageValue,
            filePath = file.absolutePath,
            storageUri = null,
            fileSizeBytes = file.length(),
            fellBackToAppFiles = fellBackToAppFiles,
        )
    }

    private fun publishToMediaStore(
        tempFile: File,
        generatedName: GeneratedRecordingName,
        format: String,
    ): RecordingSaveResult {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, generatedName.fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeTypeForFormat(format))
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$PUBLIC_RECORDINGS_DIR/")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create media item")
        try {
            val fileSizeBytes = tempFile.length()
            resolver.openOutputStream(uri, "w")?.use { output ->
                tempFile.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open media item")
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            check(resolver.update(uri, values, null, null) == 1) {
                "Could not finalize media item"
            }
            check(tempFile.delete()) { "Could not remove temporary recording: ${tempFile.absolutePath}" }
            return RecordingSaveResult(
                storageType = RecordingStorageType.MediaStore.storageValue,
                filePath = deviceMusicPath(generatedName.fileName),
                storageUri = uri.toString(),
                fileSizeBytes = fileSizeBytes,
                fellBackToAppFiles = false,
            )
        } catch (exception: Exception) {
            resolver.delete(uri, null, null)
            throw exception
        }
    }

    private fun publishToCustomFolder(
        tempFile: File,
        generatedName: GeneratedRecordingName,
        format: String,
        treeUri: Uri,
    ): RecordingSaveResult {
        val resolver = context.contentResolver
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val uri = DocumentsContract.createDocument(
            resolver,
            parentUri,
            mimeTypeForFormat(format),
            generatedName.fileName,
        ) ?: error("Could not create document")
        try {
            val fileSizeBytes = tempFile.length()
            resolver.openOutputStream(uri, "w")?.use { output ->
                tempFile.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open document")
            check(tempFile.delete()) { "Could not remove temporary recording: ${tempFile.absolutePath}" }
            return RecordingSaveResult(
                storageType = RecordingStorageType.ContentUri.storageValue,
                filePath = documentUriPath(uri.toString()).orEmpty(),
                storageUri = uri.toString(),
                fileSizeBytes = fileSizeBytes,
                fellBackToAppFiles = false,
            )
        } catch (exception: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            throw exception
        }
    }

    @Suppress("DEPRECATION")
    private fun publishToPublicMusicFile(
        tempFile: File,
        generatedName: GeneratedRecordingName,
    ): RecordingSaveResult {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "$PUBLIC_RECORDINGS_DIR/${generatedName.fileName}",
        )
        file.parentFile?.mkdirs()
        tempFile.moveTo(file)
        return RecordingSaveResult(
            storageType = RecordingStorageType.File.storageValue,
            filePath = file.absolutePath,
            storageUri = null,
            fileSizeBytes = file.length(),
            fellBackToAppFiles = false,
        )
    }

    private fun File.moveTo(destination: File) {
        destination.parentFile?.mkdirs()
        check(!destination.exists()) { "Recording destination already exists: ${destination.absolutePath}" }
        if (!renameTo(destination)) {
            val staging = File(destination.parentFile, ".${destination.name}.${UUID.randomUUID()}.pending")
            try {
                inputStream().use { input ->
                    staging.outputStream().use { output -> input.copyTo(output) }
                }
                check(staging.renameTo(destination)) {
                    "Could not finalize recording at ${destination.absolutePath}"
                }
                check(delete()) { "Could not remove temporary recording: $absolutePath" }
            } catch (exception: Exception) {
                staging.delete()
                throw exception
            }
        }
    }
}

internal enum class UriPresence {
    Present,
    Absent,
    Unknown,
}

internal interface RecordingUriOperations {
    fun delete(uri: Uri): Int
    fun deleteDocument(uri: Uri): Boolean
    fun presence(uri: Uri): UriPresence
}

private class AndroidRecordingUriOperations(context: Context) : RecordingUriOperations {
    private val resolver = context.contentResolver

    override fun delete(uri: Uri): Int = resolver.delete(uri, null, null)

    override fun deleteDocument(uri: Uri): Boolean = DocumentsContract.deleteDocument(resolver, uri)

    override fun presence(uri: Uri): UriPresence = try {
        val descriptor = resolver.openFileDescriptor(uri, "r") ?: return UriPresence.Unknown
        descriptor.use { UriPresence.Present }
    } catch (_: FileNotFoundException) {
        UriPresence.Absent
    } catch (_: Exception) {
        UriPresence.Unknown
    }
}

internal class RecordingUriDeleter(
    private val operations: RecordingUriOperations,
) {
    fun delete(uri: Uri, documentUri: Boolean): Boolean {
        if (documentUri && runCatching { operations.deleteDocument(uri) }.getOrDefault(false)) {
            return true
        }
        val deletedRows = runCatching { operations.delete(uri) }.getOrElse { return false }
        return when {
            deletedRows > 0 -> true
            deletedRows == 0 -> runCatching { operations.presence(uri) }
                .getOrDefault(UriPresence.Unknown) == UriPresence.Absent
            else -> false
        }
    }
}

data class RecordingSaveResult(
    val storageType: String,
    val filePath: String,
    val storageUri: String?,
    val fileSizeBytes: Long,
    val fellBackToAppFiles: Boolean,
)

private class PendingPublicationJournal(private val file: File) {
    private val atomicFile = AtomicFile(file)
    private val lock = Any()

    fun add(saveResult: RecordingSaveResult) = synchronized(lock) {
        write(readLocked() + saveResult)
    }

    fun read(): List<RecordingSaveResult> = synchronized(lock) { readLocked() }

    fun remove(results: Collection<RecordingSaveResult>) = synchronized(lock) {
        val remaining = readLocked().filterNot { it in results }
        if (remaining.isEmpty()) {
            atomicFile.delete()
        } else {
            write(remaining)
        }
    }

    private fun readLocked(): List<RecordingSaveResult> = runCatching {
        if (!file.exists()) return emptyList()
        atomicFile.openRead().bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.mapNotNull(::decode).toList()
        }
    }.getOrDefault(emptyList())

    private fun write(results: List<RecordingSaveResult>) {
        val output = atomicFile.startWrite()
        try {
            val contents = results.distinct().joinToString(separator = "\n", postfix = "\n", transform = ::encode)
            output.write(contents.toByteArray(StandardCharsets.UTF_8))
            output.flush()
            atomicFile.finishWrite(output)
        } catch (exception: Exception) {
            atomicFile.failWrite(output)
            throw exception
        }
    }

    private fun encode(value: RecordingSaveResult): String = listOf(
        value.storageType,
        value.filePath,
        value.storageUri.orEmpty(),
        value.fileSizeBytes.toString(),
        value.fellBackToAppFiles.toString(),
    ).joinToString("\t") { field -> Base64.getUrlEncoder().encodeToString(field.toByteArray(StandardCharsets.UTF_8)) }

    private fun decode(line: String): RecordingSaveResult? = runCatching {
        val fields = line.split('\t')
        require(fields.size == 5)
        val decoded = fields.map { field -> String(Base64.getUrlDecoder().decode(field), StandardCharsets.UTF_8) }
        RecordingSaveResult(
            storageType = decoded[0],
            filePath = decoded[1],
            storageUri = decoded[2].ifBlank { null },
            fileSizeBytes = decoded[3].toLong(),
            fellBackToAppFiles = decoded[4].toBooleanStrict(),
        )
    }.getOrNull()
}

class AbandonedRecordingFiles internal constructor(
    internal val files: List<File>,
) {
    val count: Int = files.size
    val totalBytes: Long = files.sumOf { file -> file.length().coerceAtLeast(0L) }
    val isEmpty: Boolean = files.isEmpty()
}

data class StagingCleanupResult(
    val deletedCount: Int,
    val failedFiles: AbandonedRecordingFiles,
)

internal fun abandonedRecordingFilesIn(directory: File): AbandonedRecordingFiles {
    val safeDirectory = directory.canonicalFile
    val files = safeDirectory.listFiles()
        .orEmpty()
        .asSequence()
        .filter(File::isFile)
        .mapNotNull { file -> runCatching { file.canonicalFile }.getOrNull() }
        .filter { file -> file.parentFile == safeDirectory }
        .sortedBy(File::lastModified)
        .toList()
    return AbandonedRecordingFiles(files)
}

internal fun deleteAbandonedRecordingFilesIn(
    directory: File,
    files: AbandonedRecordingFiles,
): StagingCleanupResult {
    val safeDirectory = directory.canonicalFile
    val failed = files.files.filter { file ->
        val safeFile = runCatching { file.canonicalFile }.getOrNull()
        safeFile == null ||
            safeFile.parentFile != safeDirectory ||
            (safeFile.exists() && !safeFile.delete())
    }
    return StagingCleanupResult(
        deletedCount = files.count - failed.size,
        failedFiles = AbandonedRecordingFiles(failed),
    )
}

internal fun mimeTypeForFormat(format: String): String = when (format.lowercase()) {
    "m4a" -> "audio/mp4"
    "aac" -> "audio/aac"
    "3gp" -> "audio/3gpp"
    "wav" -> "audio/wav"
    "mp3" -> "audio/mpeg"
    else -> "audio/*"
}

private const val APP_RECORDINGS_DIR = "recordings"
private const val PUBLIC_MUSIC_DIR = "Music"
private const val PUBLIC_RECORDINGS_DIR = "SoundMemo"

fun deviceMusicPath(fileName: String): String =
    "/storage/emulated/0/$PUBLIC_MUSIC_DIR/$PUBLIC_RECORDINGS_DIR/$fileName"

fun documentUriPath(uriString: String?): String? {
    if (uriString.isNullOrBlank()) return null
    val encodedDocumentId = uriString.substringAfter("/document/", missingDelimiterValue = "")
    if (encodedDocumentId.isBlank()) return null
    val documentId = URLDecoder.decode(encodedDocumentId, StandardCharsets.UTF_8.name())
    val volume = documentId.substringBefore(':', missingDelimiterValue = "")
    val path = documentId.substringAfter(':', missingDelimiterValue = "").trim('/')
    if (volume.isBlank() || path.isBlank()) return null
    return when (volume) {
        "primary" -> "/storage/emulated/0/$path"
        else -> "/storage/$volume/$path"
    }
}
private val GENERATED_RECORDING_NAME = Regex("""SoundMemo_\d{8}_\d{6}_\d{3}_[A-Za-z0-9-]+""")
