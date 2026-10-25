package com.bluebird.messaging.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.GlassHost
import com.bluebird.messaging.core.designsystem.theme.glassSource

/**
 * Temporary screen frame used until each feature gets its real design in later phases.
 *
 * Content is laid out full-height and scrolls beneath a glass top bar; [content] receives the padding it should
 * apply (for a list, as contentPadding) so nothing starts hidden under the bar.
 */
@Composable
fun PlaceholderScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    large: Boolean = false,
    containerColor: Color = BluebirdTheme.colors.background,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    GlassHost {
        Scaffold(
            containerColor = containerColor,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                BluebirdTopBar(
                    title = title,
                    large = large,
                    glass = true,
                    navigation = onBack?.let { { BackButton(onClick = it) } },
                    actions = actions,
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().glassSource()) { content(padding) }
        }
    }
}

@Composable
fun PlaceholderMessage(text: String, padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = BluebirdTheme.typography.callout,
            color = BluebirdTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
