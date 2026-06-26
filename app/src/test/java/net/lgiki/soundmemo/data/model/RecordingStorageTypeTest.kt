package net.lgiki.soundmemo.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingStorageTypeTest {
    @Test
    fun fromStorageValue_supportsContentUri() {
        assertEquals(
            RecordingStorageType.ContentUri,
            RecordingStorageType.fromStorageValue("content_uri"),
        )
    }

    @Test
    fun fromStorageValue_unknownFallsBackToFile() {
        assertEquals(
            RecordingStorageType.File,
            RecordingStorageType.fromStorageValue("unexpected"),
        )
    }
}
