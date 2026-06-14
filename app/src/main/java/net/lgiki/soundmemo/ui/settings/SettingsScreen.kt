package net.lgiki.soundmemo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bitrateOptions by viewModel.bitrateOptions.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingsSection(title = stringResource(R.string.settings_appearance)) {
                    ChipRow {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(themeModeLabel(mode)) },
                            )
                        }
                    }
                    SettingListItem(
                        headline = stringResource(R.string.settings_dynamic_color),
                        supporting = stringResource(R.string.settings_dynamic_color_desc),
                        trailing = {
                            Switch(checked = settings.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                        },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_language)) {
                    ChipRow {
                        listOf("system" to R.string.lang_system, "en" to R.string.lang_en, "zh-CN" to R.string.lang_zh_cn, "zh-TW" to R.string.lang_zh_tw).forEach { (tag, labelRes) ->
                            FilterChip(
                                selected = settings.locale == tag,
                                onClick = { viewModel.setLocale(tag) },
                                label = { Text(stringResource(labelRes)) },
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_recording_section)) {
                    Text(
                        text = stringResource(R.string.settings_bitrate),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = bitrateOptions.range?.let { range ->
                            stringResource(R.string.settings_bitrate_device_range, range.min / 1000, range.max / 1000)
                        } ?: stringResource(R.string.settings_bitrate_common_options),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChipRow {
                        bitrateOptions.values.forEach { bitrate ->
                            FilterChip(
                                selected = settings.bitrate == bitrate,
                                onClick = { viewModel.setBitrate(bitrate) },
                                label = { Text("${bitrate / 1000} kbps") },
                            )
                        }
                    }
                    SettingListItem(
                        headline = stringResource(R.string.settings_keep_screen_awake),
                        trailing = {
                            Switch(checked = settings.keepScreenAwake, onCheckedChange = viewModel::setKeepScreenAwake)
                        },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_playback_section)) {
                    ChipRow {
                        listOf(0.5f, 1f, 1.5f, 2f).forEach { speed ->
                            FilterChip(
                                selected = settings.playbackSpeed == speed,
                                onClick = { viewModel.setPlaybackSpeed(speed) },
                                label = { Text("${speed}x") },
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_privacy)) {
                    SettingListItem(
                        headline = stringResource(R.string.settings_local_only),
                        supporting = stringResource(R.string.settings_local_only_desc),
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    SettingListItem(
                        headline = stringResource(R.string.settings_about_app),
                        supporting = stringResource(R.string.settings_about_desc),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun SettingListItem(
    headline: String,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = trailing,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    )
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.System -> stringResource(R.string.settings_theme_system)
    ThemeMode.Light -> stringResource(R.string.settings_theme_light)
    ThemeMode.Dark -> stringResource(R.string.settings_theme_dark)
}
