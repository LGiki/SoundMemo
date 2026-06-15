package net.lgiki.soundmemo.ui.library

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
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
    val deleted: List<Recording> = emptyList(),
    val query: String = "",
    val sort: RecordingSort = RecordingSort.Newest,
)

class LibraryViewModel(private val container: SoundMemoContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(RecordingSort.Newest)
    val playback = PlaybackController(container.appContext, container.settingsRepository)

    init {
        viewModelScope.launch {
            container.settingsRepository.settings
                .map { it.recycleRetentionDays }
                .distinctUntilChanged()
                .collect { retentionDays ->
                    container.recordingRepository.purgeExpired(retentionDays, container.recordingStorage::deleteFile)
                }
        }
    }

    val state = combine(
        container.recordingRepository.activeRecordings,
        container.recordingRepository.deletedRecordings,
        query,
        sort,
    ) { active, deleted, queryValue, sortValue ->
        val filtered = active.filter { it.name.contains(queryValue, ignoreCase = true) }
        LibraryUiState(
            recordings = filtered.sortedWith(sortValue.comparator()),
            deleted = deleted,
            query = queryValue,
            sort = sortValue,
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
            container.recordingRepository.deletePermanently(id, container.recordingStorage::deleteFile)
        }
    }

    fun share(context: Context, recording: Recording) {
        val file = File(recording.filePath)
        if (!file.exists()) return
        val uri = container.recordingStorage.shareUri(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ContextCompat.startActivity(context, Intent.createChooser(intent, context.getString(R.string.share_title)), null)
    }

    override fun onCleared() {
        playback.release()
    }
}

private fun RecordingSort.comparator(): Comparator<Recording> =
    when (this) {
        RecordingSort.Newest -> compareByDescending { it.createdAt }
        RecordingSort.Oldest -> compareBy { it.createdAt }
        RecordingSort.Longest -> compareByDescending { it.durationMs }
        RecordingSort.Shortest -> compareBy { it.durationMs }
        RecordingSort.Name -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
