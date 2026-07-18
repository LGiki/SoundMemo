package net.lgiki.soundmemo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import net.lgiki.soundmemo.data.model.Recording
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE isDeleted = 0 ORDER BY createdAt DESC, id ASC")
    fun activeRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun deletedRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    suspend fun deletedRecordingsOnce(): List<Recording>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Recording?

    @Query(
        "SELECT COUNT(*) FROM recordings WHERE storageType = :storageType " +
            "AND ((:storageUri IS NOT NULL AND storageUri = :storageUri) " +
            "OR (:storageUri IS NULL AND filePath = :filePath))",
    )
    suspend fun countByStorageLocation(storageType: String, storageUri: String?, filePath: String): Int

    @Insert
    suspend fun insert(recording: Recording): Long

    @Insert
    suspend fun insertAll(recordings: List<Recording>): List<Long>

    @Update
    suspend fun update(recording: Recording)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM recordings WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()

    @Query("SELECT * FROM recordings WHERE isDeleted = 1 AND deletedAt <= :cutoff")
    suspend fun deletedBefore(cutoff: Long): List<Recording>
}
