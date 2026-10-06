package com.droidnova.fliptomute.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchEvent
import com.droidnova.fliptomute.ui.screens.about.AboutScreen
import com.droidnova.fliptomute.ui.screens.call_state_test.CallStateTestScreen
import com.droidnova.fliptomute.ui.screens.home.HomeRoute
import com.droidnova.fliptomute.ui.screens.permissions.PermissionsRoute
import com.droidnova.fliptomute.ui.screens.sensor_test.SensorTestScreen
import com.droidnova.fliptomute.ui.screens.settings.SettingsRoute
import com.droidnova.fliptomute.ui.screens.sound_control_test.SoundControlTestScreen

/** Navigates, ignoring repeated taps that would stack the same screen twice (from Secret Calculator). */
private fun NavHostController.go(route: String) {
    navigate(route) { launchSingleTop = true }
}

/** Pops this screen only if it's still the one showing, so a double tap on Back can't pop two. */
private fun NavHostController.back(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) popBackStack()
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    externalMonitoringRequest: MainActivityLaunchEvent,
    onExternalMonitoringRequestConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A tile or notification asked to turn Flip to Mute on: show Home, where the request is handled
    LaunchedEffect(externalMonitoringRequest.sequence) {
        if (externalMonitoringRequest.sequence > 0L) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.HOME) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier.fillMaxSize(),
        // Same motion as Secret Calculator: forward slides in a little from the right, back the other way
        enterTransition = { slideInHorizontally(tween(NAV_MS)) { it / 6 } + fadeIn(tween(NAV_MS)) },
        exitTransition = { slideOutHorizontally(tween(NAV_MS)) { -it / 10 } + fadeOut(tween(NAV_MS / 2)) },
        popEnterTransition = { slideInHorizontally(tween(NAV_MS)) { -it / 10 } + fadeIn(tween(NAV_MS)) },
        popExitTransition = { slideOutHorizontally(tween(NAV_MS)) { it / 6 } + fadeOut(tween(NAV_MS / 2)) },
    ) {
        composable(Routes.HOME) {
            HomeRoute(
                onSettingsClick = { navController.go(Routes.SETTINGS) },
                onAboutClick = { navController.go(Routes.ABOUT) },
                onPermissionsClick = { navController.go(Routes.PERMISSIONS) },
                externalMonitoringRequest = externalMonitoringRequest,
                onExternalMonitoringRequestConsumed = onExternalMonitoringRequestConsumed,
            )
        }
        composable(Routes.ABOUT) { entry ->
            AboutScreen(onBack = { navController.back(entry) })
        }
        composable(Routes.PERMISSIONS) { entry ->
            PermissionsRoute(onBack = { navController.back(entry) })
        }
        composable(Routes.SETTINGS) { entry ->
            SettingsRoute(
                onBack = { navController.back(entry) },
                onOpenSetup = { navController.go(Routes.PERMISSIONS) },
                onCallStateTest = { navController.go(Routes.CALL_STATE_TEST) },
                onSensorTest = { navController.go(Routes.SENSOR_TEST) },
                onSoundControlTest = { navController.go(Routes.SOUND_CONTROL_TEST) },
            )
        }
        composable(Routes.SENSOR_TEST) { entry ->
            SensorTestScreen(onBack = { navController.back(entry) })
        }
        composable(Routes.CALL_STATE_TEST) { entry ->
            CallStateTestScreen(
                onBack = { navController.back(entry) },
                onOpenSetup = { navController.go(Routes.PERMISSIONS) },
            )
        }
        composable(Routes.SOUND_CONTROL_TEST) { entry ->
            SoundControlTestScreen(
                onBack = { navController.back(entry) },
                onOpenSetup = { navController.go(Routes.PERMISSIONS) },
            )
        }
    }
}

private const val NAV_MS = 280
