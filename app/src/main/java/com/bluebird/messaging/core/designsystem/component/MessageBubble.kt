package com.bluebird.messaging.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.Radii
import com.bluebird.messaging.core.model.DeliveryState
import kotlin.math.min
import kotlin.math.roundToInt

/** Where a bubble sits within a run of consecutive messages from the same sender. */
enum class BubblePosition { Single, First, Middle, Last }

/** Width reserved on the outer side of every bubble for the tail, so bubbles in a group stay aligned. */
val BubbleTailWidth: Dp = 6.dp

/** As in iOS Messages, only the last bubble of a run from one sender carries a tail. */
fun bubbleHasTail(position: BubblePosition): Boolean =
    position == BubblePosition.Single || position == BubblePosition.Last

/**
 * The iOS Messages bubble: a fully rounded body, plus a small curled tail at the bottom outer corner when [tail]
 * is set. The body always stops [BubbleTailWidth] short of the outer edge so tailed and untailed bubbles align.
 */
class BubbleShape(
    private val outgoing: Boolean,
    private val tail: Boolean,
    private val radius: Dp = Radii.bubble,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        val w = size.width
        val h = size.height
        val t = BubbleTailWidth.toPx()
        val bodyRight = w - t
        val r = min(radius.toPx(), min(bodyRight, h) / 2f)
        val body = Path().apply {
            addRoundRect(RoundRect(0f, 0f, bodyRight, h, CornerRadius(r)))
        }
        val path = if (!tail) {
            body
        } else {
            val horn = Path().apply {
                // Fill the body's outer bottom corner square, then add the hook.
                moveTo(bodyRight - r, h - r)
                lineTo(bodyRight, h - r)
                lineTo(bodyRight, h - 12.dp.toPx())
                // Outer edge sweeps down and out to the tip at the bottom corner…
                cubicTo(bodyRight, h - 5.dp.toPx(), bodyRight + t * 0.6f, h - 1.dp.toPx(), w, h)
                // …and the inner edge curls back up and over into the bottom of the body.
                cubicTo(w - 3.dp.toPx(), h - 1.dp.toPx(), bodyRight, h - 5.dp.toPx(), bodyRight - 5.dp.toPx(), h - 4.dp.toPx())
                cubicTo(bodyRight - 8.dp.toPx(), h - 3.5.dp.toPx(), bodyRight - 10.dp.toPx(), h, bodyRight - 14.dp.toPx(), h)
                lineTo(bodyRight - r, h)
                close()
            }
            Path().apply { op(body, horn, PathOperation.Union) }
        }
        val mirror = outgoing == (layoutDirection == LayoutDirection.Rtl)
        if (mirror) {
            path.asAndroidPath().transform(android.graphics.Matrix().apply { setScale(-1f, 1f, w / 2f, 0f) })
        }
        Outline.Generic(path)
    }
}

/** True for short messages made only of emoji (up to three), which render large and without a bubble. */
fun isJumboEmoji(text: String): Boolean {
    val trimmed = text.filterNot { it.isWhitespace() }
    if (trimmed.isEmpty()) return false
    var symbols = 0
    var i = 0
    while (i < trimmed.length) {
        val cp = trimmed.codePointAt(i)
        i += Character.charCount(cp)
        when {
            cp == 0x200D || cp == 0xFE0F || cp in 0x1F3FB..0x1F3FF || cp in 0xE0020..0xE007F -> Unit // joiners, modifiers, tags
            cp in 0x1F1E6..0x1F1FF -> symbols += 1 // regional indicators: a flag is two, counted as one below
            Character.getType(cp) == Character.OTHER_SYMBOL.toInt() -> symbols += 2
            else -> return false
        }
    }
    // Each emoji counts as 2 (flags as 1 + 1), so up to three emoji means at most 6.
    return symbols in 1..6
}

fun deliveryLabel(state: DeliveryState, time: String?): String = when (state) {
    DeliveryState.Sending -> "Sending…"
    DeliveryState.Sent -> "Sent"
    DeliveryState.Delivered -> "Delivered"
    DeliveryState.Read -> if (time != null) "Read $time" else "Read"
    DeliveryState.Failed -> "Not delivered"
}

/** Caps a bubble's width at a fraction of the available width, without a costly BoxWithConstraints per item. */
fun Modifier.bubbleWidth(fraction: Float, max: Dp): Modifier = layout { measurable, constraints ->
    val cap = min((constraints.maxWidth * fraction).roundToInt(), max.roundToPx()).coerceAtLeast(constraints.minWidth)
    val placeable = measurable.measure(constraints.copy(maxWidth = cap))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** Lays a bubble out on the correct side of the timeline with standard gutters. */
@Composable
fun BubbleRow(
    outgoing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = BluebirdTheme.layout.gutter - 4.dp),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
    ) {
        content()
    }
}

