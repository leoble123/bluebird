package com.bluebird.messaging.feature.inbox

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluebird.messaging.core.designsystem.component.Avatar
import com.bluebird.messaging.core.designsystem.component.BluebirdDivider
import com.bluebird.messaging.core.designsystem.component.BluebirdIconButton
import com.bluebird.messaging.core.designsystem.component.ConversationRow
import com.bluebird.messaging.core.designsystem.component.ConversationRowDividerInset
import com.bluebird.messaging.core.designsystem.component.EmptyState
import com.bluebird.messaging.core.designsystem.component.GroupAvatar
import com.bluebird.messaging.core.designsystem.component.LoadingState
import com.bluebird.messaging.core.designsystem.component.PlaceholderScaffold
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.ui.formatConversationTimestamp
import java.time.Instant

/** Interim inbox using the design system's rows. The full inbox experience arrives in Prompt 3. */
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onOpenConversation: (String) -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = remember(state.conversations) { Instant.now() }
    PlaceholderScaffold(
        title = "Messages",
        large = true,
        actions = {
            BluebirdIconButton(BluebirdIcons.People, "Contacts", onClick = onOpenContacts)
            BluebirdIconButton(BluebirdIcons.Settings, "Settings", onClick = onOpenSettings)
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.conversations.isEmpty() -> EmptyState(
                modifier = Modifier.padding(padding),
                icon = BluebirdIcons.Bubble,
                title = "No conversations yet",
                message = "Start a conversation and it will appear here.",
            )
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                itemsIndexed(state.conversations, key = { _, c -> c.id }) { index, conversation ->
                    val participants = conversation.participants
                    ConversationRow(
                        title = conversation.title,
                        preview = conversation.lastMessage?.text.orEmpty(),
                        timestamp = conversation.lastMessage?.let { formatConversationTimestamp(it.sentAt, now) }.orEmpty(),
                        unread = conversation.unreadCount > 0,
                        pinned = conversation.isPinned,
                        onClick = { onOpenConversation(conversation.id) },
                        avatar = {
                            if (participants.size > 1) {
                                GroupAvatar(
                                    participants[0].displayName to participants[0].id,
                                    participants[1].displayName to participants[1].id,
                                    size = AvatarSize.lg,
                                )
                            } else {
                                val p = participants.firstOrNull()
                                Avatar(p?.displayName.orEmpty(), p?.id ?: conversation.id, size = AvatarSize.lg)
                            }
                        },
                    )
                    if (index < state.conversations.lastIndex) BluebirdDivider(startInset = ConversationRowDividerInset)
                }
            }
        }
    }
}
