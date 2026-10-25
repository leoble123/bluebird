package com.bluebird.messaging.core.designsystem.component

import com.bluebird.messaging.core.designsystem.theme.Radii
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdMotion
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.glass

/**
 * Screen header. [large] shows an iOS-style large title under the bar. [glass] turns the bar into frosted glass
 * with a hairline underneath, for when content scrolls beneath it (place the bar over a [glassSource]).
 */
@Composable
fun BluebirdTopBar(
    title: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    glass: Boolean = false,
    subtitle: String? = null,
    /** For screens with a [LargeTitle] in their list: show the compact title only once that has scrolled away. */
    titleVisible: Boolean = true,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val typography = BluebirdTheme.typography
    val colors = BluebirdTheme.colors
    val separator = colors.separator
    val titleAlpha = animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = BluebirdTheme.motion.fade(BluebirdMotion.Durations.QUICK),
        label = "topBarTitle",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (glass) Modifier.glass().drawBehind { drawLine(separator, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f) } else Modifier)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp)) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                navigation?.invoke()
            }
            if (!large) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 96.dp)
                        .graphicsLayer { alpha = titleAlpha.value },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = typography.headline,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (subtitle != null) {
                        Text(subtitle, style = typography.caption2, color = colors.textSecondary, maxLines = 1)
                    }
                }
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        if (large) {
            Text(
                text = title,
                style = typography.largeTitle,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = BluebirdTheme.layout.gutter, end = BluebirdTheme.layout.gutter, bottom = 8.dp)
                    .semantics { heading() },
            )
        }
    }
}

/**
 * iOS-style large title placed as the first item of a scrolling list, so it scrolls away under the top bar.
 * Pair with `BluebirdTopBar(titleVisible = scrolledPastTitle)`.
 */
@Composable
fun LargeTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = BluebirdTheme.layout.gutter, end = BluebirdTheme.layout.gutter, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = BluebirdTheme.typography.largeTitle,
            color = BluebirdTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).semantics { heading() },
        )
        trailing()
    }
}

/** Standard back affordance for [BluebirdTopBar]. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BluebirdIconButton(
        icon = BluebirdIcons.Back,
        contentDescription = "Back",
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * iOS Messages-style conversation header: back on the left, the avatar with the name beneath it in the middle
 * (tap for details), and secondary actions on the right. Always glass, since the timeline scrolls beneath it.
 */
@Composable
fun ConversationTopBar(
    title: String,
    onBack: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    avatar: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    val separator = colors.separator
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glass()
            .drawBehind { drawLine(separator, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f) }
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(72.dp)
            .padding(horizontal = 4.dp),
    ) {
        BackButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 72.dp)
                .clip(Radii.card)
                .clickable(role = Role.Button, onClickLabel = "Show details", onClick = onOpenDetails)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(36.dp)) { avatar() }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    title,
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                )
                Icon(BluebirdIcons.ChevronRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(12.dp))
            }
            if (subtitle != null) {
                Text(subtitle, style = typography.caption2, color = colors.textSecondary, maxLines = 1)
            }
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
