package net.lgiki.soundmemo.service.audio

import net.lgiki.soundmemo.domain.recorder.AudioInputRoute

internal interface AudioRecordingBackend {
    val maxAmplitude: Int
    val routedDevice: AudioInputRoute?

    fun start()
    fun pause()
    fun resume()
    fun stop()
    fun release()
}
