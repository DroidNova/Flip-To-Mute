package com.droidnova.fliptomute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.droidnova.fliptomute.app.FlipToMuteApplication
import com.droidnova.fliptomute.app.ViewModelFactories
import com.droidnova.fliptomute.ui.navigation.FlipToMuteNavHost
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import android.content.Intent
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequest
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequestParser
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val externalEnableRequest = MutableStateFlow(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) consumeLaunchRequest(intent)
        val factories = ViewModelFactories((application as FlipToMuteApplication).container)
        setContent {
            FlipToMuteTheme {
                FlipToMuteNavHost(
                    viewModelFactories = factories,
                    externalEnableRequest = externalEnableRequest,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchRequest(intent)
    }

    private fun consumeLaunchRequest(intent: Intent?) {
        if (MainActivityLaunchRequestParser.parse(intent) ==
            MainActivityLaunchRequest.OpenSetupAndEnableMonitoring
        ) {
            externalEnableRequest.value += 1L
            intent?.action = null
        }
    }
}
