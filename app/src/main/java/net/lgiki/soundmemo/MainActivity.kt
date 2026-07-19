package net.lgiki.soundmemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.lgiki.soundmemo.data.settings.RecordingStorageLocation
import net.lgiki.soundmemo.domain.recorder.RecordingStateHolder
import net.lgiki.soundmemo.domain.recorder.shouldKeepScreenAwake
import net.lgiki.soundmemo.domain.recorder.isRecorderWorkflowActive
import net.lgiki.soundmemo.ui.SoundMemoViewModelFactory
import net.lgiki.soundmemo.ui.library.LibraryScreen
import net.lgiki.soundmemo.ui.library.LibraryViewModel
import net.lgiki.soundmemo.ui.library.RecycleBinScreen
import net.lgiki.soundmemo.ui.recorder.RecorderScreen
import net.lgiki.soundmemo.ui.recorder.RecorderViewModel
import net.lgiki.soundmemo.ui.settings.PrivacyScreen
import net.lgiki.soundmemo.ui.settings.SettingsScreen
import net.lgiki.soundmemo.ui.settings.SettingsViewModel
import net.lgiki.soundmemo.ui.settings.ThirdPartyLicensesScreen
import net.lgiki.soundmemo.ui.theme.SoundMemoTheme
import net.lgiki.soundmemo.ui.theme.shouldUseDarkTheme
import net.lgiki.soundmemo.util.wrapWithLocale
import net.lgiki.soundmemo.util.formatFileSize

class MainActivity : ComponentActivity() {
    private var attachedLocale: String = "system"

