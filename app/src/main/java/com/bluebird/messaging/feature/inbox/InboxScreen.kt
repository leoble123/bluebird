package com.bluebird.messaging.feature.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluebird.messaging.core.designsystem.component.PlaceholderMessage
import com.bluebird.messaging.core.designsystem.component.PlaceholderScaffold

/** Placeholder inbox: proves the provider → ViewModel → UI wiring. Designed for real in Prompt 3. */
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onOpenConversation: (String) -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlaceholderScaffold(
        title = "Bluebird",
        actions = {
            TextButton(onClick = onOpenContacts) { Text("Contacts") }
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        },
    ) { padding ->
        if (!state.isLoading && state.conversations.isEmpty()) {
            PlaceholderMessage("No conversations yet", padding)
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(state.conversations, key = { it.id }) { conversation ->
                    ListItem(
                        headlineContent = { Text(conversation.title) },
                        supportingContent = { Text(conversation.lastMessage?.text.orEmpty(), maxLines = 1) },
                        modifier = Modifier.clickable { onOpenConversation(conversation.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
