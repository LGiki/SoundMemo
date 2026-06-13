package net.lgiki.soundmemo.ui.recorder

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.util.formatDuration

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
        topBar = { TopAppBar(title = { Text("SoundMemo") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Waveform(
                    amplitude = state.amplitude,
                    active = state.status == RecorderStatus.Recording,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(20.dp)
                        .semantics { contentDescription = "Recording waveform visualization" },
                )
            }
            Text(
                text = formatDuration(state.elapsedMs),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = when (state.status) {
                    RecorderStatus.Idle -> "Ready to record"
                    RecorderStatus.Recording -> "Recording"
                    RecorderStatus.Paused -> "Paused"
                    RecorderStatus.Saving -> "Saving"
                    RecorderStatus.Saved -> "Saved"
                    RecorderStatus.Error -> state.message ?: "Recording error"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (state.status == RecorderStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                when (state.status) {
                    RecorderStatus.Idle, RecorderStatus.Saved, RecorderStatus.Error -> {
                        FilledIconButton(
                            onClick = onRecordRequest,
                            modifier = Modifier.size(88.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Start recording", modifier = Modifier.size(40.dp))
                        }
                    }
                    RecorderStatus.Recording -> {
                        FilledIconButton(onClick = { viewModel.pause(context) }, modifier = Modifier.size(72.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause recording")
                        }
                        FilledIconButton(onClick = { viewModel.stop(context) }, modifier = Modifier.size(72.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop and save recording")
                        }
                    }
                    RecorderStatus.Paused -> {
                        FilledIconButton(onClick = { viewModel.resume(context) }, modifier = Modifier.size(72.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume recording")
                        }
                        FilledIconButton(onClick = { viewModel.stop(context) }, modifier = Modifier.size(72.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop and save recording")
                        }
                    }
                    RecorderStatus.Saving -> Text("Saving audio...")
                }
            }
            if (state.status == RecorderStatus.Recording || state.status == RecorderStatus.Paused) {
                OutlinedButton(onClick = { viewModel.cancel(context) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Discard")
                }
            }
        }
    }
}

@Composable
private fun Waveform(amplitude: Int, active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val color = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val bars = 36
        val step = size.width / bars
        val normalized = (amplitude / 32767f).coerceIn(0.05f, 1f)
        repeat(bars) { index ->
            val wave = kotlin.math.sin((index / bars.toFloat() + phase) * Math.PI * 2).toFloat()
            val height = size.height * (0.12f + normalized * (0.25f + 0.25f * kotlin.math.abs(wave)))
            val x = step * index + step / 2
            drawLine(
                color = color,
                start = Offset(x, size.height / 2 - height / 2),
                end = Offset(x, size.height / 2 + height / 2),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

