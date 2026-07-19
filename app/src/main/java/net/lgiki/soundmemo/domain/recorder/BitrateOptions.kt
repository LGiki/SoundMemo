package net.lgiki.soundmemo.domain.recorder

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log

private const val TAG = "AacBitrateOptions"

data class BitrateRange(
    val min: Int,
    val max: Int,
) {
    fun contains(value: Int): Boolean = value in min..max
}

data class BitrateOptions(
    val values: List<Int>,
    val ranges: List<BitrateRange>?,
) {
    val isDeviceReported: Boolean = ranges != null
}

object AacBitrateOptions {
    val fallbackValues = listOf(96_000, 128_000, 192_000, 320_000)

    private val commonValues = listOf(64_000, 96_000, 128_000, 160_000, 192_000, 256_000, 320_000)

    fun load(): BitrateOptions {
        val ranges = readDeviceAacBitrateRanges()
        return BitrateOptions(
            values = valuesForRanges(ranges),
            ranges = ranges,
        )
    }

    fun valuesForRanges(ranges: List<BitrateRange>?): List<Int> {
        if (ranges.isNullOrEmpty()) return fallbackValues
        return commonValues.filter { value -> ranges.any { it.contains(value) } }.ifEmpty {
            ranges.flatMap { range -> listOf(range.min, range.max) }.distinct().sorted()
        }
    }

    fun closestSupported(value: Int, options: List<Int>): Int {
        return options.minByOrNull { kotlin.math.abs(it - value) } ?: fallbackValues.first()
    }

    private fun readDeviceAacBitrateRanges(): List<BitrateRange>? {
        return try {
            val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            codecs
                .asSequence()
                .filter(MediaCodecInfo::isEncoder)
                .filter { codec -> codec.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true) } }
                .mapNotNull { codec -> codec.getCapabilitiesForType(MediaFormat.MIMETYPE_AUDIO_AAC).audioCapabilities?.bitrateRange }
                .map { BitrateRange(min = it.lower, max = it.upper) }
                .distinct()
                .toList()
                .takeIf { it.isNotEmpty() }
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to query AAC bitrate range", exception)
            null
        }
    }
}
