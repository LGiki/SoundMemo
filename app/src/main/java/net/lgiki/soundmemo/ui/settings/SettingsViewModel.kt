package net.lgiki.soundmemo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.lgiki.soundmemo.data.settings.SettingsRepository
import net.lgiki.soundmemo.data.settings.ThemeMode
import net.lgiki.soundmemo.domain.recorder.AacBitrateOptions
import net.lgiki.soundmemo.domain.recorder.BitrateOptions

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        net.lgiki.soundmemo.data.settings.AppSettings(),
    )

    private val _bitrateOptions = MutableStateFlow(AacBitrateOptions.load())
    val bitrateOptions: StateFlow<BitrateOptions> = _bitrateOptions.asStateFlow()

    private val _localeChanged = MutableStateFlow<String?>(null)
    val localeChanged: StateFlow<String?> = _localeChanged.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { appSettings ->
                val options = _bitrateOptions.value.values
                if (appSettings.bitrate !in options) {
                    repository.setBitrate(AacBitrateOptions.closestSupported(appSettings.bitrate, options))
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColor(enabled) }
    fun setBitrate(value: Int) = viewModelScope.launch { repository.setBitrate(value) }
    fun setKeepScreenAwake(enabled: Boolean) = viewModelScope.launch { repository.setKeepScreenAwake(enabled) }
    fun setRecycleRetentionDays(days: Int) = viewModelScope.launch { repository.setRecycleRetentionDays(days) }
    fun setPlaybackSpeed(speed: Float) = viewModelScope.launch { repository.setPlaybackSpeed(speed) }

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
