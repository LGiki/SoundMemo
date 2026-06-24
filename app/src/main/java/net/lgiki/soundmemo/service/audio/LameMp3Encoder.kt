package net.lgiki.soundmemo.service.audio

internal class LameMp3Encoder(
    sampleRate: Int,
    bitrate: Int,
    channelCount: Int,
) : AutoCloseable {
    private var handle: Long = nativeInit(sampleRate, bitrate / 1000, channelCount)

    init {
        check(handle != 0L) { "LAME encoder could not initialize" }
    }

    fun encode(samples: ShortArray, sampleCount: Int): ByteArray {
        val activeHandle = checkHandle()
        return nativeEncode(activeHandle, samples, sampleCount)
            ?: error("LAME encoder failed")
    }

    fun flush(): ByteArray {
        val activeHandle = checkHandle()
        return nativeFlush(activeHandle)
            ?: error("LAME encoder flush failed")
    }

    override fun close() {
        val activeHandle = handle
        if (activeHandle != 0L) {
            nativeClose(activeHandle)
            handle = 0L
        }
    }

    private fun checkHandle(): Long {
        val activeHandle = handle
        check(activeHandle != 0L) { "LAME encoder is closed" }
        return activeHandle
    }

    private external fun nativeInit(sampleRate: Int, bitrateKbps: Int, channelCount: Int): Long
    private external fun nativeEncode(handle: Long, samples: ShortArray, sampleCount: Int): ByteArray?
    private external fun nativeFlush(handle: Long): ByteArray?
    private external fun nativeClose(handle: Long)

    private companion object {
        init {
            System.loadLibrary("soundmemo_lame")
        }
    }
}