    override fun attachBaseContext(newBase: Context) {
        val app = newBase.applicationContext as SoundMemoApplication
        attachedLocale = app.currentLocale
        super.attachBaseContext(newBase.wrapWithLocale(app.currentLocale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SoundMemoApplication).container
        setContent {
            val factory = SoundMemoViewModelFactory(container)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val recorderState by RecordingStateHolder.state.collectAsStateWithLifecycle()
            val darkTheme = shouldUseDarkTheme(settings)
            val context = LocalContext.current
            var requestedInitialStoragePermission by remember { mutableStateOf(false) }
            val initialStoragePermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                if (granted) {
                    settingsViewModel.setRecordingStorageLocation(RecordingStorageLocation.DeviceMusic)
                } else {
                    settingsViewModel.setRecordingStorageLocation(RecordingStorageLocation.AppFiles)
                    Toast.makeText(
                        context,
                        R.string.settings_save_location_app_files_permission_denied,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
            LaunchedEffect(settings.recordingStorageLocationInitialized) {
                if (
                    !settings.recordingStorageLocationInitialized &&
                    !requestedInitialStoragePermission &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                ) {
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        settingsViewModel.setRecordingStorageLocation(RecordingStorageLocation.DeviceMusic)
                    } else {
                        requestedInitialStoragePermission = true
                        initialStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            }
            LaunchedEffect(settings.keepScreenAwake, recorderState.status) {
                if (shouldKeepScreenAwake(settings.keepScreenAwake, recorderState.status)) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            LaunchedEffect(settings.locale) {
                val app = application as SoundMemoApplication
                if (attachedLocale != settings.locale) {
                    app.updateLocale(settings.locale)
                    recreate()
                }
            }
            SoundMemoTheme(settings = settings) {
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                SoundMemoApp(
                    factory = factory,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

private val NavigationRailBreakpoint = 600.dp

@Composable
private fun SoundMemoApp(
    factory: SoundMemoViewModelFactory,
    settingsViewModel: SettingsViewModel,
) {
    val navController = rememberNavController()
    val recorderViewModel: RecorderViewModel = viewModel(factory = factory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
    val recorderState by recorderViewModel.state.collectAsStateWithLifecycle()
    val stagingFilesState by recorderViewModel.stagingFilesState.collectAsStateWithLifecycle()
    var checkedForStagingFiles by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(recorderState.status, checkedForStagingFiles) {
        if (isRecorderWorkflowActive(recorderState.status)) {
            checkedForStagingFiles = false
        } else if (!checkedForStagingFiles) {
            checkedForStagingFiles = true
            recorderViewModel.checkForAbandonedStagingFiles()
        }
    }
    val destinations = listOf(
        TopLevelDestination("recorder", stringResource(R.string.nav_recorder)) { Icon(Icons.Default.Mic, contentDescription = null) },
        TopLevelDestination("library", stringResource(R.string.nav_library)) { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
        TopLevelDestination("settings", stringResource(R.string.nav_settings)) { Icon(Icons.Default.Settings, contentDescription = null) },
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = destinations.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }
    fun navigateTo(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val content: @Composable (Modifier, Boolean) -> Unit = { modifier, parentReservesBottomNavigation ->
        NavHost(
            navController = navController,
            startDestination = "recorder",
            modifier = modifier,
        ) {
            composable("recorder") {
                val context = LocalContext.current
                val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
                fun hasPermission(permission: String): Boolean =
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                fun startRecordingWithOptionalLocation() {
                    recorderViewModel.startWithOptionalLocation(context, settings.recordLocation)
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    val audioGranted = hasPermission(Manifest.permission.RECORD_AUDIO) ||
                        result[Manifest.permission.RECORD_AUDIO] == true
                    if (audioGranted) {
                        startRecordingWithOptionalLocation()
                    }
                }
                RecorderScreen(
                    viewModel = recorderViewModel,
                    onRecordRequest = {
                        val missing = recorderViewModel.requiredPermissions().filter { !hasPermission(it) }
                        if (missing.isEmpty()) {
                            startRecordingWithOptionalLocation()
                        } else {
                            permissionLauncher.launch(missing.toTypedArray())
                        }
                    },
                )
            }
            composable("library") {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    parentReservesBottomNavigation = parentReservesBottomNavigation,
                    onStartRecording = { navController.navigate("recorder") },
                    onRecycleBinClick = { navController.navigate("recycle-bin") },
                )
            }
            composable("recycle-bin") {
                RecycleBinScreen(
                    viewModel = libraryViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    isCurrentDestination = currentDestination?.route == "settings",
                    parentReservesBottomNavigation = parentReservesBottomNavigation,
                    onPrivacyClick = { navController.navigate("privacy") },
                    onThirdPartyLicensesClick = { navController.navigate("licenses") },
                )
            }
            composable("privacy") {
                PrivacyScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable("licenses") {
                ThirdPartyLicensesScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= NavigationRailBreakpoint) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (showBottomBar) {
                    NavigationRail {
                        destinations.forEach { destination ->
                            NavigationRailItem(
                                selected = currentDestination?.hierarchy?.any {
                                    it.route == destination.route
                                } == true,
                                onClick = { navigateTo(destination) },
                                icon = destination.icon,
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
                content(Modifier.weight(1f).fillMaxSize(), false)
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets(0.dp),
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            destinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentDestination?.hierarchy?.any {
                                        it.route == destination.route
                                    } == true,
                                    onClick = { navigateTo(destination) },
                                    icon = destination.icon,
                                    label = { Text(destination.label) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                content(Modifier.padding(padding), true)
            }
        }
    }
    stagingFilesState.files?.let { files ->
        AbandonedStagingFilesDialog(
            fileCount = files.count,
            totalBytes = files.totalBytes,
            failedDeleteCount = stagingFilesState.failedDeleteCount,
            isDeleting = stagingFilesState.isDeleting,
            onDelete = recorderViewModel::deleteAbandonedStagingFiles,
            onKeep = recorderViewModel::keepAbandonedStagingFiles,
        )
    }
}

@Composable
private fun AbandonedStagingFilesDialog(
    fileCount: Int,
    totalBytes: Long,
    failedDeleteCount: Int,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
) {
    val countText = pluralStringResource(
        R.plurals.staging_files_count,
        fileCount,
        fileCount,
    )
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onKeep() },
        title = { Text(stringResource(R.string.staging_files_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.staging_files_message,
                        countText,
                        formatFileSize(totalBytes),
                    ),
                )
                if (failedDeleteCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.staging_files_delete_failed,
                            failedDeleteCount,
                            failedDeleteCount,
                        ),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.staging_files_delete),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onKeep, enabled = !isDeleting) {
                Text(stringResource(R.string.staging_files_keep))
            }
        },
    )
}
