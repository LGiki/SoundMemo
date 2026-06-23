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
import net.lgiki.soundmemo.data.storage.DEFAULT_RECORDING_NAME_TEMPLATE
import net.lgiki.soundmemo.domain.recorder.AudioInputPreference
import net.lgiki.soundmemo.domain.recorder.RecordingFormat

private val Context.settingsDataStore by preferencesDataStore("soundmemo_settings")

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System,
            dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
            recordingFormat = RecordingFormat.fromStorageValue(prefs[RECORDING_FORMAT]),
            bitrate = prefs[BITRATE] ?: 128_000,
            sampleRate = prefs[SAMPLE_RATE] ?: 44_100,
            preferredAudioInput = preferredAudioInput(
                id = prefs[PREFERRED_AUDIO_INPUT_ID],
                type = prefs[PREFERRED_AUDIO_INPUT_TYPE],
                productName = prefs[PREFERRED_AUDIO_INPUT_NAME],
            ),
            recordingNameTemplate = normalizeRecordingNameTemplate(prefs[RECORDING_NAME_TEMPLATE]),
            keepScreenAwake = prefs[KEEP_SCREEN_AWAKE] ?: true,
            recordLocation = prefs[RECORD_LOCATION] ?: false,
            writeLocationToMediaFile = prefs[WRITE_LOCATION_TO_MEDIA_FILE] ?: false,
            recycleRetentionDays = prefs[RECYCLE_RETENTION_DAYS] ?: 30,
            playbackSpeed = prefs[PLAYBACK_SPEED] ?: 1f,
            rewindSeconds = (prefs[REWIND_SECONDS] ?: 10).coerceIn(MIN_SKIP_SECONDS, MAX_SKIP_SECONDS),
            forwardSeconds = (prefs[FORWARD_SECONDS] ?: 10).coerceIn(MIN_SKIP_SECONDS, MAX_SKIP_SECONDS),
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

    suspend fun setRecordingFormat(format: RecordingFormat) {
        dataStore.edit { it[RECORDING_FORMAT] = format.storageValue }
    }

    suspend fun setPreferredAudioInput(preference: AudioInputPreference?) {
        dataStore.edit {
            if (preference == null) {
                it.remove(PREFERRED_AUDIO_INPUT_ID)
                it.remove(PREFERRED_AUDIO_INPUT_TYPE)
                it.remove(PREFERRED_AUDIO_INPUT_NAME)
            } else {
                preference.id?.let { id -> it[PREFERRED_AUDIO_INPUT_ID] = id } ?: it.remove(PREFERRED_AUDIO_INPUT_ID)
                it[PREFERRED_AUDIO_INPUT_TYPE] = preference.type
                it[PREFERRED_AUDIO_INPUT_NAME] = preference.productName
            }
        }
    }

    suspend fun setRecordingNameTemplate(template: String) {
        dataStore.edit { it[RECORDING_NAME_TEMPLATE] = template.trim().ifBlank { DEFAULT_RECORDING_NAME_TEMPLATE } }
    }

    private fun normalizeRecordingNameTemplate(template: String?): String = when (template) {
        null, OLD_DEFAULT_RECORDING_NAME_TEMPLATE -> DEFAULT_RECORDING_NAME_TEMPLATE
        else -> template
    }

    private fun preferredAudioInput(id: Int?, type: Int?, productName: String?): AudioInputPreference? {
        if (type == null || productName.isNullOrBlank()) return null
        return AudioInputPreference(id = id, type = type, productName = productName)
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_AWAKE] = enabled }
    }

    suspend fun setRecordLocation(enabled: Boolean) {
        dataStore.edit {
            it[RECORD_LOCATION] = enabled
            if (!enabled) {
                it[WRITE_LOCATION_TO_MEDIA_FILE] = false
            }
        }
    }

    suspend fun setWriteLocationToMediaFile(enabled: Boolean) {
        dataStore.edit {
            it[WRITE_LOCATION_TO_MEDIA_FILE] = enabled && it[RECORD_LOCATION] == true
        }
    }

    suspend fun setRecycleRetentionDays(days: Int) {
        dataStore.edit { it[RECYCLE_RETENTION_DAYS] = days }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { it[PLAYBACK_SPEED] = speed }
    }

    suspend fun setRewindSeconds(seconds: Int) {
        dataStore.edit { it[REWIND_SECONDS] = seconds.coerceIn(MIN_SKIP_SECONDS, MAX_SKIP_SECONDS) }
    }

    suspend fun setForwardSeconds(seconds: Int) {
        dataStore.edit { it[FORWARD_SECONDS] = seconds.coerceIn(MIN_SKIP_SECONDS, MAX_SKIP_SECONDS) }
    }

    suspend fun setLocale(tag: String) {
        dataStore.edit { it[LOCALE] = tag }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val RECORDING_FORMAT = stringPreferencesKey("recording_format")
        val BITRATE = intPreferencesKey("bitrate")
        val SAMPLE_RATE = intPreferencesKey("sample_rate")
        val PREFERRED_AUDIO_INPUT_ID = intPreferencesKey("preferred_audio_input_id")
        val PREFERRED_AUDIO_INPUT_TYPE = intPreferencesKey("preferred_audio_input_type")
        val PREFERRED_AUDIO_INPUT_NAME = stringPreferencesKey("preferred_audio_input_name")
        val RECORDING_NAME_TEMPLATE = stringPreferencesKey("recording_name_template")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val RECORD_LOCATION = booleanPreferencesKey("record_location")
        val WRITE_LOCATION_TO_MEDIA_FILE = booleanPreferencesKey("write_location_to_media_file")
        val RECYCLE_RETENTION_DAYS = intPreferencesKey("recycle_retention_days")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val REWIND_SECONDS = intPreferencesKey("rewind_seconds")
        val FORWARD_SECONDS = intPreferencesKey("forward_seconds")
        val LOCALE = stringPreferencesKey("locale")
        const val MIN_SKIP_SECONDS = 1
        const val MAX_SKIP_SECONDS = 60
        const val OLD_DEFAULT_RECORDING_NAME_TEMPLATE = "SoundMemo_{timestamp}"
    }
}
