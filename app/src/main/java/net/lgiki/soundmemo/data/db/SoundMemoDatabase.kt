package net.lgiki.soundmemo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.lgiki.soundmemo.data.model.Recording

@Database(entities = [Recording::class], version = 1, exportSchema = false)
abstract class SoundMemoDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile private var instance: SoundMemoDatabase? = null

        fun get(context: Context): SoundMemoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoundMemoDatabase::class.java,
                    "soundmemo.db",
                ).build().also { instance = it }
            }
    }
}
