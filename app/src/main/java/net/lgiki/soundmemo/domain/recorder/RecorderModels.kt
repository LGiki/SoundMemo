package net.lgiki.soundmemo.domain.recorder

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
    val lastSavedId: Long? = null,
    val message: String? = null,
)

