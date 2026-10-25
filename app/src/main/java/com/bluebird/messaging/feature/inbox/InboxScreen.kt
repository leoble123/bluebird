package com.bluebird.messaging.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluebird.messaging.core.designsystem.component.Avatar
import com.bluebird.messaging.core.designsystem.component.BluebirdDivider
import com.bluebird.messaging.core.designsystem.component.BluebirdIconButton
import com.bluebird.messaging.core.designsystem.component.BluebirdMenu
import com.bluebird.messaging.core.designsystem.component.BluebirdSearchField
import com.bluebird.messaging.core.designsystem.component.BluebirdTopBar
import com.bluebird.messaging.core.designsystem.component.ConversationRow
import com.bluebird.messaging.core.designsystem.component.ConversationRowDividerInset
import com.bluebird.messaging.core.designsystem.component.EmptyState
import com.bluebird.messaging.core.designsystem.component.GroupAvatar
import com.bluebird.messaging.core.designsystem.component.LargeTitle
import com.bluebird.messaging.core.designsystem.component.LoadingState
import com.bluebird.messaging.core.designsystem.component.MenuDivider
import com.bluebird.messaging.core.designsystem.component.MenuItem
import com.bluebird.messaging.core.designsystem.component.PinnedTile
import com.bluebird.messaging.core.designsystem.component.StatusPill
import com.bluebird.messaging.core.designsystem.component.SwipeAction
import com.bluebird.messaging.core.designsystem.component.SwipeActionRow
import com.bluebird.messaging.core.designsystem.component.UndoBar
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.GlassHost
import com.bluebird.messaging.core.designsystem.theme.WindowClass
import com.bluebird.messaging.core.designsystem.theme.glassSource

private val TopBarHeight = 52.dp

/** Callbacks for everything a user can do to one conversation from the inbox. */
private class ConversationActions(
    val open: (String) -> Unit,
    val setPinned: (String, Boolean) -> Unit,
    val setRead: (String, Boolean) -> Unit,
    val delete: (String) -> Unit,
)

@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onOpenConversation: (String) -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    searchState: TextFieldState = rememberTextFieldState(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var composing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }.collect(viewModel::onQueryChange)
    }

    val actions = remember(viewModel) {
        ConversationActions(
            open = { id ->
                viewModel.setRead(id, read = true)
                onOpenConversation(id)
            },
            setPinned = viewModel::setPinned,
            setRead = viewModel::setRead,
            delete = viewModel::delete,
        )
    }

    InboxContent(
        state = state,
        searchState = searchState,
        actions = actions,
        onCompose = { composing = true },
        onOpenContacts = onOpenContacts,
        onOpenSettings = onOpenSettings,
        onUndo = viewModel::undoDelete,
    )

    if (composing) {
        NewMessageSheet(
            viewModel = viewModel,
            onDismiss = { composing = false },
            onConversationReady = { id ->
                composing = false
                onOpenConversation(id)
            },
        )
    }
}

