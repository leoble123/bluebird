package com.bluebird.messaging.feature.conversation

import androidx.compose.runtime.Composable
import com.bluebird.messaging.core.designsystem.component.PlaceholderMessage
import com.bluebird.messaging.core.designsystem.component.PlaceholderScaffold

/** Placeholder. The real timeline and composer arrive in Prompt 4. */
@Composable
fun ConversationScreen(conversationId: String, onBack: () -> Unit) {
    PlaceholderScaffold(title = "Conversation", onBack = onBack) { padding ->
        PlaceholderMessage("Conversation $conversationId\nTimeline and composer coming in a later phase.", padding)
    }
}
