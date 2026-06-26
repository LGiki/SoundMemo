package net.lgiki.soundmemo.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaults_useDeviceMusicWithoutCustomFolder() {
        val settings = AppSettings()

        assertEquals(RecordingStorageLocation.DeviceMusic, settings.recordingStorageLocation)
        assertNull(settings.customRecordingFolderUri)
        assertNull(settings.customRecordingFolderName)
    }
}
