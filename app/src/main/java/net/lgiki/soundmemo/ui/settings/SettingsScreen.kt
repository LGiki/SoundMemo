package net.lgiki.soundmemo.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.IconButton
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.data.settings.RecordingChannelMode
import net.lgiki.soundmemo.data.settings.RecordingStorageLocation
import net.lgiki.soundmemo.data.settings.ThemeMode
import net.lgiki.soundmemo.data.storage.RecordingNameTemplate
import net.lgiki.soundmemo.domain.recorder.AacBitrateOptions
import net.lgiki.soundmemo.domain.recorder.AudioInputDevice
import net.lgiki.soundmemo.domain.recorder.AudioInputPreference
import net.lgiki.soundmemo.domain.recorder.RecordingFormat
import net.lgiki.soundmemo.domain.recorder.matches
import net.lgiki.soundmemo.domain.recorder.matchesTypeAndName
import net.lgiki.soundmemo.domain.recorder.normalizedAudioInputName
import net.lgiki.soundmemo.ui.audioInputLabel

private const val SOURCE_REPO_URL = "https://github.com/LGiki/SoundMemo"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onPrivacyClick: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bitrateOptions by viewModel.bitrateOptions.collectAsStateWithLifecycle()
    val audioInputDevices by viewModel.audioInputDevices.collectAsStateWithLifecycle()
    var openDialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val context = LocalContext.current
    val versionCode = remember(context) {
        PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0),
        )
    }
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission()
        ) {
            viewModel.setRecordLocation(true)
        }
    }
    fun toggleRecordLocation() {
        if (settings.recordLocation) {
            viewModel.setRecordLocation(false)
        } else if (hasLocationPermission()) {
            viewModel.setRecordLocation(true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
    fun hasStoragePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted || hasStoragePermission()) {
            viewModel.setRecordingStorageLocation(RecordingStorageLocation.DeviceMusic)
        } else {
            viewModel.setRecordingStorageLocation(RecordingStorageLocation.AppFiles)
        }
    }
    fun hasCustomFolderWritePermission(uri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
    fun customFolderPath(uri: Uri): String {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        val volume = documentId?.substringBefore(':', missingDelimiterValue = "")
        val path = documentId?.substringAfter(':', missingDelimiterValue = "")?.trim('/')
        return when {
            volume == "primary" && path.isNullOrBlank() -> context.getString(R.string.settings_storage_internal)
            volume == "primary" -> context.getString(R.string.settings_storage_internal_path, path)
            !volume.isNullOrBlank() && path.isNullOrBlank() -> volume
            !volume.isNullOrBlank() -> "$volume/$path"
            else -> null
        }
            ?: uri.lastPathSegment
            ?: context.getString(R.string.settings_save_location_custom_folder)
    }
    val customFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }.isSuccess || hasCustomFolderWritePermission(uri)
            if (persisted) {
                viewModel.setCustomRecordingFolder(uri.toString(), customFolderPath(uri))
            }
        }
    }
    fun selectRecordingStorageLocation(location: RecordingStorageLocation) {
        when {
            location == RecordingStorageLocation.DeviceMusic && !hasStoragePermission() -> {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            location == RecordingStorageLocation.CustomFolder -> {
                customFolderLauncher.launch(null)
            }
            else -> viewModel.setRecordingStorageLocation(location)
        }
    }
    LaunchedEffect(settings.recordLocation) {
        if (settings.recordLocation && !hasLocationPermission()) {
            viewModel.setRecordLocation(false)
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp),
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
                        leadingIcon = Icons.Default.WbSunny,
                        headline = stringResource(R.string.settings_theme),
                        supporting = themeModeLabel(settings.themeMode),
                        onClick = { openDialog = SettingsDialog.Theme },
                    )
                    PreferenceDivider()
                    SettingListItem(
                        leadingIcon = Icons.Default.ColorLens,
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
                        leadingIcon = Icons.Default.Language,
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
                        leadingIcon = Icons.Default.AudioFile,
                        headline = stringResource(R.string.settings_file_format),
                        supporting = recordingFormatLabel(settings.recordingFormat),
                        onClick = { openDialog = SettingsDialog.RecordingFormat },
                    )
                    PreferenceDivider()
                    PreferenceRow(
                        leadingIcon = Icons.Default.Mic,
                        headline = stringResource(R.string.settings_microphone),
                        supporting = microphoneSupportingText(
                            preference = settings.preferredAudioInput,
                            devices = audioInputDevices,
                            recordingFormat = settings.recordingFormat,
                        ),
                        onClick = { openDialog = SettingsDialog.Microphone },
                    )
                    PreferenceDivider()
                    PreferenceRow(
                        leadingIcon = Icons.Default.GraphicEq,
                        headline = stringResource(R.string.settings_recording_channels),
                        supporting = recordingChannelModeLabel(settings.recordingChannelMode),
                        onClick = { openDialog = SettingsDialog.RecordingChannels },
                    )
                    PreferenceDivider()
                    PreferenceRow(
                        leadingIcon = Icons.Default.GraphicEq,
                        headline = stringResource(R.string.settings_bitrate),
                        supporting = bitrateSupportingText(
                            recordingFormat = settings.recordingFormat,
                            bitrate = settings.bitrate,
                            sampleRate = settings.sampleRate,
                        ),
                        onClick = if (settings.recordingFormat.usesCustomEncodingSettings) {
                            { openDialog = SettingsDialog.Bitrate }
                        } else {
                            null
                        },
                    )
                    PreferenceDivider()
                    PreferenceRow(
                        leadingIcon = Icons.Default.DriveFileRenameOutline,
                        headline = stringResource(R.string.settings_file_name_template),
                        supporting = settings.recordingNameTemplate,
                        onClick = { openDialog = SettingsDialog.FileNameTemplate },
                    )
                    PreferenceDivider()
                    PreferenceRow(
                        leadingIcon = Icons.Default.Folder,
                        headline = stringResource(R.string.settings_save_location),
                        supporting = recordingStorageLocationLabel(
                            location = settings.recordingStorageLocation,
                            customFolderName = settings.customRecordingFolderName,
                        ),
                        onClick = { openDialog = SettingsDialog.StorageLocation },
                    )
                    PreferenceDivider()
                    SettingListItem(
                        leadingIcon = Icons.Default.Visibility,
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
                        leadingIcon = Icons.Default.FastRewind,
                        headline = stringResource(R.string.settings_rewind_seconds),
                        supporting = pluralStringResource(
                            R.plurals.settings_skip_seconds_value,
                            settings.rewindSeconds,
                            settings.rewindSeconds,
                        ),
                        onClick = { openDialog = SettingsDialog.RewindSeconds },
                    )
                    PreferenceDivider()
                    PreferenceRow(
                        leadingIcon = Icons.Default.FastForward,
                        headline = stringResource(R.string.settings_forward_seconds),
                        supporting = pluralStringResource(
                            R.plurals.settings_skip_seconds_value,
                            settings.forwardSeconds,
                            settings.forwardSeconds,
                        ),
                        onClick = { openDialog = SettingsDialog.ForwardSeconds },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_privacy)) {
                    SettingListItem(
                        leadingIcon = Icons.Default.MyLocation,
                        headline = stringResource(R.string.settings_record_location),
                        supporting = stringResource(R.string.settings_record_location_desc),
                        onClick = ::toggleRecordLocation,
                        trailing = {
                            Switch(
                                checked = settings.recordLocation,
                                onCheckedChange = { toggleRecordLocation() },
                            )
                        },
                    )
                    PreferenceDivider()
                    SettingListItem(
                        leadingIcon = Icons.Default.EditLocationAlt,
                        headline = stringResource(R.string.settings_write_location_to_media_file),
                        supporting = stringResource(R.string.settings_write_location_to_media_file_desc),
                        onClick = {
                            if (settings.recordLocation) {
                                viewModel.setWriteLocationToMediaFile(!settings.writeLocationToMediaFile)
                            }
                        },
                        trailing = {
                            Switch(
                                checked = settings.writeLocationToMediaFile,
                                onCheckedChange = viewModel::setWriteLocationToMediaFile,
                                enabled = settings.recordLocation,
                            )
                        },
                    )
                    PreferenceDivider()
                    PreferenceRow(
                        leadingIcon = Icons.Default.Security,
                        headline = stringResource(R.string.settings_local_only),
                        supporting = stringResource(R.string.settings_privacy_desc),
                        onClick = onPrivacyClick,
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    SettingListItem(
                        leadingIcon = Icons.Default.Info,
                        headline = stringResource(R.string.settings_about_app),
                        supporting = buildString {
                            append(stringResource(R.string.settings_about_desc))
                            append("\n")
                            append(stringResource(R.string.settings_version_code, versionCode))
                        },
                    )
                    PreferenceDivider()
                    PreferenceRow(
                        leadingIcon = Icons.Default.Code,
                        headline = stringResource(R.string.settings_source_repo),
                        supporting = SOURCE_REPO_URL,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_REPO_URL)))
                        },
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
        SettingsDialog.RecordingFormat -> SingleChoiceSettingsDialog(
            title = stringResource(R.string.settings_file_format),
            options = RecordingFormat.entries.map { format ->
                SettingsOption(format, recordingFormatLabel(format))
            },
            selected = settings.recordingFormat,
            onSelect = {
                viewModel.setRecordingFormat(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.Microphone -> SingleChoiceSettingsDialog(
            title = stringResource(R.string.settings_microphone),
            options = microphoneOptions(audioInputDevices),
            selected = settings.preferredAudioInput,
            isSelected = { option ->
                microphoneOptionSelected(
                    option = option,
                    selected = settings.preferredAudioInput,
                    devices = audioInputDevices,
                )
            },
            onSelect = {
                viewModel.setPreferredAudioInput(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.RecordingChannels -> SingleChoiceSettingsDialog(
            title = stringResource(R.string.settings_recording_channels),
            options = RecordingChannelMode.entries.map { mode ->
                SettingsOption(mode, recordingChannelModeLabel(mode))
            },
            selected = settings.recordingChannelMode,
            onSelect = {
                viewModel.setRecordingChannelMode(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.Bitrate -> SingleChoiceSettingsDialog(
            title = stringResource(R.string.settings_bitrate),
            options = bitrateValuesFor(settings.recordingFormat, bitrateOptions.values).map { bitrate ->
                SettingsOption(bitrate, "${bitrate / 1000} kbps")
            },
            selected = settings.bitrate,
            onSelect = {
                viewModel.setBitrate(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.FileNameTemplate -> FileNameTemplateDialog(
            initialTemplate = settings.recordingNameTemplate,
            extension = settings.recordingFormat.extension,
            onSave = {
                viewModel.setRecordingNameTemplate(it)
                openDialog = null
            },
            onReset = {
                viewModel.resetRecordingNameTemplate()
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.StorageLocation -> SingleChoiceSettingsDialog(
            title = stringResource(R.string.settings_save_location),
            options = RecordingStorageLocation.entries.map { location ->
                SettingsOption(
                    location,
                    recordingStorageLocationLabel(
                        location = location,
                        customFolderName = settings.customRecordingFolderName,
                    ),
                )
            },
            selected = settings.recordingStorageLocation,
            onSelect = {
                selectRecordingStorageLocation(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.RewindSeconds -> SkipSecondsDialog(
            title = stringResource(R.string.settings_rewind_seconds),
            initialSeconds = settings.rewindSeconds,
            onSave = {
                viewModel.setRewindSeconds(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.ForwardSeconds -> SkipSecondsDialog(
            title = stringResource(R.string.settings_forward_seconds),
            initialSeconds = settings.forwardSeconds,
            onSave = {
                viewModel.setForwardSeconds(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_privacy)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SettingsSection(title = stringResource(R.string.privacy_claims_title)) {
                    PrivacyClaim(
                        title = stringResource(R.string.privacy_claim_no_account),
                        body = stringResource(R.string.privacy_claim_no_account_desc),
                    )
                    PreferenceDivider()
                    PrivacyClaim(
                        title = stringResource(R.string.privacy_claim_no_tracking),
                        body = stringResource(R.string.privacy_claim_no_tracking_desc),
                    )
                    PreferenceDivider()
                    PrivacyClaim(
                        title = stringResource(R.string.privacy_claim_no_cloud),
                        body = stringResource(R.string.privacy_claim_no_cloud_desc),
                    )
                    PreferenceDivider()
                    PrivacyClaim(
                        title = stringResource(R.string.privacy_claim_system_backup),
                        body = stringResource(R.string.privacy_claim_system_backup_desc),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyClaim(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun PreferenceRow(
    leadingIcon: ImageVector? = null,
    headline: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(start = 24.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            supporting?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.let {
            Row(
                modifier = Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                it()
            }
        }
    }
}

@Composable
private fun SettingListItem(
    leadingIcon: ImageVector? = null,
    headline: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    PreferenceRow(
        leadingIcon = leadingIcon,
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
    isSelected: (T) -> Boolean = { option -> option == selected },
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
                            selected = isSelected(option.value),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FileNameTemplateDialog(
    initialTemplate: String,
    extension: String,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var templateValue by remember(initialTemplate) {
        mutableStateOf(
            TextFieldValue(
                text = initialTemplate,
                selection = TextRange(initialTemplate.length),
            ),
        )
    }
    val template = templateValue.text
    val previewNow = remember { System.currentTimeMillis() }
    val tokens = remember(previewNow) {
        RecordingNameTemplate.supportedTokens.map { token ->
            FileNameTemplateToken(
                value = "{$token}",
                preview = RecordingNameTemplate.previewToken(token, now = previewNow),
            )
        }
    }
    val unknownTokens = remember(template) { RecordingNameTemplate.unknownTokens(template) }
    val isValid = unknownTokens.isEmpty()
    val preview = remember(template, previewNow) {
        RecordingNameTemplate.preview(template, now = previewNow)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_file_name_template)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = templateValue,
                    onValueChange = { templateValue = it },
                    label = { Text(stringResource(R.string.settings_file_name_template_label)) },
                    singleLine = true,
                    isError = !isValid,
                    supportingText = if (!isValid) {
                        {
                            Text(
                                stringResource(
                                    R.string.settings_file_name_template_unknown_tokens,
                                    unknownTokens.joinToString(", ") { "{$it}" },
                                ),
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                FileNameTemplateTokenPicker(
                    tokens = tokens,
                    onTokenClick = { token ->
                        val selectionStart = templateValue.selection.min
                        val selectionEnd = templateValue.selection.max
                        val newText = buildString {
                            append(template.substring(0, selectionStart))
                            append(token.value)
                            append(template.substring(selectionEnd))
                        }
                        val cursor = selectionStart + token.value.length
                        templateValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(cursor),
                        )
                    },
                )
                if (isValid) {
                    FileNameTemplatePreview(preview, extension)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(templateValue.text) },
                enabled = isValid,
            ) {
                Text(stringResource(R.string.library_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.settings_file_name_template_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.library_cancel))
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FileNameTemplateTokenPicker(
    tokens: List<FileNameTemplateToken>,
    onTokenClick: (FileNameTemplateToken) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_file_name_template_insert_token),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            tokens.forEach { token ->
                AssistChip(
                    onClick = { onTokenClick(token) },
                    label = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.widthIn(min = 88.dp),
                        ) {
                            Text(
                                text = token.value,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = token.preview,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FileNameTemplatePreview(preview: String, extension: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_file_name_template_preview_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.settings_file_name_template_preview, preview, extension),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private data class FileNameTemplateToken(
    val value: String,
    val preview: String,
)

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.System -> stringResource(R.string.settings_theme_system)
    ThemeMode.Light -> stringResource(R.string.settings_theme_light)
    ThemeMode.Dark -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun recordingFormatLabel(format: RecordingFormat): String = when (format) {
    RecordingFormat.M4a -> stringResource(R.string.settings_file_format_m4a)
    RecordingFormat.Aac -> stringResource(R.string.settings_file_format_aac)
    RecordingFormat.ThreeGp -> stringResource(R.string.settings_file_format_3gp)
    RecordingFormat.Wav -> stringResource(R.string.settings_file_format_wav)
    RecordingFormat.Mp3 -> stringResource(R.string.settings_file_format_mp3)
}

@Composable
private fun recordingChannelModeLabel(mode: RecordingChannelMode): String = when (mode) {
    RecordingChannelMode.Mono -> stringResource(R.string.settings_recording_channels_mono)
    RecordingChannelMode.Stereo -> stringResource(R.string.settings_recording_channels_stereo)
}

@Composable
private fun recordingStorageLocationLabel(
    location: RecordingStorageLocation,
    customFolderName: String? = null,
): String = when (location) {
    RecordingStorageLocation.AppFiles -> stringResource(R.string.settings_save_location_app_files)
    RecordingStorageLocation.DeviceMusic -> stringResource(R.string.settings_save_location_device_music)
    RecordingStorageLocation.CustomFolder -> if (customFolderName.isNullOrBlank()) {
        stringResource(R.string.settings_save_location_custom_folder)
    } else {
        stringResource(R.string.settings_save_location_custom_folder_named, customFolderName)
    }
}

private fun bitrateValuesFor(recordingFormat: RecordingFormat, deviceAacValues: List<Int>): List<Int> =
    if (recordingFormat.usesAacBitrateRange) {
        deviceAacValues
    } else {
        AacBitrateOptions.fallbackValues
    }

@Composable
private fun microphoneOptions(devices: List<AudioInputDevice>): List<SettingsOption<AudioInputPreference?>> {
    val automatic = SettingsOption<AudioInputPreference?>(
        value = null,
        label = stringResource(R.string.settings_microphone_automatic),
    )
    return listOf(automatic) + devices.map { device ->
        SettingsOption<AudioInputPreference?>(
            value = device.preference,
            label = audioInputLabel(type = device.type, productName = device.productName),
        )
    }
}

@Composable
private fun microphoneSupportingText(
    preference: AudioInputPreference?,
    devices: List<AudioInputDevice>,
    recordingFormat: RecordingFormat,
): String {
    val selectedText = when {
        preference == null -> stringResource(R.string.settings_microphone_automatic)
        devices.none { preference.matchesTypeAndName(it) } -> stringResource(
            R.string.settings_microphone_unavailable,
            audioInputLabel(type = preference.type, productName = preference.productName),
        )
        else -> audioInputLabel(type = preference.type, productName = preference.productName)
    }
    val compatibilityNote = Build.VERSION.SDK_INT < Build.VERSION_CODES.P && !recordingFormat.usesPcmRecorder
    return if (compatibilityNote) {
        "$selectedText\n${stringResource(R.string.settings_microphone_mediarecorder_compat)}"
    } else {
        selectedText
    }
}

private fun microphoneOptionSelected(
    option: AudioInputPreference?,
    selected: AudioInputPreference?,
    devices: List<AudioInputDevice>,
): Boolean = when {
    option == null || selected == null -> option == selected
    devices.any { selected.matches(it) } -> option.id == selected.id &&
        option.type == selected.type &&
        normalizedAudioInputName(option.productName) == normalizedAudioInputName(selected.productName)
    else -> option.type == selected.type &&
        normalizedAudioInputName(option.productName) == normalizedAudioInputName(selected.productName)
}

@Composable
private fun bitrateSupportingText(
    recordingFormat: RecordingFormat,
    bitrate: Int,
    sampleRate: Int,
): String {
    if (!recordingFormat.usesCustomEncodingSettings) {
        return stringResource(
            R.string.settings_bitrate_fixed_by_format,
            recordingFormatLabel(recordingFormat),
            recordingFormat.bitrateFor(bitrate) / 1000,
            recordingFormat.sampleRateFor(sampleRate) / 1000,
        )
    }
    return "${bitrate / 1000} kbps"
}

@Composable
private fun languageOptions(): List<SettingsOption<String>> = listOf(
    SettingsOption("system", stringResource(R.string.lang_system)),
    SettingsOption("en", stringResource(R.string.lang_en)),
    SettingsOption("zh-CN", stringResource(R.string.lang_zh_cn)),
    SettingsOption("zh-TW", stringResource(R.string.lang_zh_tw)),
)

@Composable
private fun SkipSecondsDialog(
    title: String,
    initialSeconds: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var seconds by remember(initialSeconds) { mutableIntStateOf(initialSeconds.coerceIn(MIN_SKIP_SECONDS, MAX_SKIP_SECONDS)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = pluralStringResource(R.plurals.settings_skip_seconds_value, seconds, seconds),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Slider(
                    value = seconds.toFloat(),
                    onValueChange = { seconds = it.roundToInt().coerceIn(MIN_SKIP_SECONDS, MAX_SKIP_SECONDS) },
                    valueRange = MIN_SKIP_SECONDS.toFloat()..MAX_SKIP_SECONDS.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    skipSnapPoints().chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            row.forEach { snapSeconds ->
                                TextButton(
                                    onClick = { seconds = snapSeconds },
                                    modifier = Modifier.widthIn(min = 72.dp),
                                ) {
                                    Text(stringResource(R.string.player_skip_seconds_label, snapSeconds))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(seconds) }) {
                Text(stringResource(R.string.library_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_cancel))
            }
        },
    )
}

private fun skipSnapPoints(): List<Int> = listOf(1, 5, 10, 15, 30, 60)

private const val MIN_SKIP_SECONDS = 1
private const val MAX_SKIP_SECONDS = 60

private enum class SettingsDialog {
    Theme,
    Language,
    RecordingFormat,
    Microphone,
    RecordingChannels,
    Bitrate,
    FileNameTemplate,
    StorageLocation,
    RewindSeconds,
    ForwardSeconds,
}

private data class SettingsOption<T>(
    val value: T,
    val label: String,
)
