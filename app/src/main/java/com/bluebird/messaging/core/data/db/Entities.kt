package com.bluebird.messaging.core.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Room schema, version 1. Times are epoch milliseconds. IDs are strings that stay stable for the life of a row
 * (sample data uses fixed IDs; everything created at runtime uses random UUIDs).
 *
 * Any change here needs a version bump, an exported schema, and a Migration in BluebirdDatabase.MIGRATIONS.
 */

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val handle: String,
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    /** Non-null when pinned; lower values were pinned earlier and show first. */
    val pinnedOrder: Long?,
    val markedUnread: Boolean,
)

@Entity(
    tableName = "conversation_participants",
    primaryKeys = ["conversationId", "contactId"],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("contactId")],
)
data class ParticipantEntity(
    val conversationId: String,
    val contactId: String,
    /** Display order of participants in titles and avatars. */
    val position: Int,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // Timeline order and "latest message" lookups.
        Index(value = ["conversationId", "sentAt"]),
        // Unread counts.
        Index(value = ["conversationId", "deliveryState"]),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    /** Null when the local user sent the message. */
    val senderId: String?,
    val text: String,
    val sentAt: Long,
    /** A [com.bluebird.messaging.core.model.DeliveryState] name. */
    val deliveryState: String,
    /** Not a foreign key: a reply may outlive the message it quotes. */
    val replyToId: String?,
)

@Entity(
    tableName = "reactions",
    primaryKeys = ["messageId", "senderKey"],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReactionEntity(
    val messageId: String,
    /** The reacting contact's id, or [ME] for the local user (a primary key column can't be null). */
    val senderKey: String,
    val emoji: String,
    val createdAt: Long,
) {
    companion object {
        const val ME = "@me"
    }
}

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("messageId")],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val mimeType: String,
    /** A copy in app-managed storage, when the attachment has been kept locally. */
    val localPath: String?,
    /** Where the attachment originally came from (e.g. a Photo Picker content URI). */
    val sourceUri: String?,
    val fileName: String?,
    val sizeBytes: Long?,
    val width: Int?,
    val height: Int?,
    /** Display order within the message. */
    val position: Int,
)

@Entity(
    tableName = "drafts",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DraftEntity(
    @PrimaryKey val conversationId: String,
    val text: String,
    val updatedAt: Long,
)

/** Small key-value settings (theme, flags). */
@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/** One inbox row as computed by SQL: the conversation, its unread count, and its newest message. */
data class ConversationSummaryRow(
    @Embedded val conversation: ConversationEntity,
    val unreadCount: Int,
    @Embedded(prefix = "last_") val lastMessage: MessageEntity?,
)

data class ParticipantRow(
    val conversationId: String,
    val position: Int,
    @Embedded val contact: ContactEntity,
)

data class ParticipantLink(
    val conversationId: String,
    val contactId: String,
)
