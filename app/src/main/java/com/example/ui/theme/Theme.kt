package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = SpaceDark,
    primaryContainer = SpaceCardElevated,
    onPrimaryContainer = CyanAccent,
    secondary = NavicBlueLight,
    onSecondary = SpaceDark,
    secondaryContainer = SpaceCard,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberDr,
    onTertiary = SpaceDark,
    background = SpaceDark,
    onBackground = TextPrimary,
    surface = SpaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = SpaceSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SpaceBorder,
    error = RedJam,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme(
    primary = NavicBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = AmberDr,
    onSecondary = Color.White,
    background = SpaceDark,
    onBackground = TextPrimary,
    surface = SpaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = SpaceSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SpaceBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
