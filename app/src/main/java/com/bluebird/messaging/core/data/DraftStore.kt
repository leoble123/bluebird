package com.bluebird.messaging.core.data

import java.util.concurrent.ConcurrentHashMap

/** Unsent composer text, per conversation, so leaving a conversation never loses what was typed. */
interface DraftStore {
    fun get(conversationId: String): String

    /** A blank [text] clears the draft. */
    fun set(conversationId: String, text: String)
}

/** Keeps drafts for the life of the process. Local persistence replaces this in a later phase. */
class InMemoryDraftStore : DraftStore {
    private val drafts = ConcurrentHashMap<String, String>()

    override fun get(conversationId: String): String = drafts[conversationId].orEmpty()

    override fun set(conversationId: String, text: String) {
        if (text.isBlank()) drafts.remove(conversationId) else drafts[conversationId] = text
    }
}
