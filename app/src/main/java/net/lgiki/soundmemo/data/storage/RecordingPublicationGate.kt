package net.lgiki.soundmemo.data.storage

import kotlinx.coroutines.sync.Mutex

/** Serializes recording publication with startup recovery of interrupted publications. */
class RecordingPublicationGate {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T {
        mutex.lock()
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}

internal suspend fun resolvedPendingPublications(
    pending: List<RecordingSaveResult>,
    hasSaveResult: suspend (RecordingSaveResult) -> Boolean,
    deletePublishedRecording: (RecordingSaveResult) -> Boolean,
): List<RecordingSaveResult> = pending.filter { saveResult ->
    hasSaveResult(saveResult) || deletePublishedRecording(saveResult)
}
