package com.bluebird.messaging.core.designsystem.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.random.Random

/**
 * Bluebird's icy frost: hazy, cool, softly refracted surfaces, like looking up through lake ice.
 *
 * Performance contract: no blur, no runtime shaders, no animation. Every layer is a static brush built once per
 * size change inside [drawWithCache] and replayed from the cached display list afterwards. Use it on a handful of
 * chrome surfaces (search, composer, floating bars, menus, selection), never behind every message bubble.
 */
enum class FrostTone { Neutral, Accent }

@Immutable
data class FrostSettings(
    /** False renders every frost surface as a plain solid surface (performance or accessibility fallback). */
    val enabled: Boolean = true,
    val grain: Boolean = true,
)

/** A tiny tiling noise texture, generated once, that gives frost its fine crystalline grain. */
internal object FrostGrain {
    private const val SIZE = 64

    val bitmap: ImageBitmap by lazy {
        val random = Random(seed = 0x1CE)
        val pixels = IntArray(SIZE * SIZE) {
            // Mostly transparent, with sparse white and dark specks of varying strength.
            val roll = random.nextInt(100)
            val alpha = random.nextInt(40, 140)
            when {
                roll < 18 -> (alpha shl 24) or 0xFFFFFF
                roll < 30 -> (alpha shl 24) or 0x0A1A2E
                else -> 0
            }
        }
        Bitmap.createBitmap(pixels, SIZE, SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}

/**
 * Paints a frost surface behind this element's content, clipped to [shape].
 *
 * @param rim draws the delicate edge light. Turn off for surfaces that sit flush against a screen edge.
 */
@Composable
fun Modifier.frost(
    shape: Shape = RectangleShape,
    tone: FrostTone = FrostTone.Neutral,
    rim: Boolean = true,
): Modifier {
    val colors = BluebirdTheme.colors.frost
    val settings = BluebirdTheme.frostSettings
    return this
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val hairline = 1.dp.toPx()
            // Twice the hairline, because the clip removes the outer half of the stroke.
            val rimStroke = Stroke(width = hairline * 2)
            val rimBrush = Brush.verticalGradient(
                0f to colors.rim,
                0.4f to colors.rim.copy(alpha = 0f),
                0.75f to Color.Transparent,
                1f to colors.rimShadow,
            )

            if (!settings.enabled) {
                return@drawWithCache onDrawWithContent {
                    drawOutline(outline, colors.solidFallback)
                    drawContent()
                    if (rim) drawOutline(outline, rimBrush, style = rimStroke)
                }
            }

            val (top, bottom) = when (tone) {
                FrostTone.Neutral -> colors.tintTop to colors.tintBottom
                FrostTone.Accent -> colors.accentTintTop to colors.accentTintBottom
            }
            val fill = Brush.verticalGradient(listOf(top, bottom))
            val glow = Brush.radialGradient(
                colors = listOf(colors.highlight, colors.highlight.copy(alpha = 0f)),
                center = Offset(size.width * 0.22f, -size.height * 0.15f),
                radius = max(size.width * 0.55f, size.height * 1.2f),
            )
            // Two faint diagonal streaks of light, offset so they never look mechanical.
            val bandA = diagonalBand(size.width * 0.18f, size.width * 0.34f, size.height, colors.band)
            val bandB = diagonalBand(size.width * 0.58f, size.width * 0.66f, size.height, colors.band.copy(alpha = colors.band.alpha * 0.6f))
            val grain = if (settings.grain) {
                ShaderBrush(ImageShader(FrostGrain.bitmap, TileMode.Repeated, TileMode.Repeated))
            } else {
                null
            }

            onDrawWithContent {
                drawOutline(outline, fill)
                drawRect(glow)
                drawRect(bandA)
                drawRect(bandB)
                if (grain != null) drawRect(grain, alpha = colors.grainAlpha)
                drawContent()
                if (rim) drawOutline(outline, rimBrush, style = rimStroke)
            }
        }
}

private fun diagonalBand(startX: Float, endX: Float, height: Float, color: Color): Brush {
    val slant = height * 0.9f
    return Brush.linearGradient(
        0f to Color.Transparent,
        0.45f to color,
        0.55f to color,
        1f to Color.Transparent,
        start = Offset(startX, 0f),
        end = Offset(endX + slant * 0.25f, slant * 0.25f),
    )
}

/**
 * The static "under the ice" screen backdrop: a cool vertical gradient with one soft glow from above.
 * Drawn once per size; the list scrolling above it does not redraw it.
 */
@Composable
fun Modifier.icyBackdrop(): Modifier {
    val colors = BluebirdTheme.colors
    return drawWithCache {
        val gradient = Brush.verticalGradient(listOf(colors.backdropTop, colors.backdropBottom))
        val glow = Brush.radialGradient(
            colors = listOf(colors.backdropGlow, Color.Transparent),
            center = Offset(size.width * 0.5f, 0f),
            radius = max(size.width, size.height * 0.6f),
        )
        onDrawBehind {
            drawRect(gradient)
            drawRect(glow)
        }
    }
}
