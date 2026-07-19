package net.lgiki.soundmemo.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.lgiki.soundmemo.data.settings.AppSettings
import net.lgiki.soundmemo.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF006494),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF006874),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF97F0FF),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = Color(0xFF006B5C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF7AF8E0),
    onTertiaryContainer = Color(0xFF00201B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDCE3EE),
    onSurfaceVariant = Color(0xFF40484C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3F9),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE0E2E8),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceDim = Color(0xFFD8DAE0),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFC0C7CD),
    inverseSurface = Color(0xFF2E3035),
    inverseOnSurface = Color(0xFFEFF0F6),
    inversePrimary = Color(0xFF8FCDFF),
    surfaceTint = Color(0xFF006494),
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FCDFF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = Color(0xFF4FD8EB),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = Color(0xFF5CDBC4),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF005045),
    onTertiaryContainer = Color(0xFF7AF8E0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C7CD),
    surfaceContainerLowest = Color(0xFF0C0F13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2E),
    surfaceContainerHighest = Color(0xFF33353A),
    surfaceBright = Color(0xFF37393E),
    surfaceDim = Color(0xFF111418),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484C),
    inverseSurface = Color(0xFFE1E2E8),
    inverseOnSurface = Color(0xFF2E3035),
    inversePrimary = Color(0xFF006494),
    surfaceTint = Color(0xFF8FCDFF),
    scrim = Color.Black,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography()

@Composable
fun SoundMemoTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = shouldUseDarkTheme(settings)
    val colors = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColors else LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

@Composable
fun shouldUseDarkTheme(settings: AppSettings): Boolean = when (settings.themeMode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
