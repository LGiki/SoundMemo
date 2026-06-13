package net.lgiki.soundmemo.data.settings

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val bitrate: Int = 128_000,
    val sampleRate: Int = 44_100,
    val keepScreenAwake: Boolean = true,
    val recycleRetentionDays: Int = 30,
    val playbackSpeed: Float = 1f,
)

enum class ThemeMode {
    System,
    Light,
    Dark,
}

