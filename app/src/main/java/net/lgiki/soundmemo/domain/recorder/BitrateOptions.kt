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
    val range: BitrateRange?,
) {
    val isDeviceReported: Boolean = range != null
}

object AacBitrateOptions {
    val fallbackValues = listOf(96_000, 128_000, 192_000, 320_000)

    private val commonValues = listOf(64_000, 96_000, 128_000, 160_000, 192_000, 256_000, 320_000)

    fun load(): BitrateOptions {
        val range = readDeviceAacBitrateRange()
        return BitrateOptions(
            values = valuesForRange(range),
            range = range,
        )
    }

    fun valuesForRange(range: BitrateRange?): List<Int> {
        if (range == null) return fallbackValues
        return commonValues.filter(range::contains).ifEmpty {
            listOf(range.min, range.max).distinct()
        }
    }

    fun closestSupported(value: Int, options: List<Int>): Int {
        return options.minByOrNull { kotlin.math.abs(it - value) } ?: fallbackValues.first()
    }

    private fun readDeviceAacBitrateRange(): BitrateRange? {
        return try {
            val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            codecs
                .asSequence()
                .filter(MediaCodecInfo::isEncoder)
                .filter { codec -> codec.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true) } }
                .mapNotNull { codec -> codec.getCapabilitiesForType(MediaFormat.MIMETYPE_AUDIO_AAC).audioCapabilities?.bitrateRange }
                .map { BitrateRange(min = it.lower, max = it.upper) }
                .reduceOrNull { acc, range ->
                    BitrateRange(
                        min = minOf(acc.min, range.min),
                        max = maxOf(acc.max, range.max),
                    )
                }
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to query AAC bitrate range", exception)
            null
        }
    }
}
