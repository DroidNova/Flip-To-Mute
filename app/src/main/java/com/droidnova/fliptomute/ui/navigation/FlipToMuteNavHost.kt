package com.droidnova.fliptomute.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidnova.fliptomute.app.ViewModelFactories
import com.droidnova.fliptomute.ui.screens.home.HomeRoute
import com.droidnova.fliptomute.ui.screens.permissions.PermissionsRoute
import com.droidnova.fliptomute.ui.screens.sensor_test.SensorTestScreen
import com.droidnova.fliptomute.ui.screens.settings.SettingsRoute
import com.droidnova.fliptomute.ui.screens.call_state_test.CallStateTestScreen
import com.droidnova.fliptomute.ui.screens.sound_control_test.SoundControlTestScreen
import com.droidnova.fliptomute.ui.screens.about.AboutScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.LaunchedEffect
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchEvent

@Composable
fun FlipToMuteNavHost(
    viewModelFactories: ViewModelFactories,
    modifier: Modifier = Modifier,
    externalMonitoringRequest: StateFlow<MainActivityLaunchEvent>,
    onExternalMonitoringRequestConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val monitoringRequest by externalMonitoringRequest.collectAsStateWithLifecycle()
    LaunchedEffect(monitoringRequest.sequence) {
        if (monitoringRequest.sequence > 0L) {
            navController.navigate(Destination.Home.route) {
                popUpTo(Destination.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Destination.Home.route) {
            HomeRoute(
                onSettingsClick = { navController.navigateTo(Destination.Settings) },
                onAboutClick = { navController.navigateTo(Destination.About) },
                onPermissionsClick = { navController.navigateTo(Destination.Permissions) },
                viewModelFactory = viewModelFactories.home,
                externalMonitoringRequest = monitoringRequest,
                onExternalMonitoringRequestConsumed = onExternalMonitoringRequestConsumed,
            )
        }
        composable(Destination.About.route) {
            AboutScreen(onBack = { navController.navigateUp() })
        }
        composable(Destination.Permissions.route) {
            PermissionsRoute(
                onBack = { navController.navigateUp() },
                viewModelFactory = viewModelFactories.permissions,
            )
        }
        composable(Destination.Settings.route) {
            SettingsRoute(
                onBack = { navController.navigateUp() },
                onOpenSetup = { navController.navigateTo(Destination.Permissions) },
                onCallStateTest = { navController.navigateTo(Destination.CallStateTest) },
                onSensorTest = { navController.navigateTo(Destination.SensorTest) },
                onSoundControlTest = { navController.navigateTo(Destination.SoundControlTest) },
                viewModelFactory = viewModelFactories.settings,
            )
        }
        composable(Destination.SensorTest.route) {
            SensorTestScreen(
                onBack = { navController.navigateUp() },
                viewModelFactory = viewModelFactories.sensorTest,
            )
        }
        composable(Destination.CallStateTest.route) {
            CallStateTestScreen(
                onBack = { navController.navigateUp() },
                onOpenSetup = { navController.navigateTo(Destination.Permissions) },
                viewModelFactory = viewModelFactories.callStateTest,
            )
        }
        composable(Destination.SoundControlTest.route) {
            SoundControlTestScreen(
                onBack = { navController.navigateUp() },
                onOpenSetup = { navController.navigateTo(Destination.Permissions) },
                viewModelFactory = viewModelFactories.soundControlTest,
            )
        }
    }
}

private fun NavHostController.navigateTo(destination: Destination) {
    navigate(destination.route) { launchSingleTop = true }
}
