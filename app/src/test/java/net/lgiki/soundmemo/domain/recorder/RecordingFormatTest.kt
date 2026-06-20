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
        assertEquals(RecordingFormat.Wav, RecordingFormat.fromStorageValue("wav"))
        assertEquals(RecordingFormat.Mp3, RecordingFormat.fromStorageValue("mp3"))
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

    @Test
    fun wav_usesFixedPcmEncodingSettings() {
        assertFalse(RecordingFormat.Wav.usesCustomEncodingSettings)
        assertTrue(RecordingFormat.Wav.usesPcmRecorder)
        assertEquals(705_600, RecordingFormat.Wav.bitrateFor(128_000))
        assertEquals(44_100, RecordingFormat.Wav.sampleRateFor(48_000))
    }

    @Test
    fun mp3_usesConfiguredBitrateAndFixedSampleRate() {
        assertTrue(RecordingFormat.Mp3.usesCustomEncodingSettings)
        assertTrue(RecordingFormat.Mp3.usesPcmRecorder)
        assertEquals(192_000, RecordingFormat.Mp3.bitrateFor(192_000))
        assertEquals(44_100, RecordingFormat.Mp3.sampleRateFor(48_000))
    }
}
