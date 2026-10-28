package com.muse.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MuseColorScheme = darkColorScheme(
    primary = MusePurple,
    onPrimary = Color.White,
    secondary = MusePurpleDark,
    onSecondary = Color.White,
    background = MuseBg,
    onBackground = MuseOnDark,
    surface = MuseSurface,
    onSurface = MuseOnDark,
    surfaceVariant = MuseSurfaceVariant,
    onSurfaceVariant = MuseMuted,
    error = MuseDanger,
)

private val MuseTypography = Typography()

@Composable
fun MuseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MuseColorScheme,
        typography = MuseTypography,
        content = content,
    )
}
