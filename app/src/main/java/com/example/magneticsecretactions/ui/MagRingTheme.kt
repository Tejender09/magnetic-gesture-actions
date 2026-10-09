package com.example.magneticsecretactions.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Obsidian = Color(0xFF0B0F13)
private val Slate = Color(0xFF101418)
private val Pink = Color(0xFFFF69B4)
private val Cyan = Color(0xFF00F0FF)
private val Teal = Color(0xFF069494)
private val Ink = Color(0xFF101418)
private val Paper = Color(0xFFF8F7FA)

private val DarkColors = darkColorScheme(
    primary = Pink,
    onPrimary = Color(0xFF3D0024),
    secondary = Cyan,
    onSecondary = Color(0xFF002022),
    tertiary = Teal,
    background = Obsidian,
    surface = Slate,
    surfaceVariant = Color(0xFF1C2024),
    onBackground = Color(0xFFE0E3E8),
    onSurface = Color(0xFFE0E3E8),
    onSurfaceVariant = Color(0xFFD1BBC4)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB00068),
    onPrimary = Color.White,
    secondary = Color(0xFF006A72),
    onSecondary = Color.White,
    tertiary = Color(0xFF006A6A),
    background = Paper,
    surface = Color.White,
    surfaceVariant = Color(0xFFF1E7EC),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF55434B)
)

@Composable
fun MagRingTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MagRingTypography,
        content = content
    )
}
