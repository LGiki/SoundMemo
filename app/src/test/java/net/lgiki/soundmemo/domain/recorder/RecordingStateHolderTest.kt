package net.lgiki.soundmemo.domain.recorder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingStateHolderTest {
    @Test
    fun update_appliesConcurrentTransformationsAtomically() = runBlocking {
        RecordingStateHolder.set(RecorderUiState())

        coroutineScope {
            repeat(1_000) {
                launch(Dispatchers.Default) {
                    RecordingStateHolder.update { state ->
                        state.copy(elapsedMs = state.elapsedMs + 1L)
                    }
                }
            }
        }

        assertEquals(1_000L, RecordingStateHolder.state.value.elapsedMs)
        RecordingStateHolder.set(RecorderUiState())
    }
}
