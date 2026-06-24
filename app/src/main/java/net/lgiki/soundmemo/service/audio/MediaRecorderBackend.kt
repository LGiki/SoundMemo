package net.lgiki.soundmemo.service.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import net.lgiki.soundmemo.domain.recorder.AudioInputRoute
import net.lgiki.soundmemo.domain.recorder.RecordingFormat
import net.lgiki.soundmemo.domain.recorder.RecordingLocation

internal class MediaRecorderBackend(
    private val context: Context,
    private val file: File,
    private val format: RecordingFormat,
    private val bitrate: Int,
    private val sampleRate: Int,
    private val channels: RecordingChannels,
    private val location: RecordingLocation?,
    private val writeLocationToMediaFile: Boolean,
    private val preferredDevice: AudioDeviceInfo?,
) : AudioRecordingBackend {
    @Volatile private var released = false

    private val recorder: MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    override val maxAmplitude: Int
        get() = runCatching { recorder.maxAmplitude }.getOrDefault(0)

    override val routedDevice: AudioInputRoute?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !released) {
            runCatching { recorder.routedDevice?.toAudioInputRoute() }.getOrNull()
        } else {
            null
        }

    override fun start() {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            preferredDevice?.let { recorder.setPreferredDevice(it) }
        }
        recorder.setRecordingFormat(format)
        recorder.setAudioEncodingBitRate(bitrate)
        recorder.setAudioSamplingRate(sampleRate)
        recorder.setAudioChannels(channels.channelCount)
        recorder.setOutputFile(file.absolutePath)
        location
            ?.takeIf { writeLocationToMediaFile && format.supportsLocationMetadata }
            ?.let { recorder.setLocation(it.latitude.toFloat(), it.longitude.toFloat()) }
        recorder.prepare()
        recorder.start()
    }

    override fun pause() {
        recorder.pause()
    }

    override fun resume() {
        recorder.resume()
    }

    override fun stop() {
        recorder.stop()
    }

    override fun release() {
        if (released) return
        released = true
        runCatching { recorder.release() }
    }

    private fun MediaRecorder.setRecordingFormat(format: RecordingFormat) {
        val (outputFormat, audioEncoder) = when (format) {
            RecordingFormat.M4a -> MediaRecorder.OutputFormat.MPEG_4 to MediaRecorder.AudioEncoder.AAC
            RecordingFormat.Aac -> MediaRecorder.OutputFormat.AAC_ADTS to MediaRecorder.AudioEncoder.AAC
            RecordingFormat.ThreeGp -> MediaRecorder.OutputFormat.THREE_GPP to MediaRecorder.AudioEncoder.AMR_WB
            // Defensive guard: RecordingService routes these formats to PcmRecordingBackend.
            RecordingFormat.Wav,
            RecordingFormat.Mp3 -> error("PCM formats are not supported by MediaRecorder")
        }
        setOutputFormat(outputFormat)
        setAudioEncoder(audioEncoder)
    }
}
