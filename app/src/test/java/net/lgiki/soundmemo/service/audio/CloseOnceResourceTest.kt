package net.lgiki.soundmemo.service.audio

import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CloseOnceResourceTest {
    @Test
    fun close_withoutWorkerClosesResourceExactlyOnce() {
        val closeCount = AtomicInteger(0)
        val owner = PcmRecordingBackend.CloseOnceResource<AutoCloseable>()
        owner.set(AutoCloseable { closeCount.incrementAndGet() })

        owner.close()
        owner.close()

        assertEquals(1, closeCount.get())
        assertNull(owner.withResource { it })
    }
}
