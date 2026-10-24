package com.bluebird.messaging.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.FrostTone
import com.bluebird.messaging.core.designsystem.theme.IconSize
import com.bluebird.messaging.core.designsystem.theme.MinTouchTarget
import com.bluebird.messaging.core.designsystem.theme.Radii
import com.bluebird.messaging.core.designsystem.theme.frost

enum class ButtonVariant {
    /** Solid cobalt. One per screen, for the main action. */
    Primary,
    /** Soft cobalt tint. Secondary actions that still deserve emphasis. */
    Tonal,
    /** Frosted. Actions floating over content. */
    Frost,
    /** Text only. */
    Plain,
    Destructive,
}

private const val DISABLED_ALPHA = 0.4f

@Composable
fun BluebirdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = BluebirdTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val (container, content) = when (variant) {
        ButtonVariant.Primary -> colors.accent to colors.onAccent
        ButtonVariant.Tonal -> colors.accentMuted to colors.onAccentMuted
        ButtonVariant.Frost -> Color.Transparent to colors.accent
        ButtonVariant.Plain -> Color.Transparent to colors.accent
        ButtonVariant.Destructive -> colors.danger.copy(alpha = 0.12f) to colors.danger
    }
    val surface = when (variant) {
        ButtonVariant.Frost -> Modifier.frost(Radii.pill, FrostTone.Neutral)
        else -> Modifier.clip(Radii.pill).background(container)
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = MinTouchTarget)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressScale(interaction)
            .then(surface)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = content),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (variant == ButtonVariant.Plain) 12.dp else 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(IconSize.sm + 2.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = BluebirdTheme.typography.headline, color = content)
    }
}

enum class IconButtonStyle { Plain, Frost, Filled, Tonal }

/**
 * Icon-only action. The visible container stays compact while the touch target is always at least 48dp.
 */
@Composable
fun BluebirdIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Plain,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
    containerSize: Dp = 36.dp,
) {
    val colors = BluebirdTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val iconTint = when {
        tint != Color.Unspecified -> tint
        style == IconButtonStyle.Filled -> colors.onAccent
        else -> colors.accent
    }
    val container = when (style) {
        IconButtonStyle.Plain -> Modifier
        IconButtonStyle.Frost -> Modifier.frost(CircleShape)
        IconButtonStyle.Filled -> Modifier.clip(CircleShape).background(colors.accent)
        IconButtonStyle.Tonal -> Modifier.clip(CircleShape).background(colors.accentMuted)
    }
    Box(
        modifier = modifier
            .size(maxOf(MinTouchTarget, containerSize))
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = containerSize / 2 + 4.dp, color = iconTint),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .pressScale(interaction, pressedScale = 0.88f)
                .size(containerSize)
                .then(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(IconSize.md))
        }
    }
}
