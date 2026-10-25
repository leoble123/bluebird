package com.bluebird.messaging.core.provider

import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.Conversation
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.core.model.Message
import com.bluebird.messaging.core.model.MessageMatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Fully offline, in-memory provider with realistic sample conversations.
 * Nothing here touches a network; [info] reports it as simulated.
 *
 * When [replyScope] is given, sent messages progress Sent → Delivered → Read and the other person sends a
 * canned reply a moment later, so previews and unread indicators can be seen updating live.
 */
class MockMessagingProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val replyScope: CoroutineScope? = null,
    seedSampleData: Boolean = true,
) : MessagingProvider {

    override val info = ProviderInfo(displayName = "Mock (simulated)", isSimulated = true)

    private val nextId = AtomicLong(1000)
    private val replyCounter = AtomicLong(0)
    private val contacts = MutableStateFlow(SampleData.contacts)
    private val messages = MutableStateFlow(if (seedSampleData) SampleData.messages(clock.instant()) else emptyList())
    private val conversationMeta = MutableStateFlow(if (seedSampleData) SampleData.conversationMeta else emptyList())

    override fun observeContacts(): Flow<List<Contact>> =
        contacts.map { list -> list.sortedBy { it.displayName.lowercase() } }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messages.map { all -> all.filter { it.conversationId == conversationId }.sortedBy { it.sentAt } }

    override fun observeConversations(): Flow<List<Conversation>> =
        combine(conversationMeta, messages, contacts) { meta, allMessages, allContacts ->
            val byId = allContacts.associateBy { it.id }
            val threads = allMessages.groupBy { it.conversationId }
            meta.map { m ->
                val thread = threads[m.id].orEmpty()
                Conversation(
                    id = m.id,
                    participants = m.participantIds.mapNotNull(byId::get),
                    lastMessage = thread.maxByOrNull { it.sentAt },
                    unreadCount = thread.count { !it.isOutgoing && it.deliveryState != DeliveryState.Read },
                    isPinned = m.pinnedOrder != null,
                    isMarkedUnread = m.markedUnread,
                ) to m.pinnedOrder
            }.sortedWith(
                compareBy<Pair<Conversation, Long?>> { it.second == null }
                    .thenBy { it.second ?: 0L }
                    .thenByDescending { it.first.lastMessage?.sentAt ?: Instant.MIN }
                    .thenBy { it.first.id },
            ).map { it.first }
        }

    override suspend fun sendMessage(conversationId: String, text: String): Message {
        require(text.isNotBlank()) { "Cannot send an empty message" }
        val message = Message(
            id = newId("m"),
            conversationId = conversationId,
            senderId = null,
            text = text.trim(),
            sentAt = clock.instant(),
            deliveryState = DeliveryState.Sent,
        )
        messages.update { it + message }
        replyScope?.launch { simulateDeliveryAndReply(message) }
        return message
    }

    override suspend fun setPinned(conversationId: String, pinned: Boolean) {
        conversationMeta.update { all ->
            val nextOrder = (all.mapNotNull { it.pinnedOrder }.maxOrNull() ?: 0L) + 1
            all.map {
                when {
                    it.id != conversationId -> it
                    pinned && it.pinnedOrder == null -> it.copy(pinnedOrder = nextOrder)
                    !pinned -> it.copy(pinnedOrder = null)
                    else -> it
                }
            }
        }
    }

    override suspend fun markRead(conversationId: String) {
        conversationMeta.update { all -> all.map { if (it.id == conversationId) it.copy(markedUnread = false) else it } }
        messages.update { all ->
            all.map {
                if (it.conversationId == conversationId && !it.isOutgoing && it.deliveryState != DeliveryState.Read) {
                    it.copy(deliveryState = DeliveryState.Read)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun markUnread(conversationId: String) {
        conversationMeta.update { all -> all.map { if (it.id == conversationId) it.copy(markedUnread = true) else it } }
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversationMeta.update { all -> all.filterNot { it.id == conversationId } }
        messages.update { all -> all.filterNot { it.conversationId == conversationId } }
    }

    override suspend fun startConversation(contactIds: List<String>): String {
        require(contactIds.isNotEmpty()) { "A conversation needs at least one participant" }
        val wanted = contactIds.toSet()
        conversationMeta.value.firstOrNull { it.participantIds.toSet() == wanted }?.let { return it.id }
        val id = newId("t")
        conversationMeta.update { it + ConversationMeta(id, contactIds.distinct(), pinnedOrder = null) }
        return id
    }

    override suspend fun searchMessages(query: String, limit: Int): List<MessageMatch> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val live = conversationMeta.value.map { it.id }.toSet()
        return messages.value
            .filter { it.conversationId in live && it.text.contains(q, ignoreCase = true) }
            .sortedByDescending { it.sentAt }
            .take(limit)
            .map { MessageMatch(it.conversationId, it) }
    }

    private suspend fun simulateDeliveryAndReply(sent: Message) {
        delay(800)
        setDeliveryState(sent.id, DeliveryState.Delivered)
        delay(1000)
        setDeliveryState(sent.id, DeliveryState.Read)
        delay(1200)
        val meta = conversationMeta.value.firstOrNull { it.id == sent.conversationId } ?: return
        val n = replyCounter.getAndIncrement().toInt()
        val sender = meta.participantIds[n % meta.participantIds.size]
        val reply = Message(
            id = newId("m"),
            conversationId = sent.conversationId,
            senderId = sender,
            text = SampleData.replies[n % SampleData.replies.size],
            sentAt = clock.instant(),
            deliveryState = DeliveryState.Delivered,
        )
        messages.update { it + reply }
    }

    private fun setDeliveryState(messageId: String, state: DeliveryState) {
        messages.update { all -> all.map { if (it.id == messageId) it.copy(deliveryState = state) else it } }
    }

    private fun newId(prefix: String) = "$prefix${nextId.incrementAndGet()}"

    internal data class ConversationMeta(
        val id: String,
        val participantIds: List<String>,
        /** Non-null when pinned; lower values were pinned earlier and show first. */
        val pinnedOrder: Long?,
        val markedUnread: Boolean = false,
    )

    private object SampleData {
        val contacts = listOf(
            Contact("c1", "Maya Chen", "+1 555 0101"),
            Contact("c2", "Jordan Ellis", "+1 555 0102"),
            Contact("c3", "Priya Raman", "priya@example.com"),
            Contact("c4", "Sam Okafor", "+1 555 0104"),
            Contact("c5", "Lena Fischer", "+1 555 0105"),
            Contact("c6", "Diego Alvarez", "+1 555 0106"),
            Contact("c7", "Noah Kim", "+1 555 0107"),
            Contact("c8", "Ava Thompson", "ava.t@example.com"),
            Contact("c9", "Mom", "+1 555 0109"),
            Contact("c10", "Theo Martin", "+1 555 0110"),
            Contact("c11", "Isabel Moreno", "+1 555 0111"),
        )

        val conversationMeta = listOf(
            ConversationMeta("t1", listOf("c1"), pinnedOrder = 1),
            ConversationMeta("t2", listOf("c2"), pinnedOrder = null),
            ConversationMeta("t3", listOf("c3"), pinnedOrder = null),
            ConversationMeta("t4", listOf("c4", "c5"), pinnedOrder = null),
            ConversationMeta("t5", listOf("c6"), pinnedOrder = 2),
            ConversationMeta("t6", listOf("c9"), pinnedOrder = 3),
            ConversationMeta("t7", listOf("c7"), pinnedOrder = null),
            ConversationMeta("t8", listOf("c8"), pinnedOrder = null),
            ConversationMeta("t9", listOf("c10", "c11", "c2"), pinnedOrder = null),
        )

        val replies = listOf(
            "Sounds good!",
            "Haha yes 😄",
            "On it.",
            "Let me check and get back to you",
            "Perfect, thanks!",
            "👍",
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
                incoming("m12", "t6", "c9", "Call me when you get a chance ❤️", 60 * 7),
                outgoing("m13", "t6", "Will do after work", 60 * 6),
                incoming("m14", "t7", "c7", "Did you see the game last night?", 60 * 30),
                outgoing("m15", "t7", "That ending was unreal", 60 * 29),
                incoming("m16", "t8", "c8", "Here's the recipe I mentioned: lemon, garlic, lots of parsley", 60 * 24 * 4),
                incoming("m17", "t9", "c10", "Hike is still on for Sunday morning", 60 * 24 * 9),
                incoming("m18", "t9", "c11", "I'll bring coffee for everyone ☕", 60 * 24 * 9 - 10),
                outgoing("m19", "t9", "Count me in", 60 * 24 * 9 - 30),
            )
        }
    }
}
