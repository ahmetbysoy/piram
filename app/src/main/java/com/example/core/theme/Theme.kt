package com.example.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = BgLight,
    primaryContainer = PurplePastel,
    onPrimaryContainer = TextPrimaryLight,
    secondary = PinkPastel,
    onSecondary = TextPrimaryLight,
    secondaryContainer = CardLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = PurplePrimary,
    onTertiary = BgLight,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = SellCoral,
    onError = BgLight
)

@Composable
fun HFTPyramidTheme(
    content: @Composable () -> Unit
) {
    // Sistem temasına göre açık/koyu şema seçimi
    val scheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
