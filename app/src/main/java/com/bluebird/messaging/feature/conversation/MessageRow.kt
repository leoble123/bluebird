package com.bluebird.messaging.feature.conversation

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.component.Avatar
import com.bluebird.messaging.core.designsystem.component.BluebirdMenu
import com.bluebird.messaging.core.designsystem.component.MenuDivider
import com.bluebird.messaging.core.designsystem.component.MenuItem
import com.bluebird.messaging.core.designsystem.component.MessageBubble
import com.bluebird.messaging.core.designsystem.component.ReactionPickerRow
import com.bluebird.messaging.core.designsystem.component.ReactionPill
import com.bluebird.messaging.core.designsystem.component.SelectionIndicator
import com.bluebird.messaging.core.designsystem.component.rememberAnchoredMenuPosition
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.Spacing

private val GroupAvatarSize = 28.dp

/** Everything a person can do to a single message. */
internal class MessageActions(
    val openMenu: (String) -> Unit,
    val closeMenu: () -> Unit,
    val toggleTime: (String) -> Unit,
    val reply: (String) -> Unit,
    val copy: (String) -> Unit,
    val react: (String, String) -> Unit,
    val select: (String) -> Unit,
    val toggleSelected: (String) -> Unit,
    val delete: (String) -> Unit,
)

/**
 * One message in the timeline. Purely presentational: all text comes pre-formatted in [item], and per-row UI
 * state ([menuExpanded], [showTime], [selected]) is passed as plain booleans so only the affected rows recompose.
 */
@Composable
internal fun MessageRow(
    item: TimelineItem.MessageItem,
    selecting: Boolean,
    selected: Boolean,
    menuExpanded: Boolean,
    showTime: Boolean,
    actions: MessageActions,
) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    val gutter = BluebirdTheme.layout.gutter
    val haptics = LocalHapticFeedback.current
    val hasReactions = item.reactions.isNotEmpty()
    val myReaction = item.reactions.firstOrNull { it.mine }?.emoji

    val onTap = { if (selecting) actions.toggleSelected(item.id) else actions.toggleTime(item.id) }
    val onLongPress = {
        if (!selecting) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.openMenu(item.id)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = if (item.startsRun) Spacing.bubbleGroupBreak else Spacing.bubbleGroupGap)
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
            Spacer(Modifier.width(gutter - 4.dp))
            if (item.avatarColumn) {
                Box(Modifier.size(GroupAvatarSize)) {
                    item.avatar?.let { Avatar(it.name, it.seed, size = GroupAvatarSize) }
                }
                Spacer(Modifier.width(6.dp))
            }
            Row(
                Modifier.weight(1f),
                horizontalArrangement = if (item.outgoing) Arrangement.End else Arrangement.Start,
            ) {
                Box(Modifier.padding(top = if (hasReactions) 14.dp else 0.dp)) {
                    MessageBubble(
                        text = item.text,
                        outgoing = item.outgoing,
                        position = item.position,
                        selected = selected,
                        onClick = onTap,
                        onLongClick = onLongPress,
                        replyAuthor = item.reply?.author,
                        replyText = item.reply?.text,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = item.accessibilityLabel
                            if (selecting) this.selected = selected
                            onClick(label = if (selecting) "Toggle selection" else "Show time") { onTap(); true }
                            if (!selecting) {
                                onLongClick(label = "Message actions") { onLongPress(); true }
                                customActions = listOf(
                                    CustomAccessibilityAction("Reply") { actions.reply(item.id); true },
                                    CustomAccessibilityAction("Copy") { actions.copy(item.id); true },
                                    CustomAccessibilityAction("Delete") { actions.delete(item.id); true },
                                )
                            }
                        },
                    )
                    if (hasReactions) {
                        ReactionPill(
                            emoji = item.reactions.joinToString("") { it.emoji },
                            count = item.reactions.sumOf { it.count },
                            mine = myReaction != null,
                            modifier = Modifier
                                .align(if (item.outgoing) Alignment.TopStart else Alignment.TopEnd)
                                .offset(x = if (item.outgoing) (-24).dp else 24.dp, y = (-14).dp),
                        )
                    }
                    MessageMenu(item, menuExpanded, myReaction, actions)
                }
            }
            Spacer(Modifier.width(gutter - 4.dp))
        }
        val footer = when {
            showTime && item.status != null -> "${item.status} · ${item.time}"
            showTime -> item.time
            else -> item.status
        }
        if (footer != null) {
            Text(
                footer,
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
