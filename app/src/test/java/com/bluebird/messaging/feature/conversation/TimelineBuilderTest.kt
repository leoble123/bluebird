package com.bluebird.messaging.feature.conversation

import com.bluebird.messaging.core.designsystem.component.BubblePosition
import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.core.model.Message
import com.bluebird.messaging.core.model.Reaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class TimelineBuilderTest {

    private val now = Instant.parse("2026-10-24T18:00:00Z")
    private val t0 = Instant.parse("2026-10-24T09:41:00Z")
    private val maya = Contact("c1", "Maya Chen", "+1 555 0101")
    private val sam = Contact("c4", "Sam Okafor", "+1 555 0104")
    private val lena = Contact("c5", "Lena Fischer", "+1 555 0105")

    private fun msg(
        id: String,
        from: String?,
        minutes: Long,
        state: DeliveryState = DeliveryState.Read,
        replyTo: String? = null,
        reactions: List<Reaction> = emptyList(),
    ) = Message(id, "t", from, "text $id", t0.plus(Duration.ofMinutes(minutes)), state, replyTo, reactions)

    private fun build(messages: List<Message>, people: List<Contact> = listOf(maya)) =
        buildTimeline(messages, people, now, ZoneOffset.UTC, Locale.US)

    private fun List<TimelineItem>.messages() = filterIsInstance<TimelineItem.MessageItem>().reversed() // chronological

    @Test
    fun newestComesFirstWithASeparatorAtTheStart() {
        val items = build(listOf(msg("a", "c1", 0), msg("b", null, 1)))
        assertEquals("m:b", items.first().key)
        val separator = items.last() as TimelineItem.Separator
        assertEquals("Today 9:41 AM", separator.label.replace(' ', ' '))
    }

    @Test
    fun consecutiveMessagesFromOneSenderAreGrouped() {
        val rows = build(listOf(msg("a1", "c1", 0), msg("a2", "c1", 1), msg("a3", "c1", 2), msg("b1", null, 3))).messages()
        assertEquals(
            listOf(BubblePosition.First, BubblePosition.Middle, BubblePosition.Last, BubblePosition.Single),
            rows.map { it.position },
        )
        assertEquals(listOf(true, false, false, true), rows.map { it.startsRun })
    }

    @Test
    fun aPauseLongerThanFiveMinutesStartsANewGroupWithoutASeparator() {
        val items = build(listOf(msg("a1", "c1", 0), msg("a2", "c1", 6)))
        assertEquals(listOf(BubblePosition.Single, BubblePosition.Single), items.messages().map { it.position })
        assertEquals(1, items.count { it is TimelineItem.Separator })
    }

    @Test
    fun longGapsAndNewDaysGetSeparators() {
        // 61 minutes later (long gap), then 9:41 PM the same day (long gap), then just past midnight (new day).
        val items = build(listOf(msg("a", "c1", 0), msg("b", "c1", 61), msg("c", "c1", 60L * 12), msg("d", "c1", 60L * 12 + 3 * 60 + 2)))
        val labels = items.filterIsInstance<TimelineItem.Separator>().reversed().map { it.label.replace(' ', ' ') }
        assertEquals(4, labels.size)
        assertEquals("Today 10:42 AM", labels[1])
        assertTrue(labels[3].endsWith("12:43 AM"))
    }

    @Test
    fun onlyTheLatestOutgoingMessageShowsDeliveryStatus() {
        val rows = build(
            listOf(
                msg("a", null, 0, DeliveryState.Read),
                msg("b", "c1", 1),
                msg("c", null, 2, DeliveryState.Delivered),
            ),
        ).messages()
        assertNull(rows[0].status)
        assertNull(rows[1].status)
        assertEquals("Delivered", rows[2].status)
    }

    @Test
    fun failedMessagesAlwaysSayNotDelivered() {
        val rows = build(listOf(msg("a", null, 0, DeliveryState.Failed), msg("b", null, 1, DeliveryState.Sent))).messages()
        assertEquals("Not delivered", rows[0].status)
        assertTrue(rows[0].failed)
        assertEquals("Sent", rows[1].status)
    }

    @Test
    fun reactionsAreCombinedByEmoji() {
        val rows = build(
            listOf(msg("a", null, 0, reactions = listOf(Reaction("c1", "❤️"), Reaction(null, "❤️"), Reaction("c4", "😂")))),
            listOf(maya, sam),
        ).messages()
        val heart = rows.single().reactions.first { it.emoji == "❤️" }
        assertEquals(2, heart.count)
        assertTrue(heart.mine)
        assertEquals(2, rows.single().reactions.size)
    }

    @Test
    fun repliesQuoteTheOriginalOrSayItWasDeleted() {
        val rows = build(
            listOf(msg("a", null, 0), msg("b", "c1", 1, replyTo = "a"), msg("c", "c1", 2, replyTo = "gone")),
        ).messages()
        assertEquals(ReplyUi("a", "You", "text a"), rows[1].reply)
        assertEquals("Original message deleted", rows[2].reply?.text)
    }

    @Test
    fun groupConversationsNameSendersAndShowAvatarsOncePerRun() {
        val rows = build(
            listOf(msg("a1", "c4", 0), msg("a2", "c4", 1), msg("b1", "c5", 2), msg("me", null, 3)),
            listOf(sam, lena),
        ).messages()
        assertEquals(listOf("Sam Okafor", null, "Lena Fischer", null), rows.map { it.senderName })
        assertEquals(listOf(null, "c4", "c5", null), rows.map { it.avatar?.seed })
        assertEquals(listOf(true, true, true, false), rows.map { it.avatarColumn })
    }

    @Test
    fun oneToOneConversationsHaveNoSenderNamesOrAvatars() {
        val rows = build(listOf(msg("a", "c1", 0), msg("b", "c1", 1))).messages()
        assertTrue(rows.all { it.senderName == null && it.avatar == null && !it.avatarColumn })
    }

    @Test
    fun accessibilityLabelDescribesTheMessage() {
        val row = build(listOf(msg("a", "c1", 0, replyTo = null))).messages().single()
        assertTrue(row.accessibilityLabel.startsWith("Maya: text a"))
    }
}
