package com.misterdownloadmanager.presentation.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = DarkPrimary,
    secondary = DarkSecondary,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkSecondary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSurfaceBorder,
    outlineVariant = DarkSurfaceBorder.copy(alpha = 0.5f),
    surfaceTint = Color.Transparent,
    error = StateFailed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimary.copy(alpha = 0.10f),
    onPrimaryContainer = LightPrimary,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSecondary.copy(alpha = 0.10f),
    onSecondaryContainer = LightSecondary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSurfaceBorder,
    outlineVariant = LightSurfaceBorder.copy(alpha = 0.6f),
    surfaceTint = Color.Transparent,
    error = StateFailed,
    onError = Color.White
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun MdmTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MdmTypography,
        content = content
    )
}

