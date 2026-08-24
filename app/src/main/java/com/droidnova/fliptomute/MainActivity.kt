package com.droidnova.fliptomute

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.droidnova.fliptomute.ads.CollapsibleBannerAd
import com.droidnova.fliptomute.app.FlipToMuteApplication
import com.droidnova.fliptomute.app.ViewModelFactories
import com.droidnova.fliptomute.ui.navigation.FlipToMuteNavHost
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchEvent
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequest
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequestParser
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val externalMonitoringRequest = MutableStateFlow(MainActivityLaunchEvent())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) consumeLaunchRequest(intent)
        val factories = ViewModelFactories((application as FlipToMuteApplication).container)
        setContent {
            FlipToMuteTheme {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        Box(modifier = Modifier.navigationBarsPadding()) {
                            CollapsibleBannerAd()
                        }
                    },
                ) { padding ->
                    FlipToMuteNavHost(
                        viewModelFactories = factories,
                        modifier = Modifier
                            .padding(padding)
                            .consumeWindowInsets(padding),
                        externalMonitoringRequest = externalMonitoringRequest,
                        onExternalMonitoringRequestConsumed = ::clearLaunchRequest,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchRequest(intent)
    }

    private fun consumeLaunchRequest(intent: Intent?) {
        val request = MainActivityLaunchRequestParser.parse(intent)
        if (request != MainActivityLaunchRequest.None) {
            externalMonitoringRequest.value = MainActivityLaunchEvent(
                sequence = externalMonitoringRequest.value.sequence + 1L,
                request = request,
            )
            intent?.action = null
        }
    }

    private fun clearLaunchRequest() {
        externalMonitoringRequest.value = externalMonitoringRequest.value.copy(
            request = MainActivityLaunchRequest.None,
        )
    }
}
