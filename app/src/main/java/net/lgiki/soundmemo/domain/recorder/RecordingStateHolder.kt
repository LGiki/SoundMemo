package net.lgiki.soundmemo.domain.recorder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object RecordingStateHolder {
    private val mutableState = MutableStateFlow(RecorderUiState())
    val state: StateFlow<RecorderUiState> = mutableState.asStateFlow()

    fun set(state: RecorderUiState) {
        mutableState.value = state
    }

    fun update(transform: (RecorderUiState) -> RecorderUiState) {
        mutableState.update(transform)
    }
}
