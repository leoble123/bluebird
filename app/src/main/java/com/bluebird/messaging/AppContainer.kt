package com.bluebird.messaging

import android.content.Context
import com.bluebird.messaging.core.data.DraftStore
import com.bluebird.messaging.core.data.LocalMessageStore
import com.bluebird.messaging.core.data.PreferencesRepository
import com.bluebird.messaging.core.data.RoomDraftStore
import com.bluebird.messaging.core.data.db.BluebirdDatabase
import com.bluebird.messaging.core.provider.MessagingProvider
import com.bluebird.messaging.core.provider.MockMessagingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency wiring. Small enough that a DI framework would add more weight than value;
 * swapping the provider means changing one line here.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Lives as long as the process: for work that must finish even if the screen that started it goes away. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Opened lazily on first use; Room runs every query on its own background threads. */
    val database: BluebirdDatabase by lazy { BluebirdDatabase.build(appContext) }

    val messageStore: LocalMessageStore by lazy { LocalMessageStore(database) }

    val preferences: PreferencesRepository by lazy { PreferencesRepository(database) }

    val messagingProvider: MessagingProvider by lazy {
        MockMessagingProvider(messageStore, preferences, replyScope = appScope)
    }

    val draftStore: DraftStore by lazy { RoomDraftStore(database) }
}
