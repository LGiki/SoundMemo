package net.lgiki.soundmemo.ui.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.lgiki.soundmemo.SoundMemoContainer
import net.lgiki.soundmemo.domain.recorder.AudioInputDevice
import net.lgiki.soundmemo.domain.recorder.AudioInputPreference
import net.lgiki.soundmemo.domain.recorder.RecordingLocation
import net.lgiki.soundmemo.domain.recorder.RecordingLocationProvider
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.domain.recorder.RecorderUiState
import net.lgiki.soundmemo.domain.recorder.RecordingStateHolder
import net.lgiki.soundmemo.service.RecordingService

class RecorderViewModel(
    private val container: SoundMemoContainer,
) : ViewModel() {
    val state: StateFlow<RecorderUiState> = RecordingStateHolder.state
    val preferredAudioInput: StateFlow<AudioInputPreference?> = container.settingsRepository.settings
        .map { it.preferredAudioInput }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val audioInputDevices: StateFlow<List<AudioInputDevice>> = container.audioInputDeviceRepository.devices
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            container.audioInputDeviceRepository.currentDevices(),
        )

    fun start(context: Context, location: RecordingLocation? = null) {
        ContextCompat.startForegroundService(context, RecordingService.startIntent(context, RecordingService.ACTION_START, location))
    }

    fun startWithOptionalLocation(context: Context, recordLocation: Boolean) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            val location = if (recordLocation) {
                RecordingLocationProvider.currentLocation(appContext)
            } else {
                null
            }
            start(appContext, location)
        }
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

    fun setPreferredAudioInput(preference: AudioInputPreference?) {
        viewModelScope.launch {
            container.settingsRepository.setPreferredAudioInput(preference)
        }
    }

    fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    fun resetSavedMessage() {
        val current = state.value
        if (current.status == RecorderStatus.Saved) {
            RecordingStateHolder.update(current.copy(status = RecorderStatus.Idle, message = null))
        }
    }
}
