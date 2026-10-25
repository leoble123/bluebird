package com.bluebird.messaging.feature.inbox

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.Conversation
import com.bluebird.messaging.core.model.Message
import com.bluebird.messaging.core.model.MessageMatch
import com.bluebird.messaging.core.provider.MessagingProvider
import com.bluebird.messaging.core.ui.formatConversationTimestamp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Immutable
data class AvatarSpec(val name: String, val seed: String)

/** One pre-formatted inbox row. Rows render these directly; no formatting happens while scrolling. */
@Immutable
data class InboxItem(
    val id: String,
    val title: String,
    val preview: String,
    val timestamp: String,
    val unread: Boolean,
    val pinned: Boolean,
    val avatars: List<AvatarSpec>,
    /** Search text to emphasize in the title and preview, when this item is a search result. */
    val highlight: String? = null,
)

@Immutable
data class ContactItem(val id: String, val name: String, val detail: String, val avatar: AvatarSpec)

/** The inbox filter menu, as in iOS Messages. */
enum class InboxFilter { All, Unread }

@Immutable
data class UndoNotice(val conversationId: String, val message: String)

data class InboxUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val pinned: List<InboxItem> = emptyList(),
    val conversations: List<InboxItem> = emptyList(),
    val searchResults: List<InboxItem> = emptyList(),
    val hasAnyConversations: Boolean = false,
    val isSimulated: Boolean = true,
    val undo: UndoNotice? = null,
    val filter: InboxFilter = InboxFilter.All,
) {
    val isSearching: Boolean get() = query.isNotBlank()
}

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(
    private val provider: MessagingProvider,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: ZoneId = ZoneId.systemDefault(),
    /** Where delayed deletes are committed, so leaving the screen during the undo window still deletes. */
    private val commitScope: CoroutineScope? = null,
    /** Re-emits the current time so "9:41 AM" can roll over to "Yesterday". */
    timeTicks: Flow<Instant> = minuteTicks(clock),
    /** Where screen state is assembled: off the main thread, so building a long list never costs a frame. */
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val contactQuery = MutableStateFlow("")
    private val pendingDeletes = MutableStateFlow<Set<String>>(emptySet())
    private val undo = MutableStateFlow<UndoNotice?>(null)
    private val filter = MutableStateFlow(InboxFilter.All)
    private var deleteJob: Job? = null

    private data class Snapshot(val conversations: List<Conversation>, val query: String, val matches: List<MessageMatch>)

    private val snapshots: Flow<Snapshot> = combine(provider.observeConversations(), query) { c, q -> c to q }
        .mapLatest { (conversations, q) ->
            Snapshot(conversations, q, if (q.isBlank()) emptyList() else provider.searchMessages(q))
        }

    val uiState: StateFlow<InboxUiState> =
        combine(snapshots, pendingDeletes, undo, filter, timeTicks) { snap, pending, undoNotice, activeFilter, now ->
            buildState(snap, pending, undoNotice, activeFilter, now)
        }.flowOn(computeDispatcher).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    val contacts: StateFlow<List<ContactItem>> = combine(provider.observeContacts(), contactQuery) { all, q ->
        all.filter { q.isBlank() || it.displayName.contains(q, true) || it.phoneOrHandle.contains(q, true) }
            .map { ContactItem(it.id, it.displayName.ifBlank { it.phoneOrHandle }, it.phoneOrHandle, AvatarSpec(it.displayName, it.id)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onContactQueryChange(value: String) {
        contactQuery.value = value
    }

    fun setFilter(value: InboxFilter) {
        filter.value = value
    }

    fun setPinned(id: String, pinned: Boolean) {
        viewModelScope.launch { provider.setPinned(id, pinned) }
    }

    fun setRead(id: String, read: Boolean) {
        viewModelScope.launch { if (read) provider.markRead(id) else provider.markUnread(id) }
    }

    /** Hides the conversation now and deletes it for real once the undo window passes. */
    fun delete(id: String) {
        // Only one undo at a time: an earlier pending delete is committed straight away.
        val previous = undo.value?.conversationId
        if (previous != null && previous != id) {
            deleteJob?.cancel()
            val scope = commitScope ?: viewModelScope
            scope.launch { commitDelete(previous) }
        }
        pendingDeletes.update { it + id }
        undo.value = UndoNotice(id, "Conversation deleted")
        deleteJob = (commitScope ?: viewModelScope).launch {
            delay(UNDO_WINDOW_MS)
            commitDelete(id)
        }
    }

    fun undoDelete() {
        val notice = undo.value ?: return
        deleteJob?.cancel()
        pendingDeletes.update { it - notice.conversationId }
        undo.value = null
    }

    fun startConversation(contactId: String, onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(provider.startConversation(listOf(contactId))) }
    }

    private suspend fun commitDelete(id: String) {
        provider.deleteConversation(id)
        pendingDeletes.update { it - id }
        undo.update { if (it?.conversationId == id) null else it }
    }

    private fun buildState(
        snap: Snapshot,
        pending: Set<String>,
        undoNotice: UndoNotice?,
        activeFilter: InboxFilter,
        now: Instant,
    ): InboxUiState {
        // New, empty conversations stay out of the list until a message exists (unless pinned).
        val visible = snap.conversations.filter { it.id !in pending && (it.lastMessage != null || it.isPinned) }
        val base = InboxUiState(
            isLoading = false,
            query = snap.query,
            hasAnyConversations = visible.isNotEmpty(),
            isSimulated = provider.info.isSimulated,
            undo = undoNotice,
            filter = activeFilter,
        )
        if (snap.query.isBlank() && activeFilter == InboxFilter.Unread) {
            // Filtering flattens the list: unread pinned conversations appear as ordinary rows.
            return base.copy(conversations = visible.filter { it.hasUnread }.map { it.toItem(it.lastMessage, now, highlight = null) })
        }
        if (snap.query.isBlank()) {
            val (pinned, others) = visible.partition { it.isPinned }
            return base.copy(
                pinned = pinned.map { it.toItem(it.lastMessage, now, highlight = null) },
                conversations = others.map { it.toItem(it.lastMessage, now, highlight = null) },
            )
        }

        val q = snap.query.trim()
        val nameMatches = visible.filter { c ->
            c.title.contains(q, true) || c.participants.any { it.phoneOrHandle.contains(q, true) }
        }
        val nameIds = nameMatches.map { it.id }.toSet()
        val byId = visible.associateBy { it.id }
        // Newest matching message per conversation, for conversations not already matched by name.
        val contentMatches = snap.matches
            .filter { it.conversationId !in nameIds && it.conversationId in byId }
            .distinctBy { it.conversationId }
        val results = nameMatches.map { it.toItem(it.lastMessage, now, highlight = q) } +
            contentMatches.map { m -> byId.getValue(m.conversationId).toItem(m.message, now, highlight = q) }
        return base.copy(searchResults = results)
    }

    private fun Conversation.toItem(previewMessage: Message?, now: Instant, highlight: String?): InboxItem {
        val preview = previewMessage?.let { previewText(this, it) } ?: "No messages yet"
        val timestamp = previewMessage?.let { formatConversationTimestamp(it.sentAt, now, zone) }.orEmpty()
        val title = title.ifBlank { "Unknown" }
        return InboxItem(
            id = id,
            title = title,
            preview = preview,
            timestamp = timestamp,
            unread = hasUnread,
            pinned = isPinned,
            avatars = participants.take(2).map { AvatarSpec(it.displayName, it.id) },
            highlight = highlight,
        )
    }

    companion object {
        const val UNDO_WINDOW_MS = 4_000L

        /** In group conversations, incoming previews name the sender: "Lena: I can do chips". */
        internal fun previewText(conversation: Conversation, message: Message): String {
            if (!conversation.isGroup || message.isOutgoing) return message.text
            val sender = conversation.participants.firstOrNull { it.id == message.senderId }
            return if (sender != null) "${firstName(sender)}: ${message.text}" else message.text
        }

        private fun firstName(contact: Contact) = contact.displayName.trim().substringBefore(' ').ifBlank { contact.phoneOrHandle }

        private fun minuteTicks(clock: Clock): Flow<Instant> = flow {
            while (true) {
                emit(clock.instant())
                delay(60_000)
            }
        }

        fun factory(provider: MessagingProvider, commitScope: CoroutineScope): ViewModelProvider.Factory = viewModelFactory {
            initializer { InboxViewModel(provider, commitScope = commitScope) }
        }
    }
}
