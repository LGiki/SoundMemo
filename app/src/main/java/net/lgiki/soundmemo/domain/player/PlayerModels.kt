package net.lgiki.soundmemo.domain.player

import net.lgiki.soundmemo.data.model.Recording

data class PlayerUiState(
    val recording: Recording? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val error: String? = null,
)

internal fun unavailableRecordingState(
    previous: PlayerUiState,
    recording: Recording,
    error: String,
): PlayerUiState = previous.copy(
    recording = recording,
    isPlaying = false,
    positionMs = 0,
    durationMs = recording.durationMs,
    error = error,
)

internal fun selectedRecordingState(previous: PlayerUiState, recording: Recording): PlayerUiState =
    previous.copy(
        recording = recording,
        isPlaying = false,
        positionMs = 0,
        durationMs = recording.durationMs,
        error = null,
    )
