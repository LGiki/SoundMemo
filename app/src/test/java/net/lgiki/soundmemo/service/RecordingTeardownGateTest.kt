package net.lgiki.soundmemo.service

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingTeardownGateTest {
    @Test
    fun tryClaim_allowsOnlyOneConcurrentTerminationPath() {
        val gate = RecordingTeardownGate()
        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        val complete = CountDownLatch(8)
        val owners = AtomicInteger(0)

        repeat(8) {
            Thread {
                ready.countDown()
                start.await()
                if (gate.tryClaim()) owners.incrementAndGet()
                complete.countDown()
            }.start()
        }

        ready.await()
        start.countDown()
        complete.await()

        assertEquals(1, owners.get())
        gate.release()
        assertTrue(gate.tryClaim())
    }
}
