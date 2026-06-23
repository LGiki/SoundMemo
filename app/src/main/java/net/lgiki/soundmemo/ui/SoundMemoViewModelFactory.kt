package net.lgiki.soundmemo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.lgiki.soundmemo.SoundMemoContainer
import net.lgiki.soundmemo.ui.library.LibraryViewModel
import net.lgiki.soundmemo.ui.recorder.RecorderViewModel
import net.lgiki.soundmemo.ui.settings.SettingsViewModel

class SoundMemoViewModelFactory(
    private val container: SoundMemoContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(RecorderViewModel::class.java) -> RecorderViewModel(container) as T
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(container) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                repository = container.settingsRepository,
                audioInputDeviceRepository = container.audioInputDeviceRepository,
            ) as T
            else -> error("Unknown ViewModel ${modelClass.name}")
        }
}
