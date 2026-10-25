package com.bluebird.messaging.feature.conversation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.component.Avatar
import com.bluebird.messaging.core.designsystem.component.BluebirdMenu
import com.bluebird.messaging.core.designsystem.component.MenuDivider
import com.bluebird.messaging.core.designsystem.component.MenuItem
import com.bluebird.messaging.core.designsystem.component.MessageBubble
import com.bluebird.messaging.core.designsystem.component.ReactionPickerRow
import com.bluebird.messaging.core.designsystem.component.SelectionIndicator
import com.bluebird.messaging.core.designsystem.component.TapbackBadges
import com.bluebird.messaging.core.designsystem.component.rememberAnchoredMenuPosition
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.Spacing
import kotlinx.coroutines.launch

private val GroupAvatarSize = 28.dp

/** How far a left swipe slides your bubbles aside to reveal timestamps. */
internal val TimestampRevealWidth: Dp = 72.dp

/** How far a bubble must be swiped right to start a reply. */
private val ReplySwipeThreshold: Dp = 64.dp

/** Everything a person can do to a single message. */
internal class MessageActions(
    val openMenu: (String) -> Unit,
    val closeMenu: () -> Unit,
    val reply: (String) -> Unit,
    val copy: (String) -> Unit,
    val react: (String, String) -> Unit,
    val select: (String) -> Unit,
    val toggleSelected: (String) -> Unit,
    val delete: (String) -> Unit,
    /** Left swipes anywhere in the timeline reveal every message's time, as in iOS. */
    val revealTimesBy: (Float) -> Unit,
    val endReveal: () -> Unit,
)

/**
 * One message in the timeline. Purely presentational: text comes pre-formatted in [item], per-row UI state arrives
 * as plain booleans, and the swipe offsets are read only in the draw phase so gestures never recompose the list.
 */
