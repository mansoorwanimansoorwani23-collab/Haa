package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QuantumDarkColorScheme = darkColorScheme(
    primary = QuantumGreenPrimary,
    onPrimary = Color(0xFF003814),
    primaryContainer = Color(0xFF005220),
    onPrimaryContainer = QuantumGreenBright,
    secondary = QuantumCyanAccent,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF004F46),
    onSecondaryContainer = Color(0xFF70FCE5),
    tertiary = QuantumGreenBright,
    onTertiary = Color(0xFF003915),
    background = QuantumDarkBg,
    onBackground = QuantumTextPrimary,
    surface = QuantumSurfaceDark,
    onSurface = QuantumTextPrimary,
    surfaceVariant = QuantumSurfaceCard,
    onSurfaceVariant = QuantumTextSecondary,
    outline = QuantumSurfaceBorder,
    error = QuantumRedAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    // JARVIS UI is explicitly a futuristic, cybernetic quantum dark interface
    MaterialTheme(
        colorScheme = QuantumDarkColorScheme,
        typography = Typography,
        content = content
    )
}

