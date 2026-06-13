package net.lgiki.soundmemo

import android.content.Context
import net.lgiki.soundmemo.data.db.SoundMemoDatabase
import net.lgiki.soundmemo.data.repository.RecordingRepository
import net.lgiki.soundmemo.data.settings.SettingsRepository
import net.lgiki.soundmemo.data.storage.RecordingStorage

class SoundMemoContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val database = SoundMemoDatabase.get(appContext)

    val recordingRepository = RecordingRepository(database.recordingDao())
    val settingsRepository = SettingsRepository(appContext)
    val recordingStorage = RecordingStorage(appContext)
}
