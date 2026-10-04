package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CadPrimaryLight,
    onPrimary = Color(0xFF00354E),
    primaryContainer = Color(0xFF004D70),
    onPrimaryContainer = Color(0xFFC7E7FF),
    secondary = CadSecondary,
    onSecondary = Color(0xFF003822),
    tertiary = CadTertiary,
    background = CadDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = CadSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = CadSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = CadLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAE6FD),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = CadSecondary,
    onSecondary = Color.White,
    tertiary = CadTertiary,
    background = CadDarkBg, // Keep deep engineering aesthetic even in light mode
    onBackground = Color(0xFFF1F5F9),
    surface = CadSurface,
    onSurface = Color(0xFFF1F5F9)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Preserve consistent CAD dark room environment

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
