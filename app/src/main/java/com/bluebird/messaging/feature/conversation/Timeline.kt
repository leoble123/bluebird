package com.bluebird.messaging.feature.conversation

import androidx.compose.runtime.Immutable
import com.bluebird.messaging.core.designsystem.component.BubblePosition
import com.bluebird.messaging.core.designsystem.component.deliveryLabel
import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.core.model.Message
import com.bluebird.messaging.core.ui.formatSeparator
import com.bluebird.messaging.core.ui.formatTime
import com.bluebird.messaging.feature.inbox.AvatarSpec
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** A row in the message timeline. [key] is stable across updates so the list never re-creates rows. */
@Immutable
sealed interface TimelineItem {
    val key: String

    @Immutable
    data class Separator(override val key: String, val label: String) : TimelineItem

    @Immutable
    data class MessageItem(
        override val key: String,
        val id: String,
        val text: String,
        val outgoing: Boolean,
        val position: BubblePosition,
        /** True for the first bubble of a run; it gets a wider gap above it. */
        val startsRun: Boolean,
        /** Group conversations: the sender's name above the first incoming bubble of a run. */
        val senderName: String?,
        /** Group conversations: the sender's avatar beside the last incoming bubble of a run. */
        val avatar: AvatarSpec?,
        /** Whether this row leaves room for a group avatar (incoming bubbles in group conversations). */
        val avatarColumn: Boolean,
        val time: String,
        /** Delivery line under the bubble ("Delivered", "Read", "Not delivered"); only where it matters. */
        val status: String?,
        val failed: Boolean,
        val reactions: List<ReactionUi>,
        val reply: ReplyUi?,
        val accessibilityLabel: String,
    ) : TimelineItem
}

@Immutable
data class ReactionUi(val emoji: String, val count: Int, val mine: Boolean)

/** A quoted message: shown inside a reply bubble and above the composer while replying. */
@Immutable
data class ReplyUi(val messageId: String, val author: String, val text: String)

/** Consecutive messages from one sender closer together than this share a bubble group. */
internal val RunWindow: Duration = Duration.ofMinutes(5)

/** A gap longer than this (or a new day) gets a time separator. */
internal val SeparatorGap: Duration = Duration.ofHours(1)

/**
 * Turns chronological messages into timeline rows, newest first (the list is drawn bottom-up so the latest
 * message sits above the composer and new arrivals don't shift what you're reading).
 */
fun buildTimeline(
    messages: List<Message>,
    participants: List<Contact>,
    now: Instant,
    zone: ZoneId,
    locale: Locale = Locale.getDefault(),
): List<TimelineItem> {
    if (messages.isEmpty()) return emptyList()
    val isGroup = participants.size > 1
    val people = participants.associateBy { it.id }
    val byId = messages.associateBy { it.id }
    val lastOutgoingIndex = messages.indexOfLast { it.isOutgoing }

    fun needsSeparator(i: Int): Boolean {
        if (i == 0) return true
        val prev = messages[i - 1].sentAt
        val cur = messages[i].sentAt
        return Duration.between(prev, cur) > SeparatorGap || prev.atZone(zone).toLocalDate() != cur.atZone(zone).toLocalDate()
    }

    fun sameRun(i: Int, j: Int): Boolean {
        val a = messages[i]
        val b = messages[j]
        return a.senderId == b.senderId && Duration.between(a.sentAt, b.sentAt) <= RunWindow && !needsSeparator(j)
    }

    fun authorName(m: Message): String =
        if (m.isOutgoing) "You" else people[m.senderId]?.displayName?.substringBefore(' ')?.ifBlank { null } ?: "Unknown"

    val rows = ArrayList<TimelineItem>(messages.size + 8)
    messages.forEachIndexed { i, m ->
        if (needsSeparator(i)) rows += TimelineItem.Separator("sep:${m.id}", formatSeparator(m.sentAt, now, zone, locale))
        val starts = i == 0 || !sameRun(i - 1, i)
        val ends = i == messages.lastIndex || !sameRun(i, i + 1)
        val position = when {
            starts && ends -> BubblePosition.Single
            starts -> BubblePosition.First
            ends -> BubblePosition.Last
            else -> BubblePosition.Middle
        }
        val sender = people[m.senderId]
        val groupIncoming = isGroup && !m.isOutgoing
        val failed = m.deliveryState == DeliveryState.Failed
        val status = when {
            failed -> deliveryLabel(DeliveryState.Failed, null)
            i == lastOutgoingIndex -> deliveryLabel(m.deliveryState, null)
            else -> null
        }
        val reply = m.replyToId?.let { id ->
            val original = byId[id]
            if (original != null) ReplyUi(id, authorName(original), original.text) else ReplyUi(id, "", "Original message deleted")
        }
        val reactions = m.reactions
            .groupBy { it.emoji }
            .map { (emoji, list) -> ReactionUi(emoji, list.size, list.any { it.senderId == null }) }
        val time = formatTime(m.sentAt, zone, locale)
        rows += TimelineItem.MessageItem(
            key = "m:${m.id}",
            id = m.id,
            text = m.text,
            outgoing = m.isOutgoing,
            position = position,
            startsRun = starts,
            senderName = if (groupIncoming && starts) sender?.displayName ?: "Unknown" else null,
            avatar = if (groupIncoming && ends) AvatarSpec(sender?.displayName.orEmpty(), m.senderId.orEmpty()) else null,
            avatarColumn = groupIncoming,
            time = time,
            status = status,
            failed = failed,
            reactions = reactions,
            reply = reply,
            accessibilityLabel = buildString {
                append(authorName(m)).append(": ")
                if (reply != null) append("replying to ").append(reply.author.ifBlank { "a deleted message" }).append(", ")
                append(m.text).append(", ").append(time)
                if (reactions.isNotEmpty()) append(", reactions ").append(reactions.joinToString(" ") { it.emoji })
                if (status != null) append(", ").append(status)
            },
        )
    }
    return rows.reversed()
}
