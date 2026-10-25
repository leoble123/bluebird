package com.bluebird.messaging.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
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
    /** Custom placement (e.g. [AnchoredMenuPosition]); overrides [alignment] and [offset]. */
    positionProvider: PopupPositionProvider? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    if (!visibility.currentState && !visibility.targetState) return

    val motion = BluebirdTheme.motion
    val body: @Composable () -> Unit = {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(motion.fade(120)) + scaleIn(motion.snappy(), initialScale = 0.9f, transformOrigin = transformOrigin),
            exit = fadeOut(motion.fade(100)) + scaleOut(motion.fade(100), targetScale = 0.95f, transformOrigin = transformOrigin),
        ) {
            MenuSurface(modifier, content)
        }
    }
    val properties = PopupProperties(focusable = true)
    if (positionProvider != null) {
        Popup(popupPositionProvider = positionProvider, onDismissRequest = onDismissRequest, properties = properties, content = body)
    } else {
        Popup(alignment = alignment, offset = offset, onDismissRequest = onDismissRequest, properties = properties, content = body)
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
    enabled: Boolean = true,
    /** A short second line, e.g. why an item is unavailable. */
    supportingText: String? = null,
) {
    val colors = BluebirdTheme.colors
    val tint = when {
        !enabled -> colors.textTertiary
        destructive -> colors.danger
        else -> colors.textPrimary
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = BluebirdTheme.typography.body, color = tint)
            if (supportingText != null) {
                Text(supportingText, style = BluebirdTheme.typography.footnote, color = colors.textTertiary)
            }
        }
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

/** The six tapbacks offered for messages, iOS-style. */
val Tapbacks: List<String> = listOf("❤️", "👍", "👎", "😂", "‼️", "❓")

/** A row of tapbacks at the top of a message menu; the one you've already chosen is highlighted. */
@Composable
fun ReactionPickerRow(selected: String?, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = BluebirdTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Tapbacks.forEach { emoji ->
            val isSelected = emoji == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.accent else Color.Transparent)
                    .clickable(role = Role.Button, onClickLabel = "React $emoji") { onPick(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, style = BluebirdTheme.typography.title3)
            }
        }
    }
}

/**
 * Places a popup just below its anchor, aligned to the anchor's start or end edge, flipping above the anchor when
 * there isn't room below. Keeps a margin from the window edges.
 */
class AnchoredMenuPosition(
    private val alignEnd: Boolean,
    private val gapPx: Int,
    private val marginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val endAligned = alignEnd != (layoutDirection == LayoutDirection.Rtl)
        val rawX = if (endAligned) anchorBounds.right - popupContentSize.width else anchorBounds.left
        val x = rawX.coerceIn(marginPx, (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx))
        val below = anchorBounds.bottom + gapPx
        val fitsBelow = below + popupContentSize.height <= windowSize.height - marginPx
        val y = if (fitsBelow) below else (anchorBounds.top - gapPx - popupContentSize.height)
        return IntOffset(x, y.coerceIn(marginPx, (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)))
    }
}

@Composable
fun rememberAnchoredMenuPosition(alignEnd: Boolean): AnchoredMenuPosition {
    val density = LocalDensity.current
    return remember(alignEnd, density) {
        with(density) { AnchoredMenuPosition(alignEnd, gapPx = 8.dp.roundToPx(), marginPx = 12.dp.roundToPx()) }
    }
}
