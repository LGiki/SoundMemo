package net.lgiki.soundmemo.service.audio

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmCaptureWorkerTest {
    @Test
    fun paused_doesNotReadUntilResumed() {
        val readStarted = CountDownLatch(1)
        val allowRead = CountDownLatch(1)
        val worker = PcmCaptureWorker(
            bufferSize = 4,
            read = {
                readStarted.countDown()
                allowRead.await()
                0
            },
            write = { _, _ -> },
            closeOutput = {},
            onAmplitude = {},
        )

        worker.pause()
        worker.start()
        assertFalse(readStarted.await(100, TimeUnit.MILLISECONDS))
        worker.resume()
        assertTrue(readStarted.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        worker.requestStop()
        allowRead.countDown()
        worker.stopAndJoin()

        assertNull(worker.failure)
    }

    @Test
    fun negativeRead_afterStop_doesNotRecordFailure() {
        val readStarted = CountDownLatch(1)
        val allowRead = CountDownLatch(1)
        val worker = PcmCaptureWorker(
            bufferSize = 4,
            read = {
                readStarted.countDown()
                allowRead.await()
                -3
            },
            write = { _, _ -> error("write should not be called") },
            closeOutput = {},
            onAmplitude = {},
        )

        worker.start()
        assertTrue(readStarted.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        worker.requestStop()
        allowRead.countDown()
        worker.stopAndJoin()

        assertNull(worker.failure)
    }

    @Test
    fun timedStop_closesOutputAndPreventsDelayedReadFromWriting() {
        val readStarted = CountDownLatch(1)
        val allowRead = CountDownLatch(1)
        val writes = AtomicInteger(0)
        val closes = AtomicInteger(0)
        val worker = PcmCaptureWorker(
            bufferSize = 4,
            read = { samples ->
                readStarted.countDown()
                allowRead.await()
                samples[0] = 42
                1
            },
            write = { _, _ -> writes.incrementAndGet() },
            closeOutput = { closes.incrementAndGet() },
            onAmplitude = {},
        )

        worker.start()
        assertTrue(readStarted.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        worker.stopAndJoin(timeoutMs = 1)
        assertEquals(0, closes.get())
        allowRead.countDown()
        waitUntil { closes.get() == 1 }

        assertEquals(0, writes.get())
        assertEquals(1, closes.get())
        assertNull(worker.failure)
    }

    @Test
    fun stopAndJoin_waitsForBlockedReadToFinish() {
        val readStarted = CountDownLatch(1)
        val allowRead = CountDownLatch(1)
        val stopReturned = CountDownLatch(1)
        val worker = PcmCaptureWorker(
            bufferSize = 4,
            read = {
                readStarted.countDown()
                allowRead.await()
                0
            },
            write = { _, _ -> },
            closeOutput = {},
            onAmplitude = {},
        )

        worker.start()
        assertTrue(readStarted.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        Thread {
            worker.stopAndJoin()
            stopReturned.countDown()
        }.start()

        assertFalse(stopReturned.await(100, TimeUnit.MILLISECONDS))
        allowRead.countDown()
        assertTrue(stopReturned.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertNull(worker.failure)
    }

    @Test
    fun timedStop_canBeFollowedByDeferredJoin() {
        val readStarted = CountDownLatch(1)
        val allowRead = CountDownLatch(1)
        val worker = PcmCaptureWorker(
            bufferSize = 4,
            read = {
                readStarted.countDown()
                allowRead.await()
                0
            },
            write = { _, _ -> },
            closeOutput = {},
            onAmplitude = {},
        )

        worker.start()
        assertTrue(readStarted.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertFalse(worker.stopAndJoin(timeoutMs = 1))
        val deferredJoin = Thread { worker.stopAndJoin() }.apply { start() }

        assertTrue(deferredJoin.isAlive)
        allowRead.countDown()
        deferredJoin.join(TEST_TIMEOUT_MS)
        assertFalse(deferredJoin.isAlive)
        assertNull(worker.failure)
    }

    private fun waitUntil(predicate: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TEST_TIMEOUT_MS)
        while (!predicate() && System.nanoTime() < deadline) {
            Thread.sleep(5)
        }
        assertTrue(predicate())
    }

    private companion object {
        private const val TEST_TIMEOUT_MS = 1_000L
    }
}
