package net.lgiki.soundmemo.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySelectionTest {
    @Test
    fun toggle_addsAndRemovesRecordingId() {
        assertEquals(setOf(1L, 2L), setOf(1L).toggle(2L))
        assertEquals(emptySet<Long>(), setOf(1L).toggle(1L))
    }
}
