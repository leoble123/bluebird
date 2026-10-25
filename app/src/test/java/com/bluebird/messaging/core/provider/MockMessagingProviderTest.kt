package com.bluebird.messaging.core.provider

import com.bluebird.messaging.core.model.DeliveryState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MockMessagingProviderTest {

    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val provider = MockMessagingProvider(Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun reportsItselfAsSimulated() {
        assertTrue(provider.info.isSimulated)
    }

    @Test
    fun pinnedConversationsComeFirstThenMostRecent() = runTest {
        val conversations = provider.observeConversations().first()
        val pinnedCount = conversations.count { it.isPinned }
        assertTrue(pinnedCount > 0)
        assertTrue(conversations.take(pinnedCount).all { it.isPinned })
        val rest = conversations.drop(pinnedCount).map { it.lastMessage!!.sentAt }
        assertEquals(rest.sortedDescending(), rest)
    }

    @Test
    fun sendingAMessageUpdatesThreadAndPreview() = runTest {
        val sent = provider.sendMessage("t2", "  On my way  ")

        assertEquals("On my way", sent.text)
        assertTrue(sent.isOutgoing)
        assertEquals(DeliveryState.Sent, sent.deliveryState)
        assertEquals(sent, provider.observeMessages("t2").first().last())
        val preview = provider.observeConversations().first().first { it.id == "t2" }.lastMessage
        assertEquals(sent, preview)
    }

    @Test
    fun unreadCountsOnlyIncomingUnreadMessages() = runTest {
        val byId = provider.observeConversations().first().associateBy { it.id }
        assertEquals(1, byId.getValue("t1").unreadCount)
        assertEquals(2, byId.getValue("t4").unreadCount)
        assertEquals(0, byId.getValue("t3").unreadCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankMessages() = runTest {
        provider.sendMessage("t1", "   ")
    }

    @Test
    fun pinnedConversationsKeepTheOrderTheyWerePinnedIn() = runTest {
        suspend fun pinnedIds() = provider.observeConversations().first().filter { it.isPinned }.map { it.id }
        assertEquals(listOf("t1", "t5", "t6"), pinnedIds())

        provider.setPinned("t1", pinned = false)
        provider.setPinned("t2", pinned = true)
        provider.setPinned("t1", pinned = true)

        assertEquals(listOf("t5", "t6", "t2", "t1"), pinnedIds())
    }

    @Test
    fun markReadAndUnread() = runTest {
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
    fun deleteRemovesConversationAndItsMessages() = runTest {
        provider.deleteConversation("t2")
        assertNull(provider.observeConversations().first().firstOrNull { it.id == "t2" })
        assertTrue(provider.observeMessages("t2").first().isEmpty())
        assertTrue(provider.searchMessages("slides").isEmpty())
    }

    @Test
    fun startConversationReusesAnExistingThreadOrCreatesAnEmptyOne() = runTest {
        assertEquals("t1", provider.startConversation(listOf("c1")))
        assertEquals("t4", provider.startConversation(listOf("c5", "c4")))

        val created = provider.startConversation(listOf("c8", "c1"))
        val conversation = provider.observeConversations().first().first { it.id == created }
        assertNull(conversation.lastMessage)
        assertEquals(created, provider.startConversation(listOf("c1", "c8")))
    }

    @Test
    fun searchIsCaseInsensitiveAndNewestFirst() = runTest {
        val matches = provider.searchMessages("THE")
        assertTrue(matches.isNotEmpty())
        assertTrue(matches.all { it.message.text.contains("the", ignoreCase = true) })
        assertEquals(matches.map { it.message.sentAt }.sortedDescending(), matches.map { it.message.sentAt })
        assertTrue(provider.searchMessages("   ").isEmpty())
    }

    @Test
    fun sentMessagesAreDeliveredReadAndAnswered() = runTest {
        val replying = MockMessagingProvider(Clock.fixed(now, ZoneOffset.UTC), replyScope = backgroundScope)
        val sent = replying.sendMessage("t2", "Got it")

        testScheduler.advanceTimeBy(900)
        testScheduler.runCurrent()
        assertEquals(DeliveryState.Delivered, replying.observeMessages("t2").first().first { it.id == sent.id }.deliveryState)

        testScheduler.advanceTimeBy(3_000)
        testScheduler.runCurrent()
        val thread = replying.observeMessages("t2").first()
        assertEquals(DeliveryState.Read, thread.first { it.id == sent.id }.deliveryState)
        val reply = thread.last()
        assertEquals("c2", reply.senderId)
        assertTrue(replying.observeConversations().first().first { it.id == "t2" }.hasUnread)
    }

    @Test
    fun emptyProviderHasNoConversations() = runTest {
        val empty = MockMessagingProvider(Clock.fixed(now, ZoneOffset.UTC), seedSampleData = false)
        assertTrue(empty.observeConversations().first().isEmpty())
        assertTrue(empty.observeContacts().first().isNotEmpty())
    }
}
