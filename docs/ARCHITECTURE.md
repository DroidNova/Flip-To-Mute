# Target architecture: aligned with Secret Calculator

Owner decision D9 (2026-10-06): Flip to Mute v2.0 is built the same way as
Secret Calculator. That app is already released and tested, so reusing its
patterns avoids new, untested designs and the vulnerabilities they could bring.

This document records what Secret Calculator does, what Flip to Mute takes
from it, and the few places where copying it as-is would make Flip to Mute
weaker. Every v2.0 task that touches structure follows this document.

Reference project: `D:\Softwares\PlayStoreApps\Secret-Calculator`, branch
`master` at `1c80e41`, version 2.0.0 (versionCode 37), read on 2026-10-06.
Paths below starting with `SC:` are relative to
`app/src/main/java/com/droidnova/secretcalculator/` in that project.

## 1. Secret Calculator at a glance

| Area | How Secret Calculator does it |
|------|-------------------------------|
| Language and UI | Kotlin, Jetpack Compose, Material 3 |
| Dependency injection | Hilt. One `AppModule` (`SC:di/AppModule.kt`), `@HiltViewModel` view models, `hiltViewModel()` in navigation, an `@ApplicationScope` coroutine scope |
| Seams for testing | Small `fun interface` types for anything the system decides, for example `BiometricHardware`, `StorageAccess`, `AnalyticsLogger`. The module provides the real one, tests pass a lambda. |
| Activities | One main activity `AppActivity` (an `AppCompatActivity`, needed by the fingerprint prompt) hosting every screen, plus `OnBoardingActivity` for first run |
| App shell | The activity draws a root `Column` that pads for system bars once, holds the `NavHost`, and puts the banner ad underneath. Top bars use zero insets. |
| Navigation | `Routes` object of route constants and builder functions. `AppNavHost` with `go()` (single top) and `back(entry)` (ignores a second Back tap). Slide plus fade transitions, 280 ms. |
| Screen pattern | `XRoute` (gets the view model, collects state) → `XScreen(state, actions)` where `XActions` is an interface → `XViewModel` exposing one `StateFlow<XUiState>` |
| View model state | Persisted settings combined with a private `TransientState` (dialogs, pickers, one-shot message). Messages are `@StringRes` ids cleared by `messageShown()`. |
| Settings storage | Small `@Singleton` store classes, each with its own private `SharedPreferences` file: `BiometricUnlock`, `PanicExitSettings`, `HintStore`, `Funnel`, review state. Plus a global `PreferenceUtil` object for older values. |
| Appearance | `Appearance` object holding the colour theme and light/dark choice as Compose state, so a change redraws at once without restarting the activity. `AppTheme` enum of colour schemes, `ThemeMode` of System, Light, Dark. Status bar icon colour set in the theme. |
| Typography | Outfit, a downloadable Google Font, for display, headline and title styles. The platform font for body and labels. Falls back to the platform font until Outfit downloads. Shared with the developer's Speedometer app. |
| Design kit | `SC:ui/components/VaultDesign.kt`: tinted rounded cards (22 dp), tiles (14 dp), round icon badges, uppercase section labels, `SettingsGroup`, `SettingsRow`, `SwitchRow`, `ChoicePill`, `SheetHeader`, `SheetAction`, `ActionTile`, `GradientBanner`, `ProBadge`, press-scale and staggered appear animations. `SC:ui/components/States.kt` and `TopBars.kt` for empty, loading and bar states. |
| First run | `OnBoardingActivity` with an `OnboardingStep` enum: Welcome, Permission, Set PIN, Try it. A practice step at the end, which can be skipped. |
| One-time tips | `HintStore` with a `VaultHint` enum. Shown one at a time, never again after dismissal. |
| One-time dialogs | A queue on the home screen: "update available" (from Remote Config), then "what's new" (shown while the stored version is below `VERSION_CODE`). |
| Fingerprint | `BiometricUnlock` store plus `showBiometricPrompt()` helper, strong biometrics only. Turned on only after a successful fingerprint check. Status refreshed on every resume, because fingerprints can be removed in system settings. |
| Secrets | `PinStore`: salted PBKDF2-SHA256 hashes, 60,000 iterations, constant-time compare. `AttemptLimiter`: lockout after 5 wrong tries, doubling up to a cap, stored on disk. |
| Screen privacy | `ScreenPrivacy` controls `FLAG_SECURE` from Settings. |
| Backup | `android:allowBackup="false"` |
| Background work | WorkManager `CoroutineWorker` that gets its dependencies through a Hilt `@EntryPoint`, scheduled with `enqueueUniquePeriodicWork(UPDATE)` |
| Analytics | `AnalyticsLogger` wraps Firebase Analytics. `Funnel` logs each milestone once (setup, first value, day 1 return, day 7 return) and never sends user content. Ads have their own `ad_event`. |
| Crash reporting | Firebase Crashlytics with its Gradle plugin |
| Remote switches | `RemoteAdGate`: Firebase Remote Config flags per ad placement plus "latest version code" for the update dialog. Defaults are safe; nothing is decided until the fetch finishes. |
| Ads | `AdConfig` (unit ids, frequency caps per placement), banner created once by the activity with three retries tied to its lifecycle, app open and interstitial managers with caps and "is showing" guards, ad guardrails in `docs/ad_opportunities.md` |
| Rating | "Rate us" in About opens the Play Store. Play In-App Review is asked by `ReviewPolicy`: every 3rd launch, after at least 3 value moments, at most 3 times, never within 3 minutes of a full-screen ad. An earlier star "rate card" was removed in commit `038a72e`. |
| Purchases | `PremiumBillingManager`, `Security.verifyPurchase` signature check, `PremiumController` reached through a `CompositionLocal` |
| Tests | JUnit 4, Robolectric (SDK 35), Turbine, coroutines test, `TrackedViewModels` helper that clears view models before `resetMain`, Room and WorkManager testing. 32 test classes. |
| Build | Version catalog, KSP, AGP 9.3.1, Kotlin 2.4.10, Gradle 9.5.0, compile and target SDK 37, min SDK 24 |
| Testers | Firebase App Distribution for debug builds to a tester group |
| Release process | Hotfix on the old code first, then migration with behaviour unchanged, then redesign, then features. Staged rollout with Crashlytics checks. |

