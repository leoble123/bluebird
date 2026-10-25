package com.bluebird.messaging.core.provider

import com.bluebird.messaging.core.data.LocalMessageStore
import com.bluebird.messaging.core.data.PreferencesRepository
import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.Conversation
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.core.model.Message
import com.bluebird.messaging.core.model.MessageMatch
import com.bluebird.messaging.core.model.Reaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Fully offline provider with realistic sample conversations. Nothing here touches a network; [info] reports it
 * as simulated. Everything is stored in [store] (the local database), so it survives restarts; only "typing"
 * indicators are in memory, since they are momentary by nature.
 *
 * Sample data is added once, on first launch. When [replyScope] is given, sent messages progress
 * Sent → Delivered → Read, then the other person "types" and sends a canned reply.
 */
class MockMessagingProvider(
    private val store: LocalMessageStore,
    private val preferences: PreferencesRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val replyScope: CoroutineScope? = null,
    private val seedSampleData: Boolean = true,
    private val newId: (prefix: String) -> String = { prefix -> "$prefix-${UUID.randomUUID()}" },
) : MessagingProvider {

    override val info = ProviderInfo(displayName = "Mock (simulated)", isSimulated = true)

    private val replyCounter = AtomicLong(0)
    private val typing = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val seedLock = Mutex()
    @Volatile private var seeded = false

    /** Adds the sample contacts (and, unless disabled, sample conversations) the first time the app runs. */
    suspend fun ensureSeeded() {
        if (seeded) return
        seedLock.withLock {
            if (seeded) return
            if (!preferences.isSampleDataSeeded()) {
                store.upsertContacts(SampleData.contacts)
                if (seedSampleData && store.isEmpty()) {
                    val now = clock.instant()
                    SampleData.conversations.forEach { (id, contactIds, pinnedOrder) ->
                        store.createConversation(id, contactIds, now.minus(Duration.ofDays(30)), pinnedOrder)
                    }
                    store.upsertMessages(SampleData.longThread(now) + SampleData.messages(now))
                }
                preferences.markSampleDataSeeded()
            }
            seeded = true
        }
    }

    override fun observeConversations(): Flow<List<Conversation>> =
        store.observeConversations().onStart { ensureSeeded() }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        store.observeMessages(conversationId).onStart { ensureSeeded() }

    override fun observeContacts(): Flow<List<Contact>> =
        store.observeContacts().onStart { ensureSeeded() }

    override fun observeTyping(conversationId: String): Flow<Set<String>> =
        typing.map { it[conversationId].orEmpty() }

    override suspend fun sendMessage(conversationId: String, text: String, replyToId: String?): Message {
        require(text.isNotBlank()) { "Cannot send an empty message" }
        ensureSeeded()
        require(store.conversationExists(conversationId)) { "Conversation $conversationId no longer exists" }
        val message = Message(
            id = newId("m"),
            conversationId = conversationId,
            senderId = null,
            text = text.trim(),
            sentAt = clock.instant(),
            deliveryState = DeliveryState.Sent,
            replyToId = replyToId,
        )
        store.upsertMessages(listOf(message))
        replyScope?.launch { simulateDeliveryAndReply(message) }
        return message
    }

    // Every change waits for first-launch seeding, so it can never run against a half-filled database and then be
    // undone by sample data arriving afterwards.

    override suspend fun setReaction(messageId: String, emoji: String?) {
        ensureSeeded()
        store.setReaction(messageId, senderId = null, emoji = emoji, at = clock.instant())
    }

    override suspend fun deleteMessages(messageIds: Set<String>) {
        ensureSeeded()
        store.deleteMessages(messageIds)
    }

    override suspend fun setPinned(conversationId: String, pinned: Boolean) {
        ensureSeeded()
        store.setPinned(conversationId, pinned)
    }

    override suspend fun markRead(conversationId: String) {
        ensureSeeded()
        store.markRead(conversationId)
    }

    override suspend fun markUnread(conversationId: String) {
        ensureSeeded()
        store.markUnread(conversationId)
    }

    override suspend fun deleteConversation(conversationId: String) {
        ensureSeeded()
        store.deleteConversation(conversationId)
    }

    override suspend fun startConversation(contactIds: List<String>): String {
        require(contactIds.isNotEmpty()) { "A conversation needs at least one participant" }
        ensureSeeded()
        store.findConversation(contactIds.toSet())?.let { return it }
        val id = newId("t")
        store.createConversation(id, contactIds, clock.instant())
        return id
    }

    override suspend fun searchMessages(query: String, limit: Int): List<MessageMatch> {
        ensureSeeded()
        return store.searchMessages(query, limit)
    }

    private suspend fun simulateDeliveryAndReply(sent: Message) {
        delay(800)
        store.setDeliveryState(sent.id, DeliveryState.Delivered)
        delay(1000)
        store.setDeliveryState(sent.id, DeliveryState.Read)
        delay(400)
        val participants = store.participantIds(sent.conversationId)
        if (participants.isEmpty()) return
        val n = replyCounter.getAndIncrement().toInt()
        val sender = participants[n % participants.size]
        setTyping(sent.conversationId, sender, true)
        try {
            delay(1600)
        } finally {
            setTyping(sent.conversationId, sender, false)
        }
        if (!store.conversationExists(sent.conversationId)) return
        store.upsertMessages(
            listOf(
                Message(
                    id = newId("m"),
                    conversationId = sent.conversationId,
                    senderId = sender,
                    text = SampleData.replies[n % SampleData.replies.size],
                    sentAt = clock.instant(),
                    deliveryState = DeliveryState.Delivered,
                ),
            ),
        )
    }

    private fun setTyping(conversationId: String, contactId: String, isTyping: Boolean) {
        typing.update { all ->
            val current = all[conversationId].orEmpty()
            all + (conversationId to if (isTyping) current + contactId else current - contactId)
        }
    }

    internal object SampleData {
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

        /** Conversation id, participant ids, pin order (null when not pinned). */
        val conversations: List<Triple<String, List<String>, Long?>> = listOf(
            Triple("t1", listOf("c1"), 1L),
            Triple("t2", listOf("c2"), null),
            Triple("t3", listOf("c3"), null),
            Triple("t4", listOf("c4", "c5"), null),
            Triple("t5", listOf("c6"), 2L),
            Triple("t6", listOf("c9"), 3L),
            Triple("t7", listOf("c7"), null),
            Triple("t8", listOf("c8"), null),
            Triple("t9", listOf("c10", "c11", "c2"), null),
        )

        val replies = listOf(
            "Sounds good!",
            "Haha yes 😄",
            "On it.",
            "Let me check and get back to you",
            "Perfect, thanks!",
            "👍",
        )

        private val chatter = listOf(
            "Morning! Coffee later?", "Can't today, swamped with work", "How about tomorrow?", "Works for me",
            "Did you finish the report?", "Almost, sending it tonight", "Nice 👏", "Running 10 minutes late",
            "No worries", "Have you tried the new ramen place?", "Not yet, is it good?", "Honestly the best broth in town",
            "Okay we're going this week", "Deal", "Lol", "That meeting could have been an email",
            "Every single time", "Weekend plans?", "Hiking if the weather holds", "Send pics!",
        )

        /** An older back-and-forth with Jordan (t2), long enough to exercise scrolling and grouping. */
        fun longThread(now: Instant): List<Message> {
            var t = now.minus(Duration.ofDays(21))
            return (1..160).map { i ->
                // Bursts of quick messages separated by longer gaps, so separators and grouping both appear.
                t = t.plus(Duration.ofMinutes(if (i % 9 == 0) 60L * 24 else if (i % 4 == 0) 95L else 2L))
                val mine = (i / 2) % 3 == 1
                Message(
                    id = "h$i",
                    conversationId = "t2",
                    senderId = if (mine) null else "c2",
                    text = chatter[i % chatter.size],
                    sentAt = minOf(t, now.minus(Duration.ofMinutes(400))),
                    deliveryState = DeliveryState.Read,
                )
            }
        }

        fun messages(now: Instant): List<Message> {
            fun ago(minutes: Long) = now.minus(Duration.ofMinutes(minutes))
            fun incoming(id: String, t: String, from: String, text: String, min: Long, read: Boolean = true) =
                Message(id, t, from, text, ago(min), if (read) DeliveryState.Read else DeliveryState.Delivered)
            fun outgoing(id: String, t: String, text: String, min: Long, state: DeliveryState = DeliveryState.Read) =
                Message(id, t, null, text, ago(min), state)

            return listOf(
                incoming("m1", "t1", "c1", "Are we still on for dinner tonight?", 95),
                outgoing("m2", "t1", "Yes! 7:30 at the usual place?", 92).copy(reactions = listOf(Reaction("c1", "❤️"))),
                incoming("m3", "t1", "c1", "Perfect. I'll grab a table 🙌", 4, read = false).copy(replyToId = "m2"),
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
