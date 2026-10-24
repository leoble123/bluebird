package com.bluebird.messaging.core.designsystem.component

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.frost

/**
 * Screen header. [large] shows an iOS-style large title under the bar. [frosted] turns the bar into a frost
 * surface, used once content scrolls beneath it so the title stays legible without a hard divider.
 */
@Composable
fun BluebirdTopBar(
    title: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    frosted: Boolean = false,
    subtitle: String? = null,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val typography = BluebirdTheme.typography
    val colors = BluebirdTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (frosted) Modifier.frost() else Modifier)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp)) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                navigation?.invoke()
            }
            if (!large) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp),
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
