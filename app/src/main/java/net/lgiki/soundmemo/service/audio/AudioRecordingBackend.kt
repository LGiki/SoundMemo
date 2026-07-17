package net.lgiki.soundmemo.service.audio

import java.io.File
import net.lgiki.soundmemo.domain.recorder.AudioInputRoute

internal data class RecordedOutput(
    val file: File,
    val partIndex: Int = 1,
    val durationMs: Long? = null,
)

internal interface AudioRecordingBackend {
    val maxAmplitude: Int
    val routedDevice: AudioInputRoute?
    val failure: Throwable?

    fun start()
    fun pause()
    fun resume()
    fun stop(): List<RecordedOutput>
    fun release()
}
