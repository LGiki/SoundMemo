package net.lgiki.soundmemo.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("soundmemo_settings")

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System,
            dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
            bitrate = prefs[BITRATE] ?: 128_000,
            sampleRate = prefs[SAMPLE_RATE] ?: 44_100,
            keepScreenAwake = prefs[KEEP_SCREEN_AWAKE] ?: true,
            recycleRetentionDays = prefs[RECYCLE_RETENTION_DAYS] ?: 30,
            playbackSpeed = prefs[PLAYBACK_SPEED] ?: 1f,
            locale = prefs[LOCALE] ?: "system",
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setBitrate(value: Int) {
        dataStore.edit { it[BITRATE] = value }
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_AWAKE] = enabled }
    }

    suspend fun setRecycleRetentionDays(days: Int) {
        dataStore.edit { it[RECYCLE_RETENTION_DAYS] = days }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { it[PLAYBACK_SPEED] = speed }
    }

    suspend fun setLocale(tag: String) {
        dataStore.edit { it[LOCALE] = tag }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BITRATE = intPreferencesKey("bitrate")
        val SAMPLE_RATE = intPreferencesKey("sample_rate")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val RECYCLE_RETENTION_DAYS = intPreferencesKey("recycle_retention_days")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val LOCALE = stringPreferencesKey("locale")
    }
}

