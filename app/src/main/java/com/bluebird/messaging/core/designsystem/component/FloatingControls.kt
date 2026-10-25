package com.bluebird.messaging.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.liquidGlass

/** Height of the floating Liquid Glass controls (buttons, capsules, the bottom search bar). */
val FloatingControlSize: Dp = 44.dp

/**
 * A round Liquid Glass button floating above content, as used for Back, Compose and other bar actions in iOS 26.
 * Icons are monochrome (label color) unless [prominent], which tints the glass with the accent for a key action.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = FloatingControlSize,
    prominent: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = BluebirdTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .pressScale(interaction, pressedScale = 0.9f)
            .liquidGlass(CircleShape, tint = if (prominent) colors.accent else Color.Unspecified)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (prominent) colors.onAccent else colors.textPrimary,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

/** Several icon actions sharing one Liquid Glass capsule, the way iOS 26 groups related toolbar items. */
@Composable
fun GlassButtonGroup(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .height(FloatingControlSize)
            .liquidGlass(RoundedCornerShape(percent = 50))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** An icon action inside a [GlassButtonGroup]. */
@Composable
fun GroupedIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(FloatingControlSize)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = BluebirdTheme.colors.textPrimary, modifier = Modifier.size(21.dp))
    }
}

/** A text action in a Liquid Glass capsule (e.g. Cancel, Done). */
@Composable
fun GlassTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, prominent: Boolean = false) {
    val colors = BluebirdTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(FloatingControlSize)
            .pressScale(interaction, pressedScale = 0.94f)
            .liquidGlass(RoundedCornerShape(percent = 50), tint = if (prominent) colors.accent else Color.Unspecified)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = BluebirdTheme.typography.headline, color = if (prominent) colors.onAccent else colors.textPrimary)
    }
}

/**
 * The iOS 26 bottom search bar: a Liquid Glass capsule docked at the bottom of the screen, within thumb reach.
 */
@Composable
fun GlassSearchBar(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onSearch: () -> Unit = {},
) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    val isEmpty by remember(state) { derivedStateOf { state.text.isEmpty() } }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FloatingControlSize)
            .liquidGlass(RoundedCornerShape(percent = 50))
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BluebirdIcons.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(19.dp))
        BasicTextField(
            state = state,
            textStyle = typography.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = { onSearch() },
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            decorator = { innerTextField ->
                Box {
                    if (isEmpty) Text(placeholder, style = typography.body, color = colors.textSecondary)
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
                containerSize = 26.dp,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
