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
    primary = PrimaryBlue,
    onPrimary = BackgroundWhite,
    secondary = SecondaryBlue,
    onSecondary = BackgroundWhite,
    tertiary = AccentBlue,
    background = TextPrimaryDark,
    onBackground = BackgroundWhite,
    surface = TextPrimaryDark,
    onSurface = BackgroundWhite,
    surfaceVariant = TransparentSurface,
    error = ErrorRed,
    onError = BackgroundWhite
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = BackgroundWhite,
    secondary = SecondaryBlue,
    onSecondary = BackgroundWhite,
    tertiary = AccentBlue,
    background = BackgroundWhite,
    onBackground = TextPrimaryDark,
    surface = SurfaceWhite,
    onSurface = TextPrimaryDark,
    error = ErrorRed,
    onError = BackgroundWhite
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We use the Light Theme by default to ensure the White and Blue modern design is prominent
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme 
    
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
