package net.lgiki.soundmemo.service

import net.lgiki.soundmemo.domain.recorder.AudioInputRoute
import net.lgiki.soundmemo.service.audio.AudioRecordingBackend
import net.lgiki.soundmemo.service.audio.RecordedOutput
import net.lgiki.soundmemo.service.audio.RecordingChannels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingChannelFallbackTest {
    @Test
    fun stereoFailure_retriesOnceInMono() {
        val created = mutableListOf<FakeBackend>()
        var beforeRetryCalls = 0

        val result = startWithMonoFallback(
            requestedChannels = RecordingChannels.Stereo,
            beforeRetry = { beforeRetryCalls += 1 },
        ) { channels ->
            FakeBackend(failStart = channels == RecordingChannels.Stereo).also(created::add)
        }

        assertTrue(result.fellBackToMono)
        assertSame(created[1], result.backend)
        assertEquals(1, beforeRetryCalls)
        assertTrue(created[0].released)
        assertFalse(created[1].released)
    }

    @Test
    fun monoFailure_doesNotRetry() {
        var createCalls = 0

        runCatching {
            startWithMonoFallback(RecordingChannels.Mono) {
                createCalls += 1
                FakeBackend(failStart = true)
            }
        }

        assertEquals(1, createCalls)
    }

    private class FakeBackend(
        private val failStart: Boolean,
    ) : AudioRecordingBackend {
        var released = false
        override val maxAmplitude: Int = 0
        override val routedDevice: AudioInputRoute? = null
        override val failure: Throwable? = null

        override fun start() {
            if (failStart) error("unsupported channels")
        }

        override fun pause() = Unit
        override fun resume() = Unit
        override fun stop(): List<RecordedOutput> = emptyList()
        override fun release() {
            released = true
        }
    }
}
