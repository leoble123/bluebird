package com.bluebird.messaging.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Unsent composer text, per conversation, so leaving a conversation never loses what was typed. */
interface DraftStore {
    suspend fun get(conversationId: String): String

    /** A blank [text] clears the draft. */
    suspend fun set(conversationId: String, text: String)

    /** Every non-empty draft, keyed by conversation id. */
    fun observeAll(): Flow<Map<String, String>>
}

/** Keeps drafts for the life of the process only. Used in tests; the app uses [RoomDraftStore]. */
class InMemoryDraftStore : DraftStore {
    private val drafts = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun get(conversationId: String): String = drafts.value[conversationId].orEmpty()

    override suspend fun set(conversationId: String, text: String) {
        drafts.update { if (text.isBlank()) it - conversationId else it + (conversationId to text) }
    }

    override fun observeAll(): Flow<Map<String, String>> = drafts
}
