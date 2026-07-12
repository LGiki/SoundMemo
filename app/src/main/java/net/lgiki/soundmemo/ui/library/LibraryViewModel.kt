package net.lgiki.soundmemo.ui.library

import android.content.Context
import android.content.ClipData
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.SoundMemoContainer
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.data.model.RecordingSort
import net.lgiki.soundmemo.domain.player.PlaybackController

data class LibraryUiState(
    val recordings: List<Recording> = emptyList(),
    val activeCount: Int = 0,
    val deleted: List<Recording> = emptyList(),
    val query: String = "",
    val sort: RecordingSort = RecordingSort.Newest,
    val rewindSeconds: Int = 10,
    val forwardSeconds: Int = 10,
)

class LibraryViewModel(private val container: SoundMemoContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(RecordingSort.Newest)
    val playback = PlaybackController(container.appContext, container.settingsRepository, container.recordingStorage)

    init {
        viewModelScope.launch {
            container.settingsRepository.settings
                .map { it.recycleRetentionDays }
                .distinctUntilChanged()
                .collect { retentionDays ->
                    container.recordingRepository.purgeExpired(retentionDays, container.recordingStorage::deleteRecording)
                }
        }
    }

    val state = combine(
        container.recordingRepository.activeRecordings,
        container.recordingRepository.deletedRecordings,
        query,
        sort,
        container.settingsRepository.settings,
    ) { active, deleted, queryValue, sortValue, settings ->
        val filtered = active.filter { it.name.contains(queryValue, ignoreCase = true) }
        LibraryUiState(
            recordings = filtered.sortedWith(sortValue.comparator()),
            activeCount = active.size,
            deleted = deleted,
            query = queryValue,
            sort = sortValue,
            rewindSeconds = settings.rewindSeconds,
            forwardSeconds = settings.forwardSeconds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSort(value: RecordingSort) {
        sort.value = value
    }

    fun play(recording: Recording) {
        playback.play(recording)
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { container.recordingRepository.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { container.recordingRepository.moveToRecycleBin(id) }
    }

    fun restore(id: Long) {
        viewModelScope.launch { container.recordingRepository.restore(id) }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch {
            container.recordingRepository.deletePermanently(id, container.recordingStorage::deleteRecording)
        }
    }

    fun share(context: Context, recording: Recording) {
        val uri = container.recordingStorage.shareUri(recording) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = shareMimeType(recording.format)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)))
    }

    override fun onCleared() {
        playback.release()
    }
}

internal fun shareMimeType(format: String): String = when (format.lowercase()) {
    "m4a" -> "audio/mp4"
    "aac" -> "audio/aac"
    "3gp" -> "audio/3gpp"
    "wav" -> "audio/wav"
    "mp3" -> "audio/mpeg"
    else -> "audio/*"
}

private fun RecordingSort.comparator(): Comparator<Recording> =
    when (this) {
        RecordingSort.Newest -> compareByDescending { it.createdAt }
        RecordingSort.Oldest -> compareBy { it.createdAt }
        RecordingSort.Longest -> compareByDescending { it.durationMs }
        RecordingSort.Shortest -> compareBy { it.durationMs }
        RecordingSort.Name -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