## 2. What Flip to Mute adopts

| # | Pattern | Flip to Mute v2.0 | Milestone |
|---|---------|-------------------|-----------|
| A1 | Hilt | Replace the hand-written `AppContainer` and `ViewModelFactories` with Hilt modules. Interfaces stay, and are bound with `@Binds`. The service, both receivers, the tile service and the device admin receiver become `@AndroidEntryPoint`. | M2 |
| A2 | `fun interface` seams | Use the same style for new system checks: battery optimisation status, device admin availability, current time. | M1, M2 |
| A3 | Activity shell | `MainActivity` becomes an `AppCompatActivity` with the same root `Column`, insets padded once, banner underneath, route tracking. | M2 |
| A4 | Separate first run activity | `OnboardingActivity` with an `OnboardingStep` enum: Welcome, Access, Try it. It mirrors Secret Calculator's Welcome, Permission, Try it steps without the PIN step. | M4 |
| A5 | Routes and nav host | `Routes` object, `AppNavHost` with `go()` and `back(entry)`, the same 280 ms slide and fade. | M2 |
| A6 | Screen pattern | Every screen is `Route` + `Screen(state, actions)` + `ViewModel` + `UiState`, with `TransientState` and `@StringRes` messages. | M2 to M6 |
| A7 | Design kit | Copy `VaultDesign.kt`, `States.kt` and `TopBars.kt` into `ui/components/`, renamed to neutral names (`NovaCard`, `NovaTopBar`). Keep sizes, shapes and motion identical. | M3 |
| A8 | Appearance and themes | Copy `Appearance`, `AppTheme`, `ThemeMode` and the theme function. Flip to Mute gets its own colour themes. Theme mode defaults to System. | M3 |
| A9 | Typography | Same Outfit plus platform font setup, including the Google Fonts certificate array. | M3 |
| A10 | Settings screen | Same `SettingsGroup`, `SettingsRow`, `SwitchRow`, `ChoicePill` layout, the same explain-before-enable dialog, refresh on resume with `LifecycleResumeEffect`. | M6 |
| A11 | "Turn on only after the system confirms" | Secret Calculator turns fingerprint unlock on only after a successful prompt. Flip to Mute applies the same rule to Flip to lock (only after device admin is granted) and to every access step. | M6 |
| A12 | Hints and discovery cards | `HintStore` with an enum, one card at a time. | M5 |
| A13 | Home dialog queue | Update available, then what's new, the same way. | M5 |
| A14 | Analytics | `AnalyticsLogger` and a `Funnel` class with log-once milestones: setup complete, first flip, day 1 return, day 7 return. | M0, M2 |
| A15 | Crashlytics | Same plugin and setup. | M0 |
| A16 | Remote Config switches | `RemoteAdGate` style: banner on or off per placement, latest version code. Safe defaults. This reverses the removal in 1.x commit `fcf4f3a`, by decision D10. | M7 |
| A17 | Ads | `AdConfig`, one banner owned by the activity, retries, hidden on listed routes, the same guardrails. No app open or interstitial ads in v2.0. If they are added later, the Secret Calculator managers are reused unchanged. | M7 |
| A18 | Rating | Keep "Rate us" in About, which is identical in both apps today. Add `InAppReview` and `ReviewPolicy` unchanged. A value moment in Flip to Mute is a call silenced by a flip, or a passed "Check my setup". Decision D1. | M6 |
| A19 | Background work | Health check worker built like `TrashCleanupWorker`, with an `@EntryPoint`. | M1, M2 |
| A20 | Backup | `android:allowBackup="false"`, plus the extra rule in section 3. | M1 |
| A21 | Tests | Add Robolectric, Turbine, `androidx.test:core` and the `TrackedViewModels` helper. Existing 187 tests are kept. | M0, M2 |
| A22 | Build | Version catalog style, KSP, Hilt plugin, Crashlytics plugin, App Distribution plugin. Tool versions match Secret Calculator. Library versions take the newer of the two apps. | M2 |
| A23 | Testers | Firebase App Distribution group for debug builds. | M0 |
| A24 | Release process | Same order as Secret Calculator: reliability fixes first on today's code (possible 1.8), then migration with behaviour unchanged, then redesign. | Plan |
| A25 | Purchases | Not in v2.0. When "remove ads" comes (future F19), reuse `PremiumBillingManager`, `Security` and `PremiumController` unchanged. | Later |
| A26 | Fingerprint | Not in v2.0, because Flip to Mute has nothing to unlock. If a fingerprint feature is ever added (future F29), reuse `BiometricUnlock`, `BiometricHardware` and `showBiometricPrompt()` unchanged, including strong biometrics only, enable after a successful check, and refresh on resume. | Later |

