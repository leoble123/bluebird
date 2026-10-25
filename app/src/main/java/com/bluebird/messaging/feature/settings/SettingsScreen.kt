package com.bluebird.messaging.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.BuildConfig
import com.bluebird.messaging.core.designsystem.component.BluebirdCard
import com.bluebird.messaging.core.designsystem.component.BluebirdDivider
import com.bluebird.messaging.core.designsystem.component.IconBadge
import com.bluebird.messaging.core.designsystem.component.PlaceholderScaffold
import com.bluebird.messaging.core.designsystem.component.SectionHeader
import com.bluebird.messaging.core.designsystem.component.StatusPill
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme

/** Placeholder. Built out in Prompt 7. Already states plainly that the connection is simulated. */
@Composable
fun SettingsScreen(providerName: String, onOpenDesignCatalog: () -> Unit, onBack: () -> Unit) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    PlaceholderScaffold(
        title = "Settings",
        large = true,
        onBack = onBack,
        containerColor = colors.groupedBackground,
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            SectionHeader("Connection")
            BluebirdCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(BluebirdIcons.Bubble, colors.success)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(providerName, style = typography.body, color = colors.textPrimary)
                        Text("No real messages are sent or received.", style = typography.footnote, color = colors.textSecondary)
                    }
                    StatusPill("Simulated")
                }
            }
            SectionHeader("Developer", Modifier.padding(top = 16.dp))
            BluebirdCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onOpenDesignCatalog).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(BluebirdIcons.Sparkle, colors.accent)
                    Spacer(Modifier.width(12.dp))
                    Text("Design system", style = typography.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(BluebirdIcons.ChevronRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                }
                BluebirdDivider(startInset = 58.dp)
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(BluebirdIcons.Info, colors.textTertiary)
                    Spacer(Modifier.width(12.dp))
                    Text("Version", style = typography.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text(BuildConfig.VERSION_NAME, style = typography.body, color = colors.textSecondary)
                }
            }
        }
    }
}
