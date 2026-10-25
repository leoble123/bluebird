package com.bluebird.messaging.core.provider

import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.Conversation
import com.bluebird.messaging.core.model.Message
import com.bluebird.messaging.core.model.MessageMatch
import kotlinx.coroutines.flow.Flow

/**
 * The boundary between Bluebird's UI and whatever actually moves messages.
 * Screens depend only on this interface, so a real transport can replace the mock
 * without screen changes.
 */
interface MessagingProvider {
    val info: ProviderInfo

    /** Pinned conversations first (in the order they were pinned), then most recent activity first. */
    fun observeConversations(): Flow<List<Conversation>>

    fun observeMessages(conversationId: String): Flow<List<Message>>

    fun observeContacts(): Flow<List<Contact>>

    suspend fun sendMessage(conversationId: String, text: String): Message

    suspend fun setPinned(conversationId: String, pinned: Boolean)

    /** Marks every incoming message read and clears any manual unread mark. */
    suspend fun markRead(conversationId: String)

    suspend fun markUnread(conversationId: String)

    suspend fun deleteConversation(conversationId: String)

    /** Returns the existing conversation with exactly these participants, or creates an empty one. */
    suspend fun startConversation(contactIds: List<String>): String

    /** Case-insensitive search of message text, newest first. */
    suspend fun searchMessages(query: String, limit: Int = 50): List<MessageMatch>
}

data class ProviderInfo(
    val displayName: String,
    /** True when no real network is involved. The UI must say so wherever connection status appears. */
    val isSimulated: Boolean,
)