@Composable
internal fun MessageRow(
    item: TimelineItem.MessageItem,
    selecting: Boolean,
    selected: Boolean,
    menuExpanded: Boolean,
    /** Current timestamp reveal distance in pixels, read during drawing only. */
    revealPx: () -> Float,
    actions: MessageActions,
) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    val gutter = BluebirdTheme.layout.gutter
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val revealMax = with(density) { TimestampRevealWidth.toPx() }
    val replyThreshold = with(density) { ReplySwipeThreshold.toPx() }
    val replyOffset = remember { Animatable(0f) }
    val myReaction = item.reactions.firstOrNull { it.mine }?.emoji
    val hasReactions = item.reactions.isNotEmpty()

    val openMenu = {
        if (!selecting) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.openMenu(item.id)
        }
    }

    val swipe = if (selecting) {
        Modifier
    } else {
        Modifier.pointerInput(item.id) {
            var direction = 0 // -1: revealing times, 1: swiping to reply
            var crossed = false
            detectHorizontalDragGestures(
                onDragStart = {
                    direction = 0
                    crossed = false
                },
                onDragEnd = {
                    if (direction == 1) {
                        if (replyOffset.value >= replyThreshold) actions.reply(item.id)
                        scope.launch { replyOffset.animateTo(0f) }
                    } else if (direction == -1) {
                        actions.endReveal()
                    }
                },
                onDragCancel = {
                    scope.launch { replyOffset.animateTo(0f) }
                    actions.endReveal()
                },
            ) { change, dx ->
                if (direction == 0) direction = if (dx < 0) -1 else 1
                change.consume()
                if (direction == 1) {
                    val next = (replyOffset.value + dx).coerceIn(0f, replyThreshold * 1.25f)
                    if (!crossed && next >= replyThreshold) {
                        crossed = true
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
                    scope.launch { replyOffset.snapTo(next) }
                } else {
                    actions.revealTimesBy(-dx)
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .then(swipe)
            .then(
                if (selecting) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { actions.toggleSelected(item.id) },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        // The time, revealed at the trailing edge by a left swipe.
        Text(
            item.time,
            style = typography.caption,
            color = colors.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = gutter - 4.dp)
                .graphicsLayer {
                    val p = (revealPx() / revealMax).coerceIn(0f, 1f)
                    alpha = p
                    translationX = (1f - p) * revealMax * 0.5f
                },
        )
        // The reply arrow, revealed at the leading edge by a right swipe.
        Icon(
            BluebirdIcons.Reply,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(22.dp)
                .graphicsLayer {
                    val p = (replyOffset.value / replyThreshold).coerceIn(0f, 1f)
                    alpha = p
                    scaleX = 0.6f + 0.4f * p
                    scaleY = 0.6f + 0.4f * p
                },
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = if (item.startsRun) Spacing.bubbleGroupBreak else Spacing.bubbleGroupGap),
        ) {
            if (item.senderName != null) {
                Text(
                    item.senderName,
                    style = typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = gutter + GroupAvatarSize + 6.dp + 12.dp, bottom = 2.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                if (selecting) {
                    SelectionIndicator(selected, Modifier.align(Alignment.CenterVertically).padding(start = 12.dp))
                }
                Spacer(Modifier.width(gutter - 6.dp))
                if (item.avatarColumn) {
                    Box(Modifier.size(GroupAvatarSize)) {
                        item.avatar?.let { Avatar(it.name, it.seed, size = GroupAvatarSize) }
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = if (item.outgoing) Arrangement.End else Arrangement.Start,
                ) {
                    Box(
                        Modifier
                            .padding(top = if (hasReactions) 18.dp else 0.dp)
                            .graphicsLayer {
                                // Your bubbles slide aside to make room for the times; everyone's follow a reply swipe.
                                translationX = (if (item.outgoing) -revealPx() else 0f) + replyOffset.value
                            },
                    ) {
                        MessageBubble(
                            text = item.text,
                            outgoing = item.outgoing,
                            position = item.position,
                            selected = selected,
                            onClick = { if (selecting) actions.toggleSelected(item.id) },
                            onLongClick = openMenu,
                            onDoubleClick = openMenu,
                            replyAuthor = item.reply?.author,
                            replyText = item.reply?.text,
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = item.accessibilityLabel
                                if (selecting) {
                                    this.selected = selected
                                    onClick(label = "Toggle selection") { actions.toggleSelected(item.id); true }
                                } else {
                                    onLongClick(label = "Message actions") { openMenu(); true }
                                    customActions = listOf(
                                        CustomAccessibilityAction("Reply") { actions.reply(item.id); true },
                                        CustomAccessibilityAction("Copy") { actions.copy(item.id); true },
                                        CustomAccessibilityAction("Delete") { actions.delete(item.id); true },
                                    )
                                }
                            },
                        )
                        if (hasReactions) {
                            TapbackBadges(
                                tapbacks = item.reactions.map { it.emoji to it.mine },
                                bubbleOnStart = item.outgoing,
                                modifier = Modifier
                                    .align(if (item.outgoing) Alignment.TopStart else Alignment.TopEnd)
                                    .offset(x = if (item.outgoing) (-14).dp else 14.dp, y = (-20).dp),
                            )
                        }
                        MessageMenu(item, menuExpanded, myReaction, actions)
                    }
                }
                Spacer(Modifier.width(gutter - 6.dp))
            }
            if (item.status != null) {
                Text(
                    item.status,
                    style = typography.caption,
                    color = if (item.failed) colors.danger else colors.textSecondary,
                    textAlign = if (item.outgoing) TextAlign.End else TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = gutter + if (item.avatarColumn) GroupAvatarSize + 10.dp else 4.dp,
                            end = gutter + 2.dp,
                            top = 3.dp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun MessageMenu(item: TimelineItem.MessageItem, expanded: Boolean, myReaction: String?, actions: MessageActions) {
    BluebirdMenu(
        expanded = expanded,
        onDismissRequest = actions.closeMenu,
        positionProvider = rememberAnchoredMenuPosition(alignEnd = item.outgoing),
    ) {
        ReactionPickerRow(selected = myReaction, onPick = { emoji ->
            actions.closeMenu()
            actions.react(item.id, emoji)
        })
        MenuDivider()
        MenuItem("Reply", icon = BluebirdIcons.Reply, onClick = { actions.closeMenu(); actions.reply(item.id) })
        MenuItem("Copy", icon = BluebirdIcons.Copy, onClick = { actions.closeMenu(); actions.copy(item.id) })
        MenuItem("Select", icon = BluebirdIcons.Check, onClick = { actions.closeMenu(); actions.select(item.id) })
        MenuDivider()
        MenuItem(
            "Delete",
            icon = BluebirdIcons.Trash,
            destructive = true,
            onClick = { actions.closeMenu(); actions.delete(item.id) },
        )
    }
}

/** A centered time label between bursts of messages. */
@Composable
internal fun TimelineSeparator(label: String) {
    Text(
        label,
        style = BluebirdTheme.typography.caption,
        color = BluebirdTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
    )
}
