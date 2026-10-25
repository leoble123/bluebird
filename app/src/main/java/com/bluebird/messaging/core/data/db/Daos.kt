package com.bluebird.messaging.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessagingDao {

    // Inbox ------------------------------------------------------------------------------------------------------

    @Query(
        """
        SELECT c.*,
            (SELECT COUNT(*) FROM messages u
                WHERE u.conversationId = c.id AND u.senderId IS NOT NULL AND u.deliveryState != 'Read') AS unreadCount,
            m.id AS last_id, m.conversationId AS last_conversationId, m.senderId AS last_senderId,
            m.text AS last_text, m.sentAt AS last_sentAt, m.deliveryState AS last_deliveryState,
            m.replyToId AS last_replyToId
        FROM conversations c
        LEFT JOIN messages m ON m.id = (
            SELECT x.id FROM messages x WHERE x.conversationId = c.id ORDER BY x.sentAt DESC, x.id DESC LIMIT 1
        )
        """,
    )
    fun observeConversationSummaries(): Flow<List<ConversationSummaryRow>>

    @Query(
        """
        SELECT p.conversationId, p.position, ct.* FROM conversation_participants p
        JOIN contacts ct ON ct.id = p.contactId
        ORDER BY p.conversationId, p.position
        """,
    )
    fun observeParticipants(): Flow<List<ParticipantRow>>

    @Query("SELECT conversationId, contactId FROM conversation_participants")
    suspend fun participantLinks(): List<ParticipantLink>

    @Query("SELECT contactId FROM conversation_participants WHERE conversationId = :conversationId ORDER BY position")
    suspend fun participantIds(conversationId: String): List<String>

    @Query("SELECT * FROM contacts ORDER BY displayName COLLATE NOCASE, id")
    fun observeContacts(): Flow<List<ContactEntity>>

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun conversationCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM conversations WHERE id = :id)")
    suspend fun conversationExists(id: String): Boolean

    @Query("SELECT MAX(pinnedOrder) FROM conversations")
    suspend fun maxPinnedOrder(): Long?

    @Query("UPDATE conversations SET pinnedOrder = :order WHERE id = :id")
    suspend fun setPinnedOrder(id: String, order: Long?)

    @Query("SELECT pinnedOrder FROM conversations WHERE id = :id")
    suspend fun pinnedOrder(id: String): Long?

    @Query("UPDATE conversations SET markedUnread = :unread WHERE id = :id")
    suspend fun setMarkedUnread(id: String, unread: Boolean)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    // Timeline ---------------------------------------------------------------------------------------------------

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY sentAt, id")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT r.* FROM reactions r JOIN messages m ON m.id = r.messageId
        WHERE m.conversationId = :conversationId ORDER BY r.createdAt, r.senderKey
        """,
    )
    fun observeReactions(conversationId: String): Flow<List<ReactionEntity>>

    @Query(
        """
        SELECT a.* FROM attachments a JOIN messages m ON m.id = a.messageId
        WHERE m.conversationId = :conversationId ORDER BY a.messageId, a.position
        """,
    )
    fun observeAttachments(conversationId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun message(id: String): MessageEntity?

    @Query("UPDATE messages SET deliveryState = :state WHERE id = :id")
    suspend fun setDeliveryState(id: String, state: String)

    @Query("UPDATE messages SET deliveryState = 'Read' WHERE conversationId = :conversationId AND senderId IS NOT NULL")
    suspend fun markIncomingRead(conversationId: String)

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteMessages(ids: Collection<String>)

    @Query(
        """
        SELECT * FROM messages WHERE text LIKE '%' || :escapedQuery || '%' ESCAPE '\'
        ORDER BY sentAt DESC, id DESC LIMIT :limit
        """,
    )
    suspend fun searchMessages(escapedQuery: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM reactions WHERE messageId = :messageId AND senderKey = :senderKey")
    suspend fun reaction(messageId: String, senderKey: String): ReactionEntity?

    @Query("DELETE FROM reactions WHERE messageId = :messageId AND senderKey = :senderKey")
    suspend fun deleteReaction(messageId: String, senderKey: String)

    @Query("SELECT * FROM attachments WHERE messageId = :messageId ORDER BY position")
    suspend fun attachmentsFor(messageId: String): List<AttachmentEntity>

    // Writes -----------------------------------------------------------------------------------------------------

    @Upsert
    suspend fun upsertContacts(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    /** Upsert, not REPLACE: replacing a row would cascade-delete its reactions and attachments. */
    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Upsert
    suspend fun upsertReactions(reactions: List<ReactionEntity>)

    @Upsert
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)

    // Drafts -----------------------------------------------------------------------------------------------------

    @Query("SELECT text FROM drafts WHERE conversationId = :conversationId")
    suspend fun draft(conversationId: String): String?

    @Query("SELECT * FROM drafts")
    fun observeDrafts(): Flow<List<DraftEntity>>

    @Upsert
    suspend fun upsertDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE conversationId = :conversationId")
    suspend fun deleteDraft(conversationId: String)
}

@Dao
interface PreferencesDao {
    @Query("SELECT value FROM preferences WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM preferences WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Upsert
    suspend fun put(preference: PreferenceEntity)
}
