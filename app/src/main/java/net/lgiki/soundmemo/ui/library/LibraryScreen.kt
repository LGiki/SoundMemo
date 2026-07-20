package net.lgiki.soundmemo.ui.library

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.ui.AdaptiveContent
import net.lgiki.soundmemo.ui.SoundMemoScaffold
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.data.model.RecordingSort
import net.lgiki.soundmemo.data.storage.deviceMusicPath
import net.lgiki.soundmemo.data.storage.documentUriPath
import net.lgiki.soundmemo.domain.player.PlayerUiState
import net.lgiki.soundmemo.util.formatDateTime
import net.lgiki.soundmemo.util.formatDuration
import net.lgiki.soundmemo.util.formatFileSize
import net.lgiki.soundmemo.util.formatPreciseDuration
import net.lgiki.soundmemo.util.formatRecordingLocation

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    parentReservesBottomNavigation: Boolean,
    onStartRecording: () -> Unit,
    onRecycleBinClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState by viewModel.playback.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var propertiesRecording by remember { mutableStateOf<Recording?>(null) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val selectedRecordings = state.activeRecordings.filter { it.id in selectedIds }
    val activeSelectedIds = selectedRecordings.mapTo(linkedSetOf()) { it.id }
    val selectionMode = activeSelectedIds.isNotEmpty()
    fun moveToRecycleBin(recordings: List<Recording>) {
        if (recordings.isEmpty()) return
        if (recordings.any { it.id == playerState.recording?.id }) {
            viewModel.playback.stop()
        }
        val message = context.resources.getQuantityString(
            R.plurals.library_moved_to_recycle_bin,
            recordings.size,
            recordings.size,
            state.recycleRetentionDays,
        )
        viewModel.delete(recordings) {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
        selectedIds = emptySet()
    }
    SoundMemoScaffold(
        title = {
            if (selectionMode) {
                Text(pluralStringResource(R.plurals.library_selected, activeSelectedIds.size, activeSelectedIds.size))
            } else {
                Text(stringResource(R.string.library_title))
            }
        },
        navigationIcon = if (selectionMode) {
            {
                IconButton(onClick = { selectedIds = emptySet() }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.library_clear_selection))
                }
            }
        } else {
            {}
        },
        actions = {
            if (selectionMode) {
                IconButton(onClick = { selectedIds = state.recordings.mapTo(linkedSetOf()) { it.id } }) {
                    Icon(Icons.Default.DoneAll, contentDescription = stringResource(R.string.library_select_all))
                }
                IconButton(onClick = {
                    viewModel.share(context, selectedRecordings)
                    selectedIds = emptySet()
                }) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.library_share_selected))
                }
                IconButton(onClick = {
                    moveToRecycleBin(selectedRecordings)
                }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.library_delete_selected))
                }
            } else {
                IconButton(onClick = onRecycleBinClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.library_open_recycle_bin),
                    )
                }
            }
        },
        contentWindowInsets = if (parentReservesBottomNavigation) {
            WindowInsets.statusBars
        } else {
            WindowInsets.safeDrawing
        },
    ) { padding ->
        AdaptiveContent(padding = padding, maxContentWidth = 960.dp) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    label = { Text(stringResource(R.string.library_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
                SortRow(state.sort, viewModel::setSort)
                if (shouldShowEmptyLibrary(state.activeCount)) {
                    EmptyLibrary(onStartRecording = onStartRecording, modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (shouldShowNoSearchResults(state.query, state.recordings.size, state.activeCount)) {
                            item {
                                NoSearchResults()
                            }
                        }
                        items(state.recordings, key = { it.id }) { recording ->
                            val isSelected = playerState.recording?.id == recording.id
                            RecordingItem(
                                recording = recording,
                                playerState = playerState.takeIf { isSelected },
                                selectionMode = selectionMode,
                                multiSelected = recording.id in selectedIds,
                                onSelectionChange = {
                                    selectedIds = selectedIds.toggle(recording.id)
                                },
                                onPlay = {
                                    if (isSelected) {
                                        viewModel.playback.toggle()
                                    } else {
                                        viewModel.play(recording)
                                    }
                                },
                                onRename = { viewModel.rename(recording.id, it) },
                                onProperties = { propertiesRecording = recording },
                                onShare = { viewModel.share(it, recording) },
                                onDelete = { moveToRecycleBin(listOf(recording)) },
                                onSeek = viewModel.playback::seekTo,
                                rewindSeconds = state.rewindSeconds,
                                onSkipBack = { viewModel.playback.skipBy(-state.rewindSeconds * 1_000L) },
                                onToggle = viewModel.playback::toggle,
                                forwardSeconds = state.forwardSeconds,
                                onSkipForward = { viewModel.playback.skipBy(state.forwardSeconds * 1_000L) },
                                onSpeed = viewModel.playback::setSpeed,
                            )
                        }
                    }
                }
            }
        }
    }
    propertiesRecording?.let { recording ->
        RecordingPropertiesSheet(
            recording = recording,
            onDismiss = { propertiesRecording = null },
        )
    }
}

