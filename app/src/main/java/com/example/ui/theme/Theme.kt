package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NTubeDarkColorScheme = darkColorScheme(
    primary = NTubeRed,
    onPrimary = Color.White,
    primaryContainer = NTubeDarkRed,
    onPrimaryContainer = Color.White,
    secondary = DarkSurfaceElevated,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF333333)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme to match YouTube / NTube sleek design
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NTubeDarkColorScheme,
        typography = Typography,
        content = content
    )
}
