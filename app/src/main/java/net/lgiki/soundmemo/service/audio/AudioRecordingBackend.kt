package net.lgiki.soundmemo.service.audio

internal interface AudioRecordingBackend {
    val maxAmplitude: Int

    fun start()
    fun pause()
    fun resume()
    fun stop()
    fun release()
}
