package net.lgiki.soundmemo.data.storage

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingStorage(private val context: Context) {
    private val recordingsDir: File
        get() = File(context.filesDir, "recordings").apply { mkdirs() }

    fun createOutputFile(now: Long = System.currentTimeMillis()): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
        return File(recordingsDir, "SoundMemo_$stamp.m4a")
    }

    fun displayNameFor(file: File): String = file.nameWithoutExtension.replace('_', ' ')

    fun shareUri(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    fun deleteFile(path: String): Boolean = File(path).delete()
}

