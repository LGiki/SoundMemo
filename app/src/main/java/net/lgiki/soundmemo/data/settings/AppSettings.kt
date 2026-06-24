package net.lgiki.soundmemo.data.settings

import net.lgiki.soundmemo.data.storage.DEFAULT_RECORDING_NAME_TEMPLATE
import net.lgiki.soundmemo.domain.recorder.AudioInputPreference
import net.lgiki.soundmemo.domain.recorder.RecordingFormat

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val recorderVisualization: RecorderVisualization = RecorderVisualization.Waveform,
    val vuMeterValueDisplay: VuMeterValueDisplay = VuMeterValueDisplay.Percent,
    val recordingFormat: RecordingFormat = RecordingFormat.M4a,
    val bitrate: Int = 128_000,
    val sampleRate: Int = 44_100,
    val preferredAudioInput: AudioInputPreference? = null,
    val recordingNameTemplate: String = DEFAULT_RECORDING_NAME_TEMPLATE,
    val keepScreenAwake: Boolean = true,
    val recordLocation: Boolean = false,
    val writeLocationToMediaFile: Boolean = false,
    val recycleRetentionDays: Int = 30,
    val playbackSpeed: Float = 1f,
    val rewindSeconds: Int = 10,
    val forwardSeconds: Int = 10,
    val locale: String = "system",
)

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class RecorderVisualization {
    Waveform,
    VuMeter,
}

enum class VuMeterValueDisplay {
    Percent,
    Decibels,
    PercentAndDecibels,
}
