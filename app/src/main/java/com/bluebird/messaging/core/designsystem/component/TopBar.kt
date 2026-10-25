package com.bluebird.messaging.core.designsystem.component

import com.bluebird.messaging.core.designsystem.theme.Radii
import com.bluebird.messaging.core.designsystem.theme.ScrollEdge
import com.bluebird.messaging.core.designsystem.theme.liquidGlass
import com.bluebird.messaging.core.designsystem.theme.scrollEdgeEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
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

/**
 * Screen header in the iOS 26 style: floating controls over the content, no solid bar. [large] shows a large title
 * under the controls. [glass] adds the scroll edge effect, a soft blur over content scrolling beneath
 * (place the bar over a [glassSource]).
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
    /** Background the scroll edge effect fades into; match the screen (e.g. the grouped gray in Settings). */
    edgeColor: Color = BluebirdTheme.colors.background,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val typography = BluebirdTheme.typography
    val colors = BluebirdTheme.colors
    val titleAlpha = animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = BluebirdTheme.motion.fade(BluebirdMotion.Durations.QUICK),
        label = "topBarTitle",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (glass) Modifier.scrollEdgeEffect(ScrollEdge.Top, edgeColor) else Modifier)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp)) {
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
    GlassIconButton(icon = BluebirdIcons.Back, contentDescription = "Back", onClick = onClick, modifier = modifier)
}

/**
 * The iOS 26 Messages conversation header: a round glass Back button on the left and, in the middle, the
 * avatar with the name in a glass capsule beneath it (tap for details). No bar: the timeline scrolls under a
 * soft scroll edge effect.
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
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scrollEdgeEffect(ScrollEdge.Top)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 22.dp),
    ) {
        BackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(top = 6.dp))
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 64.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Show details",
                    onClick = onOpenDetails,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(52.dp)) { avatar() }
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .liquidGlass(RoundedCornerShape(percent = 50), interaction)
                    .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = typography.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                )
                Icon(BluebirdIcons.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(12.dp))
            }
            if (subtitle != null) {
                Text(subtitle, style = typography.caption2, color = colors.textSecondary, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
