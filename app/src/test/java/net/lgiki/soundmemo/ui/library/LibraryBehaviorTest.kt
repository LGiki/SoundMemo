package net.lgiki.soundmemo.ui.library

import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.data.model.RecordingStorageType
import net.lgiki.soundmemo.data.storage.documentUriPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun formatBitrate_formatsAsKbps() {
        assertEquals("128 kbps", formatBitrate(128_000))
        assertEquals("0 kbps", formatBitrate(-1))
    }

    @Test
    fun formatSampleRate_formatsWholeAndFractionalKilohertz() {
        assertEquals("48 kHz", formatSampleRate(48_000))
        assertEquals("44.1 kHz", formatSampleRate(44_100))
    }

    @Test
    fun storageLabelRes_mapsStorageTypesToFriendlyLabels() {
        assertEquals(
            R.string.library_property_storage_app_files,
            storageLabelRes(RecordingStorageType.File.storageValue),
        )
        assertEquals(
            R.string.library_property_storage_device_music,
            storageLabelRes(RecordingStorageType.MediaStore.storageValue),
        )
        assertEquals(
            R.string.library_property_storage_custom_folder,
            storageLabelRes(RecordingStorageType.ContentUri.storageValue),
        )
    }

    @Test
    fun storageLabelRes_mapsUnknownStorageToUnknownLabel() {
        assertEquals(R.string.library_property_storage_unknown, storageLabelRes("unknown"))
    }

    @Test
    fun storageDisplayValue_usesMediaStorePathWhenAvailable() {
        val recording = Recording(
            name = "Meeting",
            filePath = "/storage/emulated/0/Music/SoundMemo/Meeting_abc123.m4a",
            durationMs = 1_000,
            fileSizeBytes = 256,
            storageType = RecordingStorageType.MediaStore.storageValue,
            storageUri = "content://media/external/audio/media/1",
        )

        assertEquals(recording.filePath, storageDisplayValue(recording))
    }

    @Test
    fun storageDisplayValue_buildsDeviceMusicFallbackForLegacyMediaStoreRows() {
        val recording = Recording(
            name = "Meeting",
            filePath = "",
            durationMs = 1_000,
            fileSizeBytes = 256,
            format = "m4a",
            storageType = RecordingStorageType.MediaStore.storageValue,
            storageUri = "content://media/external/audio/media/1",
        )

        assertEquals("/storage/emulated/0/Music/SoundMemo/Meeting.m4a", storageDisplayValue(recording))
    }

    @Test
    fun storageDisplayValue_usesLegacyPublicMusicFilePath() {
        val recording = Recording(
            name = "Meeting",
            filePath = "/storage/emulated/0/Music/SoundMemo/Meeting_abc123.m4a",
            durationMs = 1_000,
            fileSizeBytes = 256,
            storageType = RecordingStorageType.File.storageValue,
        )

        assertEquals(recording.filePath, storageDisplayValue(recording))
    }

    @Test
    fun storageDisplayValue_omitsAppFilePath() {
        val recording = Recording(
            name = "Meeting",
            filePath = "/storage/emulated/0/Android/data/net.lgiki.soundmemo/files/Music/recordings/Meeting.m4a",
            durationMs = 1_000,
            fileSizeBytes = 256,
            storageType = RecordingStorageType.File.storageValue,
        )

        assertNull(storageDisplayValue(recording))
    }

    @Test
    fun documentUriPath_mapsPrimaryExternalStorageDocumentUriToFullPath() {
        val uri = "content://com.android.externalstorage.documents/tree/primary%3AMusic%2FSoundMemo/document/primary%3AMusic%2FSoundMemo%2FMeeting.m4a"

        assertEquals("/storage/emulated/0/Music/SoundMemo/Meeting.m4a", documentUriPath(uri))
    }

    @Test
    fun documentUriPath_mapsNonPrimaryExternalStorageDocumentUriToFullPath() {
        val uri = "content://com.android.externalstorage.documents/tree/0123-4567%3ASoundMemo/document/0123-4567%3ASoundMemo%2FMeeting.m4a"

        assertEquals("/storage/0123-4567/SoundMemo/Meeting.m4a", documentUriPath(uri))
    }

    @Test
    fun storageDisplayValue_usesContentUriPathWhenFilePathIsMissing() {
        val recording = Recording(
            name = "Meeting",
            filePath = "",
            durationMs = 1_000,
            fileSizeBytes = 256,
            storageType = RecordingStorageType.ContentUri.storageValue,
            storageUri = "content://com.android.externalstorage.documents/tree/primary%3AMusic%2FSoundMemo/document/primary%3AMusic%2FSoundMemo%2FMeeting.m4a",
        )

        assertEquals("/storage/emulated/0/Music/SoundMemo/Meeting.m4a", storageDisplayValue(recording))
    }
}
