package net.lgiki.soundmemo.service

/** Ensures that only one recording termination path owns cleanup at a time. */
internal class RecordingTeardownGate {
    private val lock = Any()
    private var claimed = false

    fun tryClaim(): Boolean = synchronized(lock) {
        if (claimed) {
            false
        } else {
            claimed = true
            true
        }
    }

    fun isClaimed(): Boolean = synchronized(lock) { claimed }

    fun release() {
        synchronized(lock) {
            claimed = false
        }
    }
}
