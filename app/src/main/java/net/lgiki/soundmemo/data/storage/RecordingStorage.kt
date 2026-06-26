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
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.data.model.RecordingStorageType
import net.lgiki.soundmemo.data.settings.RecordingStorageLocation

class RecordingStorage(private val context: Context) {
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
                runCatching { context.contentResolver.delete(uri, null, null) >= 0 }.getOrDefault(false)
            }
            RecordingStorageType.ContentUri -> {
                val uri = recording.storageUri?.let(Uri::parse) ?: return true
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                    .getOrElse { runCatching { context.contentResolver.delete(uri, null, null) >= 0 }.getOrDefault(false) }
            }
            RecordingStorageType.File -> deleteFile(recording.filePath)
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
            resolver.update(uri, values, null, null)
            tempFile.delete()
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
            tempFile.delete()
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
        if (!renameTo(destination)) {
            inputStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            delete()
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
