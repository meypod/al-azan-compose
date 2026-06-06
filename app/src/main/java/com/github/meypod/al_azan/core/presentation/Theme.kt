package com.github.meypod.al_azan.core.presentation

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = DarkScrim,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    surfaceTint = DarkSurfaceTint,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)
val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = LightScrim,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
    surfaceTint = LightSurfaceTint,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

private val LightHighContrastColorScheme = lightColorScheme(
    primary = LightHighContrastPrimary,
    onPrimary = LightHighContrastOnPrimary,
    primaryContainer = LightHighContrastPrimaryContainer,
    onPrimaryContainer = LightHighContrastOnPrimaryContainer,
    secondary = LightHighContrastSecondary,
    onSecondary = LightHighContrastOnSecondary,
    secondaryContainer = LightHighContrastSecondaryContainer,
    onSecondaryContainer = LightHighContrastOnSecondaryContainer,
    tertiary = LightHighContrastTertiary,
    onTertiary = LightHighContrastOnTertiary,
    tertiaryContainer = LightHighContrastTertiaryContainer,
    onTertiaryContainer = LightHighContrastOnTertiaryContainer,
    error = LightHighContrastError,
    onError = LightHighContrastOnError,
    errorContainer = LightHighContrastErrorContainer,
    onErrorContainer = LightHighContrastOnErrorContainer,
    background = LightHighContrastBackground,
    onBackground = LightHighContrastOnBackground,
    surface = LightHighContrastSurface,
    onSurface = LightHighContrastOnSurface,
    surfaceVariant = LightHighContrastSurfaceVariant,
    onSurfaceVariant = LightHighContrastOnSurfaceVariant,
    outline = LightHighContrastOutline,
    outlineVariant = LightHighContrastOutlineVariant,
    scrim = LightHighContrastScrim,
    inverseSurface = LightHighContrastInverseSurface,
    inverseOnSurface = LightHighContrastInverseOnSurface,
    inversePrimary = LightHighContrastInversePrimary,
    surfaceTint = LightHighContrastSurfaceTint,
)

private val DarkHighContrastColorScheme = darkColorScheme(
    primary = DarkHighContrastPrimary,
    onPrimary = DarkHighContrastOnPrimary,
    primaryContainer = DarkHighContrastPrimaryContainer,
    onPrimaryContainer = DarkHighContrastOnPrimaryContainer,
    secondary = DarkHighContrastSecondary,
    onSecondary = DarkHighContrastOnSecondary,
    secondaryContainer = DarkHighContrastSecondaryContainer,
    onSecondaryContainer = DarkHighContrastOnSecondaryContainer,
    tertiary = DarkHighContrastTertiary,
    onTertiary = DarkHighContrastOnTertiary,
    tertiaryContainer = DarkHighContrastTertiaryContainer,
    onTertiaryContainer = DarkHighContrastOnTertiaryContainer,
    error = DarkHighContrastError,
    onError = DarkHighContrastOnError,
    errorContainer = DarkHighContrastErrorContainer,
    onErrorContainer = DarkHighContrastOnErrorContainer,
    background = DarkHighContrastBackground,
    onBackground = DarkHighContrastOnBackground,
    surface = DarkHighContrastSurface,
    onSurface = DarkHighContrastOnSurface,
    surfaceVariant = DarkHighContrastSurfaceVariant,
    onSurfaceVariant = DarkHighContrastOnSurfaceVariant,
    outline = DarkHighContrastOutline,
    outlineVariant = DarkHighContrastOutlineVariant,
    scrim = DarkHighContrastScrim,
    inverseSurface = DarkHighContrastInverseSurface,
    inverseOnSurface = DarkHighContrastInverseOnSurface,
    inversePrimary = DarkHighContrastInversePrimary,
    surfaceTint = DarkHighContrastSurfaceTint,
)

@Composable
fun AlAzanTheme(
    themeColor: ThemeColor = ThemeColor.Default,
    content: @Composable () -> Unit,
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeColor) {
        ThemeColor.Light, ThemeColor.ClassicLight -> false
        ThemeColor.Dark, ThemeColor.ClassicDark -> true
        ThemeColor.Dynamic, ThemeColor.Default -> systemDarkTheme
    }
    val colorScheme = when {
        themeColor == ThemeColor.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else ->
            when (themeColor) {
                ThemeColor.Light -> LightColorScheme

                ThemeColor.Dark -> DarkColorScheme

                ThemeColor.ClassicLight -> LightHighContrastColorScheme

                ThemeColor.ClassicDark -> DarkHighContrastColorScheme

                ThemeColor.Dynamic,
                ThemeColor.Default,
                -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
    }

    val activity = LocalActivity.current
    if (activity != null) {
        // keep status/navigation bar icon contrast in sync with the resolved app theme,
        // which can differ from the system dark-mode setting
        SideEffect {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
