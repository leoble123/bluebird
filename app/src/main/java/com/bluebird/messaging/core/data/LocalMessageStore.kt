package com.bluebird.messaging.core.data

import androidx.room.withTransaction
import com.bluebird.messaging.core.data.db.AttachmentEntity
import com.bluebird.messaging.core.data.db.BluebirdDatabase
import com.bluebird.messaging.core.data.db.ContactEntity
import com.bluebird.messaging.core.data.db.ConversationEntity
import com.bluebird.messaging.core.data.db.ConversationSummaryRow
import com.bluebird.messaging.core.data.db.DraftEntity
import com.bluebird.messaging.core.data.db.MessageEntity
import com.bluebird.messaging.core.data.db.ParticipantEntity
import com.bluebird.messaging.core.data.db.ParticipantRow
import com.bluebird.messaging.core.data.db.ReactionEntity
import com.bluebird.messaging.core.model.Attachment
import com.bluebird.messaging.core.model.AttachmentAvailability
import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.Conversation
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.core.model.Message
import com.bluebird.messaging.core.model.MessageMatch
import com.bluebird.messaging.core.model.Reaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant

/**
 * The on-device source of truth for conversations, messages, reactions, attachments and drafts, backed by Room.
 *
 * Providers (the mock today, a real transport later) write here; screens read here through the provider. All
 * queries run off the main thread; reads are Flows, so the UI updates as soon as anything changes.
 */
