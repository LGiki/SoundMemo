package net.lgiki.soundmemo.domain.recorder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingFormatTest {
    @Test
    fun fromStorageValue_defaultsToM4a() {
        assertEquals(RecordingFormat.M4a, RecordingFormat.fromStorageValue(null))
        assertEquals(RecordingFormat.M4a, RecordingFormat.fromStorageValue("unknown"))
    }

    @Test
    fun threeGp_usesFixedEncodingSettings() {
        assertFalse(RecordingFormat.ThreeGp.usesCustomEncodingSettings)
        assertEquals(23_850, RecordingFormat.ThreeGp.bitrateFor(128_000))
        assertEquals(16_000, RecordingFormat.ThreeGp.sampleRateFor(44_100))
    }

    @Test
    fun aacFormats_useConfiguredEncodingSettings() {
        assertTrue(RecordingFormat.M4a.usesCustomEncodingSettings)
        assertTrue(RecordingFormat.Aac.usesCustomEncodingSettings)
        assertEquals(192_000, RecordingFormat.Aac.bitrateFor(192_000))
        assertEquals(48_000, RecordingFormat.Aac.sampleRateFor(48_000))
    }
}
