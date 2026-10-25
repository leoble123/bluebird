package com.bluebird.messaging.feature.inbox

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.bluebird.messaging.core.designsystem.component.BluebirdMenu
import com.bluebird.messaging.core.designsystem.component.ConversationRow
import com.bluebird.messaging.core.designsystem.component.ConversationRowDividerInset
import com.bluebird.messaging.core.designsystem.component.EmptyState
import com.bluebird.messaging.core.designsystem.component.GlassButtonGroup
import com.bluebird.messaging.core.designsystem.component.GlassIconButton
import com.bluebird.messaging.core.designsystem.component.GlassSearchBar
import com.bluebird.messaging.core.designsystem.component.GroupAvatar
import com.bluebird.messaging.core.designsystem.component.GroupedIconButton
import com.bluebird.messaging.core.designsystem.component.LargeTitle
import com.bluebird.messaging.core.designsystem.component.LoadingState
import com.bluebird.messaging.core.designsystem.component.MenuDivider
import com.bluebird.messaging.core.designsystem.component.MenuItem
import com.bluebird.messaging.core.designsystem.component.PinnedTile
import com.bluebird.messaging.core.designsystem.component.StatusPill
import com.bluebird.messaging.core.designsystem.component.SwipeAction
import com.bluebird.messaging.core.designsystem.component.SwipeActionRow
import com.bluebird.messaging.core.designsystem.component.UndoBar
import com.bluebird.messaging.core.designsystem.component.rememberAnchoredMenuPosition
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.GlassHost
import com.bluebird.messaging.core.designsystem.theme.ScrollEdge
import com.bluebird.messaging.core.designsystem.theme.WindowClass
import com.bluebird.messaging.core.designsystem.theme.glassSource
import com.bluebird.messaging.core.designsystem.theme.scrollEdgeEffect

private val TopBarHeight = 56.dp
private val BottomBarHeight = 60.dp

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
        onFilter = viewModel::setFilter,
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
    onFilter: (InboxFilter) -> Unit,
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
    var filterMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    val unreadOnly = state.filter == InboxFilter.Unread

    GlassHost {
        Box(Modifier.fillMaxSize().background(colors.background)) {
            // Content runs edge to edge; controls float above it (iOS 26 / Liquid Glass layering).
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().glassSource(),
                contentPadding = PaddingValues(top = topInset + TopBarHeight, bottom = bottomInset + BottomBarHeight + 16.dp),
            ) {
                item(key = "title", contentType = "title") {
                    LargeTitle(if (unreadOnly) "Unread" else "Messages") {
                        if (state.isSimulated) {
                            Spacer(Modifier.width(10.dp))
                            StatusPill("Simulated")
                        }
                    }
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
                    unreadOnly && state.conversations.isEmpty() -> item(key = "no-unread") {
                        EmptyState(
                            icon = BluebirdIcons.Check,
                            title = "No unread messages",
                            message = "You're all caught up.",
                            actionLabel = "Show all messages",
                            onAction = { onFilter(InboxFilter.All) },
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

            // Top: compact title once the large one scrolls away, and the Filter / More actions in one capsule.
            Box(
                Modifier
                    .fillMaxWidth()
                    .scrollEdgeEffect(ScrollEdge.Top)
                    .padding(top = topInset)
                    .height(TopBarHeight)
                    .padding(horizontal = 16.dp),
            ) {
                val titleAlpha by animateFloatAsState(if (collapsed) 1f else 0f, BluebirdTheme.motion.fade(), label = "inboxTitle")
                Text(
                    if (unreadOnly) "Unread" else "Messages",
                    style = BluebirdTheme.typography.headline,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.Center).graphicsLayer { alpha = titleAlpha },
                )
                GlassButtonGroup(Modifier.align(Alignment.CenterEnd)) {
                    Box {
                        GroupedIconButton(BluebirdIcons.Filter, "Filter conversations") { filterMenu = true }
                        BluebirdMenu(
                            expanded = filterMenu,
                            onDismissRequest = { filterMenu = false },
                            positionProvider = rememberAnchoredMenuPosition(alignEnd = true),
                        ) {
                            MenuItem(
                                "All Messages",
                                icon = if (!unreadOnly) BluebirdIcons.Check else null,
                                onClick = { filterMenu = false; onFilter(InboxFilter.All) },
                            )
                            MenuItem(
                                "Unread",
                                icon = if (unreadOnly) BluebirdIcons.Check else null,
                                onClick = { filterMenu = false; onFilter(InboxFilter.Unread) },
                            )
                        }
                    }
                    Box {
                        GroupedIconButton(BluebirdIcons.More, "More") { moreMenu = true }
                        BluebirdMenu(
                            expanded = moreMenu,
                            onDismissRequest = { moreMenu = false },
                            positionProvider = rememberAnchoredMenuPosition(alignEnd = true),
                        ) {
                            MenuItem("Contacts", icon = BluebirdIcons.People, onClick = { moreMenu = false; onOpenContacts() })
                            MenuItem("Settings", icon = BluebirdIcons.Settings, onClick = { moreMenu = false; onOpenSettings() })
                        }
                    }
                }
            }

            // Bottom: search docked within thumb reach, with Compose detached beside it, as in iOS 26 Messages.
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .scrollEdgeEffect(ScrollEdge.Bottom)
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                UndoBar(
                    visible = state.undo != null,
                    message = state.undo?.message.orEmpty(),
                    onUndo = onUndo,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassSearchBar(
                        state = searchState,
                        onSearch = { focusManager.clearFocus() },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    GlassIconButton(BluebirdIcons.Compose, "New message", onClick = onCompose)
                }
            }
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
    // Unread pins show their latest message above the avatar; every tile reserves the space so rows line up.
    val previewSpace = if (pinned.any { it.unread }) 30.dp else 0.dp
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
                                preview = if (item.unread) item.preview else null,
                                previewSpace = previewSpace,
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
