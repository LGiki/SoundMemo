package net.lgiki.soundmemo.ui.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import net.lgiki.soundmemo.SoundMemoContainer
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.domain.recorder.RecorderUiState
import net.lgiki.soundmemo.domain.recorder.RecordingStateHolder
import net.lgiki.soundmemo.service.RecordingService

class RecorderViewModel(
    @Suppress("unused") private val container: SoundMemoContainer,
) : ViewModel() {
    val state: StateFlow<RecorderUiState> = RecordingStateHolder.state

    fun start(context: Context) {
        ContextCompat.startForegroundService(context, RecordingService.startIntent(context, RecordingService.ACTION_START))
    }

    fun pause(context: Context) {
        context.startService(RecordingService.startIntent(context, RecordingService.ACTION_PAUSE))
    }

    fun resume(context: Context) {
        context.startService(RecordingService.startIntent(context, RecordingService.ACTION_RESUME))
    }

    fun stop(context: Context) {
        context.startService(RecordingService.startIntent(context, RecordingService.ACTION_STOP))
    }

    fun cancel(context: Context) {
        context.startService(RecordingService.startIntent(context, RecordingService.ACTION_CANCEL))
    }

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }

    fun resetSavedMessage() {
        val current = state.value
        if (current.status == RecorderStatus.Saved) {
            RecordingStateHolder.update(current.copy(status = RecorderStatus.Idle, message = null))
        }
    }
}

