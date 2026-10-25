package com.bluebird.messaging.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.GlassMaterial
import com.bluebird.messaging.core.designsystem.theme.Radii
import com.bluebird.messaging.core.designsystem.theme.glass

/**
 * A frosted-glass popover menu anchored to its parent. Opens with a short scale-and-fade from [transformOrigin].
 */
@Composable
fun BluebirdMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    offset: IntOffset = IntOffset.Zero,
    transformOrigin: TransformOrigin = TransformOrigin(1f, 0f),
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    if (!visibility.currentState && !visibility.targetState) return

    val motion = BluebirdTheme.motion
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(motion.fade(120)) + scaleIn(motion.snappy(), initialScale = 0.9f, transformOrigin = transformOrigin),
            exit = fadeOut(motion.fade(100)) + scaleOut(motion.fade(100), targetScale = 0.95f, transformOrigin = transformOrigin),
        ) {
            MenuSurface(modifier, content)
        }
    }
}

/** The menu's glass panel on its own, for inline use and previews. */
@Composable
fun MenuSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = 220.dp, max = 300.dp)
            .shadow(18.dp, Radii.menu, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.18f))
            .glass(Radii.menu, GlassMaterial.Thick)
            .padding(vertical = 6.dp),
        content = content,
    )
}

@Composable
fun MenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    val colors = BluebirdTheme.colors
    val tint = if (destructive) colors.danger else colors.textPrimary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = BluebirdTheme.typography.body, color = tint, modifier = Modifier.weight(1f))
        if (icon != null) {
            Spacer(Modifier.width(16.dp))
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MenuDivider() {
    BluebirdDivider(Modifier.padding(vertical = 4.dp))
}
