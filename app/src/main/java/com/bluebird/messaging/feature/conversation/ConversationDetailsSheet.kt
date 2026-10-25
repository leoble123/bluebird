package com.bluebird.messaging.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.component.Avatar
import com.bluebird.messaging.core.designsystem.component.BluebirdCard
import com.bluebird.messaging.core.designsystem.component.BluebirdDivider
import com.bluebird.messaging.core.designsystem.component.ContactRow
import com.bluebird.messaging.core.designsystem.component.ContactRowDividerInset
import com.bluebird.messaging.core.designsystem.component.SectionHeader
import com.bluebird.messaging.core.designsystem.component.StatusPill
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.feature.inbox.ConversationAvatar

/**
 * Details for the open conversation. Calling is shown but clearly marked unavailable, so the layout is ready
 * for it without pretending it works.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationDetailsSheet(state: ConversationUiState, onDismiss: () -> Unit) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.groupedBackground,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConversationAvatar(state.avatars, AvatarSize.xl)
            Spacer(Modifier.height(12.dp))
            Text(
                state.title,
                style = typography.title2,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (state.subtitle.isNotBlank()) {
                Text(state.subtitle, style = typography.subheadline, color = colors.textSecondary)
            }
            if (state.isSimulated) {
                Spacer(Modifier.height(8.dp))
                StatusPill("Simulated")
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UnavailableAction(BluebirdIcons.Phone, "Call", Modifier.weight(1f))
                UnavailableAction(BluebirdIcons.Video, "Video", Modifier.weight(1f))
            }
            Text(
                "Calling isn't available in this version.",
                style = typography.footnote,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 6.dp),
            )

            if (state.participants.size > 1) {
                SectionHeader("${state.participants.size} people", Modifier.fillMaxWidth().padding(top = 16.dp))
                BluebirdCard(Modifier.fillMaxWidth()) {
                    state.participants.forEachIndexed { index, p ->
                        ContactRow(
                            name = p.name,
                            detail = p.detail,
                            onClick = null,
                            avatar = { Avatar(p.avatar.name, p.avatar.seed, size = AvatarSize.md) },
                        )
                        if (index < state.participants.lastIndex) BluebirdDivider(startInset = ContactRowDividerInset)
                    }
                }
            }

            SectionHeader("About this conversation", Modifier.fillMaxWidth().padding(top = 16.dp))
            BluebirdCard(Modifier.fillMaxWidth()) {
                Text(
                    "Bluebird is currently a local prototype. These messages are simulated on this device: nothing is " +
                        "sent over a real network, and delivery, read receipts, typing and replies are all imitated.",
                    style = typography.subheadline,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UnavailableAction(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    val colors = BluebirdTheme.colors
    BluebirdCard(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clearAndSetSemantics { contentDescription = "$label, unavailable" },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(22.dp))
            Text(label, style = BluebirdTheme.typography.caption, color = colors.textTertiary)
        }
    }
}
