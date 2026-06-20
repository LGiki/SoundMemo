package net.lgiki.soundmemo.service.audio

import java.io.RandomAccessFile

internal object WavHeader {
    const val CHANNELS = 1
    const val BITS_PER_SAMPLE = 16
    const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8

    fun write(output: RandomAccessFile, sampleRate: Int, dataBytes: Long) {
        output.writeBytes("RIFF")
        output.writeIntLe((36L + dataBytes).coerceAtMost(MAX_UNSIGNED_INT).toInt())
        output.writeBytes("WAVE")
        output.writeBytes("fmt ")
        output.writeIntLe(16)
        output.writeShortLe(1)
        output.writeShortLe(CHANNELS)
        output.writeIntLe(sampleRate)
        output.writeIntLe(sampleRate * CHANNELS * BYTES_PER_SAMPLE)
        output.writeShortLe(CHANNELS * BYTES_PER_SAMPLE)
        output.writeShortLe(BITS_PER_SAMPLE)
        output.writeBytes("data")
        output.writeIntLe(dataBytes.coerceAtMost(MAX_UNSIGNED_INT).toInt())
    }

    private const val MAX_UNSIGNED_INT = 0xffff_ffffL

    private fun RandomAccessFile.writeIntLe(value: Int) {
        write(value and 0xff)
        write((value ushr 8) and 0xff)
        write((value ushr 16) and 0xff)
        write((value ushr 24) and 0xff)
    }

    private fun RandomAccessFile.writeShortLe(value: Int) {
        write(value and 0xff)
        write((value ushr 8) and 0xff)
    }
}
