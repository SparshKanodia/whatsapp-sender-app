package com.internal.wamessenger.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Palette ───────────────────────────────────────────────────────────────────
val Green500   = Color(0xFF00C853)
val Green700   = Color(0xFF009624)
val Green100   = Color(0xFFB9F6CA)
val Dark900    = Color(0xFF0D1117)
val Dark800    = Color(0xFF161B22)
val Dark700    = Color(0xFF21262D)
val Dark600    = Color(0xFF30363D)
val OnDark     = Color(0xFFE6EDF3)
val OnDarkMuted = Color(0xFF8B949E)
val ErrorRed   = Color(0xFFFF5449)
val WarnYellow = Color(0xFFE3B341)
val InfoBlue   = Color(0xFF58A6FF)

private val DarkColors = darkColorScheme(
    primary          = Green500,
    onPrimary        = Color.Black,
    primaryContainer = Green700,
    onPrimaryContainer = Green100,
    secondary        = InfoBlue,
    onSecondary      = Dark900,
    background       = Dark900,
    onBackground     = OnDark,
    surface          = Dark800,
    onSurface        = OnDark,
    surfaceVariant   = Dark700,
    onSurfaceVariant = OnDarkMuted,
    error            = ErrorRed,
    onError          = Color.White,
    outline          = Dark600
)

@Composable
fun WAMessengerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
