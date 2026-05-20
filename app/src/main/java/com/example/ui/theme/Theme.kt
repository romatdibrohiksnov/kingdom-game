package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SleekBrandPurple,
    secondary = SleekLightPurple,
    tertiary = SleekLightRose,
    background = SleekBg,
    surface = SleekLightSurface,
    onPrimary = SleekSolidWhite,
    onSecondary = SleekDarkPurpleText,
    onBackground = SleekTextDark,
    onSurface = SleekTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = SleekBrandPurple,
    secondary = SleekLightPurple,
    tertiary = SleekLightRose,
    background = SleekBg,
    surface = SleekLightSurface,
    onPrimary = SleekSolidWhite,
    onSecondary = SleekDarkPurpleText,
    onBackground = SleekTextDark,
    onSurface = SleekTextDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Set to false to support the bright light purple Sleek Interface theme by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
