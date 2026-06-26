package net.lgiki.soundmemo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.lgiki.soundmemo.data.model.Recording

@Database(entities = [Recording::class], version = 3, exportSchema = true)
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
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recordings ADD COLUMN locationLatitude REAL")
                db.execSQL("ALTER TABLE recordings ADD COLUMN locationLongitude REAL")
                db.execSQL("ALTER TABLE recordings ADD COLUMN locationAccuracyMeters REAL")
                db.execSQL("ALTER TABLE recordings ADD COLUMN locationCapturedAt INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recordings ADD COLUMN storageType TEXT NOT NULL DEFAULT 'file'")
                db.execSQL("ALTER TABLE recordings ADD COLUMN storageUri TEXT")
            }
        }
    }
}
