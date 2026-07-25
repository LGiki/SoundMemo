package net.lgiki.soundmemo.domain.recorder

import android.media.AudioDeviceInfo

data class AudioInputPreference(
    val id: Int? = null,
    val type: Int,
    val productName: String,
)

data class AudioInputDevice(
    val id: Int,
    val type: Int,
    val productName: String,
    val details: AudioInputDeviceDetails? = null,
) {
    val preference: AudioInputPreference
        get() = AudioInputPreference(id = id, type = type, productName = productName)
}

data class AudioInputDeviceDetails(
    val channelCounts: List<Int> = emptyList(),
    val sampleRates: List<Int> = emptyList(),
    val location: AudioInputLocation? = null,
    val directionality: AudioInputDirectionality? = null,
    val group: Int? = null,
    val indexInGroup: Int? = null,
)

enum class AudioInputLocation { MainBody, MovableMainBody, Peripheral }

enum class AudioInputDirectionality { Omni, BiDirectional, Cardioid, HyperCardioid, SuperCardioid }

data class AudioInputRoute(
    val type: Int,
    val productName: String,
)

fun AudioInputPreference.matches(device: AudioInputDevice): Boolean =
    matchesTypeAndName(device) && (id == null || id == device.id)

fun AudioInputPreference.matches(route: AudioInputRoute): Boolean =
    type == route.type && normalizedAudioInputName(productName) == normalizedAudioInputName(route.productName)

fun AudioInputPreference.matchesTypeAndName(device: AudioInputDevice): Boolean =
    type == device.type && normalizedAudioInputName(productName) == normalizedAudioInputName(device.productName)

fun normalizedAudioInputName(value: String): String =
    value.trim().lowercase()

fun audioInputProductName(type: Int, productName: CharSequence): String =
    productName.toString().ifBlank { type.toString() }

/**
 * Returns inputs that are meaningful explicit choices. Android may expose multiple logical
 * built-in microphone ports without public names or placement information; retain one route
 * for the single user-facing "Phone microphone" option while keeping external inputs distinct.
 */
fun List<AudioInputDevice>.selectableAudioInputDevices(): List<AudioInputDevice> {
    var builtInMicrophoneIncluded = false
    return filter { device ->
        if (device.type != AudioDeviceInfo.TYPE_BUILTIN_MIC) return@filter true
        if (builtInMicrophoneIncluded) return@filter false
        builtInMicrophoneIncluded = true
        true
    }
}
