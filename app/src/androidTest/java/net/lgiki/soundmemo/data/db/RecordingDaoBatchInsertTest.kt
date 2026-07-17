package net.lgiki.soundmemo.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.lgiki.soundmemo.data.model.Recording
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingDaoBatchInsertTest {
    private lateinit var database: SoundMemoDatabase
    private lateinit var dao: RecordingDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoundMemoDatabase::class.java).build()
        dao = database.recordingDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAll_insertsEveryPartInOrder() = runBlocking {
        val ids = dao.insertAll(
            listOf(
                recording(id = 0, name = "Part 1"),
                recording(id = 0, name = "Part 2"),
            ),
        )

        assertEquals(2, ids.size)
        assertEquals(setOf("Part 1", "Part 2"), dao.activeRecordings().first().map { it.name }.toSet())
    }

    @Test
    fun insertAll_rollsBackWhenAnyPartConflicts() = runBlocking {
        runCatching {
            dao.insertAll(
                listOf(
                    recording(id = 7, name = "Part 1"),
                    recording(id = 7, name = "Part 2"),
                ),
            )
        }

        assertEquals(emptyList<Recording>(), dao.activeRecordings().first())
    }

    private fun recording(id: Long, name: String): Recording = Recording(
        id = id,
        name = name,
        filePath = "/tmp/$name.wav",
        durationMs = 1_000,
        fileSizeBytes = 2_048,
        format = "wav",
    )
}
