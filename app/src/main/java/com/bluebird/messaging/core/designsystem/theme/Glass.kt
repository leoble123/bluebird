package com.bluebird.messaging.core.designsystem.theme

import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * iOS-style frosted glass: floating chrome (top bars, the composer, menus) blurs whatever scrolls behind it.
 *
 * A 24dp backdrop blur under a translucent white (light) or graphite (dark) tint, in three thicknesses.
 * Blur runs on the GPU (RenderEffect) on Android 12+; on Android 11 and below, or with [GlassSettings.blur]
 * off, glass draws a translucent [BluebirdColors.glassFallback] instead.
 *
 * Usage: wrap a screen in [GlassHost], mark the scrolling content with [glassSource], and apply [glass] to
 * surfaces drawn over it. Only use glass on a few floating surfaces, never on list rows or message bubbles.
 */
enum class GlassMaterial { Thin, Regular, Thick }

@Immutable
data class GlassSettings(
    /** False draws the translucent fallback everywhere (a performance or accessibility escape hatch). */
    val blur: Boolean = true,
)

private val LocalGlassState = staticCompositionLocalOf<HazeState?> { null }

/** Provides a glass backdrop for everything inside it. */
@Composable
fun GlassHost(content: @Composable () -> Unit) {
    val state = rememberHazeState()
    CompositionLocalProvider(LocalGlassState provides state, content = content)
}

/** Marks content that glass surfaces in the same [GlassHost] will blur. */
@Composable
fun Modifier.glassSource(): Modifier {
    val state = LocalGlassState.current ?: return this
    return hazeSource(state)
}

/**
 * Turns this element into a frosted glass surface clipped to [shape]. Outside a [GlassHost] it falls back to
 * a translucent fill, so components stay usable anywhere.
 */
@Composable
fun Modifier.glass(
    shape: Shape = RectangleShape,
    material: GlassMaterial = GlassMaterial.Regular,
): Modifier {
    val colors = BluebirdTheme.colors
    val settings = BluebirdTheme.glassSettings
    val state = LocalGlassState.current
    val clipped = if (shape == RectangleShape) this else clip(shape)
    if (state == null || !settings.blur) return clipped.background(colors.glassFallback)

    val tint = colors.glassTint.copy(
        alpha = when (material) {
            GlassMaterial.Thin -> 0.55f
            GlassMaterial.Regular -> 0.72f
            GlassMaterial.Thick -> 0.86f
        },
    )
    val style = remember(tint, colors.background, colors.glassFallback) {
        HazeBlurStyle {
            blurRadius(24.dp)
            noiseFactor(0f)
            backgroundColor(colors.background)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            fallbackColorEffect(HazeColorEffect.tint(colors.glassFallback))
        }
    }
    return clipped.hazeBlur(input = HazeInput.Sources(state), style = style)
}

/**
 * Liquid Glass for small floating controls (round buttons, capsules): the backdrop is refracted and lit with a
 * specular rim, and the material responds to presses, following Apple's guidance to use it only for the
 * controls and navigation layer. GPU shaders on Android 13+, Haze's simpler fallback below that, and a plain
 * translucent fill outside a [GlassHost] or with [GlassSettings.blur] off.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.liquidGlass(
    shape: RoundedCornerShape = CircleShape,
    interactionSource: InteractionSource? = null,
    /** Tints the glass, e.g. the accent for a prominent action. */
    tint: Color = Color.Unspecified,
): Modifier {
    val colors = BluebirdTheme.colors
    val settings = BluebirdTheme.glassSettings
    val state = LocalGlassState.current
    if (state == null || !settings.blur) {
        val fill = if (tint.isSpecified) tint else colors.glassFallback
        return clip(shape).background(fill).border(Dp.Hairline, colors.separator, shape)
    }
    val glassTint = if (tint.isSpecified) tint.copy(alpha = 0.85f) else colors.glassTint.copy(alpha = 0.55f)
    val style = remember(shape, glassTint, colors.background) {
        GlassStyle.regular.then {
            shape(shape)
            backgroundColor(colors.background)
            tint(glassTint)
        }
    }
    return hazeGlass(input = HazeInput.Sources(state), style = style, interactionSource = interactionSource)
}

enum class ScrollEdge { Top, Bottom }

/**
 * iOS's scroll edge effect: content passing under floating controls at the top or bottom of the screen is
 * softly blurred and faded toward the edge, instead of meeting a solid bar.
 */
@Composable
fun Modifier.scrollEdgeEffect(
    edge: ScrollEdge,
    /** The screen's background color, which the blurred edge fades into. */
    color: Color = BluebirdTheme.colors.background,
): Modifier {
    val settings = BluebirdTheme.glassSettings
    val state = LocalGlassState.current
    val solid = color
    val fade = remember(edge, solid) {
        val stops = listOf(solid.copy(alpha = 0.94f), solid.copy(alpha = 0.7f), solid.copy(alpha = 0f))
        Brush.verticalGradient(if (edge == ScrollEdge.Top) stops else stops.reversed())
    }
    if (state == null || !settings.blur) return background(fade)
    val style = remember(edge, solid) {
        // Fully applied across the controls, fading only in the last quarter toward the content.
        val maskStops = arrayOf(0f to Color.Black, 0.75f to Color.Black, 1f to Color.Transparent)
        val reversedStops = arrayOf(0f to Color.Transparent, 0.25f to Color.Black, 1f to Color.Black)
        HazeBlurStyle {
            blurRadius(18.dp)
            noiseFactor(0f)
            backgroundColor(solid)
            colorEffects(listOf(HazeColorEffect.tint(solid.copy(alpha = 0.72f))))
            mask(Brush.verticalGradient(*if (edge == ScrollEdge.Top) maskStops else reversedStops))
            fallbackColorEffect(HazeColorEffect.tint(solid.copy(alpha = 0.9f)))
        }
    }
    return hazeBlur(input = HazeInput.Sources(state), style = style)
}
