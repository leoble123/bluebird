package com.bluebird.messaging.core.designsystem

import com.bluebird.messaging.core.designsystem.component.AvatarPalette
import com.bluebird.messaging.core.designsystem.component.BubbleCorners
import com.bluebird.messaging.core.designsystem.component.BubblePosition
import com.bluebird.messaging.core.designsystem.component.bubbleCorners
import com.bluebird.messaging.core.designsystem.component.deliveryLabel
import com.bluebird.messaging.core.designsystem.component.initialsFor
import com.bluebird.messaging.core.designsystem.component.isJumboEmoji
import com.bluebird.messaging.core.designsystem.theme.BluebirdLayout
import com.bluebird.messaging.core.designsystem.theme.Radii
import com.bluebird.messaging.core.designsystem.theme.WindowClass
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.core.ui.formatConversationTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class DesignSystemLogicTest {

    @Test
    fun singleOutgoingBubbleHasFacetOnlyAtOuterBottom() {
        val c = bubbleCorners(outgoing = true, position = BubblePosition.Single)
        assertEquals(BubbleCorners(Radii.bubble, Radii.bubble, Radii.bubbleFacet, Radii.bubble), c)
    }

    @Test
    fun groupedIncomingBubblesTightenTheirSharedEdge() {
        val first = bubbleCorners(outgoing = false, position = BubblePosition.First)
        val middle = bubbleCorners(outgoing = false, position = BubblePosition.Middle)
        val last = bubbleCorners(outgoing = false, position = BubblePosition.Last)
        assertEquals(Radii.bubble, first.topStart)
        assertEquals(Radii.bubbleJoined, first.bottomStart)
        assertEquals(Radii.bubbleJoined, middle.topStart)
        assertEquals(Radii.bubbleJoined, middle.bottomStart)
        assertEquals(Radii.bubbleJoined, last.topStart)
        assertEquals(Radii.bubbleFacet, last.bottomStart)
        // The far side stays fully round.
        listOf(first, middle, last).forEach {
            assertEquals(Radii.bubble, it.topEnd)
            assertEquals(Radii.bubble, it.bottomEnd)
        }
    }

    @Test
    fun jumboEmojiDetection() {
        assertTrue(isJumboEmoji("🎉"))
        assertTrue(isJumboEmoji("🎉🎉 🎉"))
        assertTrue(isJumboEmoji("❤️"))
        assertTrue(isJumboEmoji("👍🏽"))
        assertTrue(isJumboEmoji("🇺🇸"))
        assertTrue(isJumboEmoji("👨‍👩‍👧"))
        assertFalse(isJumboEmoji("🎉🎉🎉🎉"))
        assertFalse(isJumboEmoji("ok 👍"))
        assertFalse(isJumboEmoji(""))
        assertFalse(isJumboEmoji("   "))
    }

    @Test
    fun initials() {
        assertEquals("MC", initialsFor("Maya Chen"))
        assertEquals("JR", initialsFor("  jordan  de la rosa "))
        assertEquals("P", initialsFor("Priya"))
        assertEquals("", initialsFor("+1 555 0101"))
        assertEquals("", initialsFor(""))
    }

    @Test
    fun avatarToneIsStableAndInRange() {
        val a = AvatarPalette.toneIndexFor("c1")
        assertEquals(a, AvatarPalette.toneIndexFor("c1"))
        val all = (0 until 200).map { AvatarPalette.toneIndexFor("contact-$it") }
        assertTrue(all.all { it in AvatarPalette.tones.indices })
        assertEquals(AvatarPalette.tones.size, all.toSet().size)
    }

    @Test
    fun layoutBreakpoints() {
        assertEquals(WindowClass.Compact, BluebirdLayout.forWidth(411).windowClass)
        assertFalse(BluebirdLayout.forWidth(411).twoPane)
        assertEquals(WindowClass.Medium, BluebirdLayout.forWidth(700).windowClass)
        assertEquals(WindowClass.Expanded, BluebirdLayout.forWidth(1280).windowClass)
        assertTrue(BluebirdLayout.forWidth(1280).twoPane)
    }

    @Test
    fun deliveryLabels() {
        assertEquals("Read 9:41 AM", deliveryLabel(DeliveryState.Read, "9:41 AM"))
        assertEquals("Read", deliveryLabel(DeliveryState.Read, null))
        assertEquals("Not delivered", deliveryLabel(DeliveryState.Failed, null))
        assertEquals("Sending…", deliveryLabel(DeliveryState.Sending, null))
    }

    @Test
    fun conversationTimestamps() {
        val now = Instant.parse("2026-10-24T15:00:00Z") // a Saturday
        fun fmt(iso: String) = formatConversationTimestamp(Instant.parse(iso), now, ZoneOffset.UTC, Locale.US)
        assertEquals("9:41 AM", fmt("2026-10-24T09:41:00Z").replace(' ', ' '))
        assertEquals("Yesterday", fmt("2026-10-23T22:00:00Z"))
        assertEquals("Tuesday", fmt("2026-10-20T10:00:00Z"))
        assertEquals("10/12/26", fmt("2026-10-12T10:00:00Z"))
    }
}
