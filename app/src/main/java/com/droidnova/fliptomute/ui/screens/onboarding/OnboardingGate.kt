package com.droidnova.fliptomute.ui.screens.onboarding

/** Where a launch of the app begins (M4-02). */
enum class StartDestination { ONBOARDING, HOME }

/**
 * Decides whether a launch shows the first run. Only someone who never finished it and has
 * neither granted access nor turned Flip to Mute on sees it, so people updating from 1.x never do.
 */
object OnboardingGate {
    fun decide(onboardingCompleted: Boolean, monitoringEnabled: Boolean, setupComplete: Boolean): StartDestination =
        if (!onboardingCompleted && !monitoringEnabled && !setupComplete) StartDestination.ONBOARDING else StartDestination.HOME

    /** An earlier user who set the app up but never pressed 1.x's "Finish setup": record it as done. */
    fun shouldMarkCompleted(onboardingCompleted: Boolean, monitoringEnabled: Boolean, setupComplete: Boolean): Boolean =
        !onboardingCompleted && (monitoringEnabled || setupComplete)
}
