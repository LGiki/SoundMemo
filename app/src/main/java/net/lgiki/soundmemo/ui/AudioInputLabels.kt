package net.lgiki.soundmemo.ui

import android.media.AudioDeviceInfo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.lgiki.soundmemo.R

@Composable
fun audioInputLabel(type: Int, productName: String): String = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_MIC -> stringResource(R.string.settings_microphone_builtin)
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_HEADSET -> namedOrGenericAudioInput(productName, R.string.settings_microphone_bluetooth)
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_HEADSET -> namedOrGenericAudioInput(productName, R.string.settings_microphone_usb)
    AudioDeviceInfo.TYPE_WIRED_HEADSET -> namedOrGenericAudioInput(productName, R.string.settings_microphone_wired)
    else -> productName.ifBlank { stringResource(R.string.settings_microphone_external) }
}

@Composable
private fun namedOrGenericAudioInput(
    productName: String,
    @StringRes genericStringId: Int,
): String =
    productName
        .takeUnless { it.isBlank() || it.all { char -> char.isDigit() } }
        ?: stringResource(genericStringId)
