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
) {
    val title: String get() = participants.joinToString { it.displayName }
}
