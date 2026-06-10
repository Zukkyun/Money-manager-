package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.data.PreferencesManager

@Composable
fun MyApplicationTheme(
    themeType: String = PreferencesManager.THEME_TEAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        PreferencesManager.THEME_ORANGE -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = OrangeDarkPrimary,
                    secondary = OrangeDarkSecondary,
                    tertiary = OrangeDarkTertiary,
                    background = OrangeDarkBackground,
                    surface = OrangeDarkSurface
                )
            } else {
                lightColorScheme(
                    primary = OrangeLightPrimary,
                    secondary = OrangeLightSecondary,
                    tertiary = OrangeLightTertiary,
                    background = OrangeLightBackground,
                    surface = OrangeLightSurface
                )
            }
        }
        PreferencesManager.THEME_BLUE -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = BlueDarkPrimary,
                    secondary = BlueDarkSecondary,
                    tertiary = BlueDarkTertiary,
                    background = BlueDarkBackground,
                    surface = BlueDarkSurface
                )
            } else {
                lightColorScheme(
                    primary = BlueLightPrimary,
                    secondary = BlueLightSecondary,
                    tertiary = BlueLightTertiary,
                    background = BlueLightBackground,
                    surface = BlueLightSurface
                )
            }
        }
        PreferencesManager.THEME_PURPLE -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = PurpleDarkPrimary,
                    secondary = PurpleDarkSecondary,
                    tertiary = PurpleDarkTertiary,
                    background = PurpleDarkBackground,
                    surface = PurpleDarkSurface
                )
            } else {
                lightColorScheme(
                    primary = PurpleLightPrimary,
                    secondary = PurpleLightSecondary,
                    tertiary = PurpleLightTertiary,
                    background = PurpleLightBackground,
                    surface = PurpleLightSurface
                )
            }
        }
        PreferencesManager.THEME_GREEN -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = GreenDarkPrimary,
                    secondary = GreenDarkSecondary,
                    tertiary = GreenDarkTertiary,
                    background = GreenDarkBackground,
                    surface = GreenDarkSurface
                )
            } else {
                lightColorScheme(
                    primary = GreenLightPrimary,
                    secondary = GreenLightSecondary,
                    tertiary = GreenLightTertiary,
                    background = GreenLightBackground,
                    surface = GreenLightSurface
                )
            }
        }
        else -> { // THEME_TEAL (Default)
            if (darkTheme) {
                darkColorScheme(
                    primary = TealDarkPrimary,
                    secondary = TealDarkSecondary,
                    tertiary = TealDarkTertiary,
                    background = TealDarkBackground,
                    surface = TealDarkSurface
                )
            } else {
                lightColorScheme(
                    primary = TealLightPrimary,
                    secondary = TealLightSecondary,
                    tertiary = TealLightTertiary,
                    background = TealLightBackground,
                    surface = TealLightSurface
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
