package com.bluebird.messaging.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Foundation palette. Prompt 2 replaces this with the full Bluebird design system.
private val BluebirdBlue = Color(0xFF1F6FEB)
private val BluebirdBlueLight = Color(0xFF7AB0FF)

private val LightColors = lightColorScheme(
    primary = BluebirdBlue,
    onPrimary = Color.White,
    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF14171C),
    surface = Color(0xFFFAF9F6),
    onSurface = Color(0xFF14171C),
    surfaceVariant = Color(0xFFEEEDE9),
    onSurfaceVariant = Color(0xFF5B6270),
)

private val DarkColors = darkColorScheme(
    primary = BluebirdBlueLight,
    onPrimary = Color(0xFF00214D),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE8EAEE),
    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFE8EAEE),
    surfaceVariant = Color(0xFF1C2028),
    onSurfaceVariant = Color(0xFFA3AAB8),
)

@Composable
fun BluebirdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
