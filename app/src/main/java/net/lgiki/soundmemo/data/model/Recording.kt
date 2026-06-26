package net.lgiki.soundmemo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val format: String = "m4a",
    val storageType: String = RecordingStorageType.File.storageValue,
    val storageUri: String? = null,
    val bitrate: Int = 128_000,
    val sampleRate: Int = 44_100,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val note: String = "",
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val locationCapturedAt: Long? = null,
)

enum class RecordingSort {
    Newest,
    Oldest,
    Longest,
    Shortest,
    Name,
}

enum class RecordingStorageType(val storageValue: String) {
    File("file"),
    MediaStore("media_store"),
    ContentUri("content_uri");

    companion object {
        fun fromStorageValue(value: String?): RecordingStorageType =
            entries.firstOrNull { it.storageValue == value } ?: File
    }
}