## 3. Where copying as-is would add risk

These are the only deviations. Each one keeps the Secret Calculator pattern
and closes a gap that matters more for Flip to Mute.

| # | Secret Calculator today | Why it matters for Flip to Mute | What Flip to Mute does |
|---|-------------------------|---------------------------------|------------------------|
| X1 | The global `PreferenceUtil` object must be initialised by an activity before use. | Flip to Mute runs code with no activity: the service, boot and update receivers, the tile and the health worker. Reading `PreferenceUtil` there would crash. | No global preference object. Only injected `@Singleton` stores, which Secret Calculator also uses for its newer settings. |
| X2 | Settings live in `SharedPreferences`. | Flip to Mute's on, paused and ringer recovery state is in DataStore, with tests that prove a ringtone is never left silent. Moving it would put that guarantee at risk for no user benefit. | Keep the existing DataStore repositories for service state and recovery. New UI-only stores (hints, review, funnel, appearance) follow the Secret Calculator store pattern. |
| X3 | `IntentUtil` opens Instagram, WhatsApp, share and the web fallback for Play Store without catching a missing app. | The same gap exists in Flip to Mute today (audit R7). It can crash on phones without a browser or with a disabled Play Store. | Same `IntentUtil` API, every launch wrapped in one guarded helper. Worth back-porting to Secret Calculator. |
| X4 | `allowBackup="false"` with the empty sample `data_extraction_rules.xml`. | On Android 12 and later this stops cloud backup but still allows device-to-device transfer. A transferred "on" flag or ringer recovery session would be wrong on the new phone. | `allowBackup="false"` and an explicit device-transfer exclude in `data_extraction_rules.xml`. Worth back-porting. |
| X5 | No ad consent form. | AdMob requires consent for users in the EEA and the UK. | Add the Google User Messaging Platform. Worth back-porting. |
| X6 | Full `play-services-ads` 23.3.0. | Flip to Mute moved to Ads Lite in commit `f2a3dcf` for 16 KB page size compliance. | Keep Ads Lite 25.0.0. Check that any new ad format is supported by Lite before adding it. |
| X7 | Older libraries: Compose BOM 2024.12.01, lifecycle 2.8.7, navigation 2.8.9. | Flip to Mute is already on Compose BOM 2026.02.01, lifecycle 2.10.0 and navigation 2.9.7. Downgrading would reintroduce fixed bugs. | Take the newer version of every library. Match only the tool chain (AGP, Kotlin, KSP, Gradle, Hilt). |
| X8 | `FLAG_SECURE` on by default. | Flip to Mute shows nothing private, and screenshots help users report problems and share the app. | Not adopted. |
| X9 | The App Distribution service account path is written in `build.gradle.kts`. | It is a path on one computer, and a key file must never be committed. | Read the path from `local.properties`, which is already git-ignored. |
| X10 | `MobileAds.initialize` called on the main thread in the activity. | Adds to cold start. | Initialise ads off the main thread, after consent. |
| X11 | min SDK 24, target SDK 37. | Flip to Mute's foreground service rules depend on the target SDK. A target bump changes service behaviour and needs device testing. | Keep min SDK 26. Raise the target to 37 as its own task, with the device matrix run afterwards. |
| X12 | The launcher activity was moved to `ui.activities` and kept reachable through an activity-alias with the old name. | Flip to Mute has no reason to rename its launcher component, and a rename can remove users' home-screen icons. | `MainActivity` stays at `com.droidnova.fliptomute.MainActivity`. Only its contents follow Secret Calculator. |
| X13 | `AppActivity` and `OnBoardingActivity` are `@AndroidEntryPoint`; the worker uses a Hilt `@EntryPoint`. | Hilt's `@AndroidEntryPoint` on a `BroadcastReceiver` needs a base class to call `super.onReceive`, which is easy to get wrong. | Activities and services use `@AndroidEntryPoint`. Receivers and the worker use one `BackgroundEntryPoint`, the same mechanism as Secret Calculator's worker. |
| X14 | Window theme is a Material3 XML theme from the Material Components library. | Flip to Mute does not need the Material Components library; Compose draws the whole screen. | `Theme.AppCompat` window theme, which `AppCompatActivity` requires. Light only until M3-05 adds the dark window and splash colours. |
| X15 | `PremiumBillingManager` reads the Play licence key from `BuildConfig`, and the premium flag lives in the global `PreferenceUtil`. | Flip to Mute has `buildConfig` switched off and no global preference object (X1). | The billing files are copied (M9-22) with one change: the licence key is a constructor parameter read from `AppConstants.PLAY_STORE_LICENSE_KEY`. The flag lives in the injected `PremiumStore`. |
| X16 | Secret Calculator has no splash: the activity draws Home as soon as it can. | The owner wants a splash like Notification History's, with the launch ads loading behind it so Home opens with them ready. | `Theme.FlipToMute.Starting` (core-splashscreen) holds the system splash until the first screen is known, then `SplashIntro` (copied from Notification History, with a phone-flip instead of a bell ring) plays over the app. Consent, Remote Config, the ads SDK, the banner, the interstitial and the native ad are all asked for in `onCreate`, before the DataStore read. The intro waits for the banner's answer, at most 2.5 s. |

