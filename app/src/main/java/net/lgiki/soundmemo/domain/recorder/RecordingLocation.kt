package net.lgiki.soundmemo.domain.recorder

data class RecordingLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAt: Long,
)
