package com.droidnova.fliptomute.core.utils.about_utils

import androidx.annotation.DrawableRes
import com.droidnova.fliptomute.R

data class OtherAppItem(
    val title: String,
    val description: String,
    val packageName: String,
    @DrawableRes val iconRes: Int
)

private val otherApps = listOf(
    OtherAppItem(
        title = "Clipboard History",
        description = "Save, search and reuse copied text quickly.",
        packageName = "com.droidnova.clipboardhistory",
        iconRes = R.drawable.ic_clipboard_history
    ),
    OtherAppItem(
        title = "Notification History",
        description = "Never miss dismissed notifications again.",
        packageName = "com.droidnova.notificationhistory",
        iconRes = R.drawable.ic_notification_history
    ),
    OtherAppItem(
        title = "Flash Alerts",
        description = "Blink flashlight for calls and notifications.",
        packageName = "com.droidnova.flashalert",
        iconRes = R.drawable.ic_flash_alert
    ),
    OtherAppItem(
        title = "Secret Calculator",
        description = "Securely hide files behind a calculator lock.",
        packageName = "com.droidnova.secretcalculator",
        iconRes = R.drawable.ic_calculator
    ),
    OtherAppItem(
        title = "Background Video Recorder",
        description = "Record videos while app stays in background.",
        packageName = "com.droidnova.backgroundcamera",
        iconRes = R.drawable.ic_bvr
    )
)

fun getFeaturedOtherApps(): List<OtherAppItem> {
    val featured = otherApps.firstOrNull { it.packageName == BACKGROUND_VIDEO_RECORDER_PACKAGE }
    val randomSecond = otherApps
        .filter { it.packageName != BACKGROUND_VIDEO_RECORDER_PACKAGE }
        .shuffled()
        .firstOrNull()

    return listOfNotNull(featured, randomSecond)
}

private const val BACKGROUND_VIDEO_RECORDER_PACKAGE = "com.droidnova.backgroundcamera"