/**
 * A message bubble. Solid colors only: the timeline must stay cheap to draw at any length, so no glass here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    text: String,
    outgoing: Boolean,
    modifier: Modifier = Modifier,
    position: BubblePosition = BubblePosition.Single,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    /** Double-tap, as in iOS, opens the Tapback picker. */
    onDoubleClick: (() -> Unit)? = null,
    /** When this message is a reply: who wrote the quoted message, and its text. */
    replyAuthor: String? = null,
    replyText: String? = null,
) {
    val colors = BluebirdTheme.colors
    val layout = BluebirdTheme.layout
    val isReply = replyText != null
    val jumbo = remember(text, isReply) { !isReply && isJumboEmoji(text) }
    val interaction = remember { MutableInteractionSource() }
    val tail = bubbleHasTail(position)
    val shape = remember(outgoing, tail) { BubbleShape(outgoing, tail) }
    val clickable = if (onClick != null || onLongClick != null || onDoubleClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick ?: {},
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
        )
    } else {
        Modifier
    }

    if (jumbo) {
        Text(
            text = text,
            style = BluebirdTheme.typography.body.copy(fontSize = 44.sp, lineHeight = 52.sp),
            modifier = modifier.pressScale(interaction).then(clickable).padding(horizontal = 4.dp),
        )
        return
    }

    val container = when {
        selected && outgoing -> colors.accentPressed
        selected -> colors.outline
        outgoing -> colors.bubbleOutgoing
        else -> colors.bubbleIncoming
    }
    val content = if (outgoing) colors.onBubbleOutgoing else colors.onBubbleIncoming
    Box(
        modifier = modifier
            .bubbleWidth(layout.bubbleMaxWidthFraction, layout.bubbleMaxWidth)
            .pressScale(interaction, pressedScale = 0.97f)
            .clip(shape)
            .background(container)
            .then(clickable)
            .padding(
                start = if (outgoing) 12.dp else 12.dp + BubbleTailWidth,
                end = if (outgoing) 12.dp + BubbleTailWidth else 12.dp,
                top = 7.dp,
                bottom = 7.dp,
            ),
    ) {
        Column {
            if (isReply) ReplyQuote(replyAuthor.orEmpty(), replyText.orEmpty(), outgoing)
            Text(text = text, style = BluebirdTheme.typography.body, color = content)
        }
    }
}

/** The quoted message at the top of a reply bubble. */
@Composable
private fun ReplyQuote(author: String, text: String, outgoing: Boolean) {
    val colors = BluebirdTheme.colors
    val bar = if (outgoing) colors.onBubbleOutgoing.copy(alpha = 0.7f) else colors.accent
    val secondary = if (outgoing) colors.onBubbleOutgoingSecondary else colors.textSecondary
    Row(Modifier.padding(bottom = 6.dp).height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().clip(Radii.pill).background(bar))
        Column(Modifier.padding(start = 8.dp)) {
            if (author.isNotEmpty()) {
                Text(author, style = BluebirdTheme.typography.caption, color = if (outgoing) colors.onBubbleOutgoing else colors.accent, maxLines = 1)
            }
            Text(text, style = BluebirdTheme.typography.footnote, color = secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The small status line under the latest outgoing message. */
@Composable
fun DeliveryStatus(state: DeliveryState, time: String?, modifier: Modifier = Modifier) {
    val colors = BluebirdTheme.colors
    val failed = state == DeliveryState.Failed
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (failed) {
            Icon(BluebirdIcons.Warning, contentDescription = null, tint = colors.danger, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = deliveryLabel(state, time),
            style = BluebirdTheme.typography.caption,
            color = when {
                failed -> colors.danger
                state == DeliveryState.Sending -> colors.textTertiary
                else -> colors.textSecondary
            },
        )
    }
}

/**
 * Three softly pulsing dots in an incoming bubble. The animation only touches the draw phase, and it stops
 * entirely when the system has animations turned off.
 */
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val colors = BluebirdTheme.colors
    val motion = BluebirdTheme.motion
    val dot = colors.textSecondary
    val phase: State<Float>? = if (motion.loops) {
        rememberInfiniteTransition(label = "typing").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
            label = "typingPhase",
        )
    } else {
        null
    }
    val shape = remember { BubbleShape(outgoing = false, tail = true) }
    Box(
        modifier = modifier
            .semantics { contentDescription = "Typing" }
            .clip(shape)
            .background(colors.bubbleIncoming)
            .size(width = 64.dp, height = 36.dp)
            .drawBehind {
                val radius = 3.6.dp.toPx()
                val gap = 9.dp.toPx()
                val tailPx = BubbleTailWidth.toPx()
                val startX = tailPx + (size.width - tailPx) / 2 - gap
                val p = phase?.value
                for (i in 0..2) {
                    val alpha = if (p == null) {
                        0.55f
                    } else {
                        val local = ((p - i * 0.18f) % 1f + 1f) % 1f
                        0.3f + 0.7f * (1f - kotlin.math.abs(local * 2f - 1f)).coerceIn(0f, 1f)
                    }
                    drawCircle(dot.copy(alpha = alpha), radius, Offset(startX + gap * i, size.height / 2))
                }
            },
    )
}

/**
 * Tapbacks, iOS-style: each reaction is a small round badge ringed in the background color, stacked when there are
 * several, with a little trailing dot pointing at the bubble. Yours is drawn in the accent color.
 */
@Composable
fun TapbackBadges(
    tapbacks: List<Pair<String, Boolean>>,
    modifier: Modifier = Modifier,
    /** Which side of the badges the bubble is on, so the trailing dot points at it. */
    bubbleOnStart: Boolean = true,
) {
    val colors = BluebirdTheme.colors
    val ring = colors.background
    val shown = tapbacks.take(3)
    Box(modifier) {
        shown.forEachIndexed { index, (emoji, mine) ->
            Box(
                Modifier
                    .offset(x = (index * 16).dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ring)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (mine) colors.accent else colors.bubbleIncoming),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, style = BluebirdTheme.typography.footnote.copy(fontSize = 15.sp, lineHeight = 17.sp))
            }
        }
        val dotColor = if (shown.firstOrNull()?.second == true) colors.accent else colors.bubbleIncoming
        Box(
            Modifier
                .align(if (bubbleOnStart) Alignment.BottomStart else Alignment.BottomEnd)
                .offset(x = if (bubbleOnStart) (-1).dp else 1.dp, y = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(ring)
                .padding(1.5.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}
