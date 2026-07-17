package net.lgiki.soundmemo.domain.recorder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingLocationFreshnessTest {
    @Test
    fun timestamp_isAcceptedOnlyWithinFreshnessWindow() {
        val now = 1_000_000L

        assertTrue(RecordingLocationProvider.isLocationTimestampFresh(now, now))
        assertTrue(
            RecordingLocationProvider.isLocationTimestampFresh(
                now - RecordingLocationProvider.MAX_LOCATION_AGE_MS,
                now,
            ),
        )
        assertFalse(
            RecordingLocationProvider.isLocationTimestampFresh(
                now - RecordingLocationProvider.MAX_LOCATION_AGE_MS - 1L,
                now,
            ),
        )
        assertFalse(RecordingLocationProvider.isLocationTimestampFresh(0L, now))
        assertFalse(RecordingLocationProvider.isLocationTimestampFresh(now + 60_001L, now))
    }
}
