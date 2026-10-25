package com.bluebird.messaging.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bluebird.messaging.core.designsystem.catalog.BubbleSample
import com.bluebird.messaging.core.designsystem.catalog.ButtonSample
import com.bluebird.messaging.core.designsystem.catalog.CatalogSection
import com.bluebird.messaging.core.designsystem.catalog.ColorSample
import com.bluebird.messaging.core.designsystem.catalog.GlassSample
import com.bluebird.messaging.core.designsystem.catalog.InputSample
import com.bluebird.messaging.core.designsystem.catalog.RowSample
import com.bluebird.messaging.core.designsystem.catalog.StateSample
import com.bluebird.messaging.core.designsystem.catalog.TypeSample
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.provider.MockMessagingProvider
import com.bluebird.messaging.feature.inbox.InboxScreen
import com.bluebird.messaging.feature.inbox.InboxViewModel
import com.bluebird.messaging.feature.settings.SettingsScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the design system on the JVM. `.\gradlew.bat recordRoborazziDebug` writes the images to
 * app/build/outputs/roborazzi/; a plain test run still composes everything as a smoke test.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h1200dp-xxhdpi")
class DesignSystemScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun snap(name: String, dark: Boolean, content: @Composable () -> Unit) {
        // Manual clock: the typing indicator loops forever, which would otherwise keep the test from idling.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            BluebirdTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(BluebirdTheme.colors.background)) {
                    Column(Modifier.padding(top = 12.dp)) { content() }
                }
            }
        }
        compose.mainClock.advanceTimeBy(600)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/${name}_${if (dark) "dark" else "light"}.png")
    }

    @Test fun glassLight() = snap("glass", dark = false) { CatalogSection("Glass") { GlassSample() } }
    @Test fun glassDark() = snap("glass", dark = true) { CatalogSection("Glass") { GlassSample() } }

    @Test fun foundationsLight() = snap("foundations", dark = false) { Foundations() }
    @Test fun foundationsDark() = snap("foundations", dark = true) { Foundations() }

    @Test fun messagesLight() = snap("messages", dark = false) { Messages() }
    @Test fun messagesDark() = snap("messages", dark = true) { Messages() }

    @Test fun statesLight() = snap("states", dark = false) { CatalogSection("States") { StateSample() } }
    @Test fun statesDark() = snap("states", dark = true) { CatalogSection("States") { StateSample() } }

    @Test fun inboxLight() = snap("inbox", dark = false) { Inbox() }
    @Test fun inboxDark() = snap("inbox", dark = true) { Inbox() }
    @Test fun inboxSearch() = snap("inbox_search", dark = false) { Inbox(query = "the") }
    @Test fun inboxEmpty() = snap("inbox_empty", dark = false) { Inbox(seed = false) }
    @Test fun inboxUndo() = snap("inbox_undo", dark = true) { Inbox(deleteFirst = "t2") }

    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    @Test fun tabletInbox() = snap("tablet_inbox", dark = false) { Inbox() }

    @Test fun settingsLight() = snap("settings", dark = false) { SettingsScreen("Mock (simulated)", onOpenDesignCatalog = {}, onBack = {}) }
    @Test fun settingsDark() = snap("settings", dark = true) { SettingsScreen("Mock (simulated)", onOpenDesignCatalog = {}, onBack = {}) }

    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    @Test fun tabletRows() = snap("tablet_rows", dark = false) { CatalogSection("Rows at tablet width") { RowSample() } }

    @Composable
    private fun Foundations() {
        CatalogSection("Color") { ColorSample() }
        CatalogSection("Type") { TypeSample() }
        CatalogSection("Buttons") { ButtonSample() }
    }

    @Composable
    private fun Messages() {
        CatalogSection("Conversation rows") { RowSample() }
        CatalogSection("Messages") { BubbleSample() }
        CatalogSection("Inputs") { InputSample() }
    }

    @Composable
    private fun Inbox(query: String = "", seed: Boolean = true, deleteFirst: String? = null) {
        val vm = remember {
            InboxViewModel(MockMessagingProvider(seedSampleData = seed)).also { vm -> deleteFirst?.let(vm::delete) }
        }
        InboxScreen(
            viewModel = vm,
            onOpenConversation = {},
            onOpenContacts = {},
            onOpenSettings = {},
            searchState = remember { TextFieldState(query) },
        )
    }
}
