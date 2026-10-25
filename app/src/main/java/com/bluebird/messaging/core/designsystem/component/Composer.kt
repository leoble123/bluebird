package com.bluebird.messaging.core.designsystem.component

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
import com.bluebird.messaging.core.designsystem.theme.glass

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
) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    val canSend by remember(state) { derivedStateOf { state.text.isNotBlank() } }
    val isEmpty by remember(state) { derivedStateOf { state.text.isEmpty() } }
    val separator = colors.separator

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glass()
            .drawBehind { drawLine(separator, Offset.Zero, Offset(size.width, 0f), strokeWidth = 1f) }
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BluebirdIconButton(
            icon = BluebirdIcons.Plus,
            contentDescription = "Add attachment",
            onClick = onAttach,
            style = IconButtonStyle.Tonal,
            enabled = enabled,
            containerSize = 34.dp,
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
                .heightIn(min = FieldMinHeight)
                .clip(RoundedCornerShape(Radii.lg))
                .background(colors.surfaceRaised.copy(alpha = if (colors.isDark) 0.55f else 0.8f))
                .border(1.dp, colors.outline.copy(alpha = 0.6f), RoundedCornerShape(Radii.lg))
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicTextField(
                state = state,
                enabled = enabled,
                textStyle = typography.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 6),
                modifier = Modifier.weight(1f).padding(vertical = 9.dp),
                decorator = { innerTextField ->
                    Box {
                        if (isEmpty) Text(placeholder, style = typography.body, color = colors.textTertiary)
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
                    modifier = Modifier.size(40.dp),
                )
            }
            SendButton(enabled = enabled && canSend, onClick = onSend)
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