internal fun shouldShowEmptyLibrary(activeCount: Int): Boolean = activeCount == 0

internal fun shouldShowNoSearchResults(query: String, filteredCount: Int, activeCount: Int): Boolean =
    query.isNotBlank() && filteredCount == 0 && activeCount > 0

@Composable
internal fun SortRow(sort: RecordingSort, onSort: (RecordingSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val values = listOf(
        RecordingSort.Newest,
        RecordingSort.Oldest,
        RecordingSort.Name,
        RecordingSort.Longest,
        RecordingSort.Shortest,
    )
    Box {
        FilledTonalButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.library_sort_by, sortLabel(sort)), maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(sortLabel(value)) },
                    onClick = {
                        onSort(value)
                        expanded = false
                    },
                    trailingIcon = if (sort == value) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onStartRecording: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp).size(28.dp),
                )
            }
            Spacer(Modifier.size(16.dp))
            Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(16.dp))
            FilledTonalButton(onClick = onStartRecording) {
                Text(stringResource(R.string.library_empty_action))
            }
        }
    }
}

@Composable
private fun NoSearchResults() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.library_no_search_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun sortLabel(sort: RecordingSort): String = when (sort) {
    RecordingSort.Newest -> stringResource(R.string.sort_newest)
    RecordingSort.Oldest -> stringResource(R.string.sort_oldest)
    RecordingSort.Name -> stringResource(R.string.sort_name)
    RecordingSort.Longest -> stringResource(R.string.sort_longest)
    RecordingSort.Shortest -> stringResource(R.string.sort_shortest)
}

