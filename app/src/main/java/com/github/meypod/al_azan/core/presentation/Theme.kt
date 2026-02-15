package com.github.meypod.al_azan.core.presentation

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColors
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository

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
)

private val LightColorScheme = lightColorScheme(
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
    settingsRepository: SettingsRepository,
    content: @Composable () -> Unit,
) {
    val settings by settingsRepository.data.collectAsState(initial = Settings(selectedLocale = "en"))
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = when {
        settings.themeColor == ThemeColors.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else ->
            when (settings.themeColor) {
                ThemeColors.Light -> LightColorScheme
                ThemeColors.Dark -> DarkColorScheme
                ThemeColors.ClassicLight -> LightHighContrastColorScheme
                ThemeColors.ClassicDark -> DarkHighContrastColorScheme
                ThemeColors.Dynamic,
                ThemeColors.Default,
                -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
