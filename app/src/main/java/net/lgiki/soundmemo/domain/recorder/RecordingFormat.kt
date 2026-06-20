package net.lgiki.soundmemo.domain.recorder

enum class RecordingFormat(
    val storageValue: String,
    val extension: String,
    private val fixedBitrate: Int? = null,
    private val fixedSampleRate: Int? = null,
    val supportsLocationMetadata: Boolean = false,
) {
    M4a(
        storageValue = "m4a",
        extension = "m4a",
        supportsLocationMetadata = true,
    ),
    Aac(
        storageValue = "aac",
        extension = "aac",
    ),
    ThreeGp(
        storageValue = "3gp",
        extension = "3gp",
        fixedBitrate = 23_850,
        fixedSampleRate = 16_000,
        supportsLocationMetadata = true,
    );

    val usesCustomEncodingSettings: Boolean = fixedBitrate == null && fixedSampleRate == null

    fun bitrateFor(configuredBitrate: Int): Int = fixedBitrate ?: configuredBitrate

    fun sampleRateFor(configuredSampleRate: Int): Int = fixedSampleRate ?: configuredSampleRate

    companion object {
        fun fromStorageValue(value: String?): RecordingFormat =
            entries.firstOrNull { it.storageValue == value } ?: M4a
    }
}
