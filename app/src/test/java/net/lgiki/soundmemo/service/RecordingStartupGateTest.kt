package net.lgiki.soundmemo.service

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingStartupGateTest {
    @Test
    fun commit_preventsLaterCancellation() {
        val gate = RecordingStartupGate()

        assertTrue(gate.tryCommit())
        assertFalse(gate.tryCancel())
        assertFalse(gate.tryCommit())
    }

    @Test
    fun cancellation_preventsLateCommit() {
        val gate = RecordingStartupGate()

        assertTrue(gate.tryCancel())
        assertFalse(gate.tryCommit())
        assertFalse(gate.tryCancel())
    }

    @Test
    fun commitAndCancellation_raceHasOneWinner() {
        val gate = RecordingStartupGate()
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val complete = CountDownLatch(2)
        val winners = AtomicInteger(0)

        Thread {
            ready.countDown()
            start.await()
            if (gate.tryCommit()) winners.incrementAndGet()
            complete.countDown()
        }.start()
        Thread {
            ready.countDown()
            start.await()
            if (gate.tryCancel()) winners.incrementAndGet()
            complete.countDown()
        }.start()

        ready.await()
        start.countDown()
        complete.await()

        assertEquals(1, winners.get())
    }
}
