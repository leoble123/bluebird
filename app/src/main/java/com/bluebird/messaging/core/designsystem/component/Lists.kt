package com.bluebird.messaging.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.Radii
import com.bluebird.messaging.core.designsystem.theme.glass
import kotlinx.coroutines.launch

/** An action revealed by swiping a row. [removesItem] actions leave the row swiped away (e.g. delete). */
@Immutable
data class SwipeAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val removesItem: Boolean = false,
    val onAction: () -> Unit,
)

/**
 * iOS-style swipe actions: swipe right for [startAction], left for [endAction]. A full swipe past ~35% of the
 * width performs the action with a haptic tick. Swipes are a shortcut only; pair them with a long-press menu
 * and accessibility actions so nothing depends on the gesture.
 */
@Composable
fun SwipeActionRow(
    startAction: SwipeAction?,
    endAction: SwipeAction?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    // Deliberately not saveable: a row that was swiped away and then restored by undo must come back settled.
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled) { distance -> distance * 0.35f } }
    val background = BluebirdTheme.colors.background
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = startAction != null,
        enableDismissFromEndToStart = endAction != null,
        onDismiss = { direction ->
            val action = if (direction == SwipeToDismissBoxValue.StartToEnd) startAction else endAction
            if (action != null) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                action.onAction()
                if (!action.removesItem) scope.launch { state.reset() }
            }
        },
        backgroundContent = { SwipeBackground(state, startAction, endAction) },
    ) {
        Box(Modifier.fillMaxWidth().drawBehind { drawRect(background) }) { content() }
    }
}

@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState, start: SwipeAction?, end: SwipeAction?) {
    val action = when (state.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> start
        SwipeToDismissBoxValue.EndToStart -> end
        SwipeToDismissBoxValue.Settled -> null
    } ?: return
    val alignStart = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
    val color = action.color
    Box(
        Modifier.fillMaxWidth().fillMaxHeight().drawBehind { drawRect(color) }.padding(horizontal = 24.dp),
        contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(action.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text(action.label, style = BluebirdTheme.typography.caption, color = Color.White)
        }
    }
}

/**
 * A floating glass pill confirming an action, with an undo button. Place it at the bottom of a screen inside a
 * GlassHost so it blurs the list behind it.
 */
@Composable
fun UndoBar(
    visible: Boolean,
    message: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "Undo",
) {
    val motion = BluebirdTheme.motion
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(motion.fade()) + slideInVertically(motion.gentle()) { it / 2 },
        exit = fadeOut(motion.fade()) + slideOutVertically(motion.gentle()) { it / 2 },
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .glass(Radii.pill)
                .padding(start = 20.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                style = BluebirdTheme.typography.callout,
                color = BluebirdTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            BluebirdButton(actionLabel, onClick = onUndo, variant = ButtonVariant.Plain)
        }
    }
}

/** A contact in a list or picker. */
@Composable
fun ContactRow(
    name: String,
    detail: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
) {
    val colors = BluebirdTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = BluebirdTheme.layout.gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(AvatarSize.md)) { avatar() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = BluebirdTheme.typography.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!detail.isNullOrBlank() && detail != name) {
                Text(detail, style = BluebirdTheme.typography.footnote, color = colors.textSecondary, maxLines = 1)
            }
        }
    }
}

/** Divider inset that lines up with [ContactRow] text. */
val ContactRowDividerInset: Dp = 16.dp + AvatarSize.md + 12.dp

/**
 * A pinned conversation: a large avatar with the name beneath, shown in a grid above the inbox list.
 * Unread pins get an accent dot at the avatar's shoulder.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedTile(
    name: String,
    unread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    avatarSize: Dp = 64.dp,
    avatar: @Composable () -> Unit,
) {
    val colors = BluebirdTheme.colors
    val ring = colors.background
    val dot = colors.unread
    Column(
        modifier = modifier
            .clip(Radii.card)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(avatarSize)) {
            avatar()
            if (unread) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset((-2).dp, (-2).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .drawBehind { drawRect(ring) }
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(dot),
                )
            }
        }
        Text(
            name,
            style = BluebirdTheme.typography.caption,
            color = if (unread) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(avatarSize + 16.dp),
        )
    }
}
