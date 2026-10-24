package com.bluebird.messaging.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.Radii
import com.bluebird.messaging.core.designsystem.theme.Spacing

/** Grouped content container (settings groups, contact details). Raised tone; a hairline edge in dark mode. */
@Composable
fun BluebirdCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = BluebirdTheme.colors
    Column(
        modifier = modifier
            .clip(Radii.card)
            .background(colors.surfaceRaised)
            .then(if (colors.isDark) Modifier.border(Dp.Hairline, colors.separator, Radii.card) else Modifier),
        content = content,
    )
}

@Composable
fun BluebirdDivider(modifier: Modifier = Modifier, startInset: Dp = 0.dp) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset),
        thickness = Dp.Hairline,
        color = BluebirdTheme.colors.separator,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = BluebirdTheme.typography.footnote,
        color = BluebirdTheme.colors.textSecondary,
        modifier = modifier
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            .semantics { heading() },
    )
}

/** A white glyph on a small rounded tile, for settings rows and list leading icons. */
@Composable
fun IconBadge(
    icon: ImageVector,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.27f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}

/** Compact status label, e.g. "Simulated" for the mock connection. */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = BluebirdTheme.colors.warm,
    container: Color = BluebirdTheme.colors.warmMuted,
    content: Color = BluebirdTheme.colors.warmStrong,
) {
    Row(
        modifier = modifier
            .clip(Radii.pill)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(6.dp))
        Text(text, style = BluebirdTheme.typography.caption, color = content)
    }
}
