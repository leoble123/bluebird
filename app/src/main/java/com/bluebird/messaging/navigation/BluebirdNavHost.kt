package com.bluebird.messaging.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bluebird.messaging.AppContainer
import com.bluebird.messaging.core.designsystem.catalog.DesignCatalogScreen
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.feature.contacts.ContactsScreen
import com.bluebird.messaging.feature.conversation.ConversationScreen
import com.bluebird.messaging.feature.conversation.ConversationViewModel
import com.bluebird.messaging.feature.inbox.InboxScreen
import com.bluebird.messaging.feature.inbox.InboxViewModel
import com.bluebird.messaging.feature.settings.SettingsScreen

object Routes {
    const val INBOX = "inbox"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
    const val DESIGN_CATALOG = "design-catalog"
    const val CONVERSATION_ARG = "conversationId"
    const val CONVERSATION = "conversation/{$CONVERSATION_ARG}"

    fun conversation(id: String) = "conversation/$id"
}

@Composable
fun BluebirdNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val reduced = BluebirdTheme.motion.reduced
    val push = remember(reduced) { PushTransitions(reduced) }
    NavHost(
        navController = navController,
        startDestination = Routes.INBOX,
        enterTransition = push.enter,
        exitTransition = push.exit,
        popEnterTransition = push.popEnter,
        popExitTransition = push.popExit,
        predictivePopEnterTransition = push.gesturePopEnter,
        predictivePopExitTransition = push.gesturePopExit,
    ) {
        composable(Routes.INBOX) {
            with(push) {
                Screen {
                    val vm: InboxViewModel = viewModel(factory = InboxViewModel.factory(container.messagingProvider, container.appScope))
                    InboxScreen(
                        viewModel = vm,
                        onOpenConversation = { navController.navigate(Routes.conversation(it)) },
                        onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
            }
        }
        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(navArgument(Routes.CONVERSATION_ARG) { type = NavType.StringType }),
        ) { entry ->
            with(push) {
                Screen {
                    val conversationId = entry.arguments?.getString(Routes.CONVERSATION_ARG).orEmpty()
                    val vm: ConversationViewModel = viewModel(
                        factory = ConversationViewModel.factory(
                            conversationId,
                            container.messagingProvider,
                            container.draftStore,
                            container.appScope,
                        ),
                    )
                    ConversationScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }
            }
        }
        composable(Routes.CONTACTS) {
            with(push) { Screen { ContactsScreen(onBack = { navController.popBackStack() }) } }
        }
        composable(Routes.SETTINGS) {
            with(push) {
                Screen {
                    SettingsScreen(
                        providerName = container.messagingProvider.info.displayName,
                        onOpenDesignCatalog = { navController.navigate(Routes.DESIGN_CATALOG) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
        composable(Routes.DESIGN_CATALOG) {
            with(push) { Screen { DesignCatalogScreen(onBack = { navController.popBackStack() }) } }
        }
    }
}
