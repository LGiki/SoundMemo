package net.lgiki.soundmemo.data.storage

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbandonedRecordingFilesTest {
    @Test
    fun scan_findsOnlyDirectFilesAndReportsTotalSize() {
        val root = Files.createTempDirectory("soundmemo-staging").toFile()
        try {
            val staging = root.resolve("recordings").apply { mkdirs() }
            staging.resolve("one.wav").writeBytes(ByteArray(3))
            staging.resolve("two.mp3").writeBytes(ByteArray(5))
            staging.resolve("nested").apply { mkdirs() }.resolve("ignored.wav").writeBytes(ByteArray(7))

            val files = abandonedRecordingFilesIn(staging)

            assertEquals(2, files.count)
            assertEquals(8L, files.totalBytes)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun delete_removesSafeFilesAndRejectsFilesOutsideStagingDirectory() {
        val root = Files.createTempDirectory("soundmemo-staging").toFile()
        try {
            val staging = root.resolve("recordings").apply { mkdirs() }
            val safeFile = staging.resolve("safe.wav").apply { writeBytes(byteArrayOf(1)) }
            val outsideFile = root.resolve("outside.wav").apply { writeBytes(byteArrayOf(2)) }
            val files = AbandonedRecordingFiles(listOf(safeFile, outsideFile))

            val result = deleteAbandonedRecordingFilesIn(staging, files)

            assertEquals(1, result.deletedCount)
            assertEquals(1, result.failedFiles.count)
            assertFalse(safeFile.exists())
            assertTrue(outsideFile.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
