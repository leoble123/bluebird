package com.bluebird.messaging.core.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bluebird.messaging.core.data.db.MessageEntity
import com.bluebird.messaging.core.model.Attachment
import com.bluebird.messaging.core.model.AttachmentAvailability
import com.bluebird.messaging.core.model.Contact
import com.bluebird.messaging.core.model.DeliveryState
import com.bluebird.messaging.core.model.Message
import com.bluebird.messaging.core.model.Reaction
import com.bluebird.messaging.testing.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Database-level behavior: storage, cascades, ordering, attachments, drafts, preferences and restarts. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LocalMessageStoreTest {

    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun withStore(
        fileExists: (String) -> Boolean = { false },
        block: suspend TestScope.(TestData) -> Unit,
    ) = runTest {
        val data = TestData(this, clock, seedSampleData = false, fileExists = fileExists)
        try {
            data.store.upsertContacts(listOf(Contact("a", "Ann Lee", "+1 555 0001"), Contact("b", "Ben Ito", "+1 555 0002")))
            data.store.createConversation("c", listOf("a"), now)
            block(data)
        } finally {
            data.close()
        }
    }

    private fun message(id: String, minutes: Long, from: String? = "a", text: String = "text $id") =
        Message(id, "c", from, text, now.plusSeconds(minutes * 60), DeliveryState.Delivered)

    @Test
    fun insertsQueriesAndUpdatesMessages() = withStore { data ->
        data.store.upsertMessages(listOf(message("1", 0), message("2", 1, from = null)))
        data.store.setDeliveryState("2", DeliveryState.Read)

        val messages = data.store.observeMessages("c").first()
        assertEquals(listOf("1", "2"), messages.map { it.id })
        assertEquals(DeliveryState.Read, messages[1].deliveryState)

        val conversation = data.store.observeConversations().first().single()
        assertEquals("2", conversation.lastMessage?.id)
        assertEquals(1, conversation.unreadCount)
        assertEquals(listOf("Ann Lee"), conversation.participants.map { it.displayName })
    }

    @Test
    fun messagesWithTheSameTimestampKeepAStableOrder() = withStore { data ->
        data.store.upsertMessages(listOf(message("b", 0), message("a", 0), message("c", 0)))
        assertEquals(listOf("a", "b", "c"), data.store.observeMessages("c").first().map { it.id })
    }

    @Test
    fun deletingAConversationRemovesEverythingThatBelongsToIt() = withStore { data ->
        val withExtras = message("1", 0).copy(
            reactions = listOf(Reaction(null, "❤️")),
            attachments = listOf(attachment("x", "1", localPath = "/files/x.jpg")),
        )
        data.store.upsertMessages(listOf(withExtras))
        RoomDraftStore(data.db).set("c", "unsent")

        data.store.deleteConversation("c")

        val dao = data.db.messaging()
        assertTrue(data.store.observeConversations().first().isEmpty())
        assertNull(dao.message("1"))
        assertNull(dao.reaction("1", "@me"))
        assertTrue(dao.attachmentsFor("1").isEmpty())
        assertEquals("", RoomDraftStore(data.db).get("c"))
        assertTrue(dao.participantIds("c").isEmpty())
    }

    @Test
    fun upsertingAMessageKeepsItsReactions() = withStore { data ->
        data.store.upsertMessages(listOf(message("1", 0)))
        data.store.setReaction("1", senderId = "a", emoji = "👍", at = now)
        data.store.upsertMessages(listOf(message("1", 0).copy(text = "edited")))

        val stored = data.store.observeMessages("c").first().single()
        assertEquals("edited", stored.text)
        assertEquals(listOf(Reaction("a", "👍")), stored.reactions)
    }

    @Test
    fun attachmentsReportWhetherTheirLocalFileStillExists() = withStore(fileExists = { it == "/files/here.jpg" }) { data ->
        data.store.upsertMessages(
            listOf(
                message("1", 0).copy(
                    attachments = listOf(
                        attachment("here", "1", localPath = "/files/here.jpg"),
                        attachment("gone", "1", localPath = "/files/gone.jpg"),
                        attachment("remote", "1", localPath = null),
                    ),
                ),
            ),
        )
        val byId = data.store.attachmentsFor("1").associateBy { it.id }
        assertEquals(AttachmentAvailability.Available, byId.getValue("here").availability)
        assertEquals(AttachmentAvailability.Missing, byId.getValue("gone").availability)
        assertEquals(AttachmentAvailability.NotStored, byId.getValue("remote").availability)
        assertEquals(listOf("here", "gone", "remote"), data.store.attachmentsFor("1").map { it.id })
    }

    @Test
    fun unknownDeliveryStatesAreReadSafely() = withStore { data ->
        data.db.messaging().upsertMessages(listOf(MessageEntity("1", "c", "a", "hi", now.toEpochMilli(), "Teleported", null)))
        assertEquals(DeliveryState.Sent, data.store.observeMessages("c").first().single().deliveryState)
    }

    @Test
    fun draftsAreStoredAndClearedAndIgnoredForMissingConversations() = withStore { data ->
        val drafts = RoomDraftStore(data.db) { now }
        drafts.set("c", "On my way")
        assertEquals("On my way", drafts.get("c"))
        assertEquals(mapOf("c" to "On my way"), drafts.observeAll().first())

        drafts.set("c", "   ")
        assertEquals("", drafts.get("c"))

        drafts.set("missing", "hello")
        assertEquals("", drafts.get("missing"))
    }

    @Test
    fun themePreferenceDefaultsToSystemAndPersists() = withStore { data ->
        assertEquals(ThemeMode.System, data.preferences.observeThemeMode().first())
        data.preferences.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, data.preferences.observeThemeMode().first())
        assertFalse(data.preferences.isSampleDataSeeded())
    }

    @Test
    fun everythingSurvivesClosingAndReopeningTheDatabase() = runTest {
        val name = "restore-test.db"
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase(name)
        try {
            val first = TestData(this, clock, name = name)
            first.provider.ensureSeeded()
            first.provider.setPinned("t2", true)
            first.provider.deleteConversation("t3")
            first.provider.setReaction("m3", "👍")
            val sent = first.provider.sendMessage("t1", "Persist me", replyToId = "m3")
            RoomDraftStore(first.db).set("t4", "half-typed")
            first.preferences.setThemeMode(ThemeMode.Light)
            first.close()

            // A brand-new database instance, as after the app process is killed and relaunched.
            val second = TestData(this, clock, name = name)
            val conversations = second.provider.observeConversations().first()
            assertTrue(conversations.first { it.id == "t2" }.isPinned)
            assertTrue(conversations.none { it.id == "t3" })
            val t1 = second.provider.observeMessages("t1").first()
            assertEquals("Persist me", t1.first { it.id == sent.id }.text)
            assertEquals("m3", t1.first { it.id == sent.id }.replyToId)
            assertTrue(t1.first { it.id == "m3" }.reactions.any { it.senderId == null && it.emoji == "👍" })
            assertEquals("half-typed", RoomDraftStore(second.db).get("t4"))
            assertEquals(ThemeMode.Light, second.preferences.observeThemeMode().first())
            // Sample data is not added a second time.
            second.provider.ensureSeeded()
            assertTrue(second.provider.observeConversations().first().none { it.id == "t3" })
            second.close()
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun attachment(id: String, messageId: String, localPath: String?) = Attachment(
        id = id,
        messageId = messageId,
        mimeType = "image/jpeg",
        localPath = localPath,
        sourceUri = "content://media/$id",
        fileName = "$id.jpg",
        sizeBytes = 1024,
        width = 800,
        height = 600,
        availability = AttachmentAvailability.NotStored,
    )
}