@Composable
private fun InboxContent(
    state: InboxUiState,
    searchState: TextFieldState,
    actions: ConversationActions,
    onCompose: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onUndo: () -> Unit,
) {
    val colors = BluebirdTheme.colors
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val collapseThreshold = with(LocalDensity.current) { 28.dp.roundToPx() }
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseThreshold }
    }
    // Like iOS, starting to scroll dismisses the keyboard.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling -> if (scrolling) focusManager.clearFocus() }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var menuFor by remember { mutableStateOf<String?>(null) }

    GlassHost {
        Box(Modifier.fillMaxSize().background(colors.background)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().glassSource(),
                contentPadding = PaddingValues(top = topInset + TopBarHeight, bottom = bottomInset + 96.dp),
            ) {
                item(key = "title", contentType = "title") {
                    LargeTitle("Messages") {
                        if (state.isSimulated) {
                            Spacer(Modifier.width(10.dp))
                            StatusPill("Simulated")
                        }
                    }
                }
                item(key = "search", contentType = "search") {
                    BluebirdSearchField(
                        state = searchState,
                        placeholder = "Search",
                        onSearch = { focusManager.clearFocus() },
                        modifier = Modifier.padding(horizontal = BluebirdTheme.layout.gutter).padding(bottom = 8.dp),
                    )
                }
                when {
                    state.isLoading -> item(key = "loading") { LoadingState() }
                    state.isSearching -> searchResults(state, actions, menuFor) { menuFor = it }
                    !state.hasAnyConversations -> item(key = "empty") {
                        EmptyState(
                            icon = BluebirdIcons.Bubble,
                            title = "No conversations yet",
                            message = "Start one and it will appear here.",
                            actionLabel = "New message",
                            onAction = onCompose,
                            modifier = Modifier.fillParentMaxHeight(0.65f),
                        )
                    }
                    else -> {
                        if (state.pinned.isNotEmpty()) {
                            item(key = "pinned", contentType = "pinned") {
                                PinnedGrid(state.pinned, actions, menuFor) { menuFor = it }
                            }
                        }
                        conversationRows(state.conversations, actions, menuFor) { menuFor = it }
                    }
                }
            }

            BluebirdTopBar(
                title = "Messages",
                glass = collapsed,
                titleVisible = collapsed,
                navigation = {
                    BluebirdIconButton(BluebirdIcons.Settings, "Settings", onClick = onOpenSettings)
                },
                actions = {
                    BluebirdIconButton(BluebirdIcons.People, "Contacts", onClick = onOpenContacts)
                    BluebirdIconButton(BluebirdIcons.Compose, "New message", onClick = onCompose)
                },
            )

            UndoBar(
                visible = state.undo != null,
                message = state.undo?.message.orEmpty(),
                onUndo = onUndo,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = bottomInset + 12.dp),
            )
        }
    }
}

private fun LazyListScope.searchResults(
    state: InboxUiState,
    actions: ConversationActions,
    menuFor: String?,
    onMenu: (String?) -> Unit,
) {
    if (state.searchResults.isEmpty()) {
        item(key = "no-results") {
            EmptyState(
                icon = BluebirdIcons.Search,
                title = "No results",
                message = "Nothing matches “${state.query.trim()}”. Try a name or a word from a message.",
                modifier = Modifier.fillParentMaxHeight(0.55f),
            )
        }
    } else {
        conversationRows(state.searchResults, actions, menuFor, keyPrefix = "result-", onMenu = onMenu)
    }
}

private fun LazyListScope.conversationRows(
    items: List<InboxItem>,
    actions: ConversationActions,
    menuFor: String?,
    keyPrefix: String = "row-",
    onMenu: (String?) -> Unit,
) {
    itemsIndexed(items, key = { _, item -> keyPrefix + item.id }, contentType = { _, _ -> "conversation" }) { index, item ->
        InboxRow(
            item = item,
            actions = actions,
            menuExpanded = menuFor == item.id,
            onMenu = onMenu,
            showDivider = index < items.lastIndex,
        )
    }
}

@Composable
private fun InboxRow(
    item: InboxItem,
    actions: ConversationActions,
    menuExpanded: Boolean,
    onMenu: (String?) -> Unit,
    showDivider: Boolean,
) {
    val colors = BluebirdTheme.colors
    val haptics = LocalHapticFeedback.current
    val readAction = SwipeAction(
        label = if (item.unread) "Read" else "Unread",
        icon = BluebirdIcons.Bubble,
        color = colors.accent,
    ) { actions.setRead(item.id, item.unread) }
    val deleteAction = SwipeAction(
        label = "Delete",
        icon = BluebirdIcons.Trash,
        color = colors.danger,
        removesItem = true,
    ) { actions.delete(item.id) }

    Box {
        SwipeActionRow(startAction = readAction, endAction = deleteAction) {
            Box {
                ConversationRow(
                    title = item.title,
                    preview = item.preview,
                    timestamp = item.timestamp,
                    unread = item.unread,
                    pinned = item.pinned,
                    highlight = item.highlight,
                    onClick = { actions.open(item.id) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMenu(item.id)
                    },
                    modifier = Modifier.semantics { customActions = accessibilityActions(item, actions) },
                    avatar = { ConversationAvatar(item.avatars, AvatarSize.lg) },
                )
                if (showDivider) {
                    BluebirdDivider(Modifier.align(Alignment.BottomStart), startInset = ConversationRowDividerInset)
                }
            }
        }
        ConversationMenu(item, menuExpanded, actions) { onMenu(null) }
    }
}

