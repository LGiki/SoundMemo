package net.lgiki.soundmemo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import net.lgiki.soundmemo.data.settings.AppSettings
import net.lgiki.soundmemo.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B5877),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E4F1),
    onPrimaryContainer = Color(0xFF0F3148),
    secondary = Color(0xFF56606A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE4EA),
    onSecondaryContainer = Color(0xFF14212B),
    tertiary = Color(0xFF7B5733),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC0),
    onTertiaryContainer = Color(0xFF2C1703),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFD),
    onBackground = Color(0xFF191C1F),
    surface = Color(0xFFFCFCFD),
    onSurface = Color(0xFF191C1F),
    surfaceVariant = Color(0xFFE0E3E8),
    onSurfaceVariant = Color(0xFF43484E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F7F9),
    surfaceContainer = Color(0xFFF0F2F4),
    surfaceContainerHigh = Color(0xFFEAECEF),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA2C9E8),
    onPrimary = Color(0xFF00344E),
    primaryContainer = Color(0xFF164B69),
    onPrimaryContainer = Color(0xFFD4E4F1),
    secondary = Color(0xFFC0C8D0),
    onSecondary = Color(0xFF2A323A),
    secondaryContainer = Color(0xFF404850),
    onSecondaryContainer = Color(0xFFDCE4EA),
    tertiary = Color(0xFFEABF93),
    onTertiary = Color(0xFF472A0B),
    tertiaryContainer = Color(0xFF62401E),
    onTertiaryContainer = Color(0xFFFFDCC0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101214),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF101214),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF43484E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    surfaceContainerLowest = Color(0xFF0B0D0F),
    surfaceContainerLow = Color(0xFF191C1F),
    surfaceContainer = Color(0xFF1D2023),
    surfaceContainerHigh = Color(0xFF282B2E),
    outline = Color(0xFF8D9299),
    outlineVariant = Color(0xFF43484E),
)

@Composable
fun SoundMemoTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colors = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColors else LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
