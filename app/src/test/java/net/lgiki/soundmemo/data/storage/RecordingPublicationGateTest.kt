package net.lgiki.soundmemo.data.storage

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingPublicationGateTest {
    @Test
    fun withLock_serializesPublicationAndRecovery() = runTest {
        val gate = RecordingPublicationGate()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        var secondEntered = false

        val first = async {
            gate.withLock {
                firstEntered.complete(Unit)
                releaseFirst.await()
            }
        }
        firstEntered.await()
        val second = async {
            gate.withLock {
                secondEntered = true
            }
        }

        testScheduler.runCurrent()
        assertFalse(secondEntered)
        releaseFirst.complete(Unit)
        first.await()
        second.await()
        assertTrue(secondEntered)
    }

    @Test
    fun resolvedPendingPublications_keepsFailedDeletionForRetry() = runTest {
        val committed = saveResult("committed")
        val deleted = saveResult("deleted")
        val failed = saveResult("failed")

        val resolved = resolvedPendingPublications(
            pending = listOf(committed, deleted, failed),
            hasSaveResult = { it == committed },
            deletePublishedRecording = { it == deleted },
        )

        assertEquals(listOf(committed, deleted), resolved)
    }

    private fun saveResult(name: String) = RecordingSaveResult(
        storageType = "file",
        filePath = "/recordings/$name.m4a",
        storageUri = null,
        fileSizeBytes = 1L,
        fellBackToAppFiles = false,
    )
}
