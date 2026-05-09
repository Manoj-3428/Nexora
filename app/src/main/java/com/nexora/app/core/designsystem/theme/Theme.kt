package com.nexora.app.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDarkBlue,
    onPrimary = TextPrimary,
    secondary = SecondaryDeepBlue,
    onSecondary = TextPrimary,
    tertiary = AccentBlue,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = TransparentSurface,
    error = ErrorRed,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLightBlue,
    onPrimary = TextPrimary,
    secondary = AccentBlue,
    onSecondary = TextPrimary,
    tertiary = PrimaryDarkBlue,
    background = SoftWhite,
    onBackground = PrimaryDarkBlue,
    surface = BluishWhite,
    onSurface = PrimaryDarkBlue,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Forcing Dark Theme for the futuristic feel as default, but retaining flexibility
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Enforcing dark theme heavily for now as per requirement
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexoraTypography,
        content = content
    )
}
