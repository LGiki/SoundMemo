package net.lgiki.soundmemo.domain.recorder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioInputDeviceTest {
    @Test
    fun preferenceMatchesDevice_ignoresCaseAndWhitespace() {
        val preference = AudioInputPreference(type = 7, productName = "  Headset Mic ")
        val device = AudioInputDevice(id = 42, type = 7, productName = "headset mic")

        assertTrue(preference.matches(device))
    }

    @Test
    fun preferenceMatchesDevice_requiresSameType() {
        val preference = AudioInputPreference(type = 7, productName = "Headset Mic")
        val device = AudioInputDevice(id = 42, type = 8, productName = "Headset Mic")

        assertFalse(preference.matches(device))
    }

    @Test
    fun preferenceMatchesDevice_usesIdWhenPresent() {
        val preference = AudioInputPreference(id = 42, type = 7, productName = "Headset Mic")

        assertTrue(preference.matches(AudioInputDevice(id = 42, type = 7, productName = "Headset Mic")))
        assertFalse(preference.matches(AudioInputDevice(id = 43, type = 7, productName = "Headset Mic")))
    }

    @Test
    fun audioInputProductName_usesTypeFallbackForBlankNames() {
        assertTrue(AudioInputPreference(type = 7, productName = "7").matchesTypeAndName(AudioInputDevice(id = 42, type = 7, productName = audioInputProductName(7, ""))))
    }

    @Test
    fun selectableAudioInputDevices_keepsOneBuiltInMicrophoneAndAllExternalDevices() {
        val devices = listOf(
            AudioInputDevice(id = 1, type = android.media.AudioDeviceInfo.TYPE_BUILTIN_MIC, productName = "Bottom mic"),
            AudioInputDevice(id = 2, type = android.media.AudioDeviceInfo.TYPE_BUILTIN_MIC, productName = "Top mic"),
            AudioInputDevice(id = 3, type = android.media.AudioDeviceInfo.TYPE_USB_DEVICE, productName = "USB mic"),
        )

        assertEquals(listOf(1, 3), devices.selectableAudioInputDevices().map { it.id })
    }
}
