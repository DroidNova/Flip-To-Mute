package com.droidnova.fliptomute

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.droidnova.fliptomute.utils.ads.CollapsibleBannerAd
import com.droidnova.fliptomute.data.analytics.Funnel
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchEvent
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequest
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequestParser
import com.droidnova.fliptomute.ui.navigation.AppNavHost
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The single activity, built like Secret Calculator's AppActivity (architecture A3): one root column
 * that pads for the system bars once, the nav host, and the banner underneath.
 *
 * It stays at `com.droidnova.fliptomute.MainActivity` on purpose: moving it would rename the
 * launcher component and can remove users' home-screen icons (architecture X12).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var funnel: Funnel

    private val externalMonitoringRequest = MutableStateFlow(MainActivityLaunchEvent())

    /** The screen showing now; ad placement uses it from M7. */
    private var currentRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            consumeLaunchRequest(intent)
            funnel.appOpened()
        }
        setContent {
            FlipToMuteTheme {
                // The root is a plain Column, as in Secret Calculator: give text a readable default colour
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            // Edge to edge: system bars, cutouts and the keyboard are padded once, here
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        val navController = rememberNavController()
                        DisposableEffect(navController) {
                            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                                currentRoute = destination.route
                            }
                            navController.addOnDestinationChangedListener(listener)
                            onDispose { navController.removeOnDestinationChangedListener(listener) }
                        }
                        val monitoringRequest by externalMonitoringRequest.collectAsStateWithLifecycle()
                        AppNavHost(
                            navController = navController,
                            externalMonitoringRequest = monitoringRequest,
                            onExternalMonitoringRequestConsumed = ::clearLaunchRequest,
                            modifier = Modifier.weight(1f),
                        )
                        CollapsibleBannerAd()
                    }
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
