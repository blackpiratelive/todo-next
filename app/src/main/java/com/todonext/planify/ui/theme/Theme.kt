package com.todonext.planify.ui.theme

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
    primary = AccentBlue,
    onPrimary = DarkOnSurface,
    primaryContainer = AccentBlueDark,
    onPrimaryContainer = AccentBlueLight,
    secondary = TodayGreen,
    onSecondary = DarkOnSurface,
    secondaryContainer = ScheduledPurple,
    onSecondaryContainer = DarkOnSurface,
    tertiary = LabelsBrown,
    onTertiary = DarkOnSurface,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = OverdueRed,
    onError = DarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = LightSurface,
    primaryContainer = AccentBlueLight,
    onPrimaryContainer = AccentBlueDark,
    secondary = TodayGreen,
    onSecondary = LightOnSurface,
    secondaryContainer = ScheduledPurple,
    onSecondaryContainer = LightSurface,
    tertiary = LabelsBrown,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = OverdueRed,
    onError = LightSurface
)

@Composable
fun PlanifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PlanifyTypography,
        shapes = PlanifyShapes,
        content = content
    )
}
