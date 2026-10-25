package com.bluebird.messaging.core.designsystem.component

import com.bluebird.messaging.core.designsystem.theme.ScrollEdge
import com.bluebird.messaging.core.designsystem.theme.liquidGlass
import com.bluebird.messaging.core.designsystem.theme.scrollEdgeEffect
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.Radii

private val FieldMinHeight: Dp = 40.dp

/**
 * The message composer: a frosted-glass bar holding an attachment button, a multiline field, and the send button.
 *
 * It applies the keyboard and navigation-bar insets itself, so the glass extends to the bottom edge while the
 * field rides above the keyboard; screens should not add imePadding around it. Typing goes through
 * [TextFieldState], so keystrokes never recompose the surrounding screen.
 */
@Composable
fun BluebirdComposer(
    state: TextFieldState,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Message",
    enabled: Boolean = true,
    onEmoji: (() -> Unit)? = null,
    /** Shown above the input row, inside the glass (e.g. the message being replied to). */
    header: (@Composable () -> Unit)? = null,
    /** A menu anchored to the attachment button (e.g. a [BluebirdMenu] opened by [onAttach]). */
    attachmentMenu: @Composable () -> Unit = {},
) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    val canSend by remember(state) { derivedStateOf { state.text.isNotBlank() } }
    val isEmpty by remember(state) { derivedStateOf { state.text.isEmpty() } }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scrollEdgeEffect(ScrollEdge.Bottom)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box {
                GlassIconButton(
                    icon = BluebirdIcons.Plus,
                    contentDescription = "Add attachment",
                    onClick = onAttach,
                    enabled = enabled,
                )
                attachmentMenu()
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = FloatingControlSize)
                    .liquidGlass(RoundedCornerShape(22.dp)),
            ) {
                header?.invoke()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    BasicTextField(
                        state = state,
                        enabled = enabled,
                        textStyle = typography.body.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.accent),
                        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 6),
                        modifier = Modifier.weight(1f).padding(vertical = 11.dp),
                        decorator = { innerTextField ->
                            Box {
                                if (isEmpty) Text(placeholder, style = typography.body, color = colors.textSecondary)
                                innerTextField()
                            }
                        },
                    )
                    if (onEmoji != null && isEmpty) {
                        BluebirdIconButton(
                            icon = BluebirdIcons.Smile,
                            contentDescription = "Emoji",
                            onClick = onEmoji,
                            tint = colors.textSecondary,
                            containerSize = 32.dp,
                            modifier = Modifier.size(FloatingControlSize),
                        )
                    }
                    // As in iOS, the send arrow only appears once there is something to send.
                    AnimatedVisibility(
                        visible = canSend,
                        enter = scaleIn(BluebirdTheme.motion.settle(), initialScale = 0.6f) + fadeIn(BluebirdTheme.motion.fade()),
                        exit = scaleOut(BluebirdTheme.motion.fade(), targetScale = 0.6f) + fadeOut(BluebirdTheme.motion.fade()),
                    ) {
                        SendButton(enabled = enabled && canSend, onClick = onSend)
                    }
                }
            }
        }
    }
}

/** Grows into place with a single soft "settle" when there is something to send. */
@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = BluebirdTheme.colors
    val motion = BluebirdTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val scale = animateFloatAsState(if (enabled) 1f else 0.82f, motion.settle(), label = "sendScale")
    val container by animateColorAsState(
        if (enabled) colors.accent else colors.surfaceSunken,
        motion.fade(),
        label = "sendColor",
    )
    val glyph = if (enabled) colors.onAccent else colors.textTertiary
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(32.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .pressScale(interaction, pressedScale = 0.86f)
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = "Send message",
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(BluebirdIcons.Send, contentDescription = "Send", tint = glyph, modifier = Modifier.size(19.dp))
    }
}

/** iOS-style filled search pill with a clear button. */
@Composable
fun BluebirdSearchField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    /** Called for the keyboard's Search key; typically clears focus to hide the keyboard. */
    onSearch: () -> Unit = {},
) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    val isEmpty by remember(state) { derivedStateOf { state.text.isEmpty() } }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(Radii.pill)
            .background(colors.surfaceSunken)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BluebirdIcons.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        BasicTextField(
            state = state,
            textStyle = typography.callout.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = { onSearch() },
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 9.dp, bottom = 9.dp),
            decorator = { innerTextField ->
                Box {
                    if (isEmpty) Text(placeholder, style = typography.callout, color = colors.textTertiary)
                    innerTextField()
                }
            },
        )
        if (!isEmpty) {
            BluebirdIconButton(
                icon = BluebirdIcons.Close,
                contentDescription = "Clear search",
                onClick = { state.clearText() },
                tint = colors.textSecondary,
                containerSize = 28.dp,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/** The quoted message shown inside the composer while replying, with a button to cancel. */
@Composable
fun ReplyComposerHeader(author: String, text: String, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BluebirdTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 14.dp, end = 2.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(32.dp).clip(Radii.pill).background(colors.accent))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Replying to $author", style = BluebirdTheme.typography.caption, color = colors.accent, maxLines = 1)
            Text(
                text,
                style = BluebirdTheme.typography.footnote,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BluebirdIconButton(
            icon = BluebirdIcons.Close,
            contentDescription = "Cancel reply",
            onClick = onCancel,
            tint = colors.textSecondary,
            containerSize = 28.dp,
        )
    }
}
