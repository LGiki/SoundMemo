package net.lgiki.soundmemo.service

/** Allows either startup completion or cancellation to win, but never both. */
internal class RecordingStartupGate {
    private val lock = Any()
    private var outcome = Outcome.Pending

    fun tryCommit(): Boolean = synchronized(lock) {
        if (outcome != Outcome.Pending) {
            false
        } else {
            outcome = Outcome.Committed
            true
        }
    }

    fun tryCancel(): Boolean = synchronized(lock) {
        if (outcome != Outcome.Pending) {
            false
        } else {
            outcome = Outcome.Cancelled
            true
        }
    }

    private enum class Outcome {
        Pending,
        Committed,
        Cancelled,
    }
}
