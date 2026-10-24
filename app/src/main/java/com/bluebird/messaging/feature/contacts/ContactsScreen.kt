package com.bluebird.messaging.feature.contacts

import androidx.compose.runtime.Composable
import com.bluebird.messaging.core.designsystem.component.PlaceholderMessage
import com.bluebird.messaging.core.designsystem.component.PlaceholderScaffold

/** Placeholder. Built out in Prompt 7. */
@Composable
fun ContactsScreen(onBack: () -> Unit) {
    PlaceholderScaffold(title = "Contacts", onBack = onBack) { padding ->
        PlaceholderMessage("Contacts coming in a later phase.", padding)
    }
}
