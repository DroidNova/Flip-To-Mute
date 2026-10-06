package com.droidnova.fliptomute.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.droidnova.fliptomute.utils.about_utils.AppConstants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Copied from Secret Calculator (architecture A25). One change: the Play licence key is a
 * constructor parameter read from AppConstants, because this project has BuildConfig switched off.
 */
class PremiumBillingManager(
    context: Context,
    private val productId: String = DEFAULT_PRODUCT_ID,
    private val licenseKey: String = AppConstants.PLAY_STORE_LICENSE_KEY,
    private val onPremiumStatusChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit = {},
) : PurchasesUpdatedListener {

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _isFetchingProductDetails = MutableStateFlow(false)
    val isFetchingProductDetails: StateFlow<Boolean> = _isFetchingProductDetails.asStateFlow()

    private val _isPurchaseInProgress = MutableStateFlow(false)
    val isPurchaseInProgress: StateFlow<Boolean> = _isPurchaseInProgress.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val readyCallbacks = mutableListOf<() -> Unit>()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun startConnection(onReady: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            _connectionState.value = ConnectionState.CONNECTED
            onReady?.invoke()
            return
        }

        onReady?.let { readyCallbacks.add(it) }

        if (_connectionState.value == ConnectionState.CONNECTING) {
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = ConnectionState.CONNECTED
                    val callbacks = readyCallbacks.toList()
                    readyCallbacks.clear()
                    callbacks.forEach { it() }
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    readyCallbacks.clear()
                    emitError("Billing unavailable right now. Please try again later.")
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = ConnectionState.DISCONNECTED
                emitError("Billing service disconnected. We'll retry when you try again.")
            }
        })
    }

    fun queryProductDetails() {
        if (_isFetchingProductDetails.value) return

        startConnection {
            _isFetchingProductDetails.value = true
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, result ->
                _isFetchingProductDetails.value = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _productDetails.value = result.productDetailsList.firstOrNull()
                    if (_productDetails.value == null) {
                        emitError("Product not available right now. Please try again later.")
                    }
                } else {
                    emitError("Failed to load product details. Please try again.")
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        if (_isPurchaseInProgress.value) return

        startConnection {
            val details = _productDetails.value
            if (details == null) {
                emitError("Product not ready yet. Loading pricing…")
                queryProductDetails()
                return@startConnection
            }

            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build()
                    )
                )
                .build()

            _isPurchaseInProgress.value = true
            try {
                val billingResult = billingClient.launchBillingFlow(activity, params)
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    _isPurchaseInProgress.value = false
                    emitError("Unable to start purchase. Please try again.")
                }
            } catch (_: NullPointerException) {
                _isPurchaseInProgress.value = false
                emitError("Unable to open billing screen on this device. Please try again later.")
            } catch (_: IllegalStateException) {
                _isPurchaseInProgress.value = false
                emitError("Billing is not ready yet. Please try again.")
            } catch (_: Throwable) {
                _isPurchaseInProgress.value = false
                emitError("Unable to start purchase. Please try again.")
            }
        }
    }

    fun queryActivePurchases() {
        startConnection {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val purchase = purchasesList.firstOrNull { it.products.contains(productId) }
                    if (purchase != null) {
                        handlePurchase(purchase)
                    } else {
                        onPremiumStatusChanged(false)
                    }
                } else {
                    emitError("Unable to verify purchases right now.")
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        _isPurchaseInProgress.value = false
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    queryActivePurchases()
                } else {
                    purchases.forEach { purchase -> handlePurchase(purchase) }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                emitError("Purchase canceled.")
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryActivePurchases()
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                _connectionState.value = ConnectionState.DISCONNECTED
                emitError("Billing service disconnected. Please try again.")
            }

            else -> {
                emitError("Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun endConnection() {
        mainHandler.removeCallbacksAndMessages(null)
        readyCallbacks.clear()
        billingClient.endConnection()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!purchase.products.contains(productId)) return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!isSignatureValid(purchase.originalJson, purchase.signature)) {
                    onPremiumStatusChanged(false)
                    emitError("Purchase verification failed.")
                    return
                }

                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                } else {
                    onPremiumStatusChanged(true)
                }
            }

            Purchase.PurchaseState.PENDING -> {
                emitError("Purchase pending. You'll get premium once it's completed.")
                onPremiumStatusChanged(false)
            }

            else -> {
                onPremiumStatusChanged(false)
            }
        }
    }

    private fun isSignatureValid(signedData: String, signature: String): Boolean {
        if (licenseKey.isBlank()) {
            return true
        }
        return Security.verifyPurchase(licenseKey, signedData, signature)
    }

    /**
     * The purchase is valid and paid at this point, so premium is granted whatever the acknowledge
     * result. A failed acknowledge (usually a network blip) used to switch premium off for a paying
     * user; now it's retried a few times here and again on every launch (queryActivePurchases), well
     * within the 3 days Google allows before refunding an unacknowledged purchase.
     */
    private fun acknowledgePurchase(purchase: Purchase, attempt: Int = 1) {
        onPremiumStatusChanged(true)
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK && attempt < MAX_ACKNOWLEDGE_ATTEMPTS) {
                mainHandler.postDelayed({ acknowledgePurchase(purchase, attempt + 1) }, ACKNOWLEDGE_RETRY_DELAY_MS * attempt)
            }
        }
    }

    private fun emitError(message: String) {
        _errors.tryEmit(message)
        onError(message)
    }

    companion object {
        const val DEFAULT_PRODUCT_ID = "one_time_remove_ads"
        private const val MAX_ACKNOWLEDGE_ATTEMPTS = 3
        private const val ACKNOWLEDGE_RETRY_DELAY_MS = 5_000L
    }
}
