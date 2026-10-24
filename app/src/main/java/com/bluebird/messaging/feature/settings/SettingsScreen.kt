package com.bluebird.messaging.feature.settings

import androidx.compose.runtime.Composable
import com.bluebird.messaging.BuildConfig
import com.bluebird.messaging.core.designsystem.component.PlaceholderMessage
import com.bluebird.messaging.core.designsystem.component.PlaceholderScaffold

/** Placeholder. Built out in Prompt 7. Already states plainly that the connection is simulated. */
@Composable
fun SettingsScreen(providerName: String, onBack: () -> Unit) {
    PlaceholderScaffold(title = "Settings", onBack = onBack) { padding ->
        PlaceholderMessage(
            "Bluebird ${BuildConfig.VERSION_NAME}\nConnection: $providerName\nNo real messages are sent or received.",
            padding,
        )
    }
}
