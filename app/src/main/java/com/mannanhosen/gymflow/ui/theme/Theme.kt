package com.mannanhosen.gymflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** App-level theme preference. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Semantic status colors. Passed through CompositionLocal so screens never
 * hardcode status colors and always adapt to the active theme.
 */
@Immutable
data class StatusColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val info: Color,
    val onInfo: Color
)

val LightStatusColors = StatusColors(
    success = SuccessGreen,
    onSuccess = Color.White,
    warning = WarningAmber,
    onWarning = Charcoal,
    error = ErrorRed,
    onError = Color.White,
    info = InfoBlue,
    onInfo = Color.White
)

val DarkStatusColors = StatusColors(
    success = SuccessGreenDark,
    onSuccess = Charcoal,
    warning = WarningAmberDark,
    onWarning = Charcoal,
    error = ErrorRedDark,
    onError = Charcoal,
    info = InfoBlueDark,
    onInfo = Charcoal
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

private val LightColors = lightColorScheme(
    primary = Lime,
    onPrimary = Charcoal,
    primaryContainer = LimeContainer,
    onPrimaryContainer = OnLimeContainerLight,
    secondary = Graphite,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = OnSurfaceLight,
    tertiary = InfoBlue,
    onTertiary = Color.White,
    background = SoftWhite,
    onBackground = OnSurfaceLight,
    surface = SurfaceWhite,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF44474E),
    outline = OutlineLight,
    outlineVariant = Color(0xFFC4C6CF),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Graphite,
    inverseOnSurface = Color(0xFFF1F1F4),
    inversePrimary = LimeDark
)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Charcoal,
    primaryContainer = LimeContainerDark,
    onPrimaryContainer = OnLimeContainerDark,
    secondary = Color(0xFFC7C6D0),
    onSecondary = Color(0xFF31312F),
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = InfoBlueDark,
    onTertiary = Color(0xFF00344F),
    background = NearBlack,
    onBackground = OnSurfaceDark,
    surface = NearBlack,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC4C6CF),
    outline = OutlineDark,
    outlineVariant = Color(0xFF44474E),
    error = ErrorRedDark,
    onError = Charcoal,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = OnSurfaceDark,
    inverseOnSurface = Graphite,
    inversePrimary = LimeDark
)

/**
 * GymFlow root theme. Applies Material 3 color roles, shapes, typography and
 * semantic status colors. Screens must read colors from [MaterialTheme] or
 * [LocalStatusColors] rather than hardcoding.
 */
@Composable
fun GymFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColors else LightColors
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = GymFlowShapes,
            typography = GymFlowTypography,
            content = content
        )
    }
}