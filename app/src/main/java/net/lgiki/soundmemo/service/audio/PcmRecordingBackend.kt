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
    private val wavPartFile: (Int) -> File = { partIndex -> recordingPartFile(file, partIndex) },
    private val wavMaxDataBytes: Long = WavHeader.maxPcmDataBytes(channels.channelCount),
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
    private val writer = CloseOnceResource<PcmAudioWriter>()
    private var captureWorker: PcmCaptureWorker? = null
    @Volatile private var completedOutputs: List<RecordedOutput> = emptyList()

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
        try {
            check(activeAudioRecord.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord could not initialize" }
            preferredDevice?.let { activeAudioRecord.setPreferredDevice(it) }
            writer.set(
                when (format) {
                    RecordingFormat.Wav -> WavAudioWriter(
                        firstFile = file,
                        sampleRate = sampleRate,
                        channelCount = channels.channelCount,
                        partFile = wavPartFile,
                        maxDataBytes = wavMaxDataBytes,
                    )
                    RecordingFormat.Mp3 -> Mp3AudioWriter(file, sampleRate, bitrate, channels.channelCount)
                    else -> error("PCM recorder does not support $format")
                },
            )
            activeAudioRecord.startRecording()
            check(activeAudioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                "AudioRecord could not start recording"
            }
            captureWorker = PcmCaptureWorker(
                bufferSize = bufferSize,
                read = { samples -> audioRecord?.read(samples, 0, samples.size) ?: ERROR_STOPPED },
                write = { samples, sampleCount ->
                    writer.withResource { it.write(samples, sampleCount) }
                },
                closeOutput = ::finishWriter,
                onAmplitude = { measured -> amplitude.updateAndGet { current -> max(current, measured) } },
            ).also { it.start() }
        } catch (throwable: Throwable) {
            release()
            throw throwable
        }
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

    override fun stop(): List<RecordedOutput> {
        captureWorker?.requestStop()
        runCatching { audioRecord?.stop() }
        val activeWorker = captureWorker
        val closeFailure = runCatching { activeWorker?.stopAndJoin() }.exceptionOrNull()
        val failure = activeWorker?.failure
        failure?.let { throw it }
        closeFailure?.let { throw it }
        return completedOutputs
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
        if (activeWorker == null) {
            runCatching { writer.close() }
            releaseAudioRecord(activeAudioRecord)
        } else if (!workerStopped) {
            Thread(
                {
                    runCatching { activeWorker.stopAndJoin() }
                    runCatching { writer.close() }
                    releaseAudioRecord(activeAudioRecord)
                },
                "SoundMemoPcmRelease",
            ).start()
        } else {
            runCatching { writer.close() }
            releaseAudioRecord(activeAudioRecord)
        }
    }

    internal class CloseOnceResource<T : AutoCloseable> : AutoCloseable {
        private val lock = Any()
        private var resource: T? = null

        fun set(value: T) {
            synchronized(lock) {
                check(resource == null) { "Resource has already been set" }
                resource = value
            }
        }

        fun <R> withResource(block: (T) -> R): R? = synchronized(lock) {
            resource?.let(block)
        }

        override fun close() {
            val activeResource = synchronized(lock) {
                resource.also { resource = null }
            }
            activeResource?.close()
        }
    }

    private fun releaseAudioRecord(activeAudioRecord: AudioRecord?) {
        runCatching { activeAudioRecord?.release() }
        audioRecord = null
        captureWorker = null
    }

    private fun finishWriter() {
        writer.withResource { activeWriter ->
            completedOutputs = activeWriter.finish()
        }
        writer.close()
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

    internal interface PcmAudioWriter : AutoCloseable {
        fun write(samples: ShortArray, sampleCount: Int)
        fun finish(): List<RecordedOutput>
    }

    internal class WavAudioWriter(
        firstFile: File,
        private val sampleRate: Int,
        private val channelCount: Int,
        private val partFile: (Int) -> File,
        maxDataBytes: Long,
    ) : PcmAudioWriter {
        private val blockAlign = channelCount * WavHeader.BYTES_PER_SAMPLE
        private val maxDataBytes = maxDataBytes
            .coerceAtMost(WavHeader.maxPcmDataBytes(channelCount))
            .let { it - it % blockAlign }
        private val completed = mutableListOf<RecordedOutput>()
        private val pendingFrame = ShortArray(channelCount)
        private var pendingSampleCount = 0
        private var partIndex = 1
        private var activeFile = firstFile
        private var output = openPart(firstFile)
        private var dataBytes = 0L
        private var finished = false

        init {
            require(this.maxDataBytes >= blockAlign) { "WAV part size must fit at least one PCM frame" }
        }

        override fun write(samples: ShortArray, sampleCount: Int) {
            for (index in 0 until sampleCount) {
                pendingFrame[pendingSampleCount++] = samples[index]
                if (pendingSampleCount == channelCount) {
                    writeFrame()
                    pendingSampleCount = 0
                }
            }
        }

        override fun finish(): List<RecordedOutput> {
            if (finished) return completed.toList()
            finished = true
            closePart()
            return completed.toList()
        }

        override fun close() {
            finish()
        }

        private fun writeFrame() {
            if (dataBytes + blockAlign > maxDataBytes) {
                closePart()
                partIndex += 1
                activeFile = partFile(partIndex)
                output = openPart(activeFile)
                dataBytes = 0L
            }
            pendingFrame.forEach { sample ->
                val value = sample.toInt()
                output.write(value and 0xff)
                output.write((value ushr 8) and 0xff)
            }
            dataBytes += blockAlign
        }

        private fun closePart() {
            output.seek(0)
            WavHeader.write(output, sampleRate, channelCount, dataBytes)
            output.close()
            val frameCount = dataBytes / blockAlign
            completed += RecordedOutput(
                file = activeFile,
                partIndex = partIndex,
                durationMs = frameCount * 1_000L / sampleRate,
            )
        }

        private fun openPart(file: File): RandomAccessFile = RandomAccessFile(file, "rw").apply {
            setLength(0)
            WavHeader.write(this, sampleRate, channelCount, dataBytes = 0)
        }
    }

    private class Mp3AudioWriter(
        private val file: File,
        sampleRate: Int,
        bitrate: Int,
        channelCount: Int,
    ) : PcmAudioWriter {
        private val output = FileOutputStream(file)
        private val encoder = LameMp3Encoder(sampleRate = sampleRate, bitrate = bitrate, channelCount = channelCount)
        private var finished = false

        override fun write(samples: ShortArray, sampleCount: Int) {
            val bytes = encoder.encode(samples, sampleCount)
            if (bytes.isNotEmpty()) {
                output.write(bytes)
            }
        }

        override fun finish(): List<RecordedOutput> {
            if (finished) return listOf(RecordedOutput(file = file))
            finished = true
            runCatching {
                val bytes = encoder.flush()
                if (bytes.isNotEmpty()) {
                    output.write(bytes)
                }
            }.also {
                encoder.close()
                output.close()
            }.getOrThrow()
            return listOf(RecordedOutput(file = file))
        }

        override fun close() {
            finish()
        }
    }

    private companion object {
        private const val WORKER_JOIN_TIMEOUT_MS = 2_000L
        private const val ERROR_STOPPED = -1
    }
}

internal fun recordingPartFile(firstFile: File, partIndex: Int): File {
    require(partIndex >= 1) { "Recording part index must be positive" }
    if (partIndex == 1) return firstFile
    val suffix = "_part${partIndex.toString().padStart(2, '0')}"
    return File(firstFile.parentFile, "${firstFile.nameWithoutExtension}$suffix.${firstFile.extension}")
}
