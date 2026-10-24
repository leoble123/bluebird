package com.bluebird.messaging.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bluebird.messaging.AppContainer
import com.bluebird.messaging.feature.contacts.ContactsScreen
import com.bluebird.messaging.feature.conversation.ConversationScreen
import com.bluebird.messaging.feature.inbox.InboxScreen
import com.bluebird.messaging.feature.inbox.InboxViewModel
import com.bluebird.messaging.feature.settings.SettingsScreen

object Routes {
    const val INBOX = "inbox"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
    const val CONVERSATION_ARG = "conversationId"
    const val CONVERSATION = "conversation/{$CONVERSATION_ARG}"

    fun conversation(id: String) = "conversation/$id"
}

@Composable
fun BluebirdNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.INBOX) {
        composable(Routes.INBOX) {
            val vm: InboxViewModel = viewModel(factory = InboxViewModel.factory(container.messagingProvider))
            InboxScreen(
                viewModel = vm,
                onOpenConversation = { navController.navigate(Routes.conversation(it)) },
                onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(navArgument(Routes.CONVERSATION_ARG) { type = NavType.StringType }),
        ) { entry ->
            ConversationScreen(
                conversationId = entry.arguments?.getString(Routes.CONVERSATION_ARG).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.CONTACTS) {
            ContactsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                providerName = container.messagingProvider.info.displayName,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
