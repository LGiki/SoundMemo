package net.lgiki.soundmemo.ui.recorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.domain.recorder.WAVEFORM_SAMPLE_COUNT
import net.lgiki.soundmemo.util.formatDuration

private const val MIN_WAVEFORM_LEVEL = 0.08f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    onRecordRequest: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.resetSavedMessage()
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.recorder_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            RecordingStatusPanel(
                elapsedMs = state.elapsedMs,
                status = state.status,
                message = state.message,
                waveform = state.waveform,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            RecorderControls(
                status = state.status,
                onRecordRequest = onRecordRequest,
                onPause = { viewModel.pause(context) },
                onResume = { viewModel.resume(context) },
                onStop = { viewModel.stop(context) },
                onCancel = { viewModel.cancel(context) },
            )
        }
    }
}

@Composable
private fun RecordingStatusPanel(
    elapsedMs: Long,
    status: RecorderStatus,
    message: String?,
    waveform: List<Float>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusPill(status = status, message = message)
            Text(
                text = formatDuration(elapsedMs),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            RecordingWaveform(
                waveform = waveform,
                active = status == RecorderStatus.Recording,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun StatusPill(status: RecorderStatus, message: String?) {
    val isError = status == RecorderStatus.Error
    val isRecording = status == RecorderStatus.Recording
    val container = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isRecording -> MaterialTheme.colorScheme.error
        status == RecorderStatus.Saved -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isRecording -> MaterialTheme.colorScheme.onError
        status == RecorderStatus.Saved -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = when {
                    isError -> Icons.Default.Error
                    status == RecorderStatus.Saved -> Icons.Default.CheckCircle
                    else -> Icons.Default.FiberManualRecord
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = when (status) {
                    RecorderStatus.Idle -> stringResource(R.string.recorder_status_idle)
                    RecorderStatus.Recording -> stringResource(R.string.recorder_status_recording)
                    RecorderStatus.Paused -> stringResource(R.string.recorder_status_paused)
                    RecorderStatus.Saving -> stringResource(R.string.recorder_status_saving)
                    RecorderStatus.Saved -> stringResource(R.string.recorder_status_saved)
                    RecorderStatus.Error -> message ?: stringResource(R.string.recorder_status_error)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun RecorderControls(
    status: RecorderStatus,
    onRecordRequest: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (status) {
                RecorderStatus.Idle, RecorderStatus.Saved, RecorderStatus.Error -> {
                    LargeFloatingActionButton(
                        onClick = onRecordRequest,
                        modifier = Modifier.size(96.dp),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = stringResource(R.string.recorder_start),
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                RecorderStatus.Recording -> {
                    TransportButton(
                        onClick = onPause,
                        icon = { Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.recorder_pause)) },
                    )
                    StopButton(onClick = onStop)
                }
                RecorderStatus.Paused -> {
                    TransportButton(
                        onClick = onResume,
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.recorder_resume)) },
                    )
                    StopButton(onClick = onStop)
                }
                RecorderStatus.Saving -> {
                    Text(
                        text = stringResource(R.string.recorder_saving_audio),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        if (status == RecorderStatus.Recording || status == RecorderStatus.Paused) {
            OutlinedButton(onClick = onCancel) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.recorder_discard))
            }
        }
    }
}

@Composable
private fun TransportButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(68.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        icon()
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(68.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.recorder_stop_save), modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun RecordingWaveform(
    waveform: List<Float>,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val waveformDesc = stringResource(R.string.recorder_waveform_desc)
    val samples = remember(waveform) {
        if (waveform.size >= WAVEFORM_SAMPLE_COUNT) {
            waveform.takeLast(WAVEFORM_SAMPLE_COUNT)
        } else {
            List(WAVEFORM_SAMPLE_COUNT - waveform.size) { 0f } + waveform
        }
    }
    val barColor = if (active) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    }
    val restingBarColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    val centerLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.54f)

    Canvas(modifier = modifier.semantics { contentDescription = waveformDesc }) {
        val centerY = size.height / 2f
        val barCount = samples.size.coerceAtLeast(1)
        val preferredGap = 3.dp.toPx()
        val preferredMinStroke = 2.dp.toPx()
        val maxStroke = 6.dp.toPx()
        val minimumContentWidth = barCount * preferredMinStroke + (barCount - 1) * preferredGap
        val gap = if (size.width >= minimumContentWidth) {
            preferredGap
        } else {
            (size.width * 0.45f / (barCount - 1).coerceAtLeast(1)).coerceIn(0f, preferredGap)
        }
        val strokeWidth = ((size.width - gap * (barCount - 1)) / barCount)
            .coerceIn(0f, maxStroke)
        val step = strokeWidth + gap
        val contentWidth = step * (barCount - 1) + strokeWidth
        val startX = (size.width - contentWidth).coerceAtLeast(0f) / 2f + strokeWidth / 2f

        drawLine(
            color = centerLineColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )

        samples.forEachIndexed { index, sample ->
            val visibleLevel = sample.coerceIn(0f, 1f)
            val barHeight = if (visibleLevel > 0f) {
                (size.height * visibleLevel.coerceAtLeast(MIN_WAVEFORM_LEVEL)).coerceAtMost(size.height)
            } else {
                6.dp.toPx()
            }
            val x = startX + index * step
            drawLine(
                color = if (visibleLevel > 0f) barColor else restingBarColor,
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
