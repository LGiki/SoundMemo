package net.lgiki.soundmemo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.lgiki.soundmemo.data.settings.SettingsRepository
import net.lgiki.soundmemo.data.settings.ThemeMode

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        net.lgiki.soundmemo.data.settings.AppSettings(),
    )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColor(enabled) }
    fun setBitrate(value: Int) = viewModelScope.launch { repository.setBitrate(value) }
    fun setKeepScreenAwake(enabled: Boolean) = viewModelScope.launch { repository.setKeepScreenAwake(enabled) }
    fun setRecycleRetentionDays(days: Int) = viewModelScope.launch { repository.setRecycleRetentionDays(days) }
    fun setPlaybackSpeed(speed: Float) = viewModelScope.launch { repository.setPlaybackSpeed(speed) }
}

