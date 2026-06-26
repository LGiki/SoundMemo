package net.lgiki.soundmemo.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {
    @Test
    fun formatPreciseDuration_includesMillisecondsForShortRecordings() {
        assertEquals("00:00.001", formatPreciseDuration(1))
        assertEquals("00:00.999", formatPreciseDuration(999))
    }

    @Test
    fun formatPreciseDuration_formatsSecondsAndHours() {
        assertEquals("00:01.234", formatPreciseDuration(1_234))
        assertEquals("1:00:00.007", formatPreciseDuration(3_600_007))
    }

    @Test
    fun formatPreciseDuration_clampsNegativeDurations() {
        assertEquals("00:00.000", formatPreciseDuration(-1))
    }

    @Test
    fun formatCoordinates_usesStableDecimalSeparator() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.FRANCE)
        try {
            assertEquals("48.85660, 2.35220", formatCoordinates(48.8566, 2.3522))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun formatCoordinates_rejectsInvalidValues() {
        assertNull(formatCoordinates(Double.NaN, 2.3522))
        assertNull(formatCoordinates(48.8566, Double.POSITIVE_INFINITY))
        assertNull(formatCoordinates(91.0, 2.3522))
        assertNull(formatCoordinates(48.8566, 181.0))
    }
}
