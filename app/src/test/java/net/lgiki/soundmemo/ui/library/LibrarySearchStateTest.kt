package net.lgiki.soundmemo.ui.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchStateTest {
    @Test
    fun shouldShowEmptyLibrary_whenThereAreNoActiveRecordings() {
        assertTrue(shouldShowEmptyLibrary(activeCount = 0))
        assertFalse(shouldShowEmptyLibrary(activeCount = 1))
    }

    @Test
    fun shouldShowNoSearchResults_onlyWhenQueryFiltersAllActiveRecordings() {
        assertTrue(shouldShowNoSearchResults(query = "missing", filteredCount = 0, activeCount = 2))
        assertFalse(shouldShowNoSearchResults(query = "", filteredCount = 0, activeCount = 2))
        assertFalse(shouldShowNoSearchResults(query = "match", filteredCount = 1, activeCount = 2))
        assertFalse(shouldShowNoSearchResults(query = "missing", filteredCount = 0, activeCount = 0))
    }
}
