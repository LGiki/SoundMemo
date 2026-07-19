package net.lgiki.soundmemo.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.ui.AdaptiveContent
import net.lgiki.soundmemo.ui.SoundMemoScaffold

@Composable
fun RecycleBinScreen(
    viewModel: LibraryViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingPermanentDeletion by remember { mutableStateOf<Recording?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    SoundMemoScaffold(
        title = { Text(stringResource(R.string.library_recycle_bin)) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.library_back),
                )
            }
        },
    ) { padding ->
        AdaptiveContent(padding = padding, maxContentWidth = 960.dp) {
            if (state.deleted.isEmpty()) {
                EmptyRecycleBin()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.deleted, key = { it.id }) { recording ->
                        DeletedRecordingItem(
                            recording = recording,
                            retentionDays = state.recycleRetentionDays,
                            nowMillis = nowMillis,
                            onRestore = { viewModel.restore(recording.id) },
                            onDeleteForever = { pendingPermanentDeletion = recording },
                        )
                    }
                }
            }
        }
    }

    pendingPermanentDeletion?.let { recording ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDeletion = null },
            title = { Text(stringResource(R.string.library_delete_permanently_title)) },
            text = { Text(stringResource(R.string.library_delete_permanently_message, recording.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePermanently(recording.id)
                        pendingPermanentDeletion = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.library_delete_permanently_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentDeletion = null }) {
                    Text(stringResource(R.string.library_cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyRecycleBin() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp),
                )
            }
            Spacer(Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.library_recycle_bin_empty),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.library_recycle_bin_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
