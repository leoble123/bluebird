package com.bluebird.messaging.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
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
    NavHost(
        navController = navController,
        startDestination = Routes.INBOX,
        // iOS-style push: the new screen slides in from the trailing edge while the old one drifts a third of the
        // way out. Short and decelerating, so it never feels like waiting. No motion at all when animations are off.
        enterTransition = { if (reduced) EnterTransition.None else slideInHorizontally(pushSpec()) { it } },
        exitTransition = { if (reduced) ExitTransition.None else slideOutHorizontally(pushSpec()) { -it / 3 } },
        popEnterTransition = { if (reduced) EnterTransition.None else slideInHorizontally(pushSpec()) { -it / 3 } },
        popExitTransition = { if (reduced) ExitTransition.None else slideOutHorizontally(pushSpec()) { it } },
    ) {
        composable(Routes.INBOX) {
            val vm: InboxViewModel = viewModel(factory = InboxViewModel.factory(container.messagingProvider, container.appScope))
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
        composable(Routes.CONTACTS) {
            ContactsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                providerName = container.messagingProvider.info.displayName,
                onOpenDesignCatalog = { navController.navigate(Routes.DESIGN_CATALOG) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.DESIGN_CATALOG) {
            DesignCatalogScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Quick start, long gentle settle: close to UIKit's navigation push curve. */
private val PushEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)

private fun pushSpec() = tween<IntOffset>(durationMillis = 320, easing = PushEasing)
