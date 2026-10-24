package com.bluebird.messaging

import com.bluebird.messaging.core.provider.MessagingProvider
import com.bluebird.messaging.core.provider.MockMessagingProvider

/**
 * Manual dependency wiring. Small enough that a DI framework would add more weight than value;
 * swapping the provider means changing one line here.
 */
class AppContainer {
    val messagingProvider: MessagingProvider by lazy { MockMessagingProvider() }
}
