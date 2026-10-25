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
    /** The message this one quotes, if it was sent as a reply. */
    val replyToId: String? = null,
    val reactions: List<Reaction> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
) {
    val isOutgoing: Boolean get() = senderId == null
}

/** A photo, video or file on a message. Only metadata and a local reference are stored, never the bytes. */
data class Attachment(
    val id: String,
    val messageId: String,
    val mimeType: String,
    val localPath: String?,
    val sourceUri: String?,
    val fileName: String?,
    val sizeBytes: Long?,
    val width: Int?,
    val height: Int?,
    val availability: AttachmentAvailability,
)

enum class AttachmentAvailability {
    /** The local copy exists and can be shown. */
    Available,

    /** A local copy was recorded but the file is gone (e.g. storage was cleared); show a placeholder. */
    Missing,

    /** Only the original reference is known; there is no local copy yet. */
    NotStored,
}

/** A tapback. Each person (including you, [senderId] null) has at most one reaction per message. */
data class Reaction(
    val senderId: String?,
    val emoji: String,
)

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
