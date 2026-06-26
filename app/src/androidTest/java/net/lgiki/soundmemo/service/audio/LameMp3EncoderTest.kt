package net.lgiki.soundmemo.service.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LameMp3EncoderTest {
    @Test
    fun encodeAndFlush_producesMp3Bytes() {
        val encoder = LameMp3Encoder(sampleRate = 44_100, bitrate = 128_000, channelCount = 1)
        encoder.use { encoder ->
            val samples = ShortArray(44_100 / 10)
            val encoded = encoder.encode(samples, samples.size) + encoder.flush()

            assertTrue(encoded.isNotEmpty())
            assertTrue(encoded.hasMp3FrameSync())
        }
    }

    private fun ByteArray.hasMp3FrameSync(): Boolean =
        asSequence()
            .windowed(size = 2)
            .any { (first, second) ->
                first.toInt() and 0xff == 0xff && second.toInt() and 0xe0 == 0xe0
            }
}
