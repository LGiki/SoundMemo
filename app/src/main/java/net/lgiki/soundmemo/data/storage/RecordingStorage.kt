package net.lgiki.soundmemo.data.storage

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

class RecordingStorage(private val context: Context) {
    private val recordingsDir: File
        get() = File(context.filesDir, "recordings").apply { mkdirs() }

    fun createOutputFile(
        template: String = DEFAULT_RECORDING_NAME_TEMPLATE,
        now: Long = System.currentTimeMillis(),
        uniqueSuffix: String = UUID.randomUUID().toString(),
    ): File {
        val generatedName = RecordingNameTemplate.generate(template, now, uniqueSuffix)
        return createOutputFile(generatedName)
    }

    fun createOutputFile(generatedName: GeneratedRecordingName): File {
        return File(recordingsDir, generatedName.fileName)
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

    fun shareUri(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    fun deleteFile(path: String): Boolean {
        val file = File(path)
        return !file.exists() || file.delete()
    }
}

private val GENERATED_RECORDING_NAME = Regex("""SoundMemo_\d{8}_\d{6}_\d{3}_[A-Za-z0-9-]+""")
