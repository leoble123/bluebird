package com.bluebird.messaging.core.provider

import com.bluebird.messaging.core.model.DeliveryState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

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
}
