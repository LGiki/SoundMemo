package net.lgiki.soundmemo.domain.recorder

const val WAVEFORM_SAMPLE_COUNT = 48

enum class RecorderStatus {
    Idle,
    Starting,
    Recording,
    Paused,
    Saving,
    Saved,
    Error,
}

internal fun shouldKeepScreenAwake(enabled: Boolean, status: RecorderStatus): Boolean =
    enabled && isRecorderWorkflowActive(status)

internal fun isRecorderWorkflowActive(status: RecorderStatus): Boolean =
    status in setOf(
        RecorderStatus.Recording,
        RecorderStatus.Paused,
        RecorderStatus.Saving,
        RecorderStatus.Starting,
    )

internal fun canManageAbandonedStagingFiles(status: RecorderStatus): Boolean =
    !isRecorderWorkflowActive(status)

internal fun consumeRecorderMessage(state: RecorderUiState): RecorderUiState =
    state.copy(
        status = if (state.status == RecorderStatus.Saved) RecorderStatus.Idle else state.status,
        message = null,
    )

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
