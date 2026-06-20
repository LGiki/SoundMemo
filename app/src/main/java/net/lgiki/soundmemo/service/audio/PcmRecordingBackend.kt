package net.lgiki.soundmemo.service.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.max
import net.lgiki.soundmemo.domain.recorder.RecordingFormat

internal class PcmRecordingBackend(
    private val file: File,
    private val format: RecordingFormat,
    private val bitrate: Int,
    private val sampleRate: Int,
) : AudioRecordingBackend {
    private val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )
    private val bufferSize = max(minBufferSize.coerceAtLeast(0) / WavHeader.BYTES_PER_SAMPLE, sampleRate / 5)
    private val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize * WavHeader.BYTES_PER_SAMPLE,
    )
    private val amplitude = AtomicInteger(0)
    private val failure = AtomicReference<Throwable?>(null)
    @Volatile private var running = false
    @Volatile private var paused = false
    private var writer: PcmAudioWriter? = null
    private var worker: Thread? = null

    override val maxAmplitude: Int
        get() = amplitude.getAndSet(0)

    override fun start() {
        check(audioRecord.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord could not initialize" }
        writer = when (format) {
            RecordingFormat.Wav -> WavAudioWriter(file, sampleRate)
            RecordingFormat.Mp3 -> Mp3AudioWriter(file, sampleRate, bitrate)
            else -> error("PCM recorder does not support $format")
        }
        running = true
        audioRecord.startRecording()
        worker = Thread(::recordLoop, "SoundMemoPcmRecorder").apply { start() }
    }

    override fun pause() {
        paused = true
        runCatching { audioRecord.stop() }
        amplitude.set(0)
    }

    override fun resume() {
        audioRecord.startRecording()
        paused = false
    }

    override fun stop() {
        running = false
        runCatching { audioRecord.stop() }
        worker?.join()
        val activeWriter = writer
        writer = null
        val closeFailure = runCatching { activeWriter?.close() }.exceptionOrNull()
        val recordingFailure = failure.get()
        release()
        recordingFailure?.let { throw it }
        closeFailure?.let { throw it }
    }

    override fun release() {
        running = false
        runCatching { audioRecord.release() }
        runCatching { writer?.close() }
        writer = null
    }

    private fun recordLoop() {
        val samples = ShortArray(bufferSize)
        try {
            while (running) {
                if (paused) {
                    Thread.sleep(PAUSED_READ_SLEEP_MS)
                    continue
                }
                val read = audioRecord.read(samples, 0, samples.size)
                if (read <= 0) continue
                if (paused) {
                    amplitude.set(0)
                    continue
                }
                var maxSample = 0
                for (index in 0 until read) {
                    maxSample = max(maxSample, abs(samples[index].toInt()))
                }
                amplitude.updateAndGet { current -> max(current, maxSample) }
                if (!paused) {
                    writer?.write(samples, read)
                }
            }
        } catch (throwable: Throwable) {
            failure.compareAndSet(null, throwable)
            running = false
        }
    }

    private interface PcmAudioWriter : AutoCloseable {
        fun write(samples: ShortArray, sampleCount: Int)
    }

    private class WavAudioWriter(
        file: File,
        private val sampleRate: Int,
    ) : PcmAudioWriter {
        private val output = RandomAccessFile(file, "rw")
        private var dataBytes = 0L

        init {
            output.setLength(0)
            WavHeader.write(output, sampleRate, dataBytes = 0)
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
            WavHeader.write(output, sampleRate, dataBytes)
            output.close()
        }
    }

    private class Mp3AudioWriter(
        file: File,
        sampleRate: Int,
        bitrate: Int,
    ) : PcmAudioWriter {
        private val output = FileOutputStream(file)
        private val encoder = LameMp3Encoder(sampleRate = sampleRate, bitrate = bitrate)

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
        private const val PAUSED_READ_SLEEP_MS = 50L
    }
}
