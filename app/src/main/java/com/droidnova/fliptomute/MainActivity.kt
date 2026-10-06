package com.droidnova.fliptomute

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.droidnova.fliptomute.billing.LocalPremiumController
import com.droidnova.fliptomute.billing.PremiumBillingManager
import com.droidnova.fliptomute.billing.PremiumController
import com.droidnova.fliptomute.billing.PremiumEvent
import com.droidnova.fliptomute.billing.PremiumUi
import com.droidnova.fliptomute.data.analytics.AnalyticsEvents
import com.droidnova.fliptomute.data.analytics.AnalyticsLogger
import com.droidnova.fliptomute.data.analytics.Funnel
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.premium.PremiumStore
import com.droidnova.fliptomute.data.review.InAppReview
import com.droidnova.fliptomute.data.review.ReviewStore
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.data.stats.FlipStatsStore
import com.droidnova.fliptomute.data.themes.ThemeUnlockStore
import com.droidnova.fliptomute.data.update.InAppUpdate
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchEvent
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequest
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequestParser
import com.droidnova.fliptomute.ui.activities.OnboardingActivity
import com.droidnova.fliptomute.ui.navigation.AppNavHost
import com.droidnova.fliptomute.ui.navigation.Routes
import com.droidnova.fliptomute.ui.screens.onboarding.OnboardingGate
import com.droidnova.fliptomute.ui.screens.onboarding.StartDestination
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.utils.MonitoringLog
import com.droidnova.fliptomute.utils.about_utils.AppConstants
import com.droidnova.fliptomute.utils.about_utils.IntentUtil
import com.droidnova.fliptomute.utils.ads.AdConfig
import com.droidnova.fliptomute.utils.ads.AdConsent
import com.droidnova.fliptomute.utils.ads.BannerPlacement
import com.droidnova.fliptomute.utils.ads.InterstitialAds
import com.droidnova.fliptomute.utils.ads.InterstitialPolicy
import com.droidnova.fliptomute.utils.ads.NativeAdCard
import com.droidnova.fliptomute.utils.ads.RemoteAdGate
import com.droidnova.fliptomute.utils.ads.shouldLoadNativeAd
import com.droidnova.fliptomute.utils.ads.shouldShowBanner
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The single activity, built like Secret Calculator's AppActivity (architecture A3): one root column
 * that pads for the system bars once, the nav host, and the banner underneath.
 *
 * It stays at `com.droidnova.fliptomute.MainActivity` on purpose: moving it would rename the
 * launcher component and can remove users' home-screen icons (architecture X12).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PremiumController {
    @Inject lateinit var analytics: AnalyticsLogger
    @Inject lateinit var funnel: Funnel
    @Inject lateinit var inAppReview: InAppReview
    @Inject lateinit var inAppUpdate: InAppUpdate
    @Inject lateinit var reviewStore: ReviewStore
    @Inject lateinit var preferencesRepository: AppPreferencesRepository
    @Inject lateinit var setupAccessRepository: SetupAccessRepository
    @Inject lateinit var flipStatsStore: FlipStatsStore
    @Inject lateinit var themeUnlockStore: ThemeUnlockStore
    @Inject lateinit var premiumStore: PremiumStore

    // "Remove ads" (future features F19), as Secret Calculator's AppActivity
    private var billingManager: PremiumBillingManager? = null
    private var hasPendingRemoveAdsClick = false
    private val mutablePremiumUi = MutableStateFlow(PremiumUi(isPremium = false))
    override val premiumUi: StateFlow<PremiumUi> = mutablePremiumUi.asStateFlow()
    private val mutablePremiumEvents = MutableSharedFlow<PremiumEvent>(extraBufferCapacity = 1)
    override val premiumEvents: SharedFlow<PremiumEvent> = mutablePremiumEvents.asSharedFlow()
    private var showPremiumWelcome by mutableStateOf(false)

    private val externalMonitoringRequest = MutableStateFlow(MainActivityLaunchEvent())

    /** The screen showing now; ad placement uses it from M7. */
    private var currentRoute by mutableStateOf<String?>(null)

    // Ads (M7-02, M7-03): requested only after consent, initialised off the main thread
    private var adsReady by mutableStateOf(false)
    private var remoteConfigReady by mutableStateOf(false)
    private var privacyOptionsRequired by mutableStateOf(false)
    private val adsStarted = AtomicBoolean(false)
    private var bannerView: AdView? = null

    /** The native ad on the activity screen (future features F32): asked for when that screen opens. */
    private var activityNativeAd by mutableStateOf<NativeAd?>(null)
    private var nativeAdRequested = false

    /** The interstitial for natural breaks (docs/AD_OPPORTUNITIES.md). A review prompt keeps away after it. */
    private val interstitialAds by lazy { InterstitialAds(this, onClosed = { reviewStore.recordAdClosed() }) }

    /** A flexible in-app update finished downloading; ask for the restart (M7-09). */
    private var updateReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState != null) {
            showContent()
            return
        }
        // Decide between the first run and Home before drawing; the themed window background shows meanwhile
        lifecycleScope.launch {
            val preferences = preferencesRepository.preferences.first()
            val setupComplete = setupAccessRepository.refreshAndGet().isSetupComplete
            if (OnboardingGate.shouldMarkCompleted(preferences.onboardingCompleted, preferences.monitoringEnabled, setupComplete)) {
                preferencesRepository.setOnboardingCompleted(true)
            }
            val destination = OnboardingGate.decide(preferences.onboardingCompleted, preferences.monitoringEnabled, setupComplete)
            if (destination == StartDestination.ONBOARDING) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
                return@launch
            }
            consumeLaunchRequest(intent)
            funnel.appOpened()
            reviewStore.recordLaunch()
            showContent()
        }
    }

    private fun showContent() {
        // A theme opened for a week by an ad goes back to the default when the week is over
        themeUnlockStore.enforce(flipStatsStore.stats.value.total)
        mutablePremiumUi.value = PremiumUi(isPremium = isPremiumPurchased())
        initializeBilling()
        setUpAds()
        setContent {
            FlipToMuteTheme {
                // The root is a plain Column, as in Secret Calculator: give text a readable default colour
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                    LocalPremiumController provides this,
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            // Edge to edge: system bars, cutouts and the keyboard are padded once, here
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        val navController = rememberNavController()
                        DisposableEffect(navController) {
                            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                                val previousRoute = currentRoute
                                currentRoute = destination.route
                                onRouteChanged(previousRoute, destination.route)
                            }
                            navController.addOnDestinationChangedListener(listener)
                            onDispose { navController.removeOnDestinationChangedListener(listener) }
                        }
                        val monitoringRequest by externalMonitoringRequest.collectAsStateWithLifecycle()
                        val premium by premiumUi.collectAsStateWithLifecycle()
                        // Read here so the switches are read again once Remote Config arrives
                        val remoteReady = remoteConfigReady
                        LaunchedEffect(currentRoute, adsReady, remoteReady, premium.isPremium) {
                            loadActivityNativeAd(remoteReady)
                        }
                        val nativeAd = activityNativeAd.takeUnless { premium.isPremium }
                        AppNavHost(
                            navController = navController,
                            externalMonitoringRequest = monitoringRequest,
                            onExternalMonitoringRequestConsumed = ::clearLaunchRequest,
                            onOpenAccess = { startActivity(OnboardingActivity.accessIntent(this@MainActivity)) },
                            onReportProblem = { summary -> IntentUtil.sendSupportMail(this@MainActivity, isBug = true, summary = summary) },
                            onOpenPrivacyPolicy = { IntentUtil.openUrl(this@MainActivity, AppConstants.PRIVACY_POLICY_URL) },
                            onRateUsTapped = { analytics.log(AnalyticsEvents.RATE_US_TAPPED, emptyMap()) },
                            onHomeCalm = { inAppReview.maybeAsk(this@MainActivity) },
                            onStartUpdate = {
                                inAppUpdate.start(
                                    this@MainActivity,
                                    onDownloaded = { updateReady = true },
                                    onFallback = { IntentUtil.openPlayStore(this@MainActivity, packageName) },
                                )
                            },
                            onActivityOpened = { source ->
                                analytics.log(AnalyticsEvents.ACTIVITY_OPENED, mapOf(AnalyticsEvents.PARAM_SOURCE to source))
                            },
                            privacyOptionsRequired = privacyOptionsRequired,
                            onOpenPrivacyOptions = {
                                AdConsent.showPrivacyOptions(this@MainActivity) { privacyOptionsRequired = AdConsent.isPrivacyOptionsRequired(this@MainActivity) }
                            },
                            rewardedThemeAvailable = adsReady && remoteConfigReady && RemoteAdGate.isRewardedThemeEnabled() &&
                                AdConfig.rewardedThemeUnitId(this@MainActivity) != null,
                            onWatchAdForTheme = ::showRewardedForTheme,
                            activityNativeAd = nativeAd?.let { ad -> { NativeAdCard(ad) } },
                            modifier = Modifier.weight(1f),
                        )
                        if (remoteReady && !premium.isPremium &&
                            shouldShowBanner(
                                adsReady = adsReady,
                                placement = BannerPlacement.forRoute(currentRoute),
                                nativeAdShowing = currentRoute == Routes.ACTIVITY && nativeAd != null,
                                isEnabled = RemoteAdGate::isBannerEnabled,
                            )
                        ) {
                            AndroidView(
                                factory = { bannerAd().also { (it.parent as? ViewGroup)?.removeView(it) } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (showPremiumWelcome) {
                        AlertDialog(
                            onDismissRequest = { showPremiumWelcome = false },
                            title = { Text(stringResource(R.string.premium_welcome_title)) },
                            text = { Text(stringResource(R.string.premium_welcome_body)) },
                            confirmButton = { TextButton(onClick = { showPremiumWelcome = false }) { Text(stringResource(R.string.got_it)) } },
                        )
                    }
                    if (updateReady) {
                        AlertDialog(
                            onDismissRequest = { updateReady = false },
                            title = { Text(stringResource(R.string.update_ready_title)) },
                            text = { Text(stringResource(R.string.update_ready_body)) },
                            confirmButton = { TextButton(onClick = { updateReady = false; inAppUpdate.completeUpdate() }) { Text(stringResource(R.string.update_restart)) } },
                            dismissButton = { TextButton(onClick = { updateReady = false }) { Text(stringResource(R.string.update_later)) } },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdate.checkDownloaded { updateReady = true }
    }

    override fun onDestroy() {
        billingManager?.endConnection()
        activityNativeAd?.destroy()
        bannerView?.destroy()
        super.onDestroy()
    }

    // --- Premium (PremiumController), as in Secret Calculator's AppActivity ---

    private fun initializeBilling() {
        if (!AppConstants.PREMIUM_ENABLED || billingManager != null) return
        val manager = try {
            PremiumBillingManager(
                context = applicationContext,
                onPremiumStatusChanged = { isPremium -> runOnUiThread { handlePremiumStatusChanged(isPremium) } },
                onError = { message ->
                    runOnUiThread {
                        // Only where the purchase is offered; a failed background check stays quiet
                        if (currentRoute == Routes.SETTINGS) Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                },
            )
        } catch (error: RuntimeException) {
            // No Play Store on this phone: the app works as before, with ads
            MonitoringLog.failure(this, "Billing unavailable", error)
            return
        }
        billingManager = manager
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    manager.productDetails,
                    manager.isFetchingProductDetails,
                    manager.isPurchaseInProgress,
                ) { details, fetching, purchasing ->
                    mutablePremiumUi.update {
                        it.copy(priceLabel = details?.oneTimePurchaseOfferDetails?.formattedPrice, isLoading = fetching || purchasing)
                    }
                }.collect {}
            }
        }
        manager.queryActivePurchases()
        manager.queryProductDetails()
    }

    private fun isDebugBuild(): Boolean = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private fun isPremiumPurchased(): Boolean = premiumStore.isPremium

    override fun launchPurchase() {
        if (!AppConstants.PREMIUM_ENABLED || isPremiumPurchased()) return
        hasPendingRemoveAdsClick = true
        if (isDebugBuild()) {
            // Debug builds unlock premium for the session without the store
            premiumStore.debugSessionPremium = true
            hasPendingRemoveAdsClick = false
            mutablePremiumUi.update { it.copy(isPremium = true) }
            onPurchased()
            return
        }
        billingManager?.launchPurchaseFlow(this)
            ?: Toast.makeText(this, R.string.premium_unavailable, Toast.LENGTH_SHORT).show()
    }

    /** Bought just now: tell the screens, and say thank you. */
    private fun onPurchased() {
        showPremiumWelcome = true
        mutablePremiumEvents.tryEmit(PremiumEvent.Purchased)
    }

    override fun onPremiumSheetDismissed() {
        hasPendingRemoveAdsClick = false
    }

    private fun handlePremiumStatusChanged(isPremium: Boolean) {
        val wasPremium = premiumStore.hasBoughtPremium
        premiumStore.hasBoughtPremium = isPremium
        mutablePremiumUi.update { it.copy(isPremium = isPremiumPurchased()) }
        if (!wasPremium && isPremium) {
            if (hasPendingRemoveAdsClick) onPurchased() else mutablePremiumEvents.tryEmit(PremiumEvent.Reactivated)
            hasPendingRemoveAdsClick = false
        }
    }

    // --- Ads, as in Secret Calculator's AppActivity ---

    private fun setUpAds() {
        RemoteAdGate.initialize { remoteConfigReady = true }
        // A stored answer allows ads at once; the update below can still change it
        if (AdConsent.canRequestAds(this)) startAds()
        AdConsent.gather(this) { canRequestAds ->
            privacyOptionsRequired = AdConsent.isPrivacyOptionsRequired(this)
            if (canRequestAds) startAds()
        }
    }

    private fun startAds() {
        if (!adsStarted.compareAndSet(false, true)) return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { MobileAds.initialize(applicationContext) }
            adsReady = true
        }
    }

    /** The bottom banner, created and loaded once, then moved between screens. */
    private fun bannerAd(): AdView = bannerView ?: AdView(this).apply {
        adUnitId = AdConfig.mainBottomBannerUnitId(this@MainActivity)
        val metrics = resources.displayMetrics
        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this@MainActivity, (metrics.widthPixels / metrics.density).toInt()))
        loadBannerWithRetry(this, AdConfig.BANNER_LOAD_ATTEMPTS)
    }.also { bannerView = it }

    private fun loadBannerWithRetry(adView: AdView, attemptsLeft: Int) {
        if (attemptsLeft <= 0) return
        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                MonitoringLog.d(this@MainActivity, "Banner failed to load: ${error.message}")
                // Tied to the activity, so a retry never outlives it
                lifecycleScope.launch {
                    delay(AdConfig.BANNER_RETRY_DELAY_MS)
                    loadBannerWithRetry(adView, attemptsLeft - 1)
                }
            }
        }
        // Collapsible, as in 1.x
        val extras = Bundle().apply { putString("collapsible", "bottom") }
        adView.loadAd(AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build())
    }

    /** Full-screen ads only at a natural break, and only within the limits of [InterstitialPolicy]. */
    private fun onRouteChanged(fromRoute: String?, toRoute: String?) {
        val allowed = adsReady && remoteConfigReady && InterstitialPolicy.mayShow(
            enabled = RemoteAdGate.isInterstitialEnabled(),
            adsRemoved = isPremiumPurchased(),
            launchCount = reviewStore.state().launchCount,
            lastShownAt = interstitialAds.lastShownAt,
            now = System.currentTimeMillis(),
            cooldownHours = RemoteAdGate.interstitialCooldownHours(),
        )
        if (!allowed) return
        if (InterstitialPolicy.shouldPreloadOn(toRoute)) interstitialAds.preload()
        if (InterstitialPolicy.isNaturalBreak(fromRoute, toRoute)) interstitialAds.show(this)
    }

    /** One native ad for the activity screen, kept until the activity ends. A failed load may be tried again on the next visit. */
    private fun loadActivityNativeAd(remoteReady: Boolean) {
        val wanted = shouldLoadNativeAd(
            onActivityScreen = currentRoute == Routes.ACTIVITY,
            adsReady = adsReady,
            remoteEnabled = remoteReady && RemoteAdGate.isNativeActivityEnabled(),
            adsRemoved = isPremiumPurchased(),
            alreadyRequested = nativeAdRequested,
        )
        if (!wanted) return
        nativeAdRequested = true
        AdLoader.Builder(this, AdConfig.nativeActivityUnitId(this))
            .forNativeAd { ad ->
                if (isFinishing || isDestroyed) {
                    ad.destroy()
                } else {
                    activityNativeAd?.destroy()
                    activityNativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    MonitoringLog.d(this@MainActivity, "Native ad failed to load: ${error.message}")
                    nativeAdRequested = false
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /** Loads and shows one rewarded ad. [onRewarded] runs only when it was watched to the end. */
    private fun showRewardedForTheme(onRewarded: () -> Unit, onUnavailable: () -> Unit) {
        val unitId = AdConfig.rewardedThemeUnitId(this)
        if (!adsReady || unitId == null) {
            onUnavailable()
            return
        }
        RewardedAd.load(
            this, unitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (isFinishing || isDestroyed) return
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() = reviewStore.recordAdClosed()
                    }
                    ad.show(this@MainActivity) { onRewarded() }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    MonitoringLog.d(this@MainActivity, "Rewarded ad failed to load: ${error.message}")
                    onUnavailable()
                }
            },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchRequest(intent)
    }

    private fun consumeLaunchRequest(intent: Intent?) {
        val request = MainActivityLaunchRequestParser.parse(intent)
        if (request != MainActivityLaunchRequest.None) {
            externalMonitoringRequest.value = MainActivityLaunchEvent(
                sequence = externalMonitoringRequest.value.sequence + 1L,
                request = request,
            )
            intent?.action = null
        }
    }

    private fun clearLaunchRequest() {
        externalMonitoringRequest.value = externalMonitoringRequest.value.copy(
            request = MainActivityLaunchRequest.None,
        )
    }
}
