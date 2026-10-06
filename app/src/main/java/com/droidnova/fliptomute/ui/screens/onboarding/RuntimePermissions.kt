package com.droidnova.fliptomute.ui.screens.onboarding

/** Runtime permissions the Access step requests. Moved unchanged from the 1.x setup screen (M4-09). */
enum class RuntimeSetupPermission(internal val savedStateKey: String) {
    PHONE("phone_permission_requested"),
    NOTIFICATIONS("notification_permission_requested"),
}

/** Whether to refresh, ask (with an explanation first), or send the user to system settings. */
enum class RuntimePermissionAction { REFRESH, SHOW_EXPLANATION, OPEN_SETTINGS }

internal fun resolveRuntimePermissionAction(
    granted: Boolean,
    shouldShowRationale: Boolean,
    requestedBefore: Boolean,
): RuntimePermissionAction = when {
    granted -> RuntimePermissionAction.REFRESH
    requestedBefore && !shouldShowRationale -> RuntimePermissionAction.OPEN_SETTINGS
    else -> RuntimePermissionAction.SHOW_EXPLANATION
}
