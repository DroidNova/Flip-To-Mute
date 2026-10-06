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
import com.droidnova.fliptomute.data.analytics.AnalyticsEvents
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchEvent
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequest
import com.droidnova.fliptomute.ui.screens.about.AboutRoute
import com.droidnova.fliptomute.ui.screens.activity.ActivityRoute
import com.droidnova.fliptomute.ui.screens.check_setup.CheckSetupRoute
import com.droidnova.fliptomute.ui.screens.home.HomeRoute
import com.droidnova.fliptomute.ui.screens.keep_running.KeepRunningRoute
import com.droidnova.fliptomute.ui.screens.settings.SettingsRoute

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
    /** Opens the Access step (OnboardingActivity in access-only mode), replacing the 1.x setup screen. */
    onOpenAccess: () -> Unit,
    /** The support email; the text is the "Check my setup" summary when there is one (M6-04). */
    onReportProblem: (summary: String?) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onRateUsTapped: () -> Unit,
    /** Home is calm: the activity may show the review prompt (M6-07). */
    onHomeCalm: () -> Unit,
    onStartUpdate: () -> Unit,
    /** The activity screen was opened; the label says from where (analytics). */
    onActivityOpened: (source: String) -> Unit,
    /** Consent requires a way to change the ad choice (M7-02). */
    privacyOptionsRequired: Boolean,
    onOpenPrivacyOptions: () -> Unit,
    /** The rewarded ad that opens an earned theme for a week (future features F34). */
    rewardedThemeAvailable: Boolean,
    onWatchAdForTheme: (onRewarded: () -> Unit, onUnavailable: () -> Unit) -> Unit,
    /** The native ad card for the activity screen, or null when there is none to show (future features F32). */
    activityNativeAd: (@Composable () -> Unit)?,
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
        // A flip or recap notification: the activity screen, with Home underneath for Back
        val request = externalMonitoringRequest.request
        if (request is MainActivityLaunchRequest.OpenActivity) {
            onActivityOpened(request.source)
            navController.go(Routes.ACTIVITY)
            onExternalMonitoringRequestConsumed()
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
                onOpenAccess = onOpenAccess,
                onCheckSetup = { navController.go(Routes.CHECK_SETUP) },
                onOpenActivity = {
                    onActivityOpened(AnalyticsEvents.SOURCE_HOME)
                    navController.go(Routes.ACTIVITY)
                },
                onCalm = onHomeCalm,
                onStartUpdate = onStartUpdate,
                externalMonitoringRequest = externalMonitoringRequest,
                onExternalMonitoringRequestConsumed = onExternalMonitoringRequestConsumed,
            )
        }
        composable(Routes.ABOUT) { entry ->
            AboutRoute(
                onBack = { navController.back(entry) },
                onReportProblem = { onReportProblem(null) },
                onRateUsTapped = onRateUsTapped,
            )
        }
        composable(Routes.SETTINGS) { entry ->
            SettingsRoute(
                onBack = { navController.back(entry) },
                onOpenKeepRunning = { navController.go(Routes.KEEP_RUNNING) },
                onOpenCheckSetup = { navController.go(Routes.CHECK_SETUP) },
                onReportProblem = { onReportProblem(null) },
                onAbout = { navController.go(Routes.ABOUT) },
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                privacyOptionsRequired = privacyOptionsRequired,
                onOpenPrivacyOptions = onOpenPrivacyOptions,
                rewardedThemeAvailable = rewardedThemeAvailable,
                onWatchAdForTheme = onWatchAdForTheme,
            )
        }
        composable(Routes.ACTIVITY) { entry ->
            ActivityRoute(onBack = { navController.back(entry) }, nativeAd = activityNativeAd)
        }
        composable(Routes.KEEP_RUNNING) { entry ->
            KeepRunningRoute(onBack = { navController.back(entry) })
        }
        composable(Routes.CHECK_SETUP) { entry ->
            CheckSetupRoute(
                onBack = { navController.back(entry) },
                onOpenAccess = onOpenAccess,
                onOpenKeepRunning = { navController.go(Routes.KEEP_RUNNING) },
                onSendReport = onReportProblem,
            )
        }
    }
}

private const val NAV_MS = 280
