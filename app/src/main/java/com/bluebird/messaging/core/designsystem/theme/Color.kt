package com.bluebird.messaging.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw palette. Screens never use these directly; they read semantic roles from [BluebirdColors].
 *
 * Identity: a confident cobalt, a warm apricot used sparingly, and cool glacier neutrals.
 */
internal object Palette {
    val Cobalt500 = Color(0xFF1F5FE0)
    val Cobalt600 = Color(0xFF1A4FC0)
    val Cobalt400 = Color(0xFF3F78F0)
    val Cobalt300 = Color(0xFF6E9BFF)
    val Cobalt100 = Color(0xFFE2EBFD)
    val Cobalt900 = Color(0xFF0E2350)

    val Apricot400 = Color(0xFFF2955E)
    val Apricot300 = Color(0xFFF5A673)
    val Apricot700 = Color(0xFFB85A22)
    val Apricot100 = Color(0xFFFDEBDD)
    val Apricot900 = Color(0xFF3A1A05)

    // Glacier neutrals (light).
    val Glacier0 = Color(0xFFFFFFFF)
    val Glacier50 = Color(0xFFF3F6FA)
    val Glacier100 = Color(0xFFE9EEF4)
    val Glacier150 = Color(0xFFE4EAF2)
    val Glacier200 = Color(0xFFD9E0E9)
    val Glacier300 = Color(0xFFC5CFDB)
    val Slate400 = Color(0xFF8C97A8)
    val Slate600 = Color(0xFF5A6678)
    val Ink900 = Color(0xFF0E1726)

    // Abyssal neutrals (dark).
    val Abyss950 = Color(0xFF070B12)
    val Abyss900 = Color(0xFF0B1119)
    val Abyss850 = Color(0xFF0F1620)
    val Abyss800 = Color(0xFF16202C)
    val Abyss750 = Color(0xFF1A2431)
    val Abyss700 = Color(0xFF1F2A38)
    val Abyss600 = Color(0xFF2A3747)
    val Mist500 = Color(0xFF6B788A)
    val Mist300 = Color(0xFF9AA7B8)
    val Frost50 = Color(0xFFE8EEF6)

    val Green500 = Color(0xFF1E9E5A)
    val Green400 = Color(0xFF34C17A)
    val Red500 = Color(0xFFE0453A)
    val Red400 = Color(0xFFFF6B5E)
}

@Immutable
data class FrostColors(
    /** Top and bottom of the translucent ice fill. Alpha is intentionally high: hazy, never see-through. */
    val tintTop: Color,
    val tintBottom: Color,
    /** Same fill, tinted toward the accent, for selected states. */
    val accentTintTop: Color,
    val accentTintBottom: Color,
    /** Soft diffuse glow near the top edge. */
    val highlight: Color,
    /** Faint diagonal light streaks ("refraction"). */
    val band: Color,
    /** Edge light along the top of the rim. */
    val rim: Color,
    /** Darker hairline along the bottom of the rim, for depth. */
    val rimShadow: Color,
    /** Opaque stand-in used when frost is turned off. */
    val solidFallback: Color,
    val grainAlpha: Float,
)

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
    val surface: Color,
    val surfaceRaised: Color,
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

    /** Screen backdrop: a static vertical gradient, like light from above the ice. */
    val backdropTop: Color,
    val backdropBottom: Color,
    val backdropGlow: Color,

    val frost: FrostColors,
)

val LightBluebirdColors = BluebirdColors(
    isDark = false,
    accent = Palette.Cobalt500,
    accentPressed = Palette.Cobalt600,
    onAccent = Color.White,
    accentMuted = Palette.Cobalt100,
    onAccentMuted = Palette.Cobalt600,
    warm = Palette.Apricot400,
    onWarm = Palette.Apricot900,
    warmStrong = Palette.Apricot700,
    warmMuted = Palette.Apricot100,
    background = Palette.Glacier50,
    surface = Palette.Glacier50,
    surfaceRaised = Palette.Glacier0,
    surfaceSunken = Palette.Glacier100,
    textPrimary = Palette.Ink900,
    textSecondary = Palette.Slate600,
    textTertiary = Palette.Slate400,
    separator = Palette.Glacier200,
    outline = Palette.Glacier300,
    bubbleOutgoing = Palette.Cobalt500,
    onBubbleOutgoing = Color.White,
    onBubbleOutgoingSecondary = Color.White.copy(alpha = 0.72f),
    bubbleIncoming = Palette.Glacier150,
    onBubbleIncoming = Palette.Ink900,
    unread = Palette.Cobalt500,
    success = Palette.Green500,
    danger = Palette.Red500,
    callAccept = Palette.Green500,
    callDecline = Palette.Red500,
    skeleton = Palette.Glacier150,
    scrim = Color(0x660E1726),
    backdropTop = Color(0xFFF8FBFE),
    backdropBottom = Color(0xFFEAF0F7),
    backdropGlow = Color(0x141F5FE0),
    frost = FrostColors(
        tintTop = Color(0xF5F6F9FD),
        tintBottom = Color(0xF2E8EFF8),
        accentTintTop = Color(0xF5EEF4FE),
        accentTintBottom = Color(0xF2DCE7FB),
        highlight = Color(0xB3FFFFFF),
        band = Color(0x4DFFFFFF),
        rim = Color(0xF2FFFFFF),
        rimShadow = Color(0x26305A8C),
        solidFallback = Palette.Glacier0,
        grainAlpha = 0.035f,
    ),
)

val DarkBluebirdColors = BluebirdColors(
    isDark = true,
    accent = Palette.Cobalt300,
    accentPressed = Palette.Cobalt400,
    onAccent = Palette.Cobalt900,
    accentMuted = Color(0xFF15264A),
    onAccentMuted = Palette.Cobalt300,
    warm = Palette.Apricot300,
    onWarm = Palette.Apricot900,
    warmStrong = Palette.Apricot300,
    warmMuted = Color(0xFF3A2416),
    background = Palette.Abyss950,
    surface = Palette.Abyss850,
    surfaceRaised = Palette.Abyss800,
    surfaceSunken = Palette.Abyss900,
    textPrimary = Palette.Frost50,
    textSecondary = Palette.Mist300,
    textTertiary = Palette.Mist500,
    separator = Palette.Abyss700,
    outline = Palette.Abyss600,
    // Slightly deeper than the dark-mode accent so white text keeps comfortable contrast.
    bubbleOutgoing = Color(0xFF2F6BF0),
    onBubbleOutgoing = Color.White,
    onBubbleOutgoingSecondary = Color.White.copy(alpha = 0.7f),
    bubbleIncoming = Palette.Abyss750,
    onBubbleIncoming = Palette.Frost50,
    unread = Palette.Cobalt300,
    success = Palette.Green400,
    danger = Palette.Red400,
    callAccept = Palette.Green400,
    callDecline = Palette.Red400,
    skeleton = Palette.Abyss750,
    scrim = Color(0x99000000),
    backdropTop = Color(0xFF0C1420),
    backdropBottom = Palette.Abyss950,
    backdropGlow = Color(0x1A6E9BFF),
    frost = FrostColors(
        tintTop = Color(0xF21A2635),
        tintBottom = Color(0xF5101925),
        accentTintTop = Color(0xF21C2D4C),
        accentTintBottom = Color(0xF5132139),
        highlight = Color(0x2EB9D8FF),
        band = Color(0x12CFE6FF),
        rim = Color(0x59A9CCFF),
        rimShadow = Color(0x80000000),
        solidFallback = Palette.Abyss800,
        grainAlpha = 0.05f,
    ),
)
