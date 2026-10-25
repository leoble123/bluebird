package com.bluebird.messaging.feature.conversation

import android.content.ClipData
import android.os.Build
import com.bluebird.messaging.core.designsystem.component.GlassIconButton
import com.bluebird.messaging.core.designsystem.component.GlassTextButton
import com.bluebird.messaging.core.designsystem.theme.ScrollEdge
import com.bluebird.messaging.core.designsystem.theme.scrollEdgeEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.Animatable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluebird.messaging.core.designsystem.component.BluebirdComposer
import com.bluebird.messaging.core.designsystem.component.BluebirdMenu
import com.bluebird.messaging.core.designsystem.component.BluebirdTopBar
import com.bluebird.messaging.core.designsystem.component.BubbleRow
import com.bluebird.messaging.core.designsystem.component.ConversationTopBar
import com.bluebird.messaging.core.designsystem.component.EmptyState
import com.bluebird.messaging.core.designsystem.component.MenuItem
import com.bluebird.messaging.core.designsystem.component.NoticePill
import com.bluebird.messaging.core.designsystem.component.ReplyComposerHeader
import com.bluebird.messaging.core.designsystem.component.TypingIndicator
import com.bluebird.messaging.core.designsystem.component.UndoBar
import com.bluebird.messaging.core.designsystem.component.rememberAnchoredMenuPosition
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.GlassHost
import com.bluebird.messaging.core.designsystem.theme.glass
import com.bluebird.messaging.core.designsystem.theme.glassSource
import com.bluebird.messaging.feature.inbox.ConversationAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onBack: () -> Unit,
    draft: TextFieldState = rememberTextFieldState(viewModel.initialDraft),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    var menuFor by remember { mutableStateOf<String?>(null) }
    val reveal = remember { Animatable(0f) }
    val revealMax = with(LocalDensity.current) { TimestampRevealWidth.toPx() }
    var attachMenu by remember { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(viewModel) {
        viewModel.setVisible(true)
        onPauseOrDispose { viewModel.setVisible(false) }
    }
    LaunchedEffect(draft) {
        snapshotFlow { draft.text.toString() }.collect(viewModel::onDraftChanged)
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(1_500)
            notice = null
        }
    }
    BackHandler(enabled = state.selecting) { viewModel.endSelection() }

    fun copy(text: String) {
        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Message", text))) }
        // Android 13+ shows its own clipboard confirmation; only older versions need ours.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) notice = "Copied"
    }

    val actions = remember(viewModel) {
        MessageActions(
            openMenu = { menuFor = it },
            closeMenu = { menuFor = null },
            reply = viewModel::startReply,
            copy = { id -> copy(viewModel.textOf(setOf(id))) },
            react = viewModel::react,
            select = viewModel::beginSelection,
            toggleSelected = viewModel::toggleSelection,
            delete = { id -> viewModel.deleteMessages(setOf(id)) },
            revealTimesBy = { delta -> scope.launch { reveal.snapTo((reveal.value + delta).coerceIn(0f, revealMax)) } },
            endReveal = { scope.launch { reveal.animateTo(0f) } },
        )
    }

    GlassHost {
        Scaffold(
            containerColor = BluebirdTheme.colors.background,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                if (state.selecting) {
                    BluebirdTopBar(
                        title = "${state.selection.size} Selected",
                        glass = true,
                        navigation = {
                            GlassTextButton("Cancel", onClick = viewModel::endSelection)
                        },
                    )
                } else {
                    ConversationTopBar(
                        title = state.title,
                        subtitle = if (state.isSimulated) "Simulated" else null,
                        onBack = onBack,
                        onOpenDetails = { showDetails = true },
                        avatar = { ConversationAvatar(state.avatars, 36.dp) },
                    )
                }
            },
            bottomBar = {
                when {
                    state.notFound -> Unit
                    state.selecting -> SelectionActionBar(
                        count = state.selection.size,
                        onCopy = {
                            copy(viewModel.textOf(state.selection))
                            viewModel.endSelection()
                        },
                        onDelete = { viewModel.deleteMessages(state.selection) },
                    )
                    else -> BluebirdComposer(
                        state = draft,
                        onSend = { if (viewModel.send(draft.text.toString())) draft.clearText() },
                        onAttach = { attachMenu = true },
                        header = state.replyingTo?.let { reply ->
                            { ReplyComposerHeader(reply.author, reply.text, onCancel = viewModel::cancelReply) }
                        },
                        attachmentMenu = { AttachmentMenu(expanded = attachMenu, onDismiss = { attachMenu = false }) },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().glassSource()) {
                    when {
                        state.isLoading -> Unit
                        state.notFound -> EmptyState(
                            icon = BluebirdIcons.Bubble,
                            title = "Conversation not found",
                            message = "It may have been deleted.",
                            actionLabel = "Back to Messages",
                            onAction = onBack,
                            modifier = Modifier.padding(padding),
                        )
                        state.isEmpty && state.typing == null -> EmptyState(
                            icon = BluebirdIcons.Bubble,
                            title = "New conversation with ${state.title}",
                            message = "Bluebird is a local prototype: messages are simulated on this device and are not " +
                                "sent to ${state.title}.",
                            modifier = Modifier.padding(padding),
                        )
                        else -> Timeline(state, padding, menuFor, revealPx = { reveal.value }, actions = actions)
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = padding.calculateBottomPadding() + 12.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    UndoBar(visible = state.undo != null, message = state.undo?.message.orEmpty(), onUndo = viewModel::undoDelete)
                    NoticePill(visible = notice != null, message = notice.orEmpty())
                }
            }
        }
    }

    if (showDetails && !state.notFound) {
        ConversationDetailsSheet(state = state, onDismiss = { showDetails = false })
    }
}

