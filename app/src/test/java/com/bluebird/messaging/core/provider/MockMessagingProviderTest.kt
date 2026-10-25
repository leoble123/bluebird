package com.bluebird.messaging.core.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.testing.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MockMessagingProviderTest {

    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    /** Runs [block] against a freshly seeded database, closing it afterwards. */
    private fun withProvider(seed: Boolean = true, replies: Boolean = false, block: suspend TestScope.(MockMessagingProvider) -> Unit) =
        runTest {
            val data = TestData(this, clock, seedSampleData = seed, replyScope = if (replies) backgroundScope else null)
            try {
                block(data.provider)
            } finally {
                data.close()
            }
        }

    @Test
    fun reportsItselfAsSimulated() = withProvider { provider ->
        assertTrue(provider.info.isSimulated)
    }

    @Test
    fun pinnedConversationsComeFirstThenMostRecent() = withProvider { provider ->
        val conversations = provider.observeConversations().first()
        val pinnedCount = conversations.count { it.isPinned }
        assertTrue(pinnedCount > 0)
        assertTrue(conversations.take(pinnedCount).all { it.isPinned })
        val rest = conversations.drop(pinnedCount).map { it.lastMessage!!.sentAt }
        assertEquals(rest.sortedDescending(), rest)
    }

    @Test
    fun sendingAMessageUpdatesThreadAndPreview() = withProvider { provider ->
        val sent = provider.sendMessage("t2", "  On my way  ")

        assertEquals("On my way", sent.text)
        assertTrue(sent.isOutgoing)
        assertEquals(DeliveryState.Sent, sent.deliveryState)
        assertEquals(sent, provider.observeMessages("t2").first().last())
        val preview = provider.observeConversations().first().first { it.id == "t2" }.lastMessage
        assertEquals(sent, preview)
    }

    @Test
    fun unreadCountsOnlyIncomingUnreadMessages() = withProvider { provider ->
        val byId = provider.observeConversations().first().associateBy { it.id }
        assertEquals(1, byId.getValue("t1").unreadCount)
        assertEquals(2, byId.getValue("t4").unreadCount)
        assertEquals(0, byId.getValue("t3").unreadCount)
    }

    @Test
    fun rejectsBlankMessages() = withProvider { provider ->
        assertTrue(runCatching { provider.sendMessage("t1", "   ") }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun rejectsMessagesToDeletedConversations() = withProvider { provider ->
        provider.deleteConversation("t3")
        assertTrue(runCatching { provider.sendMessage("t3", "hello?") }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun pinnedConversationsKeepTheOrderTheyWerePinnedIn() = withProvider { provider ->
        suspend fun pinnedIds() = provider.observeConversations().first().filter { it.isPinned }.map { it.id }
        assertEquals(listOf("t1", "t5", "t6"), pinnedIds())

        provider.setPinned("t1", pinned = false)
        provider.setPinned("t2", pinned = true)
        provider.setPinned("t1", pinned = true)

        assertEquals(listOf("t5", "t6", "t2", "t1"), pinnedIds())
    }

    @Test
    fun markReadAndUnread() = withProvider { provider ->
        suspend fun t1() = provider.observeConversations().first().first { it.id == "t1" }
        assertTrue(t1().hasUnread)

        provider.markRead("t1")
        assertEquals(0, t1().unreadCount)
        assertFalse(t1().hasUnread)

        provider.markUnread("t1")
        assertTrue(t1().hasUnread)
        assertEquals(0, t1().unreadCount)

        provider.markRead("t1")
        assertFalse(t1().hasUnread)
    }

    @Test
    fun deleteRemovesConversationAndItsMessages() = withProvider { provider ->
        provider.deleteConversation("t2")
        assertNull(provider.observeConversations().first().firstOrNull { it.id == "t2" })
        assertTrue(provider.observeMessages("t2").first().isEmpty())
        assertTrue(provider.searchMessages("slides").isEmpty())
    }

    @Test
    fun startConversationReusesAnExistingThreadOrCreatesAnEmptyOne() = withProvider { provider ->
        assertEquals("t1", provider.startConversation(listOf("c1")))
        assertEquals("t4", provider.startConversation(listOf("c5", "c4")))

        val created = provider.startConversation(listOf("c8", "c1"))
        val conversation = provider.observeConversations().first().first { it.id == created }
        assertNull(conversation.lastMessage)
        assertEquals(created, provider.startConversation(listOf("c1", "c8")))
    }

    @Test
    fun searchIsCaseInsensitiveNewestFirstAndTreatsWildcardsLiterally() = withProvider { provider ->
        val matches = provider.searchMessages("THE")
        assertTrue(matches.isNotEmpty())
        assertTrue(matches.all { it.message.text.contains("the", ignoreCase = true) })
        assertEquals(matches.map { it.message.sentAt }.sortedDescending(), matches.map { it.message.sentAt })
        assertTrue(provider.searchMessages("   ").isEmpty())
        assertTrue(provider.searchMessages("%").isEmpty())
        assertTrue(provider.searchMessages("_").isEmpty())
    }

    @Test
    fun sentMessagesAreDeliveredReadAndAnswered() = withProvider(replies = true) { provider ->
        val sent = provider.sendMessage("t2", "Got it")

        testScheduler.advanceTimeBy(900)
        testScheduler.runCurrent()
        assertEquals(DeliveryState.Delivered, provider.observeMessages("t2").first().first { it.id == sent.id }.deliveryState)

        testScheduler.advanceTimeBy(1_500)
        testScheduler.runCurrent()
        assertEquals(setOf("c2"), provider.observeTyping("t2").first())

        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()
        val thread = provider.observeMessages("t2").first()
        assertEquals(DeliveryState.Read, thread.first { it.id == sent.id }.deliveryState)
        // The reply shares the frozen clock's timestamp with the sent message, so find it by sender and id.
        val reply = thread.single { !it.isOutgoing && it.id.startsWith("m-") }
        assertEquals("c2", reply.senderId)
        assertTrue(provider.observeTyping("t2").first().isEmpty())
        assertTrue(provider.observeConversations().first().first { it.id == "t2" }.hasUnread)
    }

    @Test
    fun emptyProviderStillHasContacts() = withProvider(seed = false) { provider ->
        assertTrue(provider.observeConversations().first().isEmpty())
        assertTrue(provider.observeContacts().first().isNotEmpty())
    }

    @Test
    fun yourReactionReplacesOrTogglesButOthersStay() = withProvider { provider ->
        suspend fun reactions() = provider.observeMessages("t1").first().first { it.id == "m2" }.reactions
        assertEquals(listOf("c1"), reactions().map { it.senderId })

        provider.setReaction("m2", "👍")
        provider.setReaction("m2", "😂")
        assertEquals(setOf("❤️", "😂"), reactions().map { it.emoji }.toSet())

        provider.setReaction("m2", "😂")
        assertEquals(listOf("❤️"), reactions().map { it.emoji })
    }

    @Test
    fun deletingMessagesUpdatesThePreview() = withProvider { provider ->
        provider.deleteMessages(setOf("m3"))
        val t1 = provider.observeConversations().first().first { it.id == "t1" }
        assertEquals("m2", t1.lastMessage?.id)
        assertEquals(0, t1.unreadCount)
    }

    @Test
    fun repliesRememberWhatTheyQuote() = withProvider { provider ->
        val sent = provider.sendMessage("t1", "Sounds great", replyToId = "m3")
        assertEquals("m3", provider.observeMessages("t1").first().last { it.id == sent.id }.replyToId)
    }

    @Test
    fun sampleDataIsAddedOnlyOnce() = withProvider { provider ->
        provider.deleteConversation("t1")
        provider.ensureSeeded()
        assertNull(provider.observeConversations().first().firstOrNull { it.id == "t1" })
    }
}
