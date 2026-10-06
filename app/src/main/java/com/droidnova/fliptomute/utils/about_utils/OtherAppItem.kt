package com.droidnova.fliptomute.utils.about_utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.droidnova.fliptomute.R

/** Another DroidNova app for About, with translatable text, as in Secret Calculator (M6-10). */
data class OtherAppItem(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val packageName: String,
    @param:DrawableRes val iconRes: Int,
)

private val otherApps = listOf(
    OtherAppItem(R.string.other_app_clipboard_title, R.string.other_app_clipboard_description, "com.droidnova.clipboardhistory", R.drawable.ic_clipboard_history),
    OtherAppItem(R.string.other_app_notification_title, R.string.other_app_notification_description, "com.droidnova.notificationhistory", R.drawable.ic_notification_history),
    OtherAppItem(R.string.other_app_flash_alert_title, R.string.other_app_flash_alert_description, "com.droidnova.flashalert", R.drawable.ic_flash_alert),
    OtherAppItem(R.string.other_app_secret_calculator_title, R.string.other_app_secret_calculator_description, "com.droidnova.secretcalculator", R.drawable.ic_calculator),
    OtherAppItem(R.string.other_app_bvr_title, R.string.other_app_bvr_description, BACKGROUND_VIDEO_RECORDER_PACKAGE, R.drawable.ic_bvr),
)

/** Background Video Recorder first, then one other app at random, as in 1.x. */
fun getFeaturedOtherApps(): List<OtherAppItem> {
    val featured = otherApps.firstOrNull { it.packageName == BACKGROUND_VIDEO_RECORDER_PACKAGE }
    val randomSecond = otherApps.filter { it.packageName != BACKGROUND_VIDEO_RECORDER_PACKAGE }.shuffled().firstOrNull()
    return listOfNotNull(featured, randomSecond)
}

private const val BACKGROUND_VIDEO_RECORDER_PACKAGE = "com.droidnova.backgroundcamera"
