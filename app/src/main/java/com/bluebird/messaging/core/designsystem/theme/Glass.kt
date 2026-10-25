package com.bluebird.messaging.core.designsystem.theme

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
