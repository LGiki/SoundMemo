package net.lgiki.soundmemo.domain.recorder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecorderScreenAwakeTest {
    @Test
    fun enabled_keepsScreenAwakeOnlyDuringActiveRecordingWorkflow() {
        assertFalse(shouldKeepScreenAwake(true, RecorderStatus.Idle))
        assertTrue(shouldKeepScreenAwake(true, RecorderStatus.Starting))
        assertTrue(shouldKeepScreenAwake(true, RecorderStatus.Recording))
        assertTrue(shouldKeepScreenAwake(true, RecorderStatus.Paused))
        assertTrue(shouldKeepScreenAwake(true, RecorderStatus.Saving))
        assertFalse(shouldKeepScreenAwake(true, RecorderStatus.Saved))
        assertFalse(shouldKeepScreenAwake(true, RecorderStatus.Error))
    }

    @Test
    fun disabled_neverKeepsScreenAwake() {
        RecorderStatus.entries.forEach { status ->
            assertFalse(shouldKeepScreenAwake(false, status))
        }
    }

    @Test
    fun stagingCleanup_isBlockedWhileRecordingIsStartingOrActive() {
        assertFalse(canManageAbandonedStagingFiles(RecorderStatus.Starting))
        assertFalse(canManageAbandonedStagingFiles(RecorderStatus.Recording))
        assertFalse(canManageAbandonedStagingFiles(RecorderStatus.Saving))
        assertTrue(canManageAbandonedStagingFiles(RecorderStatus.Idle))
    }

    @Test
    fun consumeMessage_preservesErrorAndActiveStatesButReturnsSavedToIdle() {
        val error = consumeRecorderMessage(RecorderUiState(status = RecorderStatus.Error, message = "error"))
        val recording = consumeRecorderMessage(RecorderUiState(status = RecorderStatus.Recording, message = "mono"))
        val saved = consumeRecorderMessage(RecorderUiState(status = RecorderStatus.Saved, message = "saved"))

        assertEquals(RecorderStatus.Error, error.status)
        assertEquals(RecorderStatus.Recording, recording.status)
        assertEquals(RecorderStatus.Idle, saved.status)
        assertNull(error.message)
        assertNull(recording.message)
        assertNull(saved.message)
    }
}
