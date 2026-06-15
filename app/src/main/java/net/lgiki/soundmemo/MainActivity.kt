package net.lgiki.soundmemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.lgiki.soundmemo.ui.SoundMemoViewModelFactory
import net.lgiki.soundmemo.ui.library.LibraryScreen
import net.lgiki.soundmemo.ui.library.LibraryViewModel
import net.lgiki.soundmemo.ui.player.PlayerScreen
import net.lgiki.soundmemo.ui.recorder.RecorderScreen
import net.lgiki.soundmemo.ui.recorder.RecorderViewModel
import net.lgiki.soundmemo.ui.settings.SettingsScreen
import net.lgiki.soundmemo.ui.settings.SettingsViewModel
import net.lgiki.soundmemo.ui.theme.SoundMemoTheme
import net.lgiki.soundmemo.ui.theme.shouldUseDarkTheme
import net.lgiki.soundmemo.util.wrapWithLocale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val app = newBase.applicationContext as SoundMemoApplication
        super.attachBaseContext(newBase.wrapWithLocale(app.currentLocale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SoundMemoApplication).container
        setContent {
            val factory = SoundMemoViewModelFactory(container)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = shouldUseDarkTheme(settings)
            LaunchedEffect(settings.keepScreenAwake) {
                if (settings.keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            LaunchedEffect(Unit) {
                settingsViewModel.localeChanged.collect { tag ->
                    if (tag != null) {
                        val app = application as SoundMemoApplication
                        app.updateLocale(tag)
                        settingsViewModel.consumeLocaleChange()
                        recreate()
                    }
                }
            }
            SoundMemoTheme(settings = settings) {
                val systemBarColor = MaterialTheme.colorScheme.surface.toArgb()
                SideEffect {
                    window.statusBarColor = systemBarColor
                    window.navigationBarColor = systemBarColor
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

@Composable
private fun SoundMemoApp(
    factory: SoundMemoViewModelFactory,
    settingsViewModel: SettingsViewModel,
) {
    val navController = rememberNavController()
    val recorderViewModel: RecorderViewModel = viewModel(factory = factory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
    val destinations = listOf(
        TopLevelDestination("recorder", stringResource(R.string.nav_recorder)) { Icon(Icons.Default.Mic, contentDescription = null) },
        TopLevelDestination("library", stringResource(R.string.nav_library)) { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
        TopLevelDestination("settings", stringResource(R.string.nav_settings)) { Icon(Icons.Default.Settings, contentDescription = null) },
    )
    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = destination.icon,
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "recorder",
            modifier = Modifier.padding(padding),
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
                    onOpenPlayer = { navController.navigate("player") },
                    onStartRecording = { navController.navigate("recorder") },
                )
            }
            composable("player") {
                PlayerScreen(
                    controller = libraryViewModel.playback,
                    onBack = {
                        if (!navController.navigateUp()) {
                            navController.navigate("library") {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
            composable("settings") {
                SettingsScreen(settingsViewModel)
            }
        }
    }
}
