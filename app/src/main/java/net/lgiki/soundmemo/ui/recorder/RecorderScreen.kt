package net.lgiki.soundmemo.ui.recorder

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.ui.AdaptiveContent
import net.lgiki.soundmemo.ui.SingleChoiceDialog
import net.lgiki.soundmemo.ui.SoundMemoScaffold
import net.lgiki.soundmemo.data.settings.RecorderVisualization
import net.lgiki.soundmemo.data.settings.VuMeterValueDisplay
import net.lgiki.soundmemo.domain.recorder.AudioInputDevice
import net.lgiki.soundmemo.domain.recorder.AudioInputPreference
import net.lgiki.soundmemo.domain.recorder.AudioInputRoute
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.domain.recorder.WAVEFORM_SAMPLE_COUNT
import net.lgiki.soundmemo.domain.recorder.matches
import net.lgiki.soundmemo.domain.recorder.normalizedAudioInputName
import net.lgiki.soundmemo.ui.audioInputLabel
import net.lgiki.soundmemo.util.formatDuration
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val MIN_WAVEFORM_LEVEL = 0.08f
private const val VU_SEGMENT_COUNT = 20
private const val MAX_VU_AMPLITUDE = 32767f
private const val STARTING_INDICATOR_DELAY_MS = 200L

@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    onRecordRequest: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferredAudioInput by viewModel.preferredAudioInput.collectAsStateWithLifecycle()
    val recorderVisualization by viewModel.recorderVisualization.collectAsStateWithLifecycle()
    val vuMeterValueDisplay by viewModel.vuMeterValueDisplay.collectAsStateWithLifecycle()
    val audioInputDevices by viewModel.audioInputDevices.collectAsStateWithLifecycle()
    val presentedStatus = rememberPresentedRecorderStatus(state.status)
    var showAudioInputDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }
    SoundMemoScaffold(
        title = { Text(stringResource(R.string.recorder_title)) },
        forceCompactAppBar = true,
        bottomBar = {
            RecorderActionBar {
                RecorderControls(
                    status = state.status,
                    presentedStatus = presentedStatus,
                    onRecordRequest = onRecordRequest,
                    onPause = { viewModel.pause(context) },
                    onResume = { viewModel.resume(context) },
                    onStop = { viewModel.stop(context) },
                    onDiscardClick = { showDiscardConfirmDialog = true },
                )
            }
        },
    ) { padding ->
        AdaptiveContent(padding = padding) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                RecordingStatusPanel(
                    elapsedMs = state.elapsedMs,
                    status = presentedStatus,
                    amplitude = state.amplitude,
                    waveform = state.waveform,
                    recorderVisualization = recorderVisualization,
                    vuMeterValueDisplay = vuMeterValueDisplay,
                    preferredAudioInput = state.preferredAudioInput ?: preferredAudioInput,
                    actualAudioInput = state.actualAudioInput,
                    onPreferredAudioInputClick = { showAudioInputDialog = true },
                    onRecorderVisualizationChange = viewModel::setRecorderVisualization,
                    onVuMeterValueClick = viewModel::cycleVuMeterValueDisplay,
                    modifier = Modifier.fillMaxSize(),
                )
            }
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
private fun rememberPresentedRecorderStatus(status: RecorderStatus): RecorderStatus {
    var presentedStatus by remember {
        mutableStateOf(if (status == RecorderStatus.Starting) RecorderStatus.Idle else status)
    }
    LaunchedEffect(status) {
        if (status == RecorderStatus.Starting) {
            delay(STARTING_INDICATOR_DELAY_MS)
        }
        presentedStatus = status
    }
    return presentedStatus
}

