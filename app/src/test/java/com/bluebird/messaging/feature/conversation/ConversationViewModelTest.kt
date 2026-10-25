package com.bluebird.messaging.feature.conversation

import com.bluebird.messaging.core.data.InMemoryDraftStore
import com.bluebird.messaging.core.provider.MockMessagingProvider
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {

    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val drafts = InMemoryDraftStore()
    private lateinit var provider: MockMessagingProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        provider = MockMessagingProvider(clock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.newViewModel(id: String = "t1"): ConversationViewModel {
        val vm = ConversationViewModel(id, provider, drafts, clock, ZoneOffset.UTC, backgroundScope, flowOf(now))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private fun ConversationUiState.messages() = items.filterIsInstance<TimelineItem.MessageItem>()

    @Test
    fun loadsTheConversation() = runTest {
        val state = newViewModel().uiState.value
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
        val state = newViewModel("nope").uiState.value
        assertTrue(state.notFound)
    }

    @Test
    fun sendingAddsTheMessageAndClearsReplyAndDraft() = runTest {
        drafts.set("t1", "half-typed")
        val vm = newViewModel()
        assertEquals("half-typed", vm.initialDraft)

        vm.startReply("m3")
        assertEquals(ReplyUi("m3", "Maya", "Perfect. I'll grab a table 🙌"), vm.uiState.value.replyingTo)

        assertTrue(vm.send("  See you there  "))
        val newest = vm.uiState.value.messages().first()
        assertEquals("See you there", newest.text)
        assertTrue(newest.outgoing)
        assertEquals("Maya", newest.reply?.author)
        assertEquals("Sent", newest.status)
        assertNull(vm.uiState.value.replyingTo)
        assertEquals("", drafts.get("t1"))
    }

    @Test
    fun blankMessagesAreNotSent() = runTest {
        val vm = newViewModel()
        val before = vm.uiState.value.messages().size
        assertFalse(vm.send("   "))
        assertEquals(before, vm.uiState.value.messages().size)
    }

    @Test
    fun draftsAreSavedAsYouType() = runTest {
        val vm = newViewModel()
        vm.onDraftChanged("Running late")
        assertEquals("Running late", drafts.get("t1"))
        vm.onDraftChanged("")
        assertEquals("", drafts.get("t1"))
    }

    @Test
    fun reactingTogglesYourReaction() = runTest {
        val vm = newViewModel()
        vm.react("m3", "😂")
        assertTrue(vm.uiState.value.messages().first { it.id == "m3" }.reactions.single { it.emoji == "😂" }.mine)
        vm.react("m3", "😂")
        assertTrue(vm.uiState.value.messages().first { it.id == "m3" }.reactions.isEmpty())
    }

    @Test
    fun deletingHidesNowAndCommitsAfterTheUndoWindow() = runTest {
        val vm = newViewModel()
        vm.deleteMessages(setOf("m1"))
        assertTrue(vm.uiState.value.messages().none { it.id == "m1" })
        assertEquals("Message deleted", vm.uiState.value.undo?.message)

        testScheduler.advanceTimeBy(ConversationViewModel.UNDO_WINDOW_MS + 1)
        testScheduler.runCurrent()
        assertTrue(provider.observeMessages("t1").first().none { it.id == "m1" })
        assertNull(vm.uiState.value.undo)
    }

    @Test
    fun undoRestoresDeletedMessages() = runTest {
        val vm = newViewModel()
        vm.deleteMessages(setOf("m1", "m2"))
        assertEquals("2 messages deleted", vm.uiState.value.undo?.message)
        vm.undoDelete()
        testScheduler.advanceTimeBy(ConversationViewModel.UNDO_WINDOW_MS * 2)
        testScheduler.runCurrent()
        assertTrue(vm.uiState.value.messages().any { it.id == "m1" })
        assertEquals(3, provider.observeMessages("t1").first().size)
    }

    @Test
    fun copiedTextIsInReadingOrder() = runTest {
        val vm = newViewModel()
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
        assertEquals(setOf("m1", "m2"), vm.uiState.value.selection)
        assertTrue(vm.uiState.value.selecting)
        vm.toggleSelection("m1")
        assertEquals(setOf("m2"), vm.uiState.value.selection)
        vm.deleteMessages(vm.uiState.value.selection)
        assertFalse(vm.uiState.value.selecting)
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
        provider = MockMessagingProvider(clock, replyScope = backgroundScope)
        val vm = newViewModel()
        vm.send("Are you there?")

        testScheduler.advanceTimeBy(2_500)
        testScheduler.runCurrent()
        assertEquals("Maya is typing", vm.uiState.value.typing)

        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()
        assertNull(vm.uiState.value.typing)
        val newest = vm.uiState.value.messages().first()
        assertFalse(newest.outgoing)
        assertNotNull(vm.uiState.value.messages().firstOrNull { it.outgoing && it.status == "Read" })
    }

    @Test
    fun groupConversationsDescribeEveryone() = runTest {
        val state = newViewModel("t4").uiState.value
        assertEquals("Sam Okafor, Lena Fischer", state.title)
        assertEquals("2 people", state.subtitle)
        assertEquals(2, state.participants.size)
        assertEquals("Lena Fischer", state.messages().first().senderName)
    }

    @Test
    fun newConversationsAreEmpty() = runTest {
        val id = provider.startConversation(listOf("c8", "c1"))
        val state = newViewModel(id).uiState.value
        assertTrue(state.isEmpty)
        assertFalse(state.notFound)
    }
}
