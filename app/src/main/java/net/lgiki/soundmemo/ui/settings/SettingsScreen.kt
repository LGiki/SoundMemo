package net.lgiki.soundmemo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bitrateOptions by viewModel.bitrateOptions.collectAsStateWithLifecycle()
    var openDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SettingsSection(title = stringResource(R.string.settings_appearance)) {
                    PreferenceRow(
                        headline = stringResource(R.string.settings_theme),
                        supporting = themeModeLabel(settings.themeMode),
                        onClick = { openDialog = SettingsDialog.Theme },
                    )
                    PreferenceDivider()
                    SettingListItem(
                        headline = stringResource(R.string.settings_dynamic_color),
                        supporting = stringResource(R.string.settings_dynamic_color_desc),
                        onClick = { viewModel.setDynamicColor(!settings.dynamicColor) },
                        trailing = {
                            Switch(checked = settings.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                        },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_language)) {
                    PreferenceRow(
                        headline = stringResource(R.string.settings_language),
                        supporting = languageOptions()
                            .firstOrNull { it.value == settings.locale }
                            ?.label ?: stringResource(R.string.lang_system),
                        onClick = { openDialog = SettingsDialog.Language },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_recording_section)) {
                    PreferenceRow(
                        headline = stringResource(R.string.settings_bitrate),
                        supporting = buildString {
                            append("${settings.bitrate / 1000} kbps")
                            append("\n")
                            append(
                                bitrateOptions.range?.let { range ->
                                    stringResource(R.string.settings_bitrate_device_range, range.min / 1000, range.max / 1000)
                                } ?: stringResource(R.string.settings_bitrate_common_options),
                            )
                        },
                        onClick = { openDialog = SettingsDialog.Bitrate },
                    )
                    PreferenceDivider()
                    SettingListItem(
                        headline = stringResource(R.string.settings_keep_screen_awake),
                        onClick = { viewModel.setKeepScreenAwake(!settings.keepScreenAwake) },
                        trailing = {
                            Switch(checked = settings.keepScreenAwake, onCheckedChange = viewModel::setKeepScreenAwake)
                        },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_playback_section)) {
                    PreferenceRow(
                        headline = stringResource(R.string.settings_playback_speed),
                        supporting = "${settings.playbackSpeed}x",
                        onClick = { openDialog = SettingsDialog.PlaybackSpeed },
                    )
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

    when (openDialog) {
        SettingsDialog.Theme -> SingleChoiceSettingsDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { mode ->
                SettingsOption(mode, themeModeLabel(mode))
            },
            selected = settings.themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.Language -> SingleChoiceSettingsDialog(
            title = stringResource(R.string.settings_language),
            options = languageOptions(),
            selected = settings.locale,
            onSelect = {
                viewModel.setLocale(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.Bitrate -> SingleChoiceSettingsDialog(
            title = stringResource(R.string.settings_bitrate),
            options = bitrateOptions.values.map { bitrate ->
                SettingsOption(bitrate, "${bitrate / 1000} kbps")
            },
            selected = settings.bitrate,
            onSelect = {
                viewModel.setBitrate(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.PlaybackSpeed -> SingleChoiceSettingsDialog(
            title = stringResource(R.string.settings_playback_speed),
            options = listOf(0.5f, 1f, 1.5f, 2f).map { speed ->
                SettingsOption(speed, "${speed}x")
            },
            selected = settings.playbackSpeed,
            onSelect = {
                viewModel.setPlaybackSpeed(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            content = content,
        )
    }
}

@Composable
private fun PreferenceDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 24.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun PreferenceRow(
    headline: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        headlineContent = { Text(headline) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = trailing?.let {
            {
                Row(horizontalArrangement = Arrangement.End) {
                    it()
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun SettingListItem(
    headline: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    PreferenceRow(
        headline = headline,
        supporting = supporting,
        onClick = onClick,
        trailing = trailing?.let { { trailing() } },
    )
}

@Composable
private fun <T> SingleChoiceSettingsDialog(
    title: String,
    options: List<SettingsOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.value) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option.value == selected,
                            onClick = { onSelect(option.value) },
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
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
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.System -> stringResource(R.string.settings_theme_system)
    ThemeMode.Light -> stringResource(R.string.settings_theme_light)
    ThemeMode.Dark -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun languageOptions(): List<SettingsOption<String>> = listOf(
    SettingsOption("system", stringResource(R.string.lang_system)),
    SettingsOption("en", stringResource(R.string.lang_en)),
    SettingsOption("zh-CN", stringResource(R.string.lang_zh_cn)),
    SettingsOption("zh-TW", stringResource(R.string.lang_zh_tw)),
)

private enum class SettingsDialog {
    Theme,
    Language,
    Bitrate,
    PlaybackSpeed,
}

private data class SettingsOption<T>(
    val value: T,
    val label: String,
)
