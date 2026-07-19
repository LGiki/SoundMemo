package net.lgiki.soundmemo.domain.recorder

/** Keeps the newest location reported for one recording session. */
internal class RecordingLocationCapture {
    private val lock = Any()
    private var location: RecordingLocation? = null

    fun update(candidate: RecordingLocation?) {
        if (candidate == null) return
        synchronized(lock) {
            if (location == null || candidate.capturedAt > location!!.capturedAt) {
                location = candidate
            }
        }
    }

    fun latest(): RecordingLocation? = synchronized(lock) { location }
}
