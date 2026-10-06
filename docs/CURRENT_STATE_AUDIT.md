# Current state audit (v1.7)

Snapshot of `master` at commit `a39167c`, taken on 2026-10-06. Every finding
has an ID. The update plan and the milestone tracker refer to these IDs.

Paths are relative to `app/src/main/java/com/droidnova/fliptomute/` unless they
start with `app/` or are a root file.

## 1. What the app is today

| Item | Value |
|------|-------|
| Package | `com.droidnova.fliptomute` |
| Version | 1.7 (versionCode 8) |
| SDK | min 26, target 36, compile 36.1 |
| Stack | Kotlin 2.2.10, AGP 9.0.0, Jetpack Compose (BOM 2026.02.01), Material 3, Navigation Compose, DataStore Preferences, coroutines |
| Services | Firebase Analytics (dependency only), AdMob Lite 25.0.0 |
| Size of code | 105 Kotlin source files, about 8,000 lines; 41 test source files |
| Languages | English only, 270 strings |

### Features

- **Flip to mute.** A foreground service listens for ringing cellular calls. While a call rings, the gravity sensor is read. Face down for 400 ms applies Silent or Vibrate, and the previous ringer mode is restored when the call ends.
- **Pause and resume**, from the home screen, the notification and the Quick Settings tile.
- **Pocket protection** using the proximity sensor.
- **Only when lying flat**, an optional stricter gesture.
- **Flip to lock screen**, using device admin, while the service is on.
- **Start after phone restart**, optional and off by default.
- **Quick Settings tile.**
- **Diagnostics:** three test screens for sensor, call detection and sound control.
- **About:** rate, share, bug report by email, Instagram, WhatsApp, other apps.

### Screens and navigation

```
Home ── overflow menu ──┬── Settings ──┬── App setup (permissions)
  │                     │              ├── Sensor test
  └── App setup         │              ├── Call detection test
                        │              └── Sound control test
                        └── About
A collapsible banner ad sits under every screen.
```

### What is already good and must be kept

- Clean separation: interfaces, Android implementations, fakes for tests, a manual dependency container (`app/AppContainer.kt`).
- A real state machine for the service (`service/MonitoringModels.kt`) with ten runtime states.
- Durable ringer recovery, so the phone is never left silent after a crash (`audio/DefaultRingerModeController.kt`, `data/recovery/`).
- 187 unit tests covering sensors, telephony mapping, the coordinator, view models and recovery.
- Sensors run only while a call rings, so battery cost is low.
- Privacy posture: no call log, contacts or numbers.
- 16 KB page size support and an R8 enabled release build.

The v2.0 work changes the interface and adds reliability layers. It does not
rewrite this core.

## 2. Test baseline

Run on 2026-10-06 with `gradlew testDebugUnitTest` on Windows 11.

| Result | Count |
|--------|-------|
| Tests run | 187 |
| Passed | 182 (187 after task M0-05) |
| Failed | 5 |

All five failures are in `DataStoreAppPreferencesRepositoryTest` (3) and
`DataStoreRingerRecoveryRepositoryTest` (2). Each one fails with
`IOException: Unable to rename ... .tmp`. This is DataStore's atomic rename
colliding with an open file handle on Windows. It is a test harness problem,
not a production bug. The file `failing-tests.txt` in the project root is an
older run with ten failures and can be deleted once the baseline is green.

There are no instrumented tests, no Compose UI tests and no CI.

## 3. Reliability findings

