package com.pleos.radio.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Night cockpit palette sampled from the design reference (docs/design_reference.png). */
object RadioColors {
    val Background = Color(0xFF05080E)
    val Rail = Color(0xFF070B12)
    val Surface = Color(0xFF0E141D)
    val SurfaceHigh = Color(0xFF141C28)
    val Divider = Color(0xFF1A212C)
    val Outline = Color(0xFF2A3342)
    val Accent = Color(0xFF2F8CFF)
    val AccentBright = Color(0xFF4DA8FF)
    val AccentDeep = Color(0xFF0E2A4F)
    val Favorite = Color(0xFFFFC94D)
    val Text = Color(0xFFF3F6FA)
    val TextSecondary = Color(0xFFC9D1DC)
    val Muted = Color(0xFF8C97A6)
}

private val Scheme = darkColorScheme(
    primary = RadioColors.Accent,
    onPrimary = Color.White,
    background = RadioColors.Background,
    onBackground = RadioColors.Text,
    surface = RadioColors.Surface,
    onSurface = RadioColors.Text,
    surfaceVariant = RadioColors.SurfaceHigh,
    onSurfaceVariant = RadioColors.Muted,
    outline = RadioColors.Outline,
)

@Composable
fun RadioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