## 4. Flip to Mute package layout in v2.0

Package names follow Secret Calculator. Flip to Mute's domain packages stay.

```
com.droidnova.fliptomute
├── FlipToMuteApplication.kt        @HiltAndroidApp (name kept from 1.x)
├── MainActivity.kt                 stays here (X12)
├── di/                             AppModule (@Provides, one module like Secret Calculator), BackgroundEntryPoint
├── ui/
│   ├── activities/                 OnboardingActivity (M4)
│   ├── navigation/                 Routes, AppNavHost
│   ├── theme/                      Appearance, Color, Theme, Type
│   ├── components/                 NovaDesign (from VaultDesign), States, TopBars, PhoneFlipIllustration
│   ├── common/                     Hints, small UI helpers
│   ├── sheets/                     Pause sheet, access hint sheet
│   └── screens/
│       ├── home/                   HomeRoute, HomeScreen, HomeViewModel, HomeUiState
│       ├── onboarding/
│       ├── settings/
│       ├── keep_running/
│       ├── check_setup/
│       └── about/
├── data/
│   ├── preferences/                existing DataStore repository (kept)
│   ├── recovery/                   existing ringer recovery (kept)
│   ├── setup/                      existing access state (kept)
│   ├── stats/                      new, flip counts
│   ├── reliability/                new, battery optimisation
│   ├── review/                     InAppReview, ReviewPolicy (from Secret Calculator)
│   ├── analytics/                  AnalyticsLogger, Funnel (from Secret Calculator)
│   └── settings/                   Appearance store and other small stores
├── service/                        existing monitoring service and coordinator (kept)
├── sensor/  telephony/  audio/     existing detection core (kept)
├── boot/                           boot and package replaced receivers
├── quicksettings/  screenlock/  deviceadmin/  notification/   existing (kept)
├── workers/                        HealthCheckWorker
└── utils/
    ├── about_utils/                IntentUtil, OtherApps, PackageManagerExt (from Secret Calculator)
    └── ads/                        AdConfig, RemoteAdGate, consent, banner
```

## 5. How to use this document

- Before building a screen, open the matching Secret Calculator file and follow its structure.
- When something should differ, add a row to section 3 with the reason. Do not deviate silently.
- When a gap is fixed in Flip to Mute and also exists in Secret Calculator, note it in that project's docs so both apps stay aligned.
