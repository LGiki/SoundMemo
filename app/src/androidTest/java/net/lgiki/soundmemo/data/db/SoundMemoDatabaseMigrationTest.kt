package net.lgiki.soundmemo.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SoundMemoDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SoundMemoDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migration1To2_preservesRecordingAndAddsLocationColumns() {
        helper.createDatabase(TEST_DB, 1).apply {
            insertVersion1Recording()
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            SoundMemoDatabase.MIGRATION_1_2,
        )

        db.query("SELECT name, locationLatitude, locationLongitude, locationAccuracyMeters, locationCapturedAt FROM recordings WHERE id = 1")
            .use { cursor ->
                cursor.moveToFirst()
                assertEquals("Legacy recording", cursor.getString(0))
                assertNull(cursor.getDoubleOrNull(1))
                assertNull(cursor.getDoubleOrNull(2))
                assertNull(cursor.getFloatOrNull(3))
                assertNull(cursor.getLongOrNull(4))
            }
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration1To3_preservesRecordingAndAddsStorageColumns() {
        helper.createDatabase(TEST_DB, 1).apply {
            insertVersion1Recording()
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            SoundMemoDatabase.MIGRATION_1_2,
            SoundMemoDatabase.MIGRATION_2_3,
        )

        db.query("SELECT name, storageType, storageUri FROM recordings WHERE id = 1")
            .use { cursor ->
                cursor.moveToFirst()
                assertEquals("Legacy recording", cursor.getString(0))
                assertEquals("file", cursor.getString(1))
                assertNull(cursor.getStringOrNull(2))
            }
        db.close()
    }

    private fun SupportSQLiteDatabase.insertVersion1Recording() {
        execSQL(
            """
            INSERT INTO recordings (
                id,
                name,
                filePath,
                durationMs,
                fileSizeBytes,
                format,
                bitrate,
                sampleRate,
                createdAt,
                updatedAt,
                isDeleted,
                deletedAt,
                note
            ) VALUES (
                1,
                'Legacy recording',
                '/tmp/legacy.m4a',
                1000,
                2048,
                'm4a',
                128000,
                44100,
                10,
                10,
                0,
                NULL,
                ''
            )
            """.trimIndent(),
        )
    }

    private fun android.database.Cursor.getDoubleOrNull(index: Int): Double? =
        if (isNull(index)) null else getDouble(index)

    private fun android.database.Cursor.getFloatOrNull(index: Int): Float? =
        if (isNull(index)) null else getFloat(index)

    private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
        if (isNull(index)) null else getLong(index)

    private fun android.database.Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private companion object {
        const val TEST_DB = "soundmemo-migration-test"
    }
}
