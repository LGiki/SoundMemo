package net.lgiki.soundmemo.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.data.model.Recording
import net.lgiki.soundmemo.domain.player.PlaybackController
import net.lgiki.soundmemo.util.formatDateTime
import net.lgiki.soundmemo.util.formatDuration
import net.lgiki.soundmemo.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(controller: PlaybackController) {
    val state by controller.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.player_title)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val recording = state.recording
            if (recording == null) {
                EmptyPlayer(modifier = Modifier.fillMaxSize())
            } else {
                RecordingHeader(recording = recording, modifier = Modifier.fillMaxWidth())
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Slider(
                            value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1L).toFloat()),
                            onValueChange = { controller.seekTo(it.toLong()) },
                            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelLarge)
                            Text(formatDuration(state.durationMs.takeIf { it > 0 } ?: recording.durationMs), style = MaterialTheme.typography.labelLarge)
                        }
                        TransportControls(
                            isPlaying = state.isPlaying,
                            onSkipBack = { controller.skipBy(-10_000) },
                            onToggle = controller::toggle,
                            onSkipForward = { controller.skipBy(10_000) },
                        )
                        SpeedRow(currentSpeed = state.speed, onSpeed = controller::setSpeed)
                    }
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EmptyPlayer(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.player_empty), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun RecordingHeader(recording: Recording, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            recording.name,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${formatDateTime(recording.createdAt)} - ${recording.format.uppercase()} - ${formatFileSize(recording.fileSizeBytes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    onSkipBack: () -> Unit,
    onToggle: () -> Unit,
    onSkipForward: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onSkipBack, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.Replay10, contentDescription = stringResource(R.string.player_skip_back))
        }
        FloatingActionButton(
            onClick = onToggle,
            modifier = Modifier.size(72.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                modifier = Modifier.size(34.dp),
            )
        }
        IconButton(onClick = onSkipForward, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.Forward10, contentDescription = stringResource(R.string.player_skip_forward))
        }
    }
}

@Composable
private fun SpeedRow(currentSpeed: Float, onSpeed: (Float) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
            FilterChip(
                selected = currentSpeed == speed,
                onClick = { onSpeed(speed) },
                label = { Text("${speed}x") },
            )
        }
    }
    Spacer(Modifier.size(2.dp))
}
