package com.example.descargadorvideos.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary          = Accent,
    onPrimary        = White,
    primaryContainer = AccentLight,
    onPrimaryContainer = Accent,
    secondary        = TextSecondary,
    onSecondary      = White,
    background       = OffWhite,
    onBackground     = TextPrimary,
    surface          = SurfaceCard,
    onSurface        = TextPrimary,
    surfaceVariant   = Surface,
    onSurfaceVariant = TextSecondary,
    outline          = BorderLight,
    error            = Error,
    onError          = White,
)

@Composable
fun DescargadorVideosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}