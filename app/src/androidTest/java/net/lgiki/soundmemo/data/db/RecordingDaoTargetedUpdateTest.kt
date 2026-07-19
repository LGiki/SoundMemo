package net.lgiki.soundmemo.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.runBlocking
import net.lgiki.soundmemo.data.model.Recording
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingDaoTargetedUpdateTest {
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
    fun targetedUpdates_preserveUnrelatedMetadata() = runBlocking {
        val id = dao.insert(recording())

        dao.rename(id, name = "Renamed", updatedAt = 2L)
        dao.moveToRecycleBin(id, deletedAt = 3L, updatedAt = 3L)
        val deleted = requireNotNull(dao.getById(id))
        assertEquals("Renamed", deleted.name)
        assertEquals("Keep this note", deleted.note)
        assertEquals("content://recordings/1", deleted.storageUri)
        assertTrue(deleted.isDeleted)
        assertEquals(3L, deleted.deletedAt)

        dao.restore(id, updatedAt = 4L)
        val restored = requireNotNull(dao.getById(id))
        assertFalse(restored.isDeleted)
        assertNull(restored.deletedAt)
        assertEquals("Keep this note", restored.note)
        assertEquals("content://recordings/1", restored.storageUri)
    }

    private fun recording() = Recording(
        name = "Original",
        filePath = "/display/recording.m4a",
        durationMs = 1_000L,
        fileSizeBytes = 2_048L,
        storageType = "content_uri",
        storageUri = "content://recordings/1",
        note = "Keep this note",
    )
}
