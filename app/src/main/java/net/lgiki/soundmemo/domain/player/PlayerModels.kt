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

