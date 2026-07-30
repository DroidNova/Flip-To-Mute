package com.droidnova.fliptomute.quicksettings

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import com.droidnova.fliptomute.R

sealed interface QuickSettingsTileAddResult {
    data object Added : QuickSettingsTileAddResult
    data object AlreadyAdded : QuickSettingsTileAddResult
    data object NotAdded : QuickSettingsTileAddResult
    data object RequestInProgress : QuickSettingsTileAddResult
    data object ManualInstructionsRequired : QuickSettingsTileAddResult
    data object Failed : QuickSettingsTileAddResult
}

object QuickSettingsTileAddResultMapper {
    fun map(result: Int): QuickSettingsTileAddResult = when (result) {
        2 -> QuickSettingsTileAddResult.Added
        1 -> QuickSettingsTileAddResult.AlreadyAdded
        0 -> QuickSettingsTileAddResult.NotAdded
        1001 -> QuickSettingsTileAddResult.RequestInProgress
        else -> QuickSettingsTileAddResult.Failed
    }
}

class QuickSettingsTileAddRequester(private val activity: Activity) {
    fun request(onResult: (QuickSettingsTileAddResult) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(QuickSettingsTileAddResult.ManualInstructionsRequired)
            return
        }
        try {
            val manager = activity.getSystemService(StatusBarManager::class.java)
            if (manager == null) {
                onResult(QuickSettingsTileAddResult.Failed)
                return
            }
            manager.requestAddTileService(
                ComponentName(activity, FlipToMuteTileService::class.java),
                activity.getString(R.string.quick_settings_tile_label),
                Icon.createWithResource(activity, R.drawable.ic_qs_flip_to_mute),
                activity.mainExecutor,
            ) { result -> onResult(QuickSettingsTileAddResultMapper.map(result)) }
        } catch (_: SecurityException) {
            onResult(QuickSettingsTileAddResult.Failed)
        } catch (_: IllegalArgumentException) {
            onResult(QuickSettingsTileAddResult.Failed)
        }
    }
}