| ID | Severity | Finding | Evidence |
|----|----------|---------|----------|
| R1 | Critical | **The service does not come back after an app update.** Android kills the process on update and there is no `MY_PACKAGE_REPLACED` receiver. Users stay unprotected until they open the app. The v2.0 rollout itself will trigger this for every existing user. | `app/src/main/AndroidManifest.xml` declares only `BOOT_COMPLETED`. |
| R2 | Critical | **The service does not come back after a reboot unless the user found a setting.** "Start after phone restart" defaults to off. | `data/preferences/AppPreferences.kt` (`startAfterPhoneRestart = false`). |
| R3 | High | **A failed automatic start erases the user's choice silently.** Every start failure goes through `failStart`, which writes `monitoringEnabled = false`. This includes system restarts of the sticky service and the 10 second call monitor timeout. The user gets no notification. | `service/FlipMonitoringService.kt` (`failStart`, `finishStopped(writePreference = true)`), `service/FlipMonitoringCoordinator.kt` (`MONITORING_START_TIMEOUT_MILLIS`). |
| R4 | High | **No help with battery managers.** The app neither detects battery optimisation nor guides the user through manufacturer auto-start settings. On Xiaomi, Oppo, Vivo, Realme and Samsung this is the main reason background apps stop. | No use of `PowerManager.isIgnoringBatteryOptimizations` anywhere. `README.md` lists this as a known limitation. |
| R5 | High | **No field visibility.** Logs exist only in debug builds. There is no crash reporting. Firebase Analytics is on the classpath but no event is ever logged. Failures in production are invisible. | `util/MonitoringLog.kt`; no `logEvent` or Crashlytics call in the source tree. |
| R6 | Medium | **Backup can restore stale runtime state.** `allowBackup` is true with the sample rule files. One DataStore file holds settings, the on or paused flags and the ringer recovery session. A restore on a new phone brings an "on" flag without permissions and possibly a recovery session from another device. | `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`, `app/AppContainer.kt`. |
| R7 | Medium | **Crash risk from unguarded intents.** Instagram, WhatsApp, share, and the web fallbacks for Play Store links call `startActivity` without catching `ActivityNotFoundException`. | `core/utils/about_utils/IntentUtil.kt`. |
| R8 | Medium | **"Flip Feedback" does nothing.** The toggle is saved and displayed but the service never reads it. | `detectionFeedbackEnabled` appears only in `data/preferences/` and `ui/screens/settings/`. |
| R9 | High | **Ad setup problems.** (a) `admob_app_id` in `strings.xml` is Google's sample App ID, while `AdUnits.kt` holds a production ad unit. (b) `MobileAds.initialize` runs on the main thread during app start. (c) There is no consent flow for EEA and UK users. (d) The `AdView` is never paused or resumed. | `app/src/main/res/values/strings.xml` line 3, `ads/AdUnits.kt`, `app/FlipToMuteApplication.kt`, `ads/CollapsibleBannerAd.kt`. |
| R10 | Medium | **Weak release safety net.** Five red tests, no CI, no UI tests, and no release build smoke test even though 1.x shipped a crash that only happened with R8 (commit `3a05227`). | Section 2 above; git history. |
| R11 | Low | **Detection feels slower than it needs to.** The sensor is registered at `SENSOR_DELAY_NORMAL`, about five samples per second, and then needs 400 ms of stability. | `sensor/AndroidDeviceOrientationMonitor.kt`, `sensor/FaceDownDetectionModels.kt`. |
| R12 | Low | **Dead onboarding flag.** `onboardingCompleted` is written and never read. | `ui/screens/permissions/PermissionsViewModel.kt`. |
| R13 | Low | **White flash on dark phones.** The window theme is `Theme.Material.Light.NoActionBar` and there is no splash screen. | `app/src/main/res/values/themes.xml`. |
| R14 | Check | **Privacy statements must match reality.** `README.md` says the app collects no internet data, but the app declares `INTERNET`, shows ads and links Firebase. The Play Data safety form and the privacy policy have to describe ads and analytics. Settings currently shows "Privacy Policy: Not available yet". | `README.md`, `ui/screens/settings/SettingsScreen.kt`. |
| R15 | Critical | **Likely crash on Android 8 and 9 when turning on.** `SubscriptionManager.isValidSubscriptionId` only exists from Android 10 (API 29). On older phones with a SIM it throws `NoSuchMethodError`, which the surrounding code does not catch. Found by lint during M0-06 and fixed there with an equivalent check. Confirm against Play vitals crash reports for Android 8 and 9. | `telephony/AndroidCellularCallMonitor.kt` line 141. |

Note on R3: whether Android 12 and later allow a restarted sticky service to
return to the foreground could not be confirmed from code alone. It is a device
test in milestone M1. The finding stands either way, because the timeout path
and the boot path have the same effect.

## 4. Interface and ease of use findings

