package com.example.magneticsecretactions.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MagRingTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        bodyMedium = bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        labelSmall = labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp)
    )
}
