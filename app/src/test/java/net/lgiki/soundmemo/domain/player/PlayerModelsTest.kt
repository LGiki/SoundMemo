package net.lgiki.soundmemo.domain.player

import net.lgiki.soundmemo.data.model.Recording
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerModelsTest {
    @Test
    fun selectedRecordingState_resetsPreviousPlaybackProgress() {
        val nextRecording = Recording(id = 2, name = "Next", filePath = "/next.m4a", durationMs = 20_000, fileSizeBytes = 1)

        val state = selectedRecordingState(
            previous = PlayerUiState(
                recording = Recording(id = 1, name = "Previous", filePath = "/previous.m4a", durationMs = 10_000, fileSizeBytes = 1),
                isPlaying = true,
                positionMs = 3_000,
                durationMs = 10_000,
                speed = 1.5f,
                error = "Previous error",
            ),
            recording = nextRecording,
        )

        assertEquals(nextRecording, state.recording)
        assertFalse(state.isPlaying)
        assertEquals(0, state.positionMs)
        assertEquals(nextRecording.durationMs, state.durationMs)
        assertEquals(1.5f, state.speed)
        assertNull(state.error)
    }

    @Test
    fun unavailableRecordingState_selectsUnavailableRecordingAndStopsPreviousPlayback() {
        val previousRecording = Recording(id = 1, name = "Previous", filePath = "/previous.m4a", durationMs = 10_000, fileSizeBytes = 1)
        val unavailableRecording = Recording(id = 2, name = "Missing", filePath = "/missing.m4a", durationMs = 20_000, fileSizeBytes = 1)

        val state = unavailableRecordingState(
            previous = PlayerUiState(
                recording = previousRecording,
                isPlaying = true,
                positionMs = 3_000,
                durationMs = 10_000,
            ),
            recording = unavailableRecording,
            error = "Recording file is unavailable",
        )

        assertEquals(unavailableRecording, state.recording)
        assertFalse(state.isPlaying)
        assertEquals(0, state.positionMs)
        assertEquals(unavailableRecording.durationMs, state.durationMs)
        assertEquals("Recording file is unavailable", state.error)
    }
}
