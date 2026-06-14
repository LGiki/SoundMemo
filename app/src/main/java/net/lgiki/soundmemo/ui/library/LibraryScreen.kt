package net.lgiki.soundmemo.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.data.model.RecordingSort
import net.lgiki.soundmemo.util.formatDateTime
import net.lgiki.soundmemo.util.formatDuration
import net.lgiki.soundmemo.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenPlayer: () -> Unit,
    onStartRecording: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState by viewModel.playback.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.library_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text(stringResource(R.string.library_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SortRow(state.sort, viewModel::setSort)
            if (state.recordings.isEmpty()) {
                EmptyLibrary(onStartRecording = onStartRecording, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.recordings, key = { it.id }) { recording ->
                        RecordingItem(
                            recording = recording,
                            onPlay = {
                                viewModel.play(recording)
                                onOpenPlayer()
                            },
                            onRename = { viewModel.rename(recording.id, it) },
                            onShare = { viewModel.share(it, recording) },
                            onDelete = { viewModel.delete(recording.id) },
                        )
                    }
                    if (state.deleted.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.library_recycle_bin),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
                            )
                        }
                        items(state.deleted, key = { "deleted-${it.id}" }) { recording ->
                            DeletedRecordingItem(
                                recording = recording,
                                onRestore = { viewModel.restore(recording.id) },
                                onDeleteForever = { viewModel.deletePermanently(recording.id) },
                            )
                        }
                    }
                }
            }
            playerState.recording?.let { current ->
                MiniPlayer(
                    name = current.name,
                    isPlaying = playerState.isPlaying,
                    onOpenPlayer = onOpenPlayer,
                    onToggle = viewModel.playback::toggle,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortRow(sort: RecordingSort, onSort: (RecordingSort) -> Unit) {
    val values = listOf(RecordingSort.Newest, RecordingSort.Oldest, RecordingSort.Name)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        values.forEachIndexed { index, value ->
            SegmentedButton(
                selected = sort == value,
                onClick = { onSort(value) },
                shape = SegmentedButtonDefaults.itemShape(index, values.size),
            ) {
                Text(sortLabel(value), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onStartRecording: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(12.dp))
            OutlinedButton(onClick = onStartRecording) {
                Text(stringResource(R.string.library_empty_action))
            }
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
    onPlay: () -> Unit,
    onRename: (String) -> Unit,
    onShare: (android.content.Context) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    RecordingRow(
        title = recording.name,
        metadata = "${formatDateTime(recording.createdAt)} - ${formatDuration(recording.durationMs)} - ${formatFileSize(recording.fileSizeBytes)}",
        leading = {
            FilledTonalIconButton(onClick = onPlay, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.library_play_desc, recording.name))
            }
        },
        trailing = {
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

@Composable
private fun DeletedRecordingItem(
    recording: Recording,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    RecordingRow(
        title = recording.name,
        metadata = stringResource(R.string.library_deleted_prefix, recording.deletedAt?.let(::formatDateTime).orEmpty()),
        muted = true,
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
private fun RecordingRow(
    title: String,
    metadata: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    val containerColor = if (muted) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        ListItem(
            leadingContent = leading,
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            },
            supportingContent = {
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    }
}

@Composable
private fun MiniPlayer(
    name: String,
    isPlaying: Boolean,
    onOpenPlayer: () -> Unit,
    onToggle: () -> Unit,
) {
    Card(
        onClick = onOpenPlayer,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        ListItem(
            leadingContent = {
                FilledIconButton(onClick = onToggle) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.library_play_pause_desc))
                }
            },
            headlineContent = {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(
                    if (isPlaying) stringResource(R.string.library_miniplayer_playing) else stringResource(R.string.library_miniplayer_paused),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
                supportingColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
    }
}
