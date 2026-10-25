package com.bluebird.messaging.feature.conversation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bluebird.messaging.core.data.InMemoryDraftStore
import com.bluebird.messaging.core.provider.MockMessagingProvider
import com.bluebird.messaging.testing.TestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ConversationViewModelTest {

    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val drafts = InMemoryDraftStore()
    private lateinit var data: TestData
    private val provider: MockMessagingProvider get() = data.provider

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        if (::data.isInitialized) data.close()
        Dispatchers.resetMain()
    }

    /** Lets pending database work and Flow updates finish (without advancing time), then reads the state. */
    private fun TestScope.settled(vm: ConversationViewModel): ConversationUiState {
        testScheduler.runCurrent()
        return vm.uiState.value
    }

    /** Creates the database and provider on first use. */
    private fun TestScope.useData(replies: Boolean = false): TestData {
        if (!::data.isInitialized) data = TestData(this, clock, replyScope = if (replies) backgroundScope else null)
        return data
    }

    private fun TestScope.newViewModel(id: String = "t1", replies: Boolean = false): ConversationViewModel {
        useData(replies)
        val vm = ConversationViewModel(id, provider, drafts, clock, ZoneOffset.UTC, backgroundScope, flowOf(now), UnconfinedTestDispatcher(testScheduler))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private fun ConversationUiState.messages() = items.filterIsInstance<TimelineItem.MessageItem>()

    @Test
    fun loadsTheConversation() = runTest {
        val state = settled(newViewModel())
        assertFalse(state.isLoading)
        assertEquals("Maya Chen", state.title)
        assertEquals("+1 555 0101", state.subtitle)
        assertTrue(state.isSimulated)
        val newest = state.messages().first()
        assertEquals("m3", newest.id)
        assertEquals("You", newest.reply?.author)
        assertEquals("❤️", state.messages().first { it.id == "m2" }.reactions.single().emoji)
    }

    @Test
    fun unknownConversationIsNotFound() = runTest {
        val state = settled(newViewModel("nope"))
        assertTrue(state.notFound)
    }

    @Test
    fun sendingAddsTheMessageAndClearsReplyAndDraft() = runTest {
        drafts.set("t1", "half-typed")
        val vm = newViewModel()
        assertEquals("half-typed", vm.loadDraft())

        vm.startReply("m3")
        assertEquals(ReplyUi("m3", "Maya", "Perfect. I'll grab a table 🙌"), settled(vm).replyingTo)

        assertTrue(vm.send("  See you there  "))
        val newest = settled(vm).messages().first()
        assertEquals("See you there", newest.text)
        assertTrue(newest.outgoing)
        assertEquals("Maya", newest.reply?.author)
        assertEquals("Sent", newest.status)
        assertNull(settled(vm).replyingTo)
        assertEquals("", drafts.get("t1"))
    }

    @Test
    fun blankMessagesAreNotSent() = runTest {
        val vm = newViewModel()
        val before = settled(vm).messages().size
        assertFalse(vm.send("   "))
        assertEquals(before, settled(vm).messages().size)
    }

    @Test
    fun draftsAreSavedAsYouType() = runTest {
        val vm = newViewModel()
        vm.onDraftChanged("Running late")
        assertEquals("", drafts.get("t1")) // not yet: saving waits for a pause in typing
        testScheduler.advanceTimeBy(ConversationViewModel.DRAFT_SAVE_DELAY_MS + 1)
        testScheduler.runCurrent()
        assertEquals("Running late", drafts.get("t1"))
        vm.onDraftChanged("")
        testScheduler.advanceTimeBy(ConversationViewModel.DRAFT_SAVE_DELAY_MS + 1)
        testScheduler.runCurrent()
        assertEquals("", drafts.get("t1"))
    }

    @Test
    fun reactingTogglesYourReaction() = runTest {
        val vm = newViewModel()
        vm.react("m3", "😂")
        assertTrue(settled(vm).messages().first { it.id == "m3" }.reactions.single { it.emoji == "😂" }.mine)
        vm.react("m3", "😂")
        assertTrue(settled(vm).messages().first { it.id == "m3" }.reactions.isEmpty())
    }

    @Test
    fun deletingHidesNowAndCommitsAfterTheUndoWindow() = runTest {
        val vm = newViewModel()
        vm.deleteMessages(setOf("m1"))
        assertTrue(settled(vm).messages().none { it.id == "m1" })
        assertEquals("Message deleted", settled(vm).undo?.message)

        testScheduler.advanceTimeBy(ConversationViewModel.UNDO_WINDOW_MS + 1)
        testScheduler.runCurrent()
        assertTrue(provider.observeMessages("t1").first().none { it.id == "m1" })
        assertNull(settled(vm).undo)
    }

    @Test
    fun undoRestoresDeletedMessages() = runTest {
        val vm = newViewModel()
        vm.deleteMessages(setOf("m1", "m2"))
        assertEquals("2 messages deleted", settled(vm).undo?.message)
        vm.undoDelete()
        testScheduler.advanceTimeBy(ConversationViewModel.UNDO_WINDOW_MS * 2)
        testScheduler.runCurrent()
        assertTrue(settled(vm).messages().any { it.id == "m1" })
        assertEquals(3, provider.observeMessages("t1").first().size)
    }

    @Test
    fun copiedTextIsInReadingOrder() = runTest {
        val vm = newViewModel()
        testScheduler.runCurrent()
        assertEquals(
            "Are we still on for dinner tonight?\nPerfect. I'll grab a table 🙌",
            vm.textOf(setOf("m3", "m1")),
        )
    }

    @Test
    fun selectionMode() = runTest {
        val vm = newViewModel()
        vm.beginSelection("m1")
        vm.toggleSelection("m2")
        assertEquals(setOf("m1", "m2"), settled(vm).selection)
        assertTrue(settled(vm).selecting)
        vm.toggleSelection("m1")
        assertEquals(setOf("m2"), settled(vm).selection)
        vm.deleteMessages(settled(vm).selection)
        assertFalse(settled(vm).selecting)
    }

    @Test
    fun messagesAreMarkedReadOnlyWhileVisible() = runTest {
        val vm = newViewModel()
        suspend fun unread() = provider.observeConversations().first().first { it.id == "t1" }.hasUnread
        assertTrue(unread())
        vm.setVisible(true)
        assertFalse(unread())
    }

    @Test
    fun theOtherPersonTypesThenReplies() = runTest {
        val vm = newViewModel(replies = true)
        vm.send("Are you there?")

        testScheduler.advanceTimeBy(2_500)
        testScheduler.runCurrent()
        assertEquals("Maya is typing", settled(vm).typing)

        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()
        assertNull(settled(vm).typing)
        // The frozen clock gives the reply the same timestamp as the sent message, so look for it rather than
        // relying on its position.
        assertTrue(settled(vm).messages().any { !it.outgoing && it.id.startsWith("m-") })
        assertNotNull(settled(vm).messages().firstOrNull { it.outgoing && it.status == "Read" })
    }

    @Test
    fun groupConversationsDescribeEveryone() = runTest {
        val state = settled(newViewModel("t4"))
        assertEquals("Sam Okafor, Lena Fischer", state.title)
        assertEquals("2 people", state.subtitle)
        assertEquals(2, state.participants.size)
        assertEquals("Lena Fischer", state.messages().first().senderName)
    }

    @Test
    fun newConversationsAreEmpty() = runTest {
        useData()
        val id = provider.startConversation(listOf("c8", "c1"))
        val state = settled(newViewModel(id))
        assertTrue(state.isEmpty)
        assertFalse(state.notFound)
    }
}
