package net.lgiki.soundmemo.service.audio

import java.io.RandomAccessFile

internal object WavHeader {
    const val BITS_PER_SAMPLE = 16
    const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8

    fun write(output: RandomAccessFile, sampleRate: Int, channelCount: Int, dataBytes: Long) {
        require(dataBytes in 0..maxPcmDataBytes(channelCount)) { "WAV data exceeds the RIFF size limit" }
        output.writeBytes("RIFF")
        output.writeIntLe((36L + dataBytes).toInt())
        output.writeBytes("WAVE")
        output.writeBytes("fmt ")
        output.writeIntLe(16)
        output.writeShortLe(1)
        output.writeShortLe(channelCount)
        output.writeIntLe(sampleRate)
        output.writeIntLe(sampleRate * channelCount * BYTES_PER_SAMPLE)
        output.writeShortLe(channelCount * BYTES_PER_SAMPLE)
        output.writeShortLe(BITS_PER_SAMPLE)
        output.writeBytes("data")
        output.writeIntLe(dataBytes.toInt())
    }

    fun maxPcmDataBytes(channelCount: Int): Long {
        val blockAlign = channelCount * BYTES_PER_SAMPLE
        require(blockAlign > 0) { "Channel count must be positive" }
        val unalignedMaximum = MAX_UNSIGNED_INT - 36L
        return unalignedMaximum - unalignedMaximum % blockAlign
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