@Composable
private fun RecorderActionBar(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.widthIn(max = 1_200.dp).fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun RecordingStatusPanel(
    elapsedMs: Long,
    status: RecorderStatus,
    amplitude: Int,
    waveform: List<Float>,
    recorderVisualization: RecorderVisualization,
    vuMeterValueDisplay: VuMeterValueDisplay,
    preferredAudioInput: AudioInputPreference?,
    actualAudioInput: AudioInputRoute?,
    onPreferredAudioInputClick: () -> Unit,
    onRecorderVisualizationChange: (RecorderVisualization) -> Unit,
    onVuMeterValueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compactHeight = maxHeight < 260.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = if (compactHeight) 10.dp else 20.dp),
            ) {
                RecordingHeaderRow(
                    elapsedMs = elapsedMs,
                    status = status,
                    recorderVisualization = recorderVisualization,
                    onRecorderVisualizationChange = onRecorderVisualizationChange,
                    compact = compactHeight,
                )
                AudioInputLine(
                    status = status,
                    preferredAudioInput = preferredAudioInput,
                    actualAudioInput = actualAudioInput,
                    onPreferredAudioInputClick = onPreferredAudioInputClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (compactHeight) 4.dp else 12.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = if (compactHeight) 4.dp else 20.dp),
                ) {
                    when (recorderVisualization) {
                        RecorderVisualization.Waveform -> RecordingWaveform(
                            waveform = waveform,
                            status = status,
                            modifier = Modifier.fillMaxSize(),
                        )
                        RecorderVisualization.VuMeter -> RecordingVuMeter(
                            amplitude = amplitude,
                            status = status,
                            valueDisplay = vuMeterValueDisplay,
                            onValueClick = onVuMeterValueClick,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingHeaderRow(
    elapsedMs: Long,
    status: RecorderStatus,
    recorderVisualization: RecorderVisualization,
    onRecorderVisualizationChange: (RecorderVisualization) -> Unit,
    compact: Boolean,
) {
    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordingStatusBadge(status)
            Text(
                text = formatDuration(elapsedMs),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            RecorderVisualizationIconButton(
                visualization = recorderVisualization,
                onVisualizationChange = onRecorderVisualizationChange,
            )
        }
        return
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecordingStatusBadge(status)
            Text(
                text = formatDuration(elapsedMs),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        RecorderVisualizationIconButton(
            visualization = recorderVisualization,
            onVisualizationChange = onRecorderVisualizationChange,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun RecordingStatusBadge(status: RecorderStatus) {
    val label = when (status) {
        RecorderStatus.Idle -> stringResource(R.string.recorder_status_idle)
        RecorderStatus.Starting -> stringResource(R.string.notification_text_starting)
        RecorderStatus.Recording -> stringResource(R.string.recorder_status_recording)
        RecorderStatus.Paused -> stringResource(R.string.recorder_status_paused)
        RecorderStatus.Saving -> stringResource(R.string.recorder_status_saving)
        RecorderStatus.Saved -> stringResource(R.string.recorder_status_saved)
        RecorderStatus.Error -> stringResource(R.string.recorder_status_error)
    }
    val isErrorState = status == RecorderStatus.Recording || status == RecorderStatus.Error
    val containerColor = when {
        isErrorState -> MaterialTheme.colorScheme.errorContainer
        status == RecorderStatus.Paused -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        isErrorState -> MaterialTheme.colorScheme.onErrorContainer
        status == RecorderStatus.Paused -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status == RecorderStatus.Recording) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    content = {},
                )
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun AudioInputLine(
    status: RecorderStatus,
    preferredAudioInput: AudioInputPreference?,
    actualAudioInput: AudioInputRoute?,
    onPreferredAudioInputClick: () -> Unit,
    modifier: Modifier = Modifier,
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

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = container,
            contentColor = content,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .heightIn(min = 48.dp)
                .then(
                    if (!active) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = onPreferredAudioInputClick,
                        )
                    } else {
                        Modifier
                    },
                )
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
}

@Composable
private fun RecorderVisualizationIconButton(
    visualization: RecorderVisualization,
    onVisualizationChange: (RecorderVisualization) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showingVuMeter = visualization == RecorderVisualization.VuMeter
    val contentDescription = stringResource(
        if (showingVuMeter) {
            R.string.recorder_switch_to_waveform
        } else {
            R.string.recorder_switch_to_vu_meter
        },
    )

    IconButton(
        onClick = {
            onVisualizationChange(
                if (showingVuMeter) RecorderVisualization.Waveform else RecorderVisualization.VuMeter,
            )
        },
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            imageVector = if (showingVuMeter) {
                Icons.AutoMirrored.Filled.ShowChart
            } else {
                Icons.Default.GraphicEq
            },
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AudioInputPickerDialog(
    devices: List<AudioInputDevice>,
    selected: AudioInputPreference?,
    onSelect: (AudioInputPreference?) -> Unit,
    onDismiss: () -> Unit,
) {
    val automaticLabel = stringResource(R.string.settings_microphone_automatic)
    val options = listOf<AudioInputDevice?>(null) + devices
    SingleChoiceDialog(
        title = stringResource(R.string.settings_microphone),
        options = options,
        optionLabel = { device ->
            device?.let { audioInputLabel(it.type, it.productName) } ?: automaticLabel
        },
        isSelected = { device ->
            device?.let {
                audioInputPreferenceSelected(
                    option = it.preference,
                    selected = selected,
                    devices = devices,
                )
            } ?: (selected == null)
        },
        onSelect = { device -> onSelect(device?.preference) },
        dismissLabel = stringResource(R.string.library_cancel),
        onDismiss = onDismiss,
    )
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
    presentedStatus: RecorderStatus,
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
            if (status == RecorderStatus.Starting && presentedStatus != RecorderStatus.Starting) {
                Spacer(Modifier.size(96.dp))
                return@Row
            }
            when (presentedStatus) {
                RecorderStatus.Idle, RecorderStatus.Saved, RecorderStatus.Error -> {
                    LargeFloatingActionButton(
                        onClick = onRecordRequest,
                        modifier = Modifier.size(96.dp),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = stringResource(R.string.recorder_start),
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                RecorderStatus.Starting -> {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.notification_text_starting),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
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
        if (presentedStatus == RecorderStatus.Recording || presentedStatus == RecorderStatus.Paused) {
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
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
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
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
    ) {
        Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.recorder_stop_save), modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun RecordingVuMeter(
    amplitude: Int,
    status: RecorderStatus,
    valueDisplay: VuMeterValueDisplay,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vuMeterDesc = stringResource(R.string.recorder_vu_meter_desc)
    val linearLevel = if (status == RecorderStatus.Recording) {
        (amplitude / MAX_VU_AMPLITUDE).coerceIn(0f, 1f)
    } else {
        0f
    }
    val level = sqrt(linearLevel).coerceIn(0f, 1f)
    val valueText = vuMeterValueText(
        display = valueDisplay,
        linearLevel = linearLevel,
        visibleLevel = level,
    )
    val valueDescription = stringResource(R.string.recorder_vu_meter_value_desc, valueText)
    val activeSegmentCount = if (level > 0f) {
        (level * VU_SEGMENT_COUNT).toInt().coerceIn(1, VU_SEGMENT_COUNT)
    } else {
        0
    }
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
    val lowColor = MaterialTheme.colorScheme.primary
    val mediumColor = MaterialTheme.colorScheme.tertiary
    val highColor = MaterialTheme.colorScheme.error

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable(onClick = onValueClick)
                .semantics { contentDescription = valueDescription },
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics { contentDescription = vuMeterDesc },
        ) {
            val gap = 4.dp.toPx()
            val segmentWidth = ((size.width - gap * (VU_SEGMENT_COUNT - 1)) / VU_SEGMENT_COUNT).coerceAtLeast(0f)
            val meterHeight = 44.dp.toPx().coerceAtMost(size.height)
            val top = (size.height - meterHeight) / 2f
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

            repeat(VU_SEGMENT_COUNT) { index ->
                val x = index * (segmentWidth + gap)
                val color = if (index < activeSegmentCount) {
                    vuSegmentColor(
                        index = index,
                        lowColor = lowColor,
                        mediumColor = mediumColor,
                        highColor = highColor,
                    )
                } else {
                    inactiveColor
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, top),
                    size = Size(segmentWidth, meterHeight),
                    cornerRadius = cornerRadius,
                )
            }
        }
    }
}

@Composable
private fun vuMeterValueText(
    display: VuMeterValueDisplay,
    linearLevel: Float,
    visibleLevel: Float,
): String {
    val percent = (visibleLevel * 100).roundToInt().coerceIn(0, 100)
    val decibels = if (linearLevel > 0f) {
        (20f * log10(linearLevel)).roundToInt().coerceAtLeast(-120)
    } else {
        null
    }
    val percentText = stringResource(R.string.recorder_vu_meter_value_percent, percent)
    val decibelText = decibels?.let {
        stringResource(R.string.recorder_vu_meter_value_db, it)
    } ?: stringResource(R.string.recorder_vu_meter_value_db_silent)
    return when (display) {
        VuMeterValueDisplay.Percent -> percentText
        VuMeterValueDisplay.Decibels -> decibelText
        VuMeterValueDisplay.PercentAndDecibels -> stringResource(
            R.string.recorder_vu_meter_value_combined,
            percentText,
            decibelText,
        )
    }
}

private fun vuSegmentColor(
    index: Int,
    lowColor: Color,
    mediumColor: Color,
    highColor: Color,
): Color = when {
    index >= 17 -> highColor
    index >= 13 -> mediumColor
    else -> lowColor
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
