package io.lumadrop.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF050C0B)
val Panel = Color(0xFF0A1715)
val PanelRaised = Color(0xFF10211E)
val Mint = Color(0xFF76FFD1)
val MintSoft = Color(0xFFB9FFE8)
val Cyan = Color(0xFF54D6FF)
val Muted = Color(0xFF829994)
val Danger = Color(0xFFFF7A8A)

private val Colors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF002019),
    secondary = Cyan,
    background = Ink,
    onBackground = Color(0xFFE7FFF7),
    surface = Panel,
    onSurface = Color(0xFFE7FFF7),
    surfaceVariant = PanelRaised,
    onSurfaceVariant = Muted,
    error = Danger,
)

@Composable
fun LumaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

