package com.bluebird.messaging.core.provider

import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.Conversation
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.core.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Fully offline, in-memory provider with realistic sample conversations.
 * Nothing here touches a network; [info] reports it as simulated.
 */
class MockMessagingProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) : MessagingProvider {

    override val info = ProviderInfo(displayName = "Mock (simulated)", isSimulated = true)

    private val nextId = AtomicLong(1000)
    private val contacts = MutableStateFlow(SampleData.contacts)
    private val messages = MutableStateFlow(SampleData.messages(clock.instant()))
    private val conversationMeta = MutableStateFlow(SampleData.conversationMeta)

    override fun observeContacts(): Flow<List<Contact>> =
        contacts.map { list -> list.sortedBy { it.displayName.lowercase() } }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messages.map { all -> all.filter { it.conversationId == conversationId }.sortedBy { it.sentAt } }

    override fun observeConversations(): Flow<List<Conversation>> =
        combine(conversationMeta, messages, contacts) { meta, allMessages, allContacts ->
            val byId = allContacts.associateBy { it.id }
            meta.map { m ->
                val thread = allMessages.filter { it.conversationId == m.id }
                Conversation(
                    id = m.id,
                    participants = m.participantIds.mapNotNull(byId::get),
                    lastMessage = thread.maxByOrNull { it.sentAt },
                    unreadCount = thread.count { !it.isOutgoing && it.deliveryState != DeliveryState.Read },
                    isPinned = m.isPinned,
                )
            }.sortedWith(
                compareByDescending<Conversation> { it.isPinned }
                    .thenByDescending { it.lastMessage?.sentAt ?: Instant.MIN }
                    .thenBy { it.id },
            )
        }

    override suspend fun sendMessage(conversationId: String, text: String): Message {
        require(text.isNotBlank()) { "Cannot send an empty message" }
        val message = Message(
            id = "m${nextId.incrementAndGet()}",
            conversationId = conversationId,
            senderId = null,
            text = text.trim(),
            sentAt = clock.instant(),
            deliveryState = DeliveryState.Sent,
        )
        messages.update { it + message }
        return message
    }

    internal data class ConversationMeta(
        val id: String,
        val participantIds: List<String>,
        val isPinned: Boolean,
    )

    private object SampleData {
        val contacts = listOf(
            Contact("c1", "Maya Chen", "+1 555 0101"),
            Contact("c2", "Jordan Ellis", "+1 555 0102"),
            Contact("c3", "Priya Raman", "priya@example.com"),
            Contact("c4", "Sam Okafor", "+1 555 0104"),
            Contact("c5", "Lena Fischer", "+1 555 0105"),
            Contact("c6", "Diego Alvarez", "+1 555 0106"),
        )

        val conversationMeta = listOf(
            ConversationMeta("t1", listOf("c1"), isPinned = true),
            ConversationMeta("t2", listOf("c2"), isPinned = false),
            ConversationMeta("t3", listOf("c3"), isPinned = false),
            ConversationMeta("t4", listOf("c4", "c5"), isPinned = false),
            ConversationMeta("t5", listOf("c6"), isPinned = true),
        )

        fun messages(now: Instant): List<Message> {
            fun ago(minutes: Long) = now.minus(Duration.ofMinutes(minutes))
            fun incoming(id: String, t: String, from: String, text: String, min: Long, read: Boolean = true) =
                Message(id, t, from, text, ago(min), if (read) DeliveryState.Read else DeliveryState.Delivered)
            fun outgoing(id: String, t: String, text: String, min: Long, state: DeliveryState = DeliveryState.Read) =
                Message(id, t, null, text, ago(min), state)

            return listOf(
                incoming("m1", "t1", "c1", "Are we still on for dinner tonight?", 95),
                outgoing("m2", "t1", "Yes! 7:30 at the usual place?", 92),
                incoming("m3", "t1", "c1", "Perfect. I'll grab a table 🙌", 4, read = false),
                incoming("m4", "t2", "c2", "Sent you the slides from today", 300),
                outgoing("m5", "t2", "Thanks, looking now", 290, DeliveryState.Delivered),
                incoming("m6", "t3", "c3", "Happy birthday!! 🎉", 60 * 26),
                outgoing("m7", "t3", "Thank you!! ❤️", 60 * 25),
                incoming("m8", "t4", "c4", "Who's bringing snacks Saturday?", 45, read = false),
                incoming("m9", "t4", "c5", "I can do chips and salsa", 40, read = false),
                incoming("m10", "t5", "c6", "Flight lands at 6:10", 60 * 3),
                outgoing("m11", "t5", "I'll be at arrivals", 60 * 3 - 5),
            )
        }
    }
}