| ID | Finding | Evidence |
|----|---------|----------|
| U1 | **No first run experience.** A new user lands on a card that says "Set up Flip to Mute" with no picture of what the app does. | `ui/screens/home/HomeScreen.kt`. |
| U2 | **Setup takes too many steps.** Bottom sheet, then a setup list, then an explanation dialog per permission, then the system screen, then a separate "Finish setup" button. | `ui/screens/permissions/PermissionsScreen.kt`. |
| U3 | **Home has no visual hierarchy.** The main control is a small switch inside a text card. Pause, Resume and Turn off are text buttons. | `MainStatusCard` in `HomeScreen.kt`. |
| U4 | **Confusing action choice.** Two checkboxes where one must stay ticked. Unticking the last one is silently ignored. In practice "mute + vibrate" and "vibrate only" both end with a vibrating phone. | `HomeViewModel.updateSelection`, `FlipMonitoringCoordinator.handleOrientationState`. |
| U5 | **Technical wording.** "Monitoring", "Checking monitoring", "Restoring your monitoring state…", "Preparing call monitoring." | `app/src/main/res/values/strings.xml`. |
| U6 | **Settings and About are hidden** in an overflow menu. | `HomeScreen.kt`. |
| U7 | **Settings has wrong and duplicate content.** It shows "App Version 1.0" from a hardcoded string while the real version is 1.7. It repeats About. "Monitoring Status" repeats the home screen. | `SettingsScreen.kt` lines 402 to 482, `strings.xml` (`app_version_value`). |
| U8 | **Diagnostics are three separate technical screens** with raw sensor values and no guided order. | `ui/screens/sensor_test/`, `call_state_test/`, `sound_control_test/`. |
| U9 | **An expanding ad on every screen**, including permission setup and diagnostics. | `MainActivity.kt`. |
| U10 | **Errors do not say what to do.** Most failures show "Flip to Mute could not start." in a snackbar that disappears. | `HomeScreen.kt`. |
| U11 | **No proof that it works.** The user cannot see whether a call was ever silenced. | No counters anywhere. |
| U12 | **No brand identity.** Stock Material colours and typography. On Android 12 and later, wallpaper colours replace the app palette completely. | `ui/theme/`. |
| U13 | **English only, with hardcoded text.** "No email app found", other app names and descriptions are in Kotlin. Several string resources are unused. | `IntentUtil.kt`, `OtherAppItem.kt`. |
| U14 | **No layout rules for tablets or landscape.** Content stretches edge to edge. | All screens. |
| U15 | **No adaptive launcher icon.** Only legacy `webp` icons are shipped. There is no `mipmap-anydpi-v26` definition, so launchers cannot shape the icon and Android 13 themed icons are unavailable. The vector `ic_launcher_foreground.xml` is the unused Android Studio template. | `app/src/main/res/`. |

## 5. Retention findings

| ID | Finding |
|----|---------|
| T1 | **Silent stopping is the largest leak.** R1 to R4 mean the app stops working without telling anyone. The user notices weeks later when a call rings out loud in a meeting, and uninstalls. |
| T2 | **Pause has no end.** A user who pauses for one meeting stays paused until they remember to resume. |
| T3 | **Daily use features are buried.** Flip to lock and the Quick Settings tile live deep in Settings. |
| T4 | **No moments that bring people back.** No rating prompt outside About, no "what's new" after an update, no in-app update prompt. |
| T5 | **Nothing is measured.** Without events there is no setup funnel, no activation rate and no way to know whether v2.0 improved anything. |

## 6. Rating today

There is no rating card in the current code or in git history. Rating exists
as one row in the About screen:

- Label "Rate us", description "Support Flip to Mute with a review".
- Tap calls `IntentUtil.openRateUs`, which opens the Play Store listing with a `market://details?id=` link and falls back to the `https://play.google.com` link.
- There is no automatic prompt, no star pre-question, no in-app review API and no "shown" state stored.

`UPDATE_PLAN.md` section 8 defines how this is kept in v2.0, and how Secret Calculator handles rating.

## 7. What could not be checked

- **Play Store listing and Play Console data.** The public listing could not be read from this environment. Rating, install count, crash rate, ANR rate, retention and user reviews are unknown. They are needed as the baseline in `UPDATE_PLAN.md` section 2.
- **Behaviour on real devices.** Nothing was run on a phone. All findings come from reading code and running JVM unit tests.
