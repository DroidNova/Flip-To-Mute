package com.droidnova.fliptomute.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings

enum class SettingsLaunchResult { OPENED, UNAVAILABLE }

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun Context.openAppDetailsSettings(): SettingsLaunchResult =
    if (launchSettingsIntent(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName"),
        ),
    )) SettingsLaunchResult.OPENED else SettingsLaunchResult.UNAVAILABLE

fun Context.openAppNotificationSettings(): SettingsLaunchResult {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    return settingsLaunchResult(launchSettingsIntent(intent)) { openAppDetailsSettings() }
}

fun Context.openNotificationPolicySettings(): SettingsLaunchResult = settingsLaunchResult(
    launchSettingsIntent(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)),
) { openAppDetailsSettings() }

internal inline fun settingsLaunchResult(
    primaryOpened: Boolean,
    fallback: () -> SettingsLaunchResult,
): SettingsLaunchResult = if (primaryOpened) SettingsLaunchResult.OPENED else fallback()

private fun Context.launchSettingsIntent(intent: Intent): Boolean {
    return try {
        if (intent.resolveActivity(packageManager) == null) return false
        if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
