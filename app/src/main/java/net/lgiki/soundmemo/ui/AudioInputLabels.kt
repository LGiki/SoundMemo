package net.lgiki.soundmemo.ui

import android.media.AudioDeviceInfo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.domain.recorder.AudioInputDevice
import net.lgiki.soundmemo.domain.recorder.AudioInputDeviceDetails
import net.lgiki.soundmemo.domain.recorder.AudioInputDirectionality
import net.lgiki.soundmemo.domain.recorder.AudioInputLocation

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

/** Adds an ordinal only when several selectable inputs receive the same user-facing label. */
@Composable
fun audioInputLabels(devices: List<AudioInputDevice>): Map<Int, String> {
    val baseLabels = devices.associate { device ->
        device.id to audioInputLabel(device.type, device.productName)
    }
    val counts = baseLabels.values.groupingBy { it }.eachCount()
    val occurrences = mutableMapOf<String, Int>()
    return devices.associate { device ->
        val label = checkNotNull(baseLabels[device.id])
        val occurrence = (occurrences[label] ?: 0) + 1
        occurrences[label] = occurrence
        device.id to if (counts.getValue(label) > 1) {
            stringResource(R.string.settings_microphone_duplicate_number, label, occurrence)
        } else {
            label
        }
    }
}

@Composable
fun audioInputDetailsLabel(details: AudioInputDeviceDetails?): String? {
    if (details == null) return null
    val identityLabels = buildList {
        details.location?.let { location ->
            add(
                stringResource(
                    when (location) {
                        AudioInputLocation.MainBody -> R.string.settings_microphone_location_main_body
                        AudioInputLocation.MovableMainBody -> R.string.settings_microphone_location_movable_body
                        AudioInputLocation.Peripheral -> R.string.settings_microphone_location_peripheral
                    },
                ),
            )
        }
        details.directionality?.let { directionality ->
            add(
                stringResource(
                    when (directionality) {
                        AudioInputDirectionality.Omni -> R.string.settings_microphone_directionality_omni
                        AudioInputDirectionality.BiDirectional -> R.string.settings_microphone_directionality_bidirectional
                        AudioInputDirectionality.Cardioid -> R.string.settings_microphone_directionality_cardioid
                        AudioInputDirectionality.HyperCardioid -> R.string.settings_microphone_directionality_hypercardioid
                        AudioInputDirectionality.SuperCardioid -> R.string.settings_microphone_directionality_supercardioid
                    },
                ),
            )
        }
    }
    val capabilityLabels = buildList {
        details.channelCounts.maxOrNull()?.let { channels ->
            add(pluralStringResource(R.plurals.settings_microphone_channels, channels, channels))
        }
        details.sampleRates.maxOrNull()?.let { sampleRate ->
            add(stringResource(R.string.settings_microphone_sample_rate, sampleRate / 1000))
        }
        if (details.group != null && details.indexInGroup != null) {
            add(stringResource(R.string.settings_microphone_group_index, details.group, details.indexInGroup + 1))
        }
    }
    return (identityLabels + capabilityLabels)
        .takeIf { it.isNotEmpty() }
        ?.let {
            listOfNotNull(
                identityLabels.takeIf { labels -> labels.isNotEmpty() }?.joinToString(" · "),
                capabilityLabels.takeIf { labels -> labels.isNotEmpty() }?.joinToString(" · "),
            ).joinToString("\n")
        }
}

@Composable
private fun namedOrGenericAudioInput(
    productName: String,
    @StringRes genericStringId: Int,
): String =
    productName
        .takeUnless { it.isBlank() || it.all { char -> char.isDigit() } }
        ?: stringResource(genericStringId)
