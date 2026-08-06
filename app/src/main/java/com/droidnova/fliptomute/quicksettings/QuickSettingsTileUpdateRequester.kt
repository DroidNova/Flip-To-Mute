package com.droidnova.fliptomute.quicksettings

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService

fun interface QuickSettingsTileUpdateRequester { fun requestUpdate() }

class AndroidQuickSettingsTileUpdateRequester(context: Context) : QuickSettingsTileUpdateRequester {
    private val context = context.applicationContext
    override fun requestUpdate() {
        try {
            TileService.requestListeningState(context, ComponentName(context, FlipToMuteTileService::class.java))
        } catch (_: SecurityException) {
            // SystemUI may reject requests while the tile is absent or unavailable.
        } catch (_: IllegalStateException) {
            // The request is best effort and must never affect monitoring.
        } catch (_: IllegalArgumentException) {
            // Some SystemUI implementations reject requests for unavailable components.
        }
    }
}
