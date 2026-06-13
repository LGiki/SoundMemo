package net.lgiki.soundmemo.domain.recorder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecordingStateHolder {
    private val mutableState = MutableStateFlow(RecorderUiState())
    val state: StateFlow<RecorderUiState> = mutableState.asStateFlow()

    fun update(state: RecorderUiState) {
        mutableState.value = state
    }
}

