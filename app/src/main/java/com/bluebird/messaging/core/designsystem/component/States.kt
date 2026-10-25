package com.bluebird.messaging.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.Radii

/**
 * Centered empty state: an icon in a soft gray disc, a title, a short message, and an optional action.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconTint: Color = BluebirdTheme.colors.accent,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(76.dp).clip(CircleShape).background(colors.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = typography.title3, color = colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = typography.subheadline,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            BluebirdButton(text = actionLabel, onClick = onAction, variant = ButtonVariant.Tonal)
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
) {
    EmptyState(
        icon = BluebirdIcons.Warning,
        title = title,
        message = message,
        iconTint = BluebirdTheme.colors.danger,
        actionLabel = if (onRetry != null) "Try again" else null,
        onAction = onRetry,
        modifier = modifier,
    )
}

/**
 * Static skeleton rows shaped like the inbox. Deliberately not animated: a shimmer would cost frames for
 * a state that should only flash by.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier, rows: Int = 6) {
    val skeleton = BluebirdTheme.colors.skeleton
    Column(modifier.fillMaxWidth().semantics { contentDescription = "Loading" }) {
        repeat(rows) { index ->
            Row(
                Modifier.padding(start = 14.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(AvatarSize.lg).clip(CircleShape).background(skeleton))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.4f + (index % 3) * 0.12f).height(14.dp).clip(Radii.pill).background(skeleton))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.85f - (index % 2) * 0.2f).height(12.dp).clip(Radii.pill).background(skeleton))
                }
            }
        }
    }
}