@Composable
private fun Timeline(
    state: ConversationUiState,
    padding: PaddingValues,
    menuFor: String?,
    revealPx: () -> Float,
    actions: MessageActions,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val newest = state.items.firstOrNull()
    val newestKey = newest?.key
    val newestIsMine = (newest as? TimelineItem.MessageItem)?.outgoing == true

    // The list is drawn bottom-up. When something new arrives at the bottom, follow it if the reader is already
    // there (or if it's their own message); otherwise leave their place in the history alone.
    LaunchedEffect(newestKey, state.typing != null) {
        if (newestIsMine || listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0)
    }
    val showJump by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 12.dp,
            ),
        ) {
            if (state.typing != null) {
                item(key = "typing", contentType = "typing") {
                    BubbleRow(
                        outgoing = false,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    ) { TypingIndicator() }
                }
            }
            items(
                items = state.items,
                key = { it.key },
                contentType = { if (it is TimelineItem.Separator) "separator" else "message" },
            ) { item ->
                when (item) {
                    is TimelineItem.Separator -> TimelineSeparator(item.label)
                    is TimelineItem.MessageItem -> MessageRow(
                        item = item,
                        selecting = state.selecting,
                        selected = item.id in state.selection,
                        menuExpanded = menuFor == item.id,
                        revealPx = revealPx,
                        actions = actions,
                    )
                }
            }
        }
        if (showJump) {
            GlassIconButton(
                icon = BluebirdIcons.ChevronDown,
                contentDescription = "Jump to latest",
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                size = 40.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = padding.calculateBottomPadding() + 12.dp),
            )
        }
    }
}

@Composable
private fun AttachmentMenu(expanded: Boolean, onDismiss: () -> Unit) {
    BluebirdMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        positionProvider = rememberAnchoredMenuPosition(alignEnd = false),
    ) {
        MenuItem(
            "Photos & Videos",
            icon = BluebirdIcons.Photo,
            enabled = false,
            supportingText = "Coming in a later update",
            onClick = {},
        )
        MenuItem(
            "Camera",
            icon = BluebirdIcons.Camera,
            enabled = false,
            supportingText = "Coming in a later update",
            onClick = {},
        )
    }
}

@Composable
private fun SelectionActionBar(count: Int, onCopy: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .scrollEdgeEffect(ScrollEdge.Bottom)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(BluebirdIcons.Copy, "Copy", onClick = onCopy, enabled = count > 0)
        Spacer(Modifier.weight(1f))
        GlassIconButton(BluebirdIcons.Trash, "Delete", onClick = onDelete, enabled = count > 0)
    }
}
