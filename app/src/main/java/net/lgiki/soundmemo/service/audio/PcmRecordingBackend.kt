package net.lgiki.soundmemo.service.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioDeviceInfo
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import net.lgiki.soundmemo.domain.recorder.AudioInputRoute
import net.lgiki.soundmemo.domain.recorder.RecordingFormat

internal class PcmRecordingBackend(
    private val context: Context,
    private val file: File,
    private val format: RecordingFormat,
    private val bitrate: Int,
    private val sampleRate: Int,
    private val channels: RecordingChannels,
    private val preferredDevice: AudioDeviceInfo?,
) : AudioRecordingBackend {
    private val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channels.inputChannelMask,
        AudioFormat.ENCODING_PCM_16BIT,
    )
    private val bufferSize = max(
        minBufferSize.coerceAtLeast(0) / WavHeader.BYTES_PER_SAMPLE,
        sampleRate * channels.channelCount / 5,
    )
    private var audioRecord: AudioRecord? = null
    private val amplitude = AtomicInteger(0)
    @Volatile private var released = false
    private var writer: PcmAudioWriter? = null
    private var captureWorker: PcmCaptureWorker? = null

    override val maxAmplitude: Int
        get() = amplitude.getAndSet(0)

    override val routedDevice: AudioInputRoute?
        get() = if (!released) {
            runCatching { audioRecord?.routedDevice?.toAudioInputRoute() }.getOrNull()
        } else {
            null
        }

    override val failure: Throwable?
        get() = captureWorker?.failure

    override fun start() {
        val activeAudioRecord = createAudioRecord().also { audioRecord = it }
        check(activeAudioRecord.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord could not initialize" }
        preferredDevice?.let { activeAudioRecord.setPreferredDevice(it) }
        writer = when (format) {
            RecordingFormat.Wav -> WavAudioWriter(file, sampleRate, channels.channelCount)
            RecordingFormat.Mp3 -> Mp3AudioWriter(file, sampleRate, bitrate, channels.channelCount)
            else -> error("PCM recorder does not support $format")
        }
        activeAudioRecord.startRecording()
        captureWorker = PcmCaptureWorker(
            bufferSize = bufferSize,
            read = { samples -> audioRecord?.read(samples, 0, samples.size) ?: ERROR_STOPPED },
            write = { samples, sampleCount -> writer?.write(samples, sampleCount) },
            closeOutput = {
                val activeWriter = writer
                writer = null
                activeWriter?.close()
            },
            onAmplitude = { measured -> amplitude.updateAndGet { current -> max(current, measured) } },
        ).also { it.start() }
    }

    override fun pause() {
        captureWorker?.pause()
        runCatching { audioRecord?.stop() }
        amplitude.set(0)
    }

    override fun resume() {
        audioRecord?.startRecording()
        captureWorker?.resume()
    }

    override fun stop() {
        captureWorker?.requestStop()
        runCatching { audioRecord?.stop() }
        val activeWorker = captureWorker
        val closeFailure = runCatching { activeWorker?.stopAndJoin() }.exceptionOrNull()
        val failure = activeWorker?.failure
        failure?.let { throw it }
        closeFailure?.let { throw it }
    }

    override fun release() {
        if (released) return
        released = true
        val activeWorker = captureWorker
        val activeAudioRecord = audioRecord
        activeWorker?.requestStop()
        runCatching { activeAudioRecord?.stop() }
        val workerStopped = activeWorker?.let { worker ->
            runCatching { worker.stopAndJoin(WORKER_JOIN_TIMEOUT_MS) }
                .getOrElse { exception ->
                    if (exception is InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                    false
                }
        } ?: true
        if (!workerStopped) {
            Thread(
                {
                    runCatching { activeWorker.stopAndJoin() }
                    releaseAudioRecord(activeAudioRecord)
                },
                "SoundMemoPcmRelease",
            ).start()
        } else {
            releaseAudioRecord(activeAudioRecord)
        }
    }

    private fun releaseAudioRecord(activeAudioRecord: AudioRecord?) {
        runCatching { activeAudioRecord?.release() }
        audioRecord = null
        captureWorker = null
    }

    private fun createAudioRecord(): AudioRecord {
        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        ) { "Microphone permission is required" }
        return AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channels.inputChannelMask,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * WavHeader.BYTES_PER_SAMPLE,
        )
    }

    private interface PcmAudioWriter : AutoCloseable {
        fun write(samples: ShortArray, sampleCount: Int)
    }

    private class WavAudioWriter(
        file: File,
        private val sampleRate: Int,
        private val channelCount: Int,
    ) : PcmAudioWriter {
        private val output = RandomAccessFile(file, "rw")
        private var dataBytes = 0L

        init {
            output.setLength(0)
            WavHeader.write(output, sampleRate, channelCount, dataBytes = 0)
        }

        override fun write(samples: ShortArray, sampleCount: Int) {
            for (index in 0 until sampleCount) {
                val value = samples[index].toInt()
                output.write(value and 0xff)
                output.write((value ushr 8) and 0xff)
            }
            dataBytes += sampleCount * WavHeader.BYTES_PER_SAMPLE.toLong()
        }

        override fun close() {
            output.seek(0)
            WavHeader.write(output, sampleRate, channelCount, dataBytes)
            output.close()
        }
    }

    private class Mp3AudioWriter(
        file: File,
        sampleRate: Int,
        bitrate: Int,
        channelCount: Int,
    ) : PcmAudioWriter {
        private val output = FileOutputStream(file)
        private val encoder = LameMp3Encoder(sampleRate = sampleRate, bitrate = bitrate, channelCount = channelCount)

        override fun write(samples: ShortArray, sampleCount: Int) {
            val bytes = encoder.encode(samples, sampleCount)
            if (bytes.isNotEmpty()) {
                output.write(bytes)
            }
        }

        override fun close() {
            runCatching {
                val bytes = encoder.flush()
                if (bytes.isNotEmpty()) {
                    output.write(bytes)
                }
            }.also {
                encoder.close()
                output.close()
            }.getOrThrow()
        }
    }

    private companion object {
        private const val WORKER_JOIN_TIMEOUT_MS = 2_000L
        private const val ERROR_STOPPED = -1
    }
}
