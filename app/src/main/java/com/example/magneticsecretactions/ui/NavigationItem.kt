package com.example.magneticsecretactions.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val title: String,
    val icon: ImageVector,
    val badge: String? = null
) {
    FEED("Feed", Icons.Rounded.DynamicFeed),
    GESTURES("Gestures", Icons.Rounded.TouchApp),
    SETTINGS("Settings", Icons.Rounded.Settings)
}
