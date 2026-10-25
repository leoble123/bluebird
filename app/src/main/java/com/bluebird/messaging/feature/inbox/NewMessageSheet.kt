package com.bluebird.messaging.feature.inbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluebird.messaging.core.designsystem.component.Avatar
import com.bluebird.messaging.core.designsystem.component.BluebirdButton
import com.bluebird.messaging.core.designsystem.component.BluebirdDivider
import com.bluebird.messaging.core.designsystem.component.BluebirdSearchField
import com.bluebird.messaging.core.designsystem.component.ButtonVariant
import com.bluebird.messaging.core.designsystem.component.ContactRow
import com.bluebird.messaging.core.designsystem.component.ContactRowDividerInset
import com.bluebird.messaging.core.designsystem.component.EmptyState
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import kotlinx.coroutines.launch

/** "New Message": pick a contact to open (or create) a conversation with them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewMessageSheet(
    viewModel: InboxViewModel,
    onDismiss: () -> Unit,
    onConversationReady: (String) -> Unit,
) {
    val colors = BluebirdTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val query = rememberTextFieldState()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

    LaunchedEffect(query) {
        snapshotFlow { query.text.toString() }.collect(viewModel::onContactQueryChange)
    }
    DisposableEffect(Unit) { onDispose { viewModel.onContactQueryChange("") } }

    fun close(then: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (colors.isDark) colors.surfaceRaised else colors.background,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                BluebirdButton(
                    "Cancel",
                    onClick = { close(onDismiss) },
                    variant = ButtonVariant.Plain,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(
                    "New Message",
                    style = BluebirdTheme.typography.headline,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.Center).semantics { heading() },
                )
            }
            BluebirdSearchField(
                state = query,
                placeholder = "To: name or number",
                onSearch = { focusManager.clearFocus() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (contacts.isEmpty()) {
                EmptyState(
                    icon = BluebirdIcons.People,
                    title = "No matches",
                    message = "No contacts match “${query.text.toString().trim()}”.",
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth()) {
                    itemsIndexed(contacts, key = { _, c -> c.id }) { index, contact ->
                        ContactRow(
                            name = contact.name,
                            detail = contact.detail,
                            onClick = {
                                viewModel.startConversation(contact.id) { id -> close { onConversationReady(id) } }
                            },
                            avatar = { Avatar(contact.avatar.name, contact.avatar.seed, size = AvatarSize.md) },
                        )
                        if (index < contacts.lastIndex) BluebirdDivider(startInset = ContactRowDividerInset)
                    }
                }
            }
        }
    }
}