@Composable
private fun PinnedGrid(
    pinned: List<InboxItem>,
    actions: ConversationActions,
    menuFor: String?,
    onMenu: (String?) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val columns = when (BluebirdTheme.layout.windowClass) {
        WindowClass.Compact -> 3
        WindowClass.Medium -> 4
        WindowClass.Expanded -> 6
    }
    val avatarSize: Dp = if (BluebirdTheme.layout.windowClass == WindowClass.Compact) 68.dp else 72.dp
    Box(Modifier.padding(horizontal = BluebirdTheme.layout.gutter - 4.dp, vertical = 4.dp)) {
        Column {
            pinned.chunked(columns).forEach { rowItems ->
                Row(Modifier.fillMaxWidth()) {
                    rowItems.forEach { item ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                            PinnedTile(
                                name = item.title,
                                unread = item.unread,
                                avatarSize = avatarSize,
                                onClick = { actions.open(item.id) },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onMenu(item.id)
                                },
                                modifier = Modifier.semantics { customActions = accessibilityActions(item, actions) },
                                avatar = { ConversationAvatar(item.avatars, avatarSize) },
                            )
                            ConversationMenu(item, menuFor == item.id, actions) { onMenu(null) }
                        }
                    }
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ConversationMenu(item: InboxItem, expanded: Boolean, actions: ConversationActions, onDismiss: () -> Unit) {
    BluebirdMenu(expanded = expanded, onDismissRequest = onDismiss, alignment = Alignment.TopCenter) {
        MenuItem(
            label = if (item.pinned) "Unpin" else "Pin",
            icon = BluebirdIcons.Pin,
            onClick = {
                onDismiss()
                actions.setPinned(item.id, !item.pinned)
            },
        )
        MenuItem(
            label = if (item.unread) "Mark as Read" else "Mark as Unread",
            icon = BluebirdIcons.Bubble,
            onClick = {
                onDismiss()
                actions.setRead(item.id, item.unread)
            },
        )
        MenuDivider()
        MenuItem(
            label = "Delete",
            icon = BluebirdIcons.Trash,
            destructive = true,
            onClick = {
                onDismiss()
                actions.delete(item.id)
            },
        )
    }
}

/** The same actions as swipes and the long-press menu, exposed to TalkBack. */
private fun accessibilityActions(item: InboxItem, actions: ConversationActions) = listOf(
    CustomAccessibilityAction(if (item.pinned) "Unpin" else "Pin") { actions.setPinned(item.id, !item.pinned); true },
    CustomAccessibilityAction(if (item.unread) "Mark as read" else "Mark as unread") { actions.setRead(item.id, item.unread); true },
    CustomAccessibilityAction("Delete") { actions.delete(item.id); true },
)

@Composable
internal fun ConversationAvatar(avatars: List<AvatarSpec>, size: Dp) {
    when {
        avatars.size >= 2 -> GroupAvatar(avatars[0].name to avatars[0].seed, avatars[1].name to avatars[1].seed, size = size)
        avatars.size == 1 -> Avatar(avatars[0].name, avatars[0].seed, size = size)
        else -> Avatar("", "unknown", size = size)
    }
}