@Composable
private fun RecordingItem(
    recording: Recording,
    playerState: PlayerUiState?,
    selectionMode: Boolean,
    multiSelected: Boolean,
    onSelectionChange: () -> Unit,
    onPlay: () -> Unit,
    onRename: (String) -> Unit,
    onProperties: () -> Unit,
    onShare: (android.content.Context) -> Unit,
    onDelete: () -> Unit,
    onSeek: (Long) -> Unit,
    rewindSeconds: Int,
    onSkipBack: () -> Unit,
    onToggle: () -> Unit,
    forwardSeconds: Int,
    onSkipForward: () -> Unit,
    onSpeed: (Float) -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    val location = formatRecordingLocation(recording)
    val isPlaying = playerState?.isPlaying == true
    val selectionDescription = stringResource(R.string.library_toggle_selection_desc, recording.name)
    RecordingRow(
        title = recording.name,
        metadata = listOfNotNull(
            formatDateTime(recording.createdAt),
            formatDuration(recording.durationMs),
            formatFileSize(recording.fileSizeBytes),
            location?.let { stringResource(R.string.recording_location_coordinates, it) },
        ).joinToString(" - "),
        selected = if (selectionMode) multiSelected else playerState != null,
        onClick = if (selectionMode) onSelectionChange else onPlay,
        onLongClick = onSelectionChange,
        leading = {
            // Keep ListItem's text column fixed while swapping the 44 dp play control
            // for the checkbox's 48 dp minimum touch target.
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = multiSelected,
                        onCheckedChange = { onSelectionChange() },
                        modifier = Modifier.semantics {
                            contentDescription = selectionDescription
                        },
                    )
                } else {
                    FilledTonalIconButton(onClick = onPlay, modifier = Modifier.size(44.dp)) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) {
                                stringResource(R.string.player_pause)
                            } else {
                                stringResource(R.string.library_play_desc, recording.name)
                            },
                        )
                    }
                }
            }
        },
        trailing = {
            if (!selectionMode) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.library_more_actions))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_rename)) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        renameOpen = true
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_properties)) },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onProperties()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_share)) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onShare(context)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_delete)) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
            }
        },
        expandedContent = {
            playerState?.let {
                InlinePlaybackPanel(
                    state = it,
                    fallbackDurationMs = recording.durationMs,
                    onSeek = onSeek,
                    rewindSeconds = rewindSeconds,
                    onSkipBack = onSkipBack,
                    onToggle = onToggle,
                    forwardSeconds = forwardSeconds,
                    onSkipForward = onSkipForward,
                    onSpeed = onSpeed,
                )
            }
        },
    )
    if (renameOpen) {
        var name by remember(recording.id) { mutableStateOf(recording.name) }
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(stringResource(R.string.library_rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.library_name_label)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(name.trim().ifEmpty { recording.name })
                    renameOpen = false
                }) { Text(stringResource(R.string.library_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text(stringResource(R.string.library_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordingPropertiesSheet(recording: Recording, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(ClipboardManager::class.java)
    val storagePath = storageDisplayValue(recording)
    val storageValue = storagePath ?: stringResource(storageLabelRes(recording.storageType))
    val copyStorageLabel = stringResource(R.string.library_property_storage_copy)
    val copiedMessage = stringResource(R.string.library_property_storage_copied)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.library_properties_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PropertyRow(
                label = stringResource(R.string.library_property_name),
                value = recording.name,
            )
            PropertyRow(
                label = stringResource(R.string.library_property_duration),
                value = formatPreciseDuration(recording.durationMs),
            )
            PropertyRow(
                label = stringResource(R.string.library_property_file_size),
                value = formatFileSize(recording.fileSizeBytes),
            )
            PropertyRow(
                label = stringResource(R.string.library_property_format),
                value = recording.format.uppercase(),
            )
            PropertyRow(
                label = stringResource(R.string.library_property_bitrate),
                value = formatBitrate(recording.bitrate),
            )
            PropertyRow(
                label = stringResource(R.string.library_property_sample_rate),
                value = formatSampleRate(recording.sampleRate),
            )
            PropertyRow(
                label = stringResource(R.string.library_property_created),
                value = formatDateTime(recording.createdAt),
            )
            PropertyRow(
                label = stringResource(R.string.library_property_modified),
                value = formatDateTime(recording.updatedAt),
            )
            PropertyRow(
                label = stringResource(R.string.library_property_storage),
                value = storageValue,
                onCopy = storagePath?.let {
                    {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(copyStorageLabel, it))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    }
                },
                copyLabel = copyStorageLabel,
            )
            formatRecordingLocation(recording)?.let { location ->
                PropertyRow(
                    label = stringResource(R.string.library_property_location),
                    value = location,
                )
            }
            recording.locationAccuracyMeters?.let { accuracy ->
                PropertyRow(
                    label = stringResource(R.string.library_property_location_accuracy),
                    value = stringResource(R.string.library_property_location_accuracy_value, accuracy),
                )
            }
            recording.locationCapturedAt?.let { capturedAt ->
                PropertyRow(
                    label = stringResource(R.string.library_property_location_captured),
                    value = formatDateTime(capturedAt),
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.library_properties_close))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PropertyRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null,
    copyLabel: String? = null,
) {
    val rowModifier = if (onCopy == null) {
        Modifier
    } else {
        Modifier.combinedClickable(
            onClick = {},
            onLongClick = onCopy,
            onLongClickLabel = copyLabel,
        )
    }
    Column(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

internal fun formatBitrate(bitrate: Int): String =
    "${(bitrate / 1_000).coerceAtLeast(0)} kbps"

internal fun formatSampleRate(sampleRate: Int): String =
    if (sampleRate % 1_000 == 0) {
        "${sampleRate / 1_000} kHz"
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f kHz", sampleRate / 1_000.0)
    }

@StringRes
internal fun storageLabelRes(storageType: String): Int =
    when (storageType) {
        "file" -> R.string.library_property_storage_app_files
        "media_store" -> R.string.library_property_storage_device_music
        "content_uri" -> R.string.library_property_storage_custom_folder
        else -> R.string.library_property_storage_unknown
    }

internal fun storageDisplayValue(recording: Recording): String? =
    when (recording.storageType) {
        "media_store" -> recording.filePath.ifBlank {
            deviceMusicPath("${recording.name}.${recording.format.lowercase()}")
        }
        "content_uri" -> recording.filePath.ifBlank {
            documentUriPath(recording.storageUri)
        }
        "file" -> recording.filePath.takeIf { it.isNotBlank() && it.contains("/Music/SoundMemo/") }
        else -> null
    }

@Composable
internal fun DeletedRecordingItem(
    recording: Recording,
    retentionDays: Int,
    nowMillis: Long,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    val remainingDays = remainingRecycleBinDays(
        deletedAt = recording.deletedAt,
        retentionDays = retentionDays,
        nowMillis = nowMillis,
    )
    val deletionStatus = if (remainingDays > 0) {
        pluralStringResource(
            R.plurals.library_permanently_deleted_in_days,
            remainingDays,
            remainingDays,
        )
    } else {
        stringResource(R.string.library_permanent_deletion_scheduled)
    }
    val isNearDeletion = isNearPermanentDeletion(remainingDays)
    val deletedAtText = stringResource(
        R.string.library_deleted_prefix,
        recording.deletedAt?.let(::formatDateTime).orEmpty(),
    )
    RecordingRow(
        title = recording.name,
        metadata = deletedAtText,
        muted = true,
        urgent = isNearDeletion,
        supportingContent = {
            Column {
                Text(
                    text = deletedAtText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = deletionStatus,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isNearDeletion) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isNearDeletion) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        trailing = {
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.library_restore_desc, recording.name))
            }
            IconButton(onClick = onDeleteForever) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.library_delete_perm_desc, recording.name))
            }
        },
    )
}

@Composable
internal fun RecordingRow(
    title: String,
    metadata: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    urgent: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit = {},
) {
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        muted -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val content: @Composable ColumnScope.() -> Unit = {
        Column {
            ListItem(
                leadingContent = leading,
                headlineContent = {
                    Text(
                        text = title,
                        style = if (urgent) {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = when {
                            urgent -> MaterialTheme.colorScheme.onSurface
                            muted -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> contentColor
                        },
                    )
                },
                supportingContent = {
                    if (supportingContent != null) {
                        supportingContent()
                    } else {
                        Text(
                            text = metadata,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        trailing()
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            )
            expandedContent()
        }
    }
    val shape = MaterialTheme.shapes.large
    val cardModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .then(
            if (onClick != null) {
                Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    role = Role.Button,
                )
            } else {
                Modifier
            },
        )
    if (onClick == null) {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
            content = content,
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
            content = content,
        )
    }
}

private const val MillisecondsPerDay = 24L * 60L * 60L * 1_000L

internal fun remainingRecycleBinDays(
    deletedAt: Long?,
    retentionDays: Int,
    nowMillis: Long = System.currentTimeMillis(),
): Int {
    val normalizedRetentionDays = retentionDays.coerceAtLeast(1)
    if (deletedAt == null) return normalizedRetentionDays
    val permanentDeletionAt = deletedAt + normalizedRetentionDays.toLong() * MillisecondsPerDay
    val remainingMillis = permanentDeletionAt - nowMillis
    if (remainingMillis <= 0L) return 0
    return ((remainingMillis + MillisecondsPerDay - 1L) / MillisecondsPerDay).toInt()
}

internal fun isNearPermanentDeletion(remainingDays: Int): Boolean = remainingDays in 0..3

internal fun Set<Long>.toggle(id: Long): Set<Long> =
    if (id in this) this - id else this + id

@Composable
private fun InlinePlaybackPanel(
    state: PlayerUiState,
    fallbackDurationMs: Long,
    onSeek: (Long) -> Unit,
    rewindSeconds: Int,
    onSkipBack: () -> Unit,
    onToggle: () -> Unit,
    forwardSeconds: Int,
    onSkipForward: () -> Unit,
    onSpeed: (Float) -> Unit,
) {
    val durationMs = (state.durationMs.takeIf { it > 0 } ?: fallbackDurationMs).coerceAtLeast(1L)
    val positionMs = state.positionMs.coerceIn(0L, durationMs)
    var showRemainingTime by remember(state.recording?.id) { mutableStateOf(false) }
    val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
    val endTimeText = if (showRemainingTime) {
        "-${formatDuration(remainingMs)}"
    } else {
        formatDuration(durationMs)
    }
    val toggleEndTimeLabel = stringResource(R.string.player_toggle_remaining_time)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f))
        Slider(
            value = positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..durationMs.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(positionMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = endTimeText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = toggleEndTimeLabel,
                    onClick = { showRemainingTime = !showRemainingTime },
                ),
            )
        }
        TransportControls(
            isPlaying = state.isPlaying,
            rewindSeconds = rewindSeconds,
            onSkipBack = onSkipBack,
            onToggle = onToggle,
            forwardSeconds = forwardSeconds,
            onSkipForward = onSkipForward,
        )
        SpeedMenu(currentSpeed = state.speed, onSpeed = onSpeed)
        state.error?.let {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    rewindSeconds: Int,
    onSkipBack: () -> Unit,
    onToggle: () -> Unit,
    forwardSeconds: Int,
    onSkipForward: () -> Unit,
) {
    val skipBackDescription = pluralStringResource(R.plurals.player_skip_back, rewindSeconds, rewindSeconds)
    val skipForwardDescription = pluralStringResource(R.plurals.player_skip_forward, forwardSeconds, forwardSeconds)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onSkipBack,
            modifier = Modifier.semantics {
                contentDescription = skipBackDescription
            },
        ) {
            Icon(Icons.Default.FastRewind, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.player_skip_seconds_label, rewindSeconds))
        }
        Surface(
            onClick = onToggle,
            modifier = Modifier.size(58.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )
        }
        FilledTonalButton(
            onClick = onSkipForward,
            modifier = Modifier.semantics {
                contentDescription = skipForwardDescription
            },
        ) {
            Text(stringResource(R.string.player_skip_seconds_label, forwardSeconds))
            Spacer(Modifier.size(6.dp))
            Icon(Icons.Default.FastForward, contentDescription = null)
        }
    }
}

@Composable
private fun SpeedMenu(currentSpeed: Float, onSpeed: (Float) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val speedLabel = "${currentSpeed}x"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box {
            TextButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.Speed, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(speedLabel)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                    val label = "${speed}x"
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            menuOpen = false
                            onSpeed(speed)
                        },
                    )
                }
            }
        }
    }
}
