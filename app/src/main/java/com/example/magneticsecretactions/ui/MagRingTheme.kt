package com.example.magneticsecretactions.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Pink = Color(0xFFFF69B4)
private val Cyan = Color(0xFF00F0FF)
private val Teal = Color(0xFF069494)
private val Ink = Color(0xFF101418)
private val Paper = Color(0xFFFFFBF1)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9ACB),
    onPrimary = Color(0xFF4A1531),
    secondary = Color(0xFF8FEAF0),
    onSecondary = Color(0xFF00363A),
    tertiary = Color(0xFFC1E58F),
    background = Color(0xFF171513),
    surface = Color(0xFF24201D),
    surfaceVariant = Color(0xFF3A3330),
    onBackground = Color(0xFFF3EDE5),
    onSurface = Color(0xFFF3EDE5),
    onSurfaceVariant = Color(0xFFD7C8C0)
)

private val LightColors = lightColorScheme(
    primary = Pink,
    onPrimary = Color(0xFF4A1531),
    secondary = Cyan,
    onSecondary = Color(0xFF00363A),
    tertiary = Teal,
    background = Paper,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5E8DC),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF62545A)
)

@Composable
fun MagRingTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MagRingTypography,
        content = content
    )
}
