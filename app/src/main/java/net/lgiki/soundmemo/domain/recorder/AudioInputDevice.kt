package net.lgiki.soundmemo.domain.recorder

data class AudioInputPreference(
    val id: Int? = null,
    val type: Int,
    val productName: String,
)

data class AudioInputDevice(
    val id: Int,
    val type: Int,
    val productName: String,
) {
    val preference: AudioInputPreference
        get() = AudioInputPreference(id = id, type = type, productName = productName)
}

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
