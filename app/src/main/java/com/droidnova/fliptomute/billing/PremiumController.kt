package com.droidnova.fliptomute.billing

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** What screens show about the one-time "remove ads" purchase. */
data class PremiumUi(
    val isPremium: Boolean = false,
    /** Store price, or null while product details haven't loaded. */
    val priceLabel: String? = null,
    val isLoading: Boolean = false,
)

sealed interface PremiumEvent {
    /** Bought just now from the premium sheet. */
    data object Purchased : PremiumEvent
    /** An earlier purchase was found again (reinstall, new device). */
    data object Reactivated : PremiumEvent
}

/** Implemented by MainActivity, which owns billing; screens reach it through [LocalPremiumController]. */
interface PremiumController {
    val premiumUi: StateFlow<PremiumUi>
    val premiumEvents: SharedFlow<PremiumEvent>
    fun launchPurchase()
    fun onPremiumSheetDismissed()
}

val LocalPremiumController = staticCompositionLocalOf<PremiumController> {
    error("PremiumController not provided")
}
