package com.pvlm.rorafreeze.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pure-black "true OLED" palette. Everything is pitch black with subtle raised
// grey panels so the display turns pixels off and saves power on AMOLED.
private val AmoledColors = darkColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0F2447),
    onPrimaryContainer = Color(0xFFCBDFFF),
    secondary = Color(0xFF9FB8E8),
    onSecondary = Color(0xFF00224D),
    secondaryContainer = Color(0xFF20406E),
    onSecondaryContainer = Color(0xFFD1E1FF),
    tertiary = Color(0xFFFFB199),
    onTertiary = Color(0xFF3A2300),
    tertiaryContainer = Color(0xFF482900),
    onTertiaryContainer = Color(0xFFFFDBB5),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF191919),
    onSurfaceVariant = Color(0xFF9A9A9A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainer = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceBright = Color(0xFF1E1E1E),
    outline = Color(0xFF2B2B2B),
    outlineVariant = Color(0xFF202020),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFF4C0807),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun FreezeAppsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledColors,
        typography = Typography,
        content = content
    )
}