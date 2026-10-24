package com.bluebird.messaging.core.provider

import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.Conversation
import com.bluebird.messaging.core.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * The boundary between Bluebird's UI and whatever actually moves messages.
 * Screens depend only on this interface, so a real transport can replace the mock
 * without screen changes.
 */
interface MessagingProvider {
    val info: ProviderInfo

    fun observeConversations(): Flow<List<Conversation>>

    fun observeMessages(conversationId: String): Flow<List<Message>>

    fun observeContacts(): Flow<List<Contact>>

    suspend fun sendMessage(conversationId: String, text: String): Message
}

data class ProviderInfo(
    val displayName: String,
    /** True when no real network is involved. The UI must say so wherever connection status appears. */
    val isSimulated: Boolean,
)
