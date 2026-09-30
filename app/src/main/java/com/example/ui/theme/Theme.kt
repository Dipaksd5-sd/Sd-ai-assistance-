package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SDAssistantColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepCosmic,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricSky,
    onSecondary = DeepCosmic,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = ElectricSky,
    tertiary = CyberViolet,
    onTertiary = Color.White,
    background = DeepCosmic,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = AccentCoral,
    onError = Color.White
)

@Composable
fun SDAssistantTheme(
    darkTheme: Boolean = true, // Voice assistant feels most futuristic in deep cosmic dark
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SDAssistantColorScheme,
        typography = Typography,
        content = content
    )
}
