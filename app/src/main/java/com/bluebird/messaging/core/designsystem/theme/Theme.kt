package com.bluebird.messaging.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

private val LocalBluebirdColors = staticCompositionLocalOf { LightBluebirdColors }
private val LocalBluebirdTypography = staticCompositionLocalOf { DefaultBluebirdTypography }
private val LocalBluebirdMotion = staticCompositionLocalOf { BluebirdMotion(reduced = false) }
private val LocalBluebirdLayout = staticCompositionLocalOf { BluebirdLayout.forWidth(411) }
private val LocalFrostSettings = staticCompositionLocalOf { FrostSettings() }

/**
 * Bluebird's theme. Provides the design system through [BluebirdTheme.colors] etc., and maps it onto
 * Material 3 so any stock component used in the app still matches.
 */
@Composable
fun BluebirdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    frostSettings: FrostSettings = FrostSettings(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkBluebirdColors else LightBluebirdColors
    val context = LocalContext.current
    val motion = remember(context) { BluebirdMotion.fromSystem(context) }
    val widthDp = LocalConfiguration.current.screenWidthDp
    val layout = remember(widthDp) { BluebirdLayout.forWidth(widthDp) }
    val materialColors = remember(colors) { colors.toMaterial() }
    val materialTypography = remember { DefaultBluebirdTypography.toMaterial() }

    CompositionLocalProvider(
        LocalBluebirdColors provides colors,
        LocalBluebirdTypography provides DefaultBluebirdTypography,
        LocalBluebirdMotion provides motion,
        LocalBluebirdLayout provides layout,
        LocalFrostSettings provides frostSettings,
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = materialTypography,
            content = content,
        )
    }
}

object BluebirdTheme {
    val colors: BluebirdColors
        @Composable @ReadOnlyComposable get() = LocalBluebirdColors.current
    val typography: BluebirdTypography
        @Composable @ReadOnlyComposable get() = LocalBluebirdTypography.current
    val motion: BluebirdMotion
        @Composable @ReadOnlyComposable get() = LocalBluebirdMotion.current
    val layout: BluebirdLayout
        @Composable @ReadOnlyComposable get() = LocalBluebirdLayout.current
    val frostSettings: FrostSettings
        @Composable @ReadOnlyComposable get() = LocalFrostSettings.current
}

private fun BluebirdColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentMuted,
        onPrimaryContainer = onAccentMuted,
        secondary = warm,
        onSecondary = onWarm,
        secondaryContainer = warmMuted,
        onSecondaryContainer = warmStrong,
        tertiary = warm,
        onTertiary = onWarm,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surfaceSunken,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceRaised,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        surfaceTint = accent,
        outline = outline,
        outlineVariant = separator,
        error = danger,
        onError = if (isDark) Palette.Abyss950 else Palette.Glacier0,
        scrim = scrim,
    )
}
