package com.bluebird.messaging.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw palette. Screens never use these directly; they read semantic roles from [BluebirdColors].
 *
 * Identity: iOS 26 system blue (Apple HIG: #0088FF light, #0091FF dark) and a warm apricot used sparingly,
 * over iOS-style neutrals.
 */
internal object Palette {
    val BlueLight = Color(0xFF0088FF)
    val BlueLightPressed = Color(0xFF0070D9)
    val BlueDark = Color(0xFF0091FF)
    val BlueDarkPressed = Color(0xFF0078D6)
    val BlueTintLight = Color(0xFFE0EFFF)
    val BlueTintDark = Color(0xFF0A2A4A)
    val BlueInkLight = Color(0xFF006BD6)
    val BlueInkDark = Color(0xFF5CB8FF)

    val Apricot400 = Color(0xFFF2955E)
    val Apricot300 = Color(0xFFF5A673)
    val Apricot700 = Color(0xFFB85A22)
    val Apricot100 = Color(0xFFFDEBDD)
    val Apricot900 = Color(0xFF3A1A05)

    // Light neutrals.
    val White = Color(0xFFFFFFFF)
    val Gray50 = Color(0xFFF2F2F7)
    val Gray100 = Color(0xFFE9E9EB)
    val Gray150 = Color(0xFFE5E5EA)
    val Gray200 = Color(0xFFD8D8DC)
    val Gray300 = Color(0xFFC7C7CC)
    val Gray500 = Color(0xFFA1A1A6)
    val Gray600 = Color(0xFF6E6E73)
    val Ink = Color(0xFF0B0B0C)

    // Dark neutrals.
    val Black = Color(0xFF000000)
    val Graphite900 = Color(0xFF111113)
    val Graphite800 = Color(0xFF1C1C1E)
    val Graphite750 = Color(0xFF26252A)
    val Graphite700 = Color(0xFF2C2C2E)
    val Graphite600 = Color(0xFF38383A)
    val Graphite500 = Color(0xFF48484A)
    val Graphite400 = Color(0xFF636366)
    val Graphite300 = Color(0xFF98989F)

    val Green500 = Color(0xFF1E9E5A)
    val Green400 = Color(0xFF34C17A)
    val Red500 = Color(0xFFE0453A)
    val Red400 = Color(0xFFFF6B5E)
}

@Immutable
data class BluebirdColors(
    val isDark: Boolean,

    val accent: Color,
    val accentPressed: Color,
    val onAccent: Color,
    val accentMuted: Color,
    val onAccentMuted: Color,

    /** Apricot. Fills, pins, reactions, small moments of warmth. */
    val warm: Color,
    val onWarm: Color,
    /** Apricot dark enough to use for text and icons. */
    val warmStrong: Color,
    val warmMuted: Color,

    val background: Color,
    /** Behind grouped cards (settings, contact details). */
    val groupedBackground: Color,
    val surface: Color,
    val surfaceRaised: Color,
    /** Filled controls: search field, secondary buttons, empty-state discs. */
    val surfaceSunken: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    val separator: Color,
    val outline: Color,

    val bubbleOutgoing: Color,
    val onBubbleOutgoing: Color,
    val onBubbleOutgoingSecondary: Color,
    val bubbleIncoming: Color,
    val onBubbleIncoming: Color,

    val unread: Color,
    val success: Color,
    val danger: Color,
    /** Reserved for a future calling experience. */
    val callAccept: Color,
    val callDecline: Color,

    val skeleton: Color,
    val scrim: Color,

    /** Base tint laid over the blurred backdrop; [GlassMaterial] sets its opacity. */
    val glassTint: Color,
    /** What glass surfaces show when blur isn't available (Android 11 and below, or turned off). */
    val glassFallback: Color,
)

val LightBluebirdColors = BluebirdColors(
    isDark = false,
    accent = Palette.BlueLight,
    accentPressed = Palette.BlueLightPressed,
    onAccent = Color.White,
    accentMuted = Palette.BlueTintLight,
    onAccentMuted = Palette.BlueInkLight,
    warm = Palette.Apricot400,
    onWarm = Palette.Apricot900,
    warmStrong = Palette.Apricot700,
    warmMuted = Palette.Apricot100,
    background = Palette.White,
    groupedBackground = Palette.Gray50,
    surface = Palette.White,
    surfaceRaised = Palette.White,
    surfaceSunken = Palette.Gray50,
    textPrimary = Palette.Ink,
    textSecondary = Palette.Gray600,
    textTertiary = Palette.Gray500,
    separator = Palette.Gray200,
    outline = Palette.Gray300,
    bubbleOutgoing = Palette.BlueLight,
    onBubbleOutgoing = Color.White,
    onBubbleOutgoingSecondary = Color.White.copy(alpha = 0.72f),
    bubbleIncoming = Palette.Gray100,
    onBubbleIncoming = Palette.Ink,
    unread = Palette.BlueLight,
    success = Palette.Green500,
    danger = Palette.Red500,
    callAccept = Palette.Green500,
    callDecline = Palette.Red500,
    skeleton = Palette.Gray150,
    scrim = Color(0x66000000),
    glassTint = Color(0xFFF9F9F9),
    glassFallback = Color(0xF2F9F9F9),
)

val DarkBluebirdColors = BluebirdColors(
    isDark = true,
    accent = Palette.BlueDark,
    accentPressed = Palette.BlueDarkPressed,
    onAccent = Color.White,
    accentMuted = Palette.BlueTintDark,
    onAccentMuted = Palette.BlueInkDark,
    warm = Palette.Apricot300,
    onWarm = Palette.Apricot900,
    warmStrong = Palette.Apricot300,
    warmMuted = Color(0xFF3A2416),
    background = Palette.Black,
    groupedBackground = Palette.Black,
    surface = Palette.Black,
    surfaceRaised = Palette.Graphite800,
    surfaceSunken = Palette.Graphite800,
    textPrimary = Color.White,
    textSecondary = Palette.Graphite300,
    textTertiary = Palette.Graphite400,
    separator = Palette.Graphite600,
    outline = Palette.Graphite500,
    bubbleOutgoing = Palette.BlueDark,
    onBubbleOutgoing = Color.White,
    onBubbleOutgoingSecondary = Color.White.copy(alpha = 0.7f),
    bubbleIncoming = Palette.Graphite750,
    onBubbleIncoming = Color.White,
    unread = Palette.BlueDark,
    success = Palette.Green400,
    danger = Palette.Red400,
    callAccept = Palette.Green400,
    callDecline = Palette.Red400,
    skeleton = Palette.Graphite800,
    scrim = Color(0x99000000),
    glassTint = Palette.Graphite800,
    glassFallback = Color(0xF21C1C1E),
)
