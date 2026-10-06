package com.droidnova.fliptomute.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.IconBadge
import com.droidnova.fliptomute.ui.components.SheetHeader
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.ui.theme.labelRes
import com.droidnova.fliptomute.utils.AppTheme

/** What the Premium sheet needs to draw itself. */
data class PremiumSheetState(
    /** Store price, or null while it has not loaded. */
    val price: String? = null,
    val isLoading: Boolean = false,
    /** The locked theme whose tap opened the sheet, or null when it was opened from its Settings row. */
    val theme: AppTheme? = null,
    val totalFlips: Int = 0,
    /** A rewarded ad can open [theme] for a week. */
    val rewardedAvailable: Boolean = false,
)

/**
 * Flip to Mute Premium (future features F19, F20): what it adds, what stays free, and one button.
 * It opens only when the user asks for it, from its row in Settings or by tapping a locked theme,
 * and it always shows the free ways to the same theme next to the paid one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSheet(state: PremiumSheetState, onBuy: () -> Unit, onWatchAd: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        PremiumSheetContent(state, onBuy, onWatchAd, onDismiss)
    }
}

@Composable
fun PremiumSheetContent(state: PremiumSheetState, onBuy: () -> Unit, onWatchAd: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SheetHeader(
            Icons.Filled.WorkspacePremium,
            stringResource(R.string.premium_title),
            subtitle = stringResource(R.string.premium_subtitle),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Benefit(Icons.Filled.Block, stringResource(R.string.premium_benefit_ads_title), stringResource(R.string.premium_benefit_ads_body))
        Benefit(Icons.Filled.Palette, stringResource(R.string.premium_benefit_themes_title), stringResource(R.string.premium_benefit_themes_body))
        Benefit(Icons.Filled.Favorite, stringResource(R.string.premium_benefit_support_title), stringResource(R.string.premium_benefit_support_body))
        Text(
            stringResource(R.string.premium_free_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onBuy, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(state.price?.let { stringResource(R.string.premium_buy_price, it) } ?: stringResource(R.string.premium_buy))
        }
        // The free ways to the theme that was tapped, always next to the paid one
        state.theme?.let { theme ->
            val name = stringResource(theme.labelRes())
            if (state.rewardedAvailable) {
                OutlinedButton(onClick = onWatchAd, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.premium_watch_ad_for_theme, name), textAlign = TextAlign.Center)
                }
            }
            Text(
                stringResource(R.string.premium_earn_theme, name, theme.unlockAt, state.totalFlips),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.discovery_not_now))
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PremiumSheetPreview() {
    FlipToMuteTheme {
        PremiumSheetContent(
            PremiumSheetState(price = "₹99", theme = AppTheme.ROSE, totalFlips = 12, rewardedAvailable = true),
            onBuy = {}, onWatchAd = {}, onDismiss = {},
        )
    }
}
