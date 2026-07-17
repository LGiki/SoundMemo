package net.lgiki.soundmemo.data.storage

import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingUriDeleterTest {
    private val uri = Uri.parse("content://net.lgiki.soundmemo.test/recording/1")

    @Test
    fun positiveDeleteCount_succeeds() {
        assertTrue(deleter(deleteRows = 1).delete(uri, documentUri = false))
    }

    @Test
    fun zeroDeleteCount_succeedsOnlyWhenUriIsConfirmedAbsent() {
        assertTrue(deleter(deleteRows = 0, presence = UriPresence.Absent).delete(uri, documentUri = false))
        assertFalse(deleter(deleteRows = 0, presence = UriPresence.Present).delete(uri, documentUri = false))
        assertFalse(deleter(deleteRows = 0, presence = UriPresence.Unknown).delete(uri, documentUri = false))
    }

    @Test
    fun documentDeleteFallsBackToGenericDelete() {
        assertTrue(deleter(deleteRows = 1, documentDeleted = false).delete(uri, documentUri = true))
    }

    @Test
    fun deleteException_keepsMetadataForRetry() {
        val operations = FakeOperations(deleteFailure = SecurityException("denied"))
        assertFalse(RecordingUriDeleter(operations).delete(uri, documentUri = false))
    }

    private fun deleter(
        deleteRows: Int,
        presence: UriPresence = UriPresence.Present,
        documentDeleted: Boolean = false,
    ) = RecordingUriDeleter(
        FakeOperations(
            deleteRows = deleteRows,
            presence = presence,
            documentDeleted = documentDeleted,
        ),
    )

    private class FakeOperations(
        private val deleteRows: Int = 0,
        private val presence: UriPresence = UriPresence.Unknown,
        private val documentDeleted: Boolean = false,
        private val deleteFailure: Exception? = null,
    ) : RecordingUriOperations {
        override fun delete(uri: Uri): Int {
            deleteFailure?.let { throw it }
            return deleteRows
        }

        override fun deleteDocument(uri: Uri): Boolean = documentDeleted

        override fun presence(uri: Uri): UriPresence = presence
    }
}
