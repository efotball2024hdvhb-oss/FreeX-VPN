package com.freex.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FreeXColors = darkColorScheme(
    primary = Color(0xFF65F5D1),
    secondary = Color(0xFF9C7CFF),
    background = Color(0xFF050608),
    surface = Color(0xFF0B0E15),
    onBackground = Color(0xFFF4F7FB),
    onSurface = Color(0xFFF4F7FB)
)

@Composable
fun FreeXTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FreeXColors, content = content)
}
