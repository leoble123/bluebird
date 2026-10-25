package com.bluebird.messaging.core.model

import java.time.Instant

data class Contact(
    val id: String,
    val displayName: String,
    val phoneOrHandle: String,
)

enum class DeliveryState { Sending, Sent, Delivered, Read, Failed }

data class Message(
    val id: String,
    val conversationId: String,
    /** Null when the local user sent the message. */
    val senderId: String?,
    val text: String,
    val sentAt: Instant,
    val deliveryState: DeliveryState,
) {
    val isOutgoing: Boolean get() = senderId == null
}

data class Conversation(
    val id: String,
    val participants: List<Contact>,
    val lastMessage: Message?,
    val unreadCount: Int,
    val isPinned: Boolean,
    /** Set when the user explicitly marks a conversation unread, independent of message state. */
    val isMarkedUnread: Boolean = false,
) {
    val title: String get() = participants.joinToString { it.displayName }
    val hasUnread: Boolean get() = unreadCount > 0 || isMarkedUnread
    val isGroup: Boolean get() = participants.size > 1
}

/** A message whose text matched a search query. */
data class MessageMatch(
    val conversationId: String,
    val message: Message,
)
