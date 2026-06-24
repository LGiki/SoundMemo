package net.lgiki.soundmemo.service.audio

import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WavHeaderTest {
    @Test
    fun write_writesMonoRiffWaveHeaderWithPatchedSizes() {
        assertWavHeader(
            channelCount = 1,
            expectedByteRate = 88_200,
            expectedBlockAlign = 2,
        )
    }

    @Test
    fun write_writesStereoRiffWaveHeaderWithPatchedSizes() {
        assertWavHeader(
            channelCount = 2,
            expectedByteRate = 176_400,
            expectedBlockAlign = 4,
        )
    }

    private fun assertWavHeader(
        channelCount: Int,
        expectedByteRate: Int,
        expectedBlockAlign: Int,
    ) {
        val file = File.createTempFile("soundmemo", ".wav")
        try {
            RandomAccessFile(file, "rw").use { output ->
                WavHeader.write(output, sampleRate = 44_100, channelCount = channelCount, dataBytes = 4)
            }

            val bytes = file.readBytes()
            assertEquals(44, bytes.size)
            assertAscii("RIFF", bytes, 0)
            assertEquals(40, bytes.intLeAt(4))
            assertAscii("WAVE", bytes, 8)
            assertAscii("fmt ", bytes, 12)
            assertEquals(16, bytes.intLeAt(16))
            assertEquals(1, bytes.shortLeAt(20))
            assertEquals(channelCount, bytes.shortLeAt(22))
            assertEquals(44_100, bytes.intLeAt(24))
            assertEquals(expectedByteRate, bytes.intLeAt(28))
            assertEquals(expectedBlockAlign, bytes.shortLeAt(32))
            assertEquals(16, bytes.shortLeAt(34))
            assertAscii("data", bytes, 36)
            assertEquals(4, bytes.intLeAt(40))
        } finally {
            file.delete()
        }
    }

    private fun assertAscii(expected: String, bytes: ByteArray, offset: Int) {
        assertArrayEquals(expected.toByteArray(Charsets.US_ASCII), bytes.copyOfRange(offset, offset + expected.length))
    }

    private fun ByteArray.intLeAt(offset: Int): Int =
        (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16) or
            ((this[offset + 3].toInt() and 0xff) shl 24)

    private fun ByteArray.shortLeAt(offset: Int): Int =
        (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)
}
