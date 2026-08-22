package com.droidnova.fliptomute.ads

import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private const val COLLAPSIBLE_KEY = "collapsible"
private const val BOTTOM_PLACEMENT = "bottom"

/** Displays an anchored adaptive banner that may expand upward from the bottom edge. */
@Composable
fun CollapsibleBannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(320)
    val adSize = remember(context, widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }
    val height = with(LocalDensity.current) { adSize.getHeightInPixels(context).toDp() }
    val adView = remember(context, adSize) {
        AdView(context).apply {
            adUnitId = AdUnits.HOME_COLLAPSIBLE_BANNER
            setAdSize(adSize)
            val extras = Bundle().apply { putString(COLLAPSIBLE_KEY, BOTTOM_PLACEMENT) }
            loadAd(
                AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                    .build()
            )
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(height)) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth(),
            update = {
                it.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            },
        )
    }

    DisposableEffect(adView) {
        onDispose(adView::destroy)
    }
}
