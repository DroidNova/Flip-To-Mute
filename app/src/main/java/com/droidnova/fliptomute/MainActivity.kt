package com.droidnova.fliptomute

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.droidnova.fliptomute.data.analytics.AnalyticsEvents
import com.droidnova.fliptomute.data.analytics.AnalyticsLogger
import com.droidnova.fliptomute.data.analytics.Funnel
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.review.InAppReview
import com.droidnova.fliptomute.data.review.ReviewStore
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.data.update.InAppUpdate
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchEvent
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequest
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequestParser
import com.droidnova.fliptomute.ui.activities.OnboardingActivity
import com.droidnova.fliptomute.ui.navigation.AppNavHost
import com.droidnova.fliptomute.ui.screens.onboarding.OnboardingGate
import com.droidnova.fliptomute.ui.screens.onboarding.StartDestination
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.utils.MonitoringLog
import com.droidnova.fliptomute.utils.about_utils.AppConstants
import com.droidnova.fliptomute.utils.about_utils.IntentUtil
import com.droidnova.fliptomute.utils.ads.AdConfig
import com.droidnova.fliptomute.utils.ads.AdConsent
import com.droidnova.fliptomute.utils.ads.BannerPlacement
import com.droidnova.fliptomute.utils.ads.RemoteAdGate
import com.droidnova.fliptomute.utils.ads.shouldShowBanner
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class MainActivity : AppCompatActivity() {
    @Inject lateinit var analytics: AnalyticsLogger
    @Inject lateinit var funnel: Funnel
    @Inject lateinit var inAppReview: InAppReview
    @Inject lateinit var inAppUpdate: InAppUpdate
    @Inject lateinit var reviewStore: ReviewStore
    @Inject lateinit var preferencesRepository: AppPreferencesRepository
    @Inject lateinit var setupAccessRepository: SetupAccessRepository

    private val externalMonitoringRequest = MutableStateFlow(MainActivityLaunchEvent())

    /** The screen showing now; ad placement uses it from M7. */
    private var currentRoute by mutableStateOf<String?>(null)

    // Ads (M7-02, M7-03): requested only after consent, initialised off the main thread
    private var adsReady by mutableStateOf(false)
    private var remoteConfigReady by mutableStateOf(false)
    private var privacyOptionsRequired by mutableStateOf(false)
    private val adsStarted = AtomicBoolean(false)
    private var bannerView: AdView? = null

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
        setUpAds()
        setContent {
            FlipToMuteTheme {
                // The root is a plain Column, as in Secret Calculator: give text a readable default colour
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
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
                                currentRoute = destination.route
                            }
                            navController.addOnDestinationChangedListener(listener)
                            onDispose { navController.removeOnDestinationChangedListener(listener) }
                        }
                        val monitoringRequest by externalMonitoringRequest.collectAsStateWithLifecycle()
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
                            privacyOptionsRequired = privacyOptionsRequired,
                            onOpenPrivacyOptions = {
                                AdConsent.showPrivacyOptions(this@MainActivity) { privacyOptionsRequired = AdConsent.isPrivacyOptionsRequired(this@MainActivity) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        // Read here so the switches are read again once Remote Config arrives
                        val remoteReady = remoteConfigReady
                        if (remoteReady && shouldShowBanner(adsReady, BannerPlacement.forRoute(currentRoute), RemoteAdGate::isBannerEnabled)) {
                            AndroidView(
                                factory = { bannerAd().also { (it.parent as? ViewGroup)?.removeView(it) } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
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
        bannerView?.destroy()
        super.onDestroy()
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
