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

    private val _bitrateOptions = MutableStateFlow(
        BitrateOptions(
            values = AacBitrateOptions.fallbackValues,
            range = null,
        ),
    )
    val bitrateOptions: StateFlow<BitrateOptions> = _bitrateOptions.asStateFlow()
    private var bitrateOptionsReady = false

    private val _localeChanged = MutableStateFlow<String?>(null)
    val localeChanged: StateFlow<String?> = _localeChanged.asStateFlow()

    init {
        viewModelScope.launch {
            val options = withContext(Dispatchers.Default) {
                AacBitrateOptions.load()
            }
            _bitrateOptions.value = options
            bitrateOptionsReady = true
            ensureSupportedBitrate(settings.value.bitrate, options.values)
        }
        viewModelScope.launch {
            repository.settings.collect { appSettings ->
                if (bitrateOptionsReady) {
                    ensureSupportedBitrate(appSettings.bitrate, _bitrateOptions.value.values)
                }
            }
        }
    }

    private suspend fun ensureSupportedBitrate(bitrate: Int, options: List<Int>) {
        if (bitrate !in options) {
            repository.setBitrate(AacBitrateOptions.closestSupported(bitrate, options))
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColor(enabled) }
    fun setBitrate(value: Int) = viewModelScope.launch { repository.setBitrate(value) }
    fun setKeepScreenAwake(enabled: Boolean) = viewModelScope.launch { repository.setKeepScreenAwake(enabled) }
    fun setRecordLocation(enabled: Boolean) = viewModelScope.launch { repository.setRecordLocation(enabled) }
    fun setWriteLocationToMediaFile(enabled: Boolean) = viewModelScope.launch { repository.setWriteLocationToMediaFile(enabled) }
    fun setRecycleRetentionDays(days: Int) = viewModelScope.launch { repository.setRecycleRetentionDays(days) }

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
