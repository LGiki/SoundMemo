package net.lgiki.soundmemo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lgiki.soundmemo.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.name) },
                    )
                }
            }
            ListItem(
                headlineContent = { Text("Dynamic color") },
                supportingContent = { Text("Use system colors on Android 12 and later") },
                trailingContent = {
                    Switch(checked = settings.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                },
            )
            Text("Recording")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(96_000, 128_000, 192_000).forEach { bitrate ->
                    FilterChip(
                        selected = settings.bitrate == bitrate,
                        onClick = { viewModel.setBitrate(bitrate) },
                        label = { Text("${bitrate / 1000} kbps") },
                    )
                }
            }
            ListItem(
                headlineContent = { Text("Keep screen awake while recording") },
                trailingContent = {
                    Switch(checked = settings.keepScreenAwake, onCheckedChange = viewModel::setKeepScreenAwake)
                },
            )
            Text("Playback")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(0.5f, 1f, 1.5f, 2f).forEach { speed ->
                    FilterChip(
                        selected = settings.playbackSpeed == speed,
                        onClick = { viewModel.setPlaybackSpeed(speed) },
                        label = { Text("${speed}x") },
                    )
                }
            }
            Text("Privacy")
            ListItem(
                headlineContent = { Text("Local recordings only") },
                supportingContent = { Text("SoundMemo has no account, ads, analytics, or cloud upload.") },
            )
            Text("About")
            ListItem(
                headlineContent = { Text("SoundMemo") },
                supportingContent = { Text("Open source Android voice recorder") },
            )
        }
    }
}

