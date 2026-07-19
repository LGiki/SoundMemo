package net.lgiki.soundmemo.data.repository

import net.lgiki.soundmemo.data.db.RecordingDao
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.data.storage.RecordingSaveResult
import net.lgiki.soundmemo.domain.recorder.RecordingLocation
import java.io.File
import kotlinx.coroutines.flow.Flow

class RecordingRepository(private val dao: RecordingDao) {
    val activeRecordings: Flow<List<Recording>> = dao.activeRecordings()
    val deletedRecordings: Flow<List<Recording>> = dao.deletedRecordings()

    suspend fun addFromFile(
        file: File,
        name: String = file.toRecordingName(),
        durationMs: Long,
        bitrate: Int,
        sampleRate: Int,
        format: String = file.extension.lowercase(),
        location: RecordingLocation?,
    ): Long = dao.insert(
        Recording(
            name = name.trim().ifBlank { file.toRecordingName() },
            filePath = file.absolutePath,
            durationMs = durationMs,
            fileSizeBytes = file.length(),
            format = format,
            bitrate = bitrate,
            sampleRate = sampleRate,
            locationLatitude = location?.latitude,
            locationLongitude = location?.longitude,
            locationAccuracyMeters = location?.accuracyMeters,
            locationCapturedAt = location?.capturedAt,
        ),
    )

    suspend fun addFromSaveResult(
        saveResult: RecordingSaveResult,
        name: String,
        durationMs: Long,
        bitrate: Int,
        sampleRate: Int,
        format: String,
        location: RecordingLocation?,
    ): Long = addFromSaveResults(
        parts = listOf(
            RecordingPartSave(
                saveResult = saveResult,
                name = name,
                durationMs = durationMs,
            ),
        ),
        bitrate = bitrate,
        sampleRate = sampleRate,
        format = format,
        location = location,
    ).single()

    suspend fun addFromSaveResults(
        parts: List<RecordingPartSave>,
        bitrate: Int,
        sampleRate: Int,
        format: String,
        location: RecordingLocation?,
    ): List<Long> {
        require(parts.isNotEmpty()) { "At least one recording part is required" }
        val createdAt = System.currentTimeMillis()
        return dao.insertAll(
            parts.map { part ->
                val saveResult = part.saveResult
                Recording(
                    name = part.name.trim().ifBlank { File(saveResult.filePath).toRecordingName() },
                    filePath = saveResult.filePath,
                    fileSizeBytes = saveResult.fileSizeBytes,
                    storageType = saveResult.storageType,
                    storageUri = saveResult.storageUri,
                    durationMs = part.durationMs,
                    format = format,
                    bitrate = bitrate,
                    sampleRate = sampleRate,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    locationLatitude = location?.latitude,
                    locationLongitude = location?.longitude,
                    locationAccuracyMeters = location?.accuracyMeters,
                    locationCapturedAt = location?.capturedAt,
                )
            },
        )
    }

    suspend fun get(id: Long): Recording? = dao.getById(id)

    suspend fun hasSaveResult(saveResult: RecordingSaveResult): Boolean =
        dao.countByStorageLocation(
            storageType = saveResult.storageType,
            storageUri = saveResult.storageUri,
            filePath = saveResult.filePath,
        ) > 0

    suspend fun rename(id: Long, name: String) {
        val normalized = name.trim()
        if (normalized.isNotBlank()) {
            dao.rename(id = id, name = normalized, updatedAt = System.currentTimeMillis())
        }
    }

    suspend fun moveToRecycleBin(id: Long) {
        val now = System.currentTimeMillis()
        dao.moveToRecycleBin(id = id, deletedAt = now, updatedAt = now)
    }

    suspend fun restore(id: Long) {
        dao.restore(id = id, updatedAt = System.currentTimeMillis())
    }

    suspend fun deletePermanently(id: Long, deleteRecording: (Recording) -> Boolean) {
        dao.getById(id)?.let {
            if (deleteRecording(it)) {
                dao.deletePermanently(id)
            }
        }
    }

    suspend fun emptyRecycleBin(deleteRecording: (Recording) -> Boolean) {
        dao.deletedRecordingsOnce().forEach { recording ->
            if (deleteRecording(recording)) {
                dao.deletePermanently(recording.id)
            }
        }
    }

    suspend fun purgeExpired(retentionDays: Int, deleteRecording: (Recording) -> Boolean) {
        val cutoff = System.currentTimeMillis() - retentionDays.coerceAtLeast(1) * 24L * 60L * 60L * 1000L
        dao.deletedBefore(cutoff).forEach { recording ->
            if (deleteRecording(recording)) {
                dao.deletePermanently(recording.id)
            }
        }
    }
}

data class RecordingPartSave(
    val saveResult: RecordingSaveResult,
    val name: String,
    val durationMs: Long,
)

private fun File.toRecordingName(): String {
    val name = nameWithoutExtension
    val displayName = if (name.matches(GENERATED_RECORDING_NAME)) {
        name.substringBeforeLast('_')
    } else {
        name
    }
    return displayName.replace('_', ' ')
}

private val GENERATED_RECORDING_NAME = Regex("""SoundMemo_\d{8}_\d{6}_\d{3}_[A-Za-z0-9-]+""")
