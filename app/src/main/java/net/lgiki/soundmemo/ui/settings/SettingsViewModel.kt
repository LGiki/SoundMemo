package net.lgiki.soundmemo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lgiki.soundmemo.data.settings.AppSettings
import net.lgiki.soundmemo.data.settings.SettingsRepository
import net.lgiki.soundmemo.data.settings.ThemeMode
import net.lgiki.soundmemo.data.storage.DEFAULT_RECORDING_NAME_TEMPLATE
import net.lgiki.soundmemo.domain.recorder.AacBitrateOptions
import net.lgiki.soundmemo.domain.recorder.AudioInputDevice
import net.lgiki.soundmemo.domain.recorder.AudioInputDeviceRepository
import net.lgiki.soundmemo.domain.recorder.AudioInputPreference
import net.lgiki.soundmemo.domain.recorder.BitrateOptions
import net.lgiki.soundmemo.domain.recorder.RecordingFormat

class SettingsViewModel(
    private val repository: SettingsRepository,
    audioInputDeviceRepository: AudioInputDeviceRepository,
) : ViewModel() {
    val settings = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        net.lgiki.soundmemo.data.settings.AppSettings(),
    )

    private val _bitrateOptions = MutableStateFlow(
        BitrateOptions(
            values = AacBitrateOptions.fallbackValues,
            range = null,
        ),
    )
    val bitrateOptions: StateFlow<BitrateOptions> = _bitrateOptions.asStateFlow()
    private var bitrateOptionsReady = false

    val audioInputDevices: StateFlow<List<AudioInputDevice>> = audioInputDeviceRepository.devices.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        audioInputDeviceRepository.currentDevices(),
    )

    private val _localeChanged = MutableStateFlow<String?>(null)
    val localeChanged: StateFlow<String?> = _localeChanged.asStateFlow()

    init {
        viewModelScope.launch {
            val options = withContext(Dispatchers.Default) {
                AacBitrateOptions.load()
            }
            _bitrateOptions.value = options
            bitrateOptionsReady = true
            ensureSupportedBitrate(settings.value, options)
        }
        viewModelScope.launch {
            repository.settings.collect { appSettings ->
                if (bitrateOptionsReady) {
                    ensureSupportedBitrate(appSettings, _bitrateOptions.value)
                }
            }
        }
    }

    private suspend fun ensureSupportedBitrate(settings: AppSettings, options: BitrateOptions) {
        if (!settings.recordingFormat.usesCustomEncodingSettings) return
        val values = if (settings.recordingFormat.usesAacBitrateRange) {
            options.values
        } else {
            AacBitrateOptions.fallbackValues
        }
        if (settings.bitrate !in values) {
            repository.setBitrate(AacBitrateOptions.closestSupported(settings.bitrate, values))
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColor(enabled) }
    fun setRecordingFormat(format: RecordingFormat) = viewModelScope.launch { repository.setRecordingFormat(format) }
    fun setPreferredAudioInput(preference: AudioInputPreference?) = viewModelScope.launch { repository.setPreferredAudioInput(preference) }
    fun setBitrate(value: Int) = viewModelScope.launch { repository.setBitrate(value) }
    fun setRecordingNameTemplate(template: String) = viewModelScope.launch { repository.setRecordingNameTemplate(template) }
    fun resetRecordingNameTemplate() = viewModelScope.launch { repository.setRecordingNameTemplate(DEFAULT_RECORDING_NAME_TEMPLATE) }
    fun setKeepScreenAwake(enabled: Boolean) = viewModelScope.launch { repository.setKeepScreenAwake(enabled) }
    fun setRecordLocation(enabled: Boolean) = viewModelScope.launch { repository.setRecordLocation(enabled) }
    fun setWriteLocationToMediaFile(enabled: Boolean) = viewModelScope.launch { repository.setWriteLocationToMediaFile(enabled) }
    fun setRecycleRetentionDays(days: Int) = viewModelScope.launch { repository.setRecycleRetentionDays(days) }
    fun setRewindSeconds(seconds: Int) = viewModelScope.launch { repository.setRewindSeconds(seconds) }
    fun setForwardSeconds(seconds: Int) = viewModelScope.launch { repository.setForwardSeconds(seconds) }

    fun setLocale(tag: String) {
        if (settings.value.locale == tag) return
        viewModelScope.launch {
            repository.setLocale(tag)
            _localeChanged.value = tag
        }
    }

    fun consumeLocaleChange(): String? {
        val tag = _localeChanged.value
        if (tag != null) _localeChanged.value = null
        return tag
    }
}
