package net.lgiki.soundmemo.domain.recorder

enum class RecordingFormat(
    val storageValue: String,
    val extension: String,
    private val fixedBitrate: Int? = null,
    private val fixedSampleRate: Int? = null,
    val supportsLocationMetadata: Boolean = false,
    val usesPcmRecorder: Boolean = false,
    val usesAacBitrateRange: Boolean = false,
) {
    M4a(
        storageValue = "m4a",
        extension = "m4a",
        supportsLocationMetadata = true,
        usesAacBitrateRange = true,
    ),
    Aac(
        storageValue = "aac",
        extension = "aac",
        usesAacBitrateRange = true,
    ),
    ThreeGp(
        storageValue = "3gp",
        extension = "3gp",
        fixedBitrate = 23_850,
        fixedSampleRate = 16_000,
        supportsLocationMetadata = true,
    ),
    Wav(
        storageValue = "wav",
        extension = "wav",
        fixedBitrate = 705_600,
        fixedSampleRate = 44_100,
        usesPcmRecorder = true,
    ),
    Mp3(
        storageValue = "mp3",
        extension = "mp3",
        fixedSampleRate = 44_100,
        usesPcmRecorder = true,
    );

    val usesCustomEncodingSettings: Boolean = fixedBitrate == null

    fun bitrateFor(configuredBitrate: Int): Int = fixedBitrate ?: configuredBitrate

    fun sampleRateFor(configuredSampleRate: Int): Int = fixedSampleRate ?: configuredSampleRate

    companion object {
        fun fromStorageValue(value: String?): RecordingFormat =
            entries.firstOrNull { it.storageValue == value } ?: M4a
    }
}
