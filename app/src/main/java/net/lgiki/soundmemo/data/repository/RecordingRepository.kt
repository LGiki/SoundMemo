package net.lgiki.soundmemo.data.repository

import net.lgiki.soundmemo.data.db.RecordingDao
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.domain.recorder.RecordingLocation
import java.io.File
import kotlinx.coroutines.flow.Flow

class RecordingRepository(private val dao: RecordingDao) {
    val activeRecordings: Flow<List<Recording>> = dao.activeRecordings()
    val deletedRecordings: Flow<List<Recording>> = dao.deletedRecordings()

    suspend fun addFromFile(
        file: File,
        durationMs: Long,
        bitrate: Int,
        sampleRate: Int,
        location: RecordingLocation?,
    ): Long = dao.insert(
        Recording(
            name = file.nameWithoutExtension.replace('_', ' '),
            filePath = file.absolutePath,
            durationMs = durationMs,
            fileSizeBytes = file.length(),
            bitrate = bitrate,
            sampleRate = sampleRate,
            locationLatitude = location?.latitude,
            locationLongitude = location?.longitude,
            locationAccuracyMeters = location?.accuracyMeters,
            locationCapturedAt = location?.capturedAt,
        ),
    )

    suspend fun get(id: Long): Recording? = dao.getById(id)

    suspend fun rename(id: Long, name: String) {
        dao.getById(id)?.let {
            dao.update(it.copy(name = name.trim().ifBlank { it.name }, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun moveToRecycleBin(id: Long) {
        dao.getById(id)?.let {
            dao.update(it.copy(isDeleted = true, deletedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun restore(id: Long) {
        dao.getById(id)?.let {
            dao.update(it.copy(isDeleted = false, deletedAt = null, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deletePermanently(id: Long, deleteFile: (String) -> Unit) {
        dao.getById(id)?.let {
            deleteFile(it.filePath)
            dao.deletePermanently(id)
        }
    }

    suspend fun emptyRecycleBin(deleteFile: (String) -> Unit) {
        dao.deletedRecordingsOnce().forEach { deleteFile(it.filePath) }
        dao.emptyRecycleBin()
    }

    suspend fun purgeExpired(retentionDays: Int, deleteFile: (String) -> Unit) {
        val cutoff = System.currentTimeMillis() - retentionDays.coerceAtLeast(1) * 24L * 60L * 60L * 1000L
        dao.deletedBefore(cutoff).forEach { recording ->
            deleteFile(recording.filePath)
            dao.deletePermanently(recording.id)
        }
    }
}
