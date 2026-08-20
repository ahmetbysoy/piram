package com.example.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PurplePastel,
    onPrimary = BgDark,
    primaryContainer = PurpleDark,
    onPrimaryContainer = PinkPastel,
    secondary = PinkPastel,
    onSecondary = BgDark,
    secondaryContainer = CardDark,
    onSecondaryContainer = PurplePastel,
    tertiary = NeonCyan,
    onTertiary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = BorderGlow,
    error = SellRed,
    onError = TextPrimary
)

@Composable
fun HFTPyramidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
