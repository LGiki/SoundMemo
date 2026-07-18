package net.lgiki.soundmemo.service.audio

import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WavAudioWriterTest {
    @Test
    fun pcmFrameBuffer_carriesOddStereoSampleIntoNextRead() {
        val buffer = PcmFrameBuffer(channelCount = 2)

        val (first, firstCount) = buffer.takeCompleteFrames(shortArrayOf(1, 2, 3), 3)
        val (second, secondCount) = buffer.takeCompleteFrames(shortArrayOf(4, 5, 6), 3)

        assertEquals(2, firstCount)
        assertArrayEquals(shortArrayOf(1, 2), first.copyOf(firstCount))
        assertEquals(4, secondCount)
        assertArrayEquals(shortArrayOf(3, 4, 5, 6), second.copyOf(secondCount))
    }

    @Test
    fun finish_withoutCompleteFrames_discardsEmptyWavFile() {
        val directory = Files.createTempDirectory("soundmemo-empty-wav").toFile()
        try {
            val firstFile = directory.resolve("recording.wav")
            val writer = PcmRecordingBackend.WavAudioWriter(
                firstFile = firstFile,
                sampleRate = 44_100,
                channelCount = 2,
                partFile = { part -> recordingPartFile(firstFile, part) },
                maxDataBytes = 8,
            )

            val outputs = writer.finish()

            assertEquals(emptyList<RecordedOutput>(), outputs)
            assertFalse(firstFile.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun write_splitsAtFrameBoundaryWithoutDroppingSamples() {
        val directory = Files.createTempDirectory("soundmemo-wav-split").toFile()
        try {
            val firstFile = directory.resolve("recording.wav")
            val writer = PcmRecordingBackend.WavAudioWriter(
                firstFile = firstFile,
                sampleRate = 1_000,
                channelCount = 2,
                partFile = { part -> recordingPartFile(firstFile, part) },
                maxDataBytes = 8,
            )
            writer.write(shortArrayOf(1, 2, 3, 4, 5), 5)
            writer.write(shortArrayOf(6), 1)

            val outputs = writer.finish()

            assertEquals(2, outputs.size)
            assertEquals(listOf(1, 2), outputs.map { it.partIndex })
            assertEquals(listOf(2L, 1L), outputs.map { it.durationMs })
            assertEquals("recording_part02.wav", outputs[1].file.name)
            assertEquals(8, outputs[0].file.readBytes().littleEndianIntAt(40))
            assertEquals(4, outputs[1].file.readBytes().littleEndianIntAt(40))
            val combinedPcm = outputs.flatMap { output ->
                output.file.readBytes().drop(44)
            }.toByteArray()
            assertArrayEquals(
                shortArrayOf(1, 2, 3, 4, 5, 6).toLittleEndianBytes(),
                combinedPcm,
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun ShortArray.toLittleEndianBytes(): ByteArray = flatMap { sample ->
        listOf(
            (sample.toInt() and 0xff).toByte(),
            ((sample.toInt() ushr 8) and 0xff).toByte(),
        )
    }.toByteArray()

    private fun ByteArray.littleEndianIntAt(offset: Int): Int =
        (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16) or
            ((this[offset + 3].toInt() and 0xff) shl 24)
}
