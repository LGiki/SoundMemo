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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.settings_appearance))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(themeModeLabel(mode)) },
                    )
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.settings_dynamic_color_desc)) },
                trailingContent = {
                    Switch(checked = settings.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                },
            )

            Text(stringResource(R.string.settings_language))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to R.string.lang_system, "en" to R.string.lang_en, "zh-CN" to R.string.lang_zh_cn, "zh-TW" to R.string.lang_zh_tw).forEach { (tag, labelRes) ->
                    FilterChip(
                        selected = settings.locale == tag,
                        onClick = { viewModel.setLocale(tag) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }

            Text(stringResource(R.string.settings_recording_section))
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
                headlineContent = { Text(stringResource(R.string.settings_keep_screen_awake)) },
                trailingContent = {
                    Switch(checked = settings.keepScreenAwake, onCheckedChange = viewModel::setKeepScreenAwake)
                },
            )

            Text(stringResource(R.string.settings_playback_section))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(0.5f, 1f, 1.5f, 2f).forEach { speed ->
                    FilterChip(
                        selected = settings.playbackSpeed == speed,
                        onClick = { viewModel.setPlaybackSpeed(speed) },
                        label = { Text("${speed}x") },
                    )
                }
            }

            Text(stringResource(R.string.settings_privacy))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_local_only)) },
                supportingContent = { Text(stringResource(R.string.settings_local_only_desc)) },
            )

            Text(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about_app)) },
                supportingContent = { Text(stringResource(R.string.settings_about_desc)) },
            )
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.System -> stringResource(R.string.settings_theme_system)
    ThemeMode.Light -> stringResource(R.string.settings_theme_light)
    ThemeMode.Dark -> stringResource(R.string.settings_theme_dark)
}
