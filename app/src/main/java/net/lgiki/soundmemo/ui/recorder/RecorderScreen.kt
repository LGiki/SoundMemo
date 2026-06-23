package net.lgiki.soundmemo.ui.recorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.domain.recorder.AudioInputDevice
import net.lgiki.soundmemo.domain.recorder.AudioInputPreference
import net.lgiki.soundmemo.domain.recorder.AudioInputRoute
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.domain.recorder.WAVEFORM_SAMPLE_COUNT
import net.lgiki.soundmemo.domain.recorder.matches
import net.lgiki.soundmemo.domain.recorder.normalizedAudioInputName
import net.lgiki.soundmemo.ui.audioInputLabel
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
    val preferredAudioInput by viewModel.preferredAudioInput.collectAsStateWithLifecycle()
    val audioInputDevices by viewModel.audioInputDevices.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAudioInputDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
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

                waveform = state.waveform,
                preferredAudioInput = state.preferredAudioInput ?: preferredAudioInput,
                actualAudioInput = state.actualAudioInput,
                onPreferredAudioInputClick = { showAudioInputDialog = true },
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
                onDiscardClick = { showDiscardConfirmDialog = true },
            )
        }
    }
    if (showDiscardConfirmDialog) {
        DiscardRecordingDialog(
            onConfirm = {
                showDiscardConfirmDialog = false
                viewModel.cancel(context)
            },
            onDismiss = { showDiscardConfirmDialog = false },
        )
    }
    if (showAudioInputDialog) {
        AudioInputPickerDialog(
            devices = audioInputDevices,
            selected = preferredAudioInput,
            onSelect = {
                viewModel.setPreferredAudioInput(it)
                showAudioInputDialog = false
            },
            onDismiss = { showAudioInputDialog = false },
        )
    }
}

@Composable
private fun RecordingStatusPanel(
    elapsedMs: Long,
    status: RecorderStatus,
    waveform: List<Float>,
    preferredAudioInput: AudioInputPreference?,
    actualAudioInput: AudioInputRoute?,
    onPreferredAudioInputClick: () -> Unit,
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

            Text(
                text = formatDuration(elapsedMs),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AudioInputLine(
                status = status,
                preferredAudioInput = preferredAudioInput,
                actualAudioInput = actualAudioInput,
                onPreferredAudioInputClick = onPreferredAudioInputClick,
            )
            RecordingWaveform(
                waveform = waveform,
                status = status,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun AudioInputLine(
    status: RecorderStatus,
    preferredAudioInput: AudioInputPreference?,
    actualAudioInput: AudioInputRoute?,
    onPreferredAudioInputClick: () -> Unit,
) {
    val active = status == RecorderStatus.Recording || status == RecorderStatus.Paused
    val title = if (active) {
        stringResource(R.string.recorder_audio_input_current)
    } else {
        stringResource(R.string.recorder_audio_input_preferred)
    }
    val device = when {
        active && actualAudioInput != null -> audioInputLabel(actualAudioInput.type, actualAudioInput.productName)
        active -> stringResource(R.string.recorder_audio_input_detecting)
        preferredAudioInput != null -> audioInputLabel(preferredAudioInput.type, preferredAudioInput.productName)
        else -> stringResource(R.string.settings_microphone_automatic)
    }
    val container = if (active && actualAudioInput != null) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (active && actualAudioInput != null) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val description = stringResource(R.string.recorder_audio_input_content_desc, title, device)

    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = Modifier
            .widthIn(max = 320.dp)
            .heightIn(min = 36.dp)
            .then(if (!active) Modifier.clickable(onClick = onPreferredAudioInputClick) else Modifier)
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = stringResource(R.string.recorder_audio_input_badge, title, device),
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (!active) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AudioInputPickerDialog(
    devices: List<AudioInputDevice>,
    selected: AudioInputPreference?,
    onSelect: (AudioInputPreference?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_microphone)) },
        text = {
            Column {
                AudioInputOptionRow(
                    label = stringResource(R.string.settings_microphone_automatic),
                    selected = selected == null,
                    onClick = { onSelect(null) },
                )
                devices.forEach { device ->
                    val preference = device.preference
                    AudioInputOptionRow(
                        label = audioInputLabel(device.type, device.productName),
                        selected = audioInputPreferenceSelected(
                            option = preference,
                            selected = selected,
                            devices = devices,
                        ),
                        onClick = { onSelect(preference) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_cancel))
            }
        },
    )
}

@Composable
private fun AudioInputOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun audioInputPreferenceSelected(
    option: AudioInputPreference,
    selected: AudioInputPreference?,
    devices: List<AudioInputDevice>,
): Boolean {
    if (selected == null) return false
    return if (devices.any { selected.matches(it) }) {
        option.id == selected.id &&
            option.type == selected.type &&
            normalizedAudioInputName(option.productName) == normalizedAudioInputName(selected.productName)
    } else {
        option.type == selected.type &&
            normalizedAudioInputName(option.productName) == normalizedAudioInputName(selected.productName)
    }
}


@Composable
private fun DiscardRecordingDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recorder_discard_confirm_title)) },
        text = { Text(stringResource(R.string.recorder_discard_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.recorder_discard),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_cancel))
            }
        },
    )
}

@Composable
private fun RecorderControls(
    status: RecorderStatus,
    onRecordRequest: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDiscardClick: () -> Unit,
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
            OutlinedButton(onClick = onDiscardClick) {
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
    status: RecorderStatus,
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
    val activeSampleColor = when (status) {
        RecorderStatus.Recording -> MaterialTheme.colorScheme.primary
        RecorderStatus.Paused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    }
    val restingBarColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.64f)
    val centerLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

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
                color = if (visibleLevel > 0f) activeSampleColor else restingBarColor,
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
