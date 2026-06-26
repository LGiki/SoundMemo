package net.lgiki.soundmemo.util

import net.lgiki.soundmemo.data.model.Recording
import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

fun formatPreciseDuration(ms: Long): String {
    val positiveMs = ms.coerceAtLeast(0)
    val totalSeconds = positiveMs / 1000
    val milliseconds = positiveMs % 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d.%03d", minutes, seconds, milliseconds)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    return String.format(Locale.getDefault(), "%.1f MB", kb / 1024.0)
}

fun formatDateTime(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))

fun formatCoordinates(latitude: Double, longitude: Double): String? {
    if (!latitude.isFinite() || !longitude.isFinite()) return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
}

fun formatRecordingLocation(recording: Recording): String? {
    val latitude = recording.locationLatitude ?: return null
    val longitude = recording.locationLongitude ?: return null
    return formatCoordinates(latitude, longitude)
}
