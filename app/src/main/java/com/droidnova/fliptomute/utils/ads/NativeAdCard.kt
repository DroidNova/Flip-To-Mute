package com.droidnova.fliptomute.utils.ads

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.NovaCardShape
import com.droidnova.fliptomute.ui.components.novaCardColor
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * One native ad drawn as a card of the app (future features F32). The ad SDK needs real Android
 * views to count taps, so the card is built from views and coloured from the Compose theme. It
 * always shows the "Ad" label; the SDK adds the AdChoices mark itself.
 */
@Composable
fun NativeAdCard(ad: NativeAd, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = NativeAdColors(
        title = scheme.onSurface.toArgb(),
        body = scheme.onSurfaceVariant.toArgb(),
        accent = scheme.primary.toArgb(),
        onAccent = scheme.onPrimary.toArgb(),
    )
    AndroidView(
        modifier = modifier.fillMaxWidth().clip(NovaCardShape).background(novaCardColor()),
        factory = { context -> buildNativeAdView(context) },
        update = { view -> bindNativeAd(view, ad, colors) },
    )
}

private data class NativeAdColors(val title: Int, val body: Int, val accent: Int, val onAccent: Int)

/** The parts of the card, kept on the view so [bindNativeAd] finds them again. */
private class NativeAdParts(
    val icon: ImageView,
    val headline: TextView,
    val badge: TextView,
    val body: TextView,
    val media: MediaView,
    val action: Button,
)

private fun buildNativeAdView(context: Context): NativeAdView {
    fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

    val icon = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) }
    }
    val headline = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    val badge = TextView(context).apply {
        setText(R.string.ad_badge)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(6), dp(1), dp(6), dp(1))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { marginEnd = dp(8) }
    }
    val body = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    val badgeRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(2), 0, 0)
        addView(badge)
        addView(body, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }
    val texts = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(headline)
        addView(badgeRow)
    }
    val header = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(icon)
        addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }
    val media = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(160)).apply { topMargin = dp(12) }
    }
    val action = Button(context).apply {
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        stateListAnimator = null
        // 48 dp to touch (design spec 7)
        minHeight = dp(48)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(12) }
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        addView(header)
        addView(media)
        addView(action)
    }
    return NativeAdView(context).apply {
        addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        headlineView = headline
        bodyView = body
        iconView = icon
        mediaView = media
        callToActionView = action
        tag = NativeAdParts(icon, headline, badge, body, media, action)
    }
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd, colors: NativeAdColors) {
    val parts = view.tag as? NativeAdParts ?: return
    val radius = 50f * view.resources.displayMetrics.density

    parts.headline.text = ad.headline
    parts.headline.setTextColor(colors.title)

    parts.badge.setTextColor(colors.onAccent)
    parts.badge.background = GradientDrawable().apply {
        cornerRadius = radius
        setColor(colors.accent)
    }

    // The advertiser's line when there is one, else its name
    val line = ad.body?.takeIf { it.isNotBlank() } ?: ad.advertiser.orEmpty()
    parts.body.text = line
    parts.body.setTextColor(colors.body)

    val iconDrawable = ad.icon?.drawable
    parts.icon.setImageDrawable(iconDrawable)
    parts.icon.visibility = if (iconDrawable != null) View.VISIBLE else View.GONE

    ad.mediaContent?.let { parts.media.mediaContent = it }

    val action = ad.callToAction
    parts.action.text = action
    parts.action.visibility = if (action.isNullOrBlank()) View.GONE else View.VISIBLE
    parts.action.setTextColor(colors.onAccent)
    parts.action.background = GradientDrawable().apply {
        cornerRadius = radius
        setColor(colors.accent)
    }

    view.setNativeAd(ad)
}
