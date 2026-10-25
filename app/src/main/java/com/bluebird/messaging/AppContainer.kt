package com.bluebird.messaging

import com.bluebird.messaging.core.data.DraftStore
import com.bluebird.messaging.core.data.InMemoryDraftStore
import com.bluebird.messaging.core.provider.MessagingProvider
import com.bluebird.messaging.core.provider.MockMessagingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency wiring. Small enough that a DI framework would add more weight than value;
 * swapping the provider means changing one line here.
 */
class AppContainer {
    /** Lives as long as the process: for work that must finish even if the screen that started it goes away. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val messagingProvider: MessagingProvider by lazy { MockMessagingProvider(replyScope = appScope) }

    val draftStore: DraftStore by lazy { InMemoryDraftStore() }
}
