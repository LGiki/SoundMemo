package net.lgiki.soundmemo.service.audio

import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.max

/** Platform-independent PCM read loop with coordinated output shutdown. */
internal class PcmCaptureWorker(
    private val bufferSize: Int,
    private val read: (ShortArray) -> Int,
    private val write: (ShortArray, Int) -> Unit,
    private val closeOutput: () -> Unit,
    private val onAmplitude: (Int) -> Unit,
) {
    private val outputLock = Any()
    private val lifecycleLock = Any()
    private val failureRef = AtomicReference<Throwable?>(null)

    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var captureStateVersion = 0L
    private var worker: Thread? = null
    private var outputClosed = false
    private var closeWhenWorkerExits = false
    private var workerFinished = false

    val failure: Throwable?
        get() = failureRef.get()

    fun start() {
        check(worker == null) { "PCM capture worker has already started" }
        running = true
        worker = Thread(::recordLoop, "SoundMemoPcmRecorder").apply { start() }
    }

    fun pause() {
        paused = true
        captureStateVersion += 1
    }

    fun resume() {
        paused = false
        captureStateVersion += 1
    }

    fun requestStop() {
        running = false
    }

    /** Returns false when [timeoutMs] expires before the read worker exits. */
    fun stopAndJoin(timeoutMs: Long? = null): Boolean {
        requestStop()
        if (join(timeoutMs)) {
            synchronized(lifecycleLock) {
                if (!workerFinished) {
                    closeWhenWorkerExits = true
                    return false
                }
            }
        }
        closeOutputOnce()
        return true
    }

    private fun recordLoop() {
        val samples = ShortArray(bufferSize)
        try {
            while (running) {
                if (paused) {
                    Thread.sleep(PAUSED_READ_SLEEP_MS)
                    continue
                }
                val readStateVersion = captureStateVersion
                val readCount = read(samples)
                if (!running) break
                if (paused || readStateVersion != captureStateVersion) continue
                if (readCount < 0) {
                    error("AudioRecord read failed: $readCount")
                }
                if (readCount == 0) {
                    Thread.sleep(EMPTY_READ_SLEEP_MS)
                    continue
                }
                if (paused) {
                    onAmplitude(0)
                    continue
                }

                var maxSample = 0
                for (index in 0 until readCount) {
                    maxSample = max(maxSample, abs(samples[index].toInt()))
                }
                onAmplitude(maxSample)
                synchronized(outputLock) {
                    if (running && !paused) {
                        write(samples, readCount)
                    }
                }
            }
        } catch (throwable: Throwable) {
            failureRef.compareAndSet(null, throwable)
            running = false
        } finally {
            val closeWhenFinished = synchronized(lifecycleLock) {
                workerFinished = true
                closeWhenWorkerExits
            }
            if (closeWhenFinished) {
                runCatching { closeOutputOnce() }
            }
        }
    }

    private fun join(timeoutMs: Long?): Boolean {
        val activeWorker = worker ?: return false
        if (activeWorker !== Thread.currentThread()) {
            if (timeoutMs == null) activeWorker.join() else activeWorker.join(timeoutMs)
        }
        if (!activeWorker.isAlive) {
            worker = null
        }
        return activeWorker.isAlive
    }

    private fun closeOutputOnce() {
        synchronized(outputLock) {
            if (!outputClosed) {
                outputClosed = true
                closeOutput.invoke()
            }
        }
    }

    private companion object {
        private const val PAUSED_READ_SLEEP_MS = 50L
        private const val EMPTY_READ_SLEEP_MS = 10L
    }
}
