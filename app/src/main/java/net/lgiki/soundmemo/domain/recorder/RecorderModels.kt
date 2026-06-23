package net.lgiki.soundmemo.domain.recorder

const val WAVEFORM_SAMPLE_COUNT = 48

enum class RecorderStatus {
    Idle,
    Recording,
    Paused,
    Saving,
    Saved,
    Error,
}

data class RecorderUiState(
    val status: RecorderStatus = RecorderStatus.Idle,
    val elapsedMs: Long = 0,
    val amplitude: Int = 0,
    val waveform: List<Float> = emptyList(),
    val preferredAudioInput: AudioInputPreference? = null,
    val actualAudioInput: AudioInputRoute? = null,
    val lastSavedId: Long? = null,
    val message: String? = null,
)
