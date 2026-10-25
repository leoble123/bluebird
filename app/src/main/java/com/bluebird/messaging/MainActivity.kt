package com.bluebird.messaging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluebird.messaging.core.data.ThemeMode
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.navigation.BluebirdNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as BluebirdApplication).container
        setContent {
            // The saved choice loads from the database in a moment; until then, follow the system.
            val themeFlow = remember { container.preferences.observeThemeMode() }
            val themeMode by themeFlow.collectAsStateWithLifecycle(ThemeMode.System)
            val dark = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            BluebirdTheme(darkTheme = dark) {
                BluebirdNavHost(container)
            }
        }
    }
}