class LocalMessageStore(
    private val db: BluebirdDatabase,
    /** For mapping work that touches the file system (checking attachment files). */
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fileExists: (String) -> Boolean = { File(it).exists() },
) {
    private val dao = db.messaging()

    // Reads ------------------------------------------------------------------------------------------------------

    /** Pinned conversations first (in pin order), then by latest activity, then by id for a stable order. */
    fun observeConversations(): Flow<List<Conversation>> =
        combine(dao.observeConversationSummaries(), dao.observeParticipants()) { summaries, participants ->
            val byConversation = participants.groupBy(ParticipantRow::conversationId)
            summaries
                .map { it.toConversation(byConversation[it.conversation.id].orEmpty()) to it }
                .sortedWith(
                    compareBy<Pair<Conversation, ConversationSummaryRow>> { it.second.conversation.pinnedOrder == null }
                        .thenBy { it.second.conversation.pinnedOrder ?: 0L }
                        .thenByDescending { it.second.lastMessage?.sentAt ?: it.second.conversation.createdAt }
                        .thenBy { it.first.id },
                )
                .map { it.first }
        }.distinctUntilChanged()

    fun observeMessages(conversationId: String): Flow<List<Message>> =
        combine(
            dao.observeMessages(conversationId),
            dao.observeReactions(conversationId),
            dao.observeAttachments(conversationId),
        ) { messages, reactions, attachments ->
            val reactionsByMessage = reactions.groupBy { it.messageId }
            val attachmentsByMessage = attachments.groupBy { it.messageId }
            messages.map { m ->
                m.toMessage(
                    reactions = reactionsByMessage[m.id].orEmpty().map { it.toReaction() },
                    attachments = attachmentsByMessage[m.id].orEmpty().map { it.toAttachment() },
                )
            }
        }.distinctUntilChanged().flowOn(ioDispatcher)

    fun observeContacts(): Flow<List<Contact>> =
        dao.observeContacts().map { list -> list.map { Contact(it.id, it.displayName, it.handle) } }

    suspend fun participantIds(conversationId: String): List<String> = dao.participantIds(conversationId)

    suspend fun conversationExists(id: String): Boolean = dao.conversationExists(id)

    suspend fun isEmpty(): Boolean = dao.conversationCount() == 0

    /** Case-insensitive (for ASCII) substring search of message text, newest first. */
    suspend fun searchMessages(query: String, limit: Int): List<MessageMatch> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return dao.searchMessages(escaped, limit).map { MessageMatch(it.conversationId, it.toMessage()) }
    }

    /** The existing conversation with exactly these participants, if any. */
    suspend fun findConversation(contactIds: Set<String>): String? =
        dao.participantLinks()
            .groupBy({ it.conversationId }, { it.contactId })
            .entries
            .firstOrNull { it.value.toSet() == contactIds }
            ?.key

    suspend fun attachmentsFor(messageId: String): List<Attachment> = dao.attachmentsFor(messageId).map { it.toAttachment() }

    // Writes -----------------------------------------------------------------------------------------------------

    suspend fun upsertContacts(contacts: List<Contact>) {
        dao.upsertContacts(contacts.map { ContactEntity(it.id, it.displayName, it.phoneOrHandle) })
    }

    suspend fun createConversation(id: String, contactIds: List<String>, createdAt: Instant, pinnedOrder: Long? = null) {
        db.withTransaction {
            dao.insertConversations(listOf(ConversationEntity(id, createdAt.toEpochMilli(), pinnedOrder, markedUnread = false)))
            dao.insertParticipants(contactIds.distinct().mapIndexed { i, c -> ParticipantEntity(id, c, i) })
        }
    }

    suspend fun upsertMessages(messages: List<Message>) {
        db.withTransaction {
            dao.upsertMessages(messages.map { it.toEntity() })
            val reactions = messages.flatMap { m ->
                m.reactions.mapIndexed { i, r ->
                    ReactionEntity(m.id, r.senderId ?: ReactionEntity.ME, r.emoji, m.sentAt.toEpochMilli() + i)
                }
            }
            if (reactions.isNotEmpty()) dao.upsertReactions(reactions)
            val attachments = messages.flatMap { m -> m.attachments.mapIndexed { i, a -> a.toEntity(i) } }
            if (attachments.isNotEmpty()) dao.upsertAttachments(attachments)
        }
    }

    suspend fun setDeliveryState(messageId: String, state: DeliveryState) = dao.setDeliveryState(messageId, state.name)

    /**
     * Sets a reaction for [senderId] (null = you). As with Tapbacks, one reaction per person per message: the
     * same emoji again, or a null [emoji], removes it.
     */
    suspend fun setReaction(messageId: String, senderId: String?, emoji: String?, at: Instant) {
        val key = senderId ?: ReactionEntity.ME
        db.withTransaction {
            val current = dao.reaction(messageId, key)
            if (emoji == null || current?.emoji == emoji) {
                dao.deleteReaction(messageId, key)
            } else {
                dao.upsertReactions(listOf(ReactionEntity(messageId, key, emoji, at.toEpochMilli())))
            }
        }
    }

    suspend fun deleteMessages(ids: Set<String>) {
        if (ids.isNotEmpty()) dao.deleteMessages(ids)
    }

    suspend fun setPinned(conversationId: String, pinned: Boolean) {
        db.withTransaction {
            val current = dao.pinnedOrder(conversationId)
            when {
                pinned && current == null -> dao.setPinnedOrder(conversationId, (dao.maxPinnedOrder() ?: 0L) + 1)
                !pinned -> dao.setPinnedOrder(conversationId, null)
            }
        }
    }

    suspend fun markRead(conversationId: String) {
        db.withTransaction {
            dao.setMarkedUnread(conversationId, false)
            dao.markIncomingRead(conversationId)
        }
    }

    suspend fun markUnread(conversationId: String) = dao.setMarkedUnread(conversationId, true)

    /** Removes the conversation along with its participants, messages, reactions, attachments and draft. */
    suspend fun deleteConversation(conversationId: String) = dao.deleteConversation(conversationId)

    // Mapping ----------------------------------------------------------------------------------------------------

    private fun ConversationSummaryRow.toConversation(participants: List<ParticipantRow>) = Conversation(
        id = conversation.id,
        participants = participants.map { Contact(it.contact.id, it.contact.displayName, it.contact.handle) },
        lastMessage = lastMessage?.toMessage(),
        unreadCount = unreadCount,
        isPinned = conversation.pinnedOrder != null,
        isMarkedUnread = conversation.markedUnread,
    )

    private fun MessageEntity.toMessage(
        reactions: List<Reaction> = emptyList(),
        attachments: List<Attachment> = emptyList(),
    ) = Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        text = text,
        sentAt = Instant.ofEpochMilli(sentAt),
        // An unknown state (e.g. written by a newer app version) is shown as Sent rather than crashing.
        deliveryState = DeliveryState.entries.firstOrNull { it.name == deliveryState } ?: DeliveryState.Sent,
        replyToId = replyToId,
        reactions = reactions,
        attachments = attachments,
    )

    private fun Message.toEntity() = MessageEntity(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        text = text,
        sentAt = sentAt.toEpochMilli(),
        deliveryState = deliveryState.name,
        replyToId = replyToId,
    )

    private fun ReactionEntity.toReaction() = Reaction(if (senderKey == ReactionEntity.ME) null else senderKey, emoji)

    private fun AttachmentEntity.toAttachment() = Attachment(
        id = id,
        messageId = messageId,
        mimeType = mimeType,
        localPath = localPath,
        sourceUri = sourceUri,
        fileName = fileName,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        availability = when {
            localPath == null -> AttachmentAvailability.NotStored
            fileExists(localPath) -> AttachmentAvailability.Available
            else -> AttachmentAvailability.Missing
        },
    )

    private fun Attachment.toEntity(position: Int) = AttachmentEntity(
        id = id,
        messageId = messageId,
        mimeType = mimeType,
        localPath = localPath,
        sourceUri = sourceUri,
        fileName = fileName,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        position = position,
    )
}

/** Drafts persisted in the database, so unsent text survives the app being closed or killed. */
class RoomDraftStore(private val db: BluebirdDatabase, private val now: () -> Instant = Instant::now) : DraftStore {
    private val dao = db.messaging()

    override suspend fun get(conversationId: String): String = dao.draft(conversationId).orEmpty()

    override suspend fun set(conversationId: String, text: String) {
        if (text.isBlank()) {
            dao.deleteDraft(conversationId)
        } else if (dao.conversationExists(conversationId)) {
            dao.upsertDraft(DraftEntity(conversationId, text, now().toEpochMilli()))
        }
    }

    override fun observeAll(): Flow<Map<String, String>> =
        dao.observeDrafts().map { list -> list.associate { it.conversationId to it.text } }
}
