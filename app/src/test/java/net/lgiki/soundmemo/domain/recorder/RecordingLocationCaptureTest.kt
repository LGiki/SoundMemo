package net.lgiki.soundmemo.domain.recorder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordingLocationCaptureTest {
    @Test
    fun update_keepsNewestSessionLocation() {
        val capture = RecordingLocationCapture()
        val cached = location(capturedAt = 1_000L, latitude = 1.0)
        val current = location(capturedAt = 2_000L, latitude = 2.0)

        capture.update(current)
        capture.update(cached)

        assertEquals(current, capture.latest())
    }

    @Test
    fun update_ignoresMissingLocation() {
        val capture = RecordingLocationCapture()

        capture.update(null)

        assertNull(capture.latest())
    }

    private fun location(capturedAt: Long, latitude: Double) = RecordingLocation(
        latitude = latitude,
        longitude = 10.0,
        accuracyMeters = 5f,
        capturedAt = capturedAt,
    )
}
