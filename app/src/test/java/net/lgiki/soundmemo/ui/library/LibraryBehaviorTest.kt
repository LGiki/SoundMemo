package net.lgiki.soundmemo.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryBehaviorTest {
    @Test
    fun shareMimeType_mapsSupportedRecordingFormats() {
        assertEquals("audio/mp4", shareMimeType("m4a"))
        assertEquals("audio/aac", shareMimeType("aac"))
        assertEquals("audio/3gpp", shareMimeType("3gp"))
        assertEquals("audio/wav", shareMimeType("wav"))
        assertEquals("audio/mpeg", shareMimeType("mp3"))
    }

    @Test
    fun shareMimeType_defaultsToGenericAudio() {
        assertEquals("audio/*", shareMimeType("unknown"))
        assertEquals("audio/mpeg", shareMimeType("MP3"))
    }

}
