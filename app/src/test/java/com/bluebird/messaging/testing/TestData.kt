package com.bluebird.messaging.testing

import androidx.test.core.app.ApplicationProvider
import com.bluebird.messaging.core.data.LocalMessageStore
import com.bluebird.messaging.core.data.PreferencesRepository
import com.bluebird.messaging.core.data.db.BluebirdDatabase
import com.bluebird.messaging.core.provider.MockMessagingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import java.time.Clock

/**
 * A fresh in-memory database with the mock provider on top, for one test. Queries run on the test's scheduler,
 * so `testScheduler.runCurrent()` settles all pending database work and virtual time controls simulated delays.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestData(
    scope: TestScope,
    clock: Clock,
    seedSampleData: Boolean = true,
    replyScope: CoroutineScope? = null,
    fileExists: (String) -> Boolean = { false },
    name: String? = null,
) {
    private val dispatcher = StandardTestDispatcher(scope.testScheduler)

    val db: BluebirdDatabase = BluebirdDatabase.build(
        ApplicationProvider.getApplicationContext(),
        name = name,
        queryContext = dispatcher,
        allowMainThread = true,
    )
    val store = LocalMessageStore(db, ioDispatcher = dispatcher, fileExists = fileExists)
    val preferences = PreferencesRepository(db)
    val provider = MockMessagingProvider(store, preferences, clock, replyScope, seedSampleData)

    fun close() = db.close()
}
