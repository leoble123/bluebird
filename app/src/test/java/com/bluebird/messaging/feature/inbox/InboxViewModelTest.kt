package com.bluebird.messaging.feature.inbox

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
class InboxViewModelTest {

    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
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

    private fun TestScope.newViewModel(): InboxViewModel {
        val vm = InboxViewModel(provider, clock, ZoneOffset.UTC, commitScope = backgroundScope, timeTicks = flowOf(now))
        // Keep the state flows active, as a visible screen would.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.contacts.collect {} }
        return vm
    }

    @Test
    fun pinnedConversationsAreSeparatedFromTheList() = runTest {
        val state = newViewModel().uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("t1", "t5", "t6"), state.pinned.map { it.id })
        assertTrue(state.conversations.none { it.pinned })
        assertEquals("t4", state.conversations.first().id) // most recent unpinned activity
    }

    @Test
    fun rowsArePreFormatted() = runTest {
        val state = newViewModel().uiState.value
        val group = state.conversations.first { it.id == "t4" }
        assertEquals("Lena: I can do chips and salsa", group.preview)
        assertTrue(group.unread)
        assertEquals(2, group.avatars.size)
        val maya = state.pinned.first { it.id == "t1" }
        assertEquals("11:56 AM", maya.timestamp.replace(' ', ' '))
    }

    @Test
    fun searchMatchesNamesAndHighlightsThem() = runTest {
        val vm = newViewModel()
        vm.onQueryChange("maya")
        val state = vm.uiState.value
        assertTrue(state.isSearching)
        assertEquals(listOf("t1"), state.searchResults.map { it.id })
        assertEquals("maya", state.searchResults.single().highlight)
    }

    @Test
    fun searchByContentShowsTheMatchingMessageRatherThanTheLatest() = runTest {
        val vm = newViewModel()
        vm.onQueryChange("slides")
        val result = vm.uiState.value.searchResults.single()
        assertEquals("t2", result.id)
        assertEquals("Sent you the slides from today", result.preview)
    }

    @Test
    fun searchWithNoMatchesIsEmpty() = runTest {
        val vm = newViewModel()
        vm.onQueryChange("zebra crossing")
        val state = vm.uiState.value
        assertTrue(state.isSearching)
        assertTrue(state.searchResults.isEmpty())
        assertTrue(state.hasAnyConversations)
    }

    @Test
    fun deleteHidesImmediatelyAndCommitsAfterTheUndoWindow() = runTest {
        val vm = newViewModel()
        vm.delete("t2")
        assertTrue(vm.uiState.value.conversations.none { it.id == "t2" })
        assertNotNull(vm.uiState.value.undo)
        assertNotNull(provider.observeConversations().first().firstOrNull { it.id == "t2" })

        testScheduler.advanceTimeBy(InboxViewModel.UNDO_WINDOW_MS + 1)
        testScheduler.runCurrent()

        assertNull(provider.observeConversations().first().firstOrNull { it.id == "t2" })
        assertNull(vm.uiState.value.undo)
    }

    @Test
    fun undoRestoresTheConversation() = runTest {
        val vm = newViewModel()
        vm.delete("t2")
        vm.undoDelete()
        testScheduler.advanceTimeBy(InboxViewModel.UNDO_WINDOW_MS * 2)
        testScheduler.runCurrent()

        assertTrue(vm.uiState.value.conversations.any { it.id == "t2" })
        assertNull(vm.uiState.value.undo)
        assertNotNull(provider.observeConversations().first().firstOrNull { it.id == "t2" })
    }

    @Test
    fun aSecondDeleteCommitsTheFirstStraightAway() = runTest {
        val vm = newViewModel()
        vm.delete("t2")
        vm.delete("t3")
        testScheduler.runCurrent()

        assertNull(provider.observeConversations().first().firstOrNull { it.id == "t2" })
        assertEquals("t3", vm.uiState.value.undo?.conversationId)
    }

    @Test
    fun pinAndReadActionsUpdateTheList() = runTest {
        val vm = newViewModel()
        vm.setPinned("t2", true)
        assertTrue(vm.uiState.value.pinned.any { it.id == "t2" })

        vm.setRead("t4", read = true)
        assertFalse(vm.uiState.value.conversations.first { it.id == "t4" }.unread)

        vm.setRead("t3", read = false)
        assertTrue(vm.uiState.value.conversations.first { it.id == "t3" }.unread)
    }

    @Test
    fun startingAConversationReusesOrCreates() = runTest {
        val vm = newViewModel()
        var opened: String? = null
        vm.startConversation("c1") { opened = it }
        assertEquals("t1", opened)

        vm.startConversation("c8") { opened = it }
        assertEquals("t8", opened)

        vm.startConversation("c3") { opened = it }
        assertEquals("t3", opened)
    }

    @Test
    fun contactSearchFilters() = runTest {
        val vm = newViewModel()
        assertTrue(vm.contacts.value.size > 5)
        vm.onContactQueryChange("ava")
        assertEquals(listOf("Ava Thompson"), vm.contacts.value.map { it.name })
    }

    @Test
    fun emptyInbox() = runTest {
        provider = MockMessagingProvider(clock, seedSampleData = false)
        val state = newViewModel().uiState.value
        assertFalse(state.hasAnyConversations)
        assertTrue(state.pinned.isEmpty() && state.conversations.isEmpty())
    }

    @Test
    fun unreadFilterFlattensPinsIntoTheList() = runTest {
        val vm = newViewModel()
        vm.setFilter(InboxFilter.Unread)
        val state = vm.uiState.value
        assertEquals(InboxFilter.Unread, state.filter)
        assertTrue(state.pinned.isEmpty())
        assertEquals(setOf("t1", "t4"), state.conversations.map { it.id }.toSet())
        assertTrue(state.conversations.all { it.unread })

        vm.setRead("t1", read = true)
        vm.setRead("t4", read = true)
        assertTrue(vm.uiState.value.conversations.isEmpty())

        vm.setFilter(InboxFilter.All)
        assertEquals(listOf("t1", "t5", "t6"), vm.uiState.value.pinned.map { it.id })
    }
}
