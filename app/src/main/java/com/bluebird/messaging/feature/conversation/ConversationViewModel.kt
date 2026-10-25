package com.bluebird.messaging.feature.conversation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bluebird.messaging.core.data.DraftStore
import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.Conversation
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.core.model.Message
import com.bluebird.messaging.core.provider.MessagingProvider
import com.bluebird.messaging.feature.inbox.AvatarSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Immutable
data class ParticipantUi(val id: String, val name: String, val detail: String, val avatar: AvatarSpec)

@Immutable
data class MessageUndo(val messageIds: Set<String>, val message: String)

data class ConversationUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val avatars: List<AvatarSpec> = emptyList(),
    val participants: List<ParticipantUi> = emptyList(),
    val isSimulated: Boolean = true,
    /** Newest first. */
    val items: List<TimelineItem> = emptyList(),
    /** "Maya is typing", or null. */
    val typing: String? = null,
    val replyingTo: ReplyUi? = null,
    val selecting: Boolean = false,
    val selection: Set<String> = emptySet(),
    val undo: MessageUndo? = null,
) {
    val isEmpty: Boolean get() = !isLoading && !notFound && items.isEmpty()
}

class ConversationViewModel(
    private val conversationId: String,
    private val provider: MessagingProvider,
    private val drafts: DraftStore,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: ZoneId = ZoneId.systemDefault(),
    /** Where delayed deletes are committed, so leaving during the undo window still deletes. */
    private val commitScope: CoroutineScope? = null,
    timeTicks: Flow<Instant> = minuteTicks(clock),
) : ViewModel() {

    /** The draft to restore into the composer when the screen opens. */
    val initialDraft: String = drafts.get(conversationId)

    private val replyToId = MutableStateFlow<String?>(null)
    private val selecting = MutableStateFlow(false)
    private val selection = MutableStateFlow<Set<String>>(emptySet())
    private val pendingDeletes = MutableStateFlow<Set<String>>(emptySet())
    private val undo = MutableStateFlow<MessageUndo?>(null)
    private val visible = MutableStateFlow(false)
    private var deleteJob: Job? = null

    private data class Local(
        val replyToId: String?,
        val selecting: Boolean,
        val selection: Set<String>,
        val pending: Set<String>,
        val undo: MessageUndo?,
    )

    private val local = combine(replyToId, selecting, selection, pendingDeletes, undo) { r, s, sel, p, u -> Local(r, s, sel, p, u) }
    private val conversation = provider.observeConversations().map { all -> all.firstOrNull { it.id == conversationId } }
    private val messages = provider.observeMessages(conversationId)

    val uiState: StateFlow<ConversationUiState> =
        combine(conversation, messages, provider.observeTyping(conversationId), local, timeTicks) { conv, msgs, typing, l, now ->
            buildState(conv, msgs, typing, l, now)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversationUiState())

    init {
        // Incoming messages count as read only while the conversation is actually on screen.
        viewModelScope.launch {
            combine(visible, conversation, messages) { isVisible, conv, msgs ->
                isVisible && conv != null && (conv.isMarkedUnread || msgs.any { !it.isOutgoing && it.deliveryState != DeliveryState.Read })
            }.collect { needsRead -> if (needsRead) provider.markRead(conversationId) }
        }
    }

    fun setVisible(isVisible: Boolean) {
        visible.value = isVisible
    }

    fun onDraftChanged(text: String) {
        drafts.set(conversationId, text)
    }

    /** Sends [text] (replying to the quoted message, if any). Returns false for blank input. */
    fun send(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val replyTo = replyToId.value
        replyToId.value = null
        drafts.set(conversationId, "")
        viewModelScope.launch { provider.sendMessage(conversationId, trimmed, replyTo) }
        return true
    }

    fun startReply(messageId: String) {
        endSelection()
        replyToId.value = messageId
    }

    fun cancelReply() {
        replyToId.value = null
    }

    fun react(messageId: String, emoji: String) {
        viewModelScope.launch { provider.setReaction(messageId, emoji) }
    }

    /** Text of the given messages in reading order, one per line, for copying. */
    fun textOf(messageIds: Set<String>): String =
        uiState.value.items
            .filterIsInstance<TimelineItem.MessageItem>()
            .filter { it.id in messageIds }
            .asReversed()
            .joinToString("\n") { it.text }

    fun beginSelection(messageId: String) {
        selecting.value = true
        selection.value = setOf(messageId)
    }

    fun toggleSelection(messageId: String) {
        selection.update { if (messageId in it) it - messageId else it + messageId }
    }

    fun endSelection() {
        selecting.value = false
        selection.value = emptySet()
    }

    /** Hides the messages now and deletes them for real once the undo window passes. */
    fun deleteMessages(messageIds: Set<String>) {
        if (messageIds.isEmpty()) return
        endSelection()
        if (replyToId.value in messageIds) replyToId.value = null
        val scope = commitScope ?: viewModelScope
        undo.value?.let { previous ->
            deleteJob?.cancel()
            scope.launch { commitDelete(previous.messageIds) }
        }
        pendingDeletes.update { it + messageIds }
        undo.value = MessageUndo(
            messageIds,
            if (messageIds.size == 1) "Message deleted" else "${messageIds.size} messages deleted",
        )
        deleteJob = scope.launch {
            delay(UNDO_WINDOW_MS)
            commitDelete(messageIds)
        }
    }

    fun undoDelete() {
        val notice = undo.value ?: return
        deleteJob?.cancel()
        pendingDeletes.update { it - notice.messageIds }
        undo.value = null
    }

    private suspend fun commitDelete(ids: Set<String>) {
        provider.deleteMessages(ids)
        pendingDeletes.update { it - ids }
        undo.update { if (it?.messageIds == ids) null else it }
    }

    private fun buildState(
        conv: Conversation?,
        msgs: List<Message>,
        typingIds: Set<String>,
        l: Local,
        now: Instant,
    ): ConversationUiState {
        if (conv == null) return ConversationUiState(isLoading = false, notFound = true)
        val visibleMessages = msgs.filter { it.id !in l.pending }
        val people = conv.participants
        val replying = l.replyToId?.let { id ->
            visibleMessages.firstOrNull { it.id == id }?.let { m ->
                val author = if (m.isOutgoing) "You" else people.firstOrNull { it.id == m.senderId }?.firstName() ?: "Unknown"
                ReplyUi(m.id, author, m.text)
            }
        }
        val typingNames = people.filter { it.id in typingIds }.map { it.firstName() }
        return ConversationUiState(
            isLoading = false,
            title = conv.title.ifBlank { "Unknown" },
            subtitle = when {
                conv.isGroup -> "${people.size} people"
                else -> people.firstOrNull()?.phoneOrHandle.orEmpty()
            },
            avatars = people.take(2).map { AvatarSpec(it.displayName, it.id) },
            participants = people.map { ParticipantUi(it.id, it.displayName, it.phoneOrHandle, AvatarSpec(it.displayName, it.id)) },
            isSimulated = provider.info.isSimulated,
            items = buildTimeline(visibleMessages, people, now, zone),
            typing = when (typingNames.size) {
                0 -> null
                1 -> "${typingNames[0]} is typing"
                else -> "${typingNames.dropLast(1).joinToString()} and ${typingNames.last()} are typing"
            },
            replyingTo = replying,
            selecting = l.selecting,
            selection = l.selection,
            undo = l.undo,
        )
    }

    companion object {
        const val UNDO_WINDOW_MS = 4_000L

        private fun Contact.firstName() = displayName.trim().substringBefore(' ').ifBlank { phoneOrHandle }

        private fun minuteTicks(clock: Clock): Flow<Instant> = flow {
            while (true) {
                emit(clock.instant())
                delay(60_000)
            }
        }

        fun factory(
            conversationId: String,
            provider: MessagingProvider,
            drafts: DraftStore,
            commitScope: CoroutineScope,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ConversationViewModel(conversationId, provider, drafts, commitScope = commitScope) }
        }
    }
}
