package net.lgiki.soundmemo.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {
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
