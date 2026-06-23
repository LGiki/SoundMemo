package net.lgiki.soundmemo.domain.recorder

import org.junit.Assert.assertFalse
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
}
