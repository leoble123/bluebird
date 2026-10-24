package com.bluebird.messaging.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.icyBackdrop

/** Temporary screen frame used until each feature gets its real design in later phases. */
@Composable
fun PlaceholderScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    large: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Column(Modifier.fillMaxSize().icyBackdrop()) {
        BluebirdTopBar(
            title = title,
            large = large,
            navigation = onBack?.let { { BackButton(onClick = it) } },
            actions = actions,
        )
        Box(Modifier.weight(1f)) { content(PaddingValues(0.dp)) }
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
