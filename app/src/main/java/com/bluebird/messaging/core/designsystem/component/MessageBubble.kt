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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.FrostTone
import com.bluebird.messaging.core.designsystem.theme.Radii
import com.bluebird.messaging.core.designsystem.theme.frost
import com.bluebird.messaging.core.model.DeliveryState
import kotlin.math.min
import kotlin.math.roundToInt

/** Where a bubble sits within a run of consecutive messages from the same sender. */
enum class BubblePosition { Single, First, Middle, Last }

@Immutable
data class BubbleCorners(val topStart: Dp, val topEnd: Dp, val bottomEnd: Dp, val bottomStart: Dp)

/**
 * Corner geometry. Bubbles in a group tighten their corners where they touch; the last bubble of a group gets the
 * "ice facet", a sharply tapered outer-bottom corner that replaces a speech tail.
 */
fun bubbleCorners(outgoing: Boolean, position: BubblePosition): BubbleCorners {
    val round = Radii.bubble
    val joined = Radii.bubbleJoined
    val facet = Radii.bubbleFacet
    val (outerTop, outerBottom) = when (position) {
        BubblePosition.Single -> round to facet
        BubblePosition.First -> round to joined
        BubblePosition.Middle -> joined to joined
        BubblePosition.Last -> joined to facet
    }
    return if (outgoing) {
        BubbleCorners(topStart = round, topEnd = outerTop, bottomEnd = outerBottom, bottomStart = round)
    } else {
        BubbleCorners(topStart = outerTop, topEnd = round, bottomEnd = round, bottomStart = outerBottom)
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
 * A message bubble. Solid colors only: the timeline must stay cheap to draw at any length, so no frost here.
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
) {
    val colors = BluebirdTheme.colors
    val layout = BluebirdTheme.layout
    val jumbo = remember(text) { isJumboEmoji(text) }
    val interaction = remember { MutableInteractionSource() }
    val corners = bubbleCorners(outgoing, position)
    val shape = remember(corners) {
        RoundedCornerShape(corners.topStart, corners.topEnd, corners.bottomEnd, corners.bottomStart)
    }
    val clickable = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick ?: {},
            onLongClick = onLongClick,
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
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = BluebirdTheme.typography.body, color = content)
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
    val corners = bubbleCorners(outgoing = false, position = BubblePosition.Single)
    Box(
        modifier = modifier
            .semantics { contentDescription = "Typing" }
            .clip(RoundedCornerShape(corners.topStart, corners.topEnd, corners.bottomEnd, corners.bottomStart))
            .background(colors.bubbleIncoming)
            .size(width = 58.dp, height = 36.dp)
            .drawBehind {
                val radius = 3.6.dp.toPx()
                val gap = 9.dp.toPx()
                val startX = size.width / 2 - gap
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

/** A reaction ("tapback") pinned to the corner of a bubble. Your own reactions take the accent-tinted frost. */
@Composable
fun ReactionPill(
    emoji: String,
    modifier: Modifier = Modifier,
    mine: Boolean = false,
    count: Int = 1,
) {
    Row(
        modifier = modifier
            .frost(Radii.pill, if (mine) FrostTone.Accent else FrostTone.Neutral)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = BluebirdTheme.typography.callout)
        if (count > 1) {
            Spacer(Modifier.width(3.dp))
            Text(
                count.toString(),
                style = BluebirdTheme.typography.caption,
                color = if (mine) BluebirdTheme.colors.accent else BluebirdTheme.colors.textSecondary,
            )
        }
    }
}
