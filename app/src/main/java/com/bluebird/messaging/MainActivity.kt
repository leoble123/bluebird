package com.bluebird.messaging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.navigation.BluebirdNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as BluebirdApplication).container
        setContent {
            BluebirdTheme {
                BluebirdNavHost(container)
            }
        }
    }
}
