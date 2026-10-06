# Flip to Mute v2.0 update plan

Status: draft for owner review. Written 2026-10-06 against `master` at `a39167c`.
Updated the same day to build on Secret Calculator's architecture (decision D9).

Finding IDs such as R1, U4 and T2 refer to `CURRENT_STATE_AUDIT.md`.
Pattern IDs such as A1 and X3 refer to `ARCHITECTURE.md`.
Task IDs such as M1-03 refer to `MILESTONES.md`.

## 1. Goals

| # | Goal | What it means in practice |
|---|------|---------------------------|
| G1 | **More stable** | The service keeps running through reboots, app updates and battery managers. When it cannot run, the user is told and can fix it in one tap. Crashes and failures are visible to us. |
| G2 | **Better design** | The same visual language as Secret Calculator, a clear main control, proper dark theme, motion that explains the gesture. |
| G3 | **Simpler to use** | A new user is protected within one minute. One decision on the home screen. No technical words. |
| G4 | **Better retention** | Fewer silent failures, visible proof of value, a pause that ends by itself, a reason to come back after updates. |
| G5 | **Used more often** | Daily use features such as Flip to lock and the Quick Settings tile are discovered instead of buried. |
| G6 | **Rating unchanged** | "Rate us" behaves as it does today. The automatic review prompt follows Secret Calculator's tested policy (section 7). |
| G7 | **One proven architecture** | Flip to Mute is built the same way as Secret Calculator, so tested patterns are reused instead of new ones being invented. |

### Non-goals for v2.0

- No rewrite of the detection core, the ringer recovery or the service state machine. They move into the new structure unchanged.
- No new permissions beyond what 1.7 already declares.
- No new flip triggers such as internet calls, media or Do Not Disturb. They are in `FUTURE_FEATURES.md`.
- No paid tier and no fingerprint feature. Both are in `FUTURE_FEATURES.md`, to be built with Secret Calculator's code.

## 2. Success metrics

Baselines must be copied from Play Console and Firebase before M1 starts
(task M0-11). Targets are proposals and should be adjusted once baselines are known.

| Metric | Source | Baseline (1.7) | Target (2.0, 30 days after full rollout) |
|--------|--------|----------------|------------------------------------------|
| Crash-free users | Play vitals, Crashlytics | to fill | 99.5% or better |
| User-perceived ANR rate | Play vitals | to fill | under 0.2% |
| Setup completion: first open to all access granted | `Funnel` | not measured | 65% or better |
| Activation: turned on in first session | `Funnel` | not measured | 60% or better |
| Still on 7 days after activation | Analytics | not measured | 70% or better |
| Day 1, day 7, day 30 retention | Play Console, `Funnel` | to fill | 20% relative improvement |
| Uninstalls within 7 days | Play Console | to fill | 20% relative reduction |
| Average rating | Play Console | to fill | 4.3 or +0.2, whichever is higher |
| Flip to lock enabled among active users | Analytics | not measured | 15% or better |

## 3. Scope at a glance

| Pillar | In v2.0 | Not in v2.0 |
|--------|---------|-------------|
| Stability | Resume after update and reboot, keep user intent on failed auto start, "stopped" alert, health check, battery guidance, crash reporting, backup fix, intent crash guards, CI, green tests | Server side kill switches for the service |
| Architecture | Hilt, Secret Calculator activity shell, routes, screen pattern, stores, analytics funnel, test tooling, tool chain versions | Multi-module split |
| Design | Secret Calculator design kit, themes and fonts; new home, first run, settings, troubleshoot, about; dark theme; adaptive width | Widgets, icon packs |
| Simplicity | Three step first run, single action choice, plain wording, guided troubleshooting | Tutorial videos |
| Retention | Flip counter, timed pause, discovery cards, update and what's new dialogs, in-app review policy | Weekly recap notifications, purchases |
| Ads | Consent, Remote Config switches, calmer placement | App open and interstitial ads |
| Reach | Translation ready strings, Hindi as a stretch goal | More languages |

## 4. Pillar A: stability and reliability

This pillar ships first, on today's code, exactly like Secret Calculator's 0.31
hotfix came before its migration. A beautiful app that silently stops is still
uninstalled.

### 4.1 Keep running, or say so

| Work item | Fixes | Approach |
|-----------|-------|----------|
| **Resume after app update** | R1 | Add a manifest receiver for `ACTION_MY_PACKAGE_REPLACED`. It reuses the boot path in `boot/BootMonitoringCoordinator.kt` with a new source value. If the user had the service on and not paused, start it. If the start is rejected, post the "stopped" alert. |
| **Resume after reboot by default** | R2 | Read `startAfterPhoneRestart` as true when the key is absent. Users who explicitly turned it off keep their choice. Mention it in the "what's new" dialog. |
| **Never erase the user's choice on an automatic failure** | R3 | Give every start a source: user, boot, update, system restart, health check. Only a user initiated failure or a real loss of access may write `monitoringEnabled = false`. For other sources keep the flag, publish an "interrupted" error state and post the alert. |
| **"Flip to Mute stopped" alert** | R3, T1 | A second notification channel named Alerts at default importance. Text: "Flip to Mute stopped. Tap to turn it back on." Tapping opens the app, which is then allowed to start the service. Shown at most once per interruption. |
| **Health check** | R3, R4, T1 | A periodic `CoroutineWorker`, about every six hours, built like Secret Calculator's `TrashCleanupWorker` (A19). If the user wants the service on, it is not paused, and it is not running, try to start it. If Android rejects the background start, post the alert. |
| **Battery guidance** | R4 | A "Keep it running" screen. A `fun interface` reads `PowerManager.isIgnoringBatteryOptimizations` (A2). The button opens the system battery optimisation list with `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, which needs no permission. Below it, short manufacturer specific steps chosen by `Build.MANUFACTURER`, with an "Open app info" button. Only public intents, each guarded. |
| **Device verification of sticky restart** | R3 | Test on Android 12 to 16 that a process kill brings the service back to the foreground. Record results in the device matrix. |

### 4.2 See what happens in the field

| Work item | Fixes | Approach |
|-----------|-------|----------|
| **Crash reporting** | R5 | Firebase Crashlytics with its Gradle plugin, as in Secret Calculator (A15). Record a non-fatal for each `MonitoringFailure` with the start source, Android version and manufacturer as keys. Never attach call data. |
| **Analytics** | R5, T5 | Secret Calculator's `AnalyticsLogger` and `Funnel` (A14). Events in section 9. |
| **Report a problem** | U10 | `IntentUtil.sendSupportMail` from Secret Calculator, plus an automatic summary: app version, device, Android version, access status, battery status, last failure reason. No personal data. |

### 4.3 Remove known defects

| Work item | Fixes | Approach |
|-----------|-------|----------|
| Guard every external intent | R7, X3 | One guarded helper used by rate, share, email, Instagram, WhatsApp and Play Store links. Secret Calculator has the same gap, so the fix should be back-ported there. |
| Make flip feedback real | R8 | When the flip action is applied and the setting is on, play one short haptic tick. |
| Fix backup | R6, A20, X4 | `android:allowBackup="false"` as in Secret Calculator, plus an explicit device-transfer exclude so Android 12 and later cannot copy the state to a new phone. |
| Faster detection | R11 | Register the sensor at `SENSOR_DELAY_UI` while a call rings. Keep the 400 ms stability window. Secret Calculator uses the game rate for its face-down gesture because some Motorola phones report too few readings at the UI rate. Test a Motorola if one is available and move to the game rate if needed. |
| Clean dead code | R12, U7, U13 | Use `onboardingCompleted` for the new first run. Remove the hardcoded version string, unused strings and duplicate URL constants. Move hardcoded text to resources. |
| No white flash | R13 | Light and dark window themes, set before `super.onCreate` as Secret Calculator does. |

### 4.4 Release safety net

| Work item | Fixes | Approach |
|-----------|-------|----------|
| Green unit tests on Windows and Linux | R10 | Fix the five DataStore tests by giving each test its own file and closing the scope before the file is reopened. |
| Test tooling | R10, A21 | Robolectric, Turbine, `androidx.test:core` and `TrackedViewModels`, as in Secret Calculator. |
| Continuous integration | R10 | GitHub Actions on every pull request to `release/v2.0`: unit tests, lint, `assembleRelease`. |
| Tester builds | A23 | Firebase App Distribution for debug builds, as in Secret Calculator. |
| UI tests for the critical flow | R10 | Compose tests for first run, turning on, pausing and the review policy. |
| Device matrix | R3, R4 | Manual test script on at least one phone each from Samsung, Xiaomi and one of Oppo, Vivo or Realme, plus a Pixel or emulator for Android 16. |

## 5. Pillar B: architecture alignment

Full detail is in `ARCHITECTURE.md`. In short, Flip to Mute takes Secret
Calculator's structure: Hilt, one `AppCompatActivity` shell with an
onboarding activity, a `Routes` object and `AppNavHost`, the Route, Screen,
Actions, ViewModel and UiState pattern, small injected stores, the analytics
funnel, the review policy, the ad configuration, Remote Config switches, the
design kit, themes and fonts, and the same test tools.

The migration is its own milestone, M2, with behaviour unchanged. It follows
Secret Calculator's rule that migration, redesign and features never ship in
the same step.

Three things are kept from Flip to Mute because they are stronger for this app:
the DataStore repositories that protect the ringer state (X2), Ads Lite for
16 KB page support (X6), and the newer library versions (X7). Four gaps found
in Secret Calculator are closed in Flip to Mute and should be back-ported:
unguarded intents (X3), device-to-device transfer (X4), missing ad consent (X5)
and the global preference object that needs an activity first (X1).

## 6. Pillar C: redesign

Full detail is in `DESIGN_SPEC.md`. Summary:

| Area | Today | v2.0 |
|------|-------|------|
| First run | None (U1) | Onboarding activity: Welcome, Access, Try it |
| Setup | Sheet, list, dialogs, finish button (U2) | One screen, one primary button that walks through the three steps |
| Home | Text card with a small switch (U3) | Large on and off control with a phone illustration, state colour and one sentence of status |
| Action choice | Two checkboxes (U4) | One selector with two options: Silence, Vibrate |
| Navigation | Overflow menu (U6) | Visible Settings icon; About as its own screen like Secret Calculator |
| Settings | Seven groups with duplicates (U7) | Five groups in Secret Calculator's settings style: Flip behaviour, More gestures, Keep it running, Appearance, Help and about |
| Diagnostics | Three technical screens (U8) | One guided "Check my setup" flow that reuses the three existing view models |
| Errors | Passing snackbar (U10) | A "needs attention" card on Home with one fix button |
| Look | Stock Material (U12) | Secret Calculator design kit, Outfit headings, Flip to Mute colour themes, flip animation |
| Wording | "Monitoring" (U5) | "Flip to Mute is on", "off", "paused until 3:30 PM" |

## 7. Pillar D: retention and frequency of use

| Feature | Fixes | Description |
|---------|-------|-------------|
| **Reliability work in section 4.1** | T1 | The largest retention gain in this release. |
| **Flip counter** | U11 | Home shows "Silenced 14 calls this month" and "Last flip: today 2:10 PM". Stored on the device only as counts and one timestamp. No numbers, names or call details. Shown once at least one flip has happened. |
| **Timed pause** | T2 | Pause offers 30 minutes, 1 hour, 2 hours and "until I turn it back on". During a timed pause the foreground service stays alive and ignores calls until the end time, so no alarm permission and no background start is needed. The notification and the tile show the end time. |
| **Discovery cards** | T3, G5 | Secret Calculator's `HintStore` pattern (A12). One dismissible card at a time: "Lock your screen with a flip", "Add the Quick Settings tile", "Keep it on after a restart". |
| **Update and what's new dialogs** | T4 | Secret Calculator's home dialog queue (A13). "Update available" when Remote Config reports a newer version code, then "what's new" once per version. |
| **Review prompt** | T4 | Section 8. |
| **Health card** | T1 | When access was lost or battery restrictions are detected, Home shows what is wrong and a single fix button. |
| **In-app update prompt** (stretch) | T4 | Flexible update flow from the Play library, only if time allows in M7. |

Frequency of use in v2.0 comes from making Flip to lock and the tile visible.
New triggers that would raise daily use further are left for v2.1 and later.
They are ranked in `FUTURE_FEATURES.md`.

## 8. Rating

### What exists today

| App | Manual | Automatic |
|-----|--------|-----------|
| Flip to Mute 1.7 | "Rate us" row in About opens the Play Store listing through `IntentUtil.openRateUs` | None |
| Secret Calculator 2.0.0 | Identical "Rate us" in About | Play In-App Review, asked by `ReviewPolicy` |

Secret Calculator used to show a five star "rate card" on its home screen.
Five stars opened the Play Store, fewer opened the feedback email, and "Rate
later" brought it back after three launches. It was removed in commit
`038a72e` and replaced by Play In-App Review. Google's review guidelines ask
apps not to ask for the user's opinion before showing the review prompt, which
the star card did.

### What v2.0 does (default for decision D1)

- **"Rate us" in About stays exactly as today.** One tap opens the Play Store listing with the `market://` link and the web link as fallback, now with the crash guard.
- **Secret Calculator's `InAppReview` and `ReviewPolicy` are copied unchanged** (A18):

| Rule | Value |
|------|-------|
| Launch | Only on every 3rd launch, and at most once in that launch |
| Value moments | At least 3 before the first ask |
| Maximum asks | 3 in total |
| Ads | Never within 3 minutes of a full-screen ad |
| Timing | 700 ms after the screen settles, only while the app is in the foreground |
| Google quota | Play may still decide not to show the sheet |

- **Value moments in Flip to Mute** are a call silenced or switched to vibrate by a flip, and a "Check my setup" run with no problems. Flips happen in the background, so they are counted in a store and the policy is checked the next time Home is opened.
- **Never asked** during the first run, while an attention card is showing, or while paused.

If you meant Secret Calculator's old star card when you wrote "rating card",
say so and the Home placement will use that card instead. The plan does not
recommend it, for the guideline reason above.

## 9. Ads, consent and remote switches

| Topic | Today | v2.0 |
|-------|-------|------|
| App ID | Google sample ID in `strings.xml` (R9) | Production ID in the manifest, as Secret Calculator does, confirmed by the owner |
| Structure | One composable banner | Secret Calculator's `AdConfig` with unit ids and caps, and one banner created by the activity with three retries (A17) |
| SDK | Ads Lite 25.0.0 | Kept (X6) |
| Initialisation | Main thread at app start | Background thread, after consent (X10) |
| Consent | None | Google User Messaging Platform. "Privacy options" row in Settings when required (X5). |
| Remote switches | None | Remote Config flags per banner placement and the latest version code, following `RemoteAdGate` (A16, D10). Safe defaults. |
| Placement | Collapsible banner on every screen (U9) | **Proposed:** no ads in onboarding, access setup and Check my setup, the way Secret Calculator keeps ads off its calculator and PIN screens. Banner on Home, Settings and About. Collapsible variant on Home only. |
| Full-screen ads | None | None. If added later, Secret Calculator's managers and guardrails are reused. |

Remote Config keys, created in the Firebase console for v2.0 (M7-04). Until a key exists, the default in the app applies.

| Key | Type | Default | Effect |
|-----|------|---------|--------|
| `ad_banner_home_enabled` | Boolean | true | Banner on Home |
| `ad_banner_settings_enabled` | Boolean | true | Banner on Settings |
| `ad_banner_about_enabled` | Boolean | true | Banner on About |
| `ad_banner_keep_running_enabled` | Boolean | true | Banner on Keep it running |
| `ad_banner_check_setup_enabled` | Boolean | true | Banner on Check my setup |
| `latest_play_store_version_code` | Number | -1 | Home shows the update dialog when this is higher than the installed version code |

No banner shows until consent allows ads and the first fetch of the session finishes, so a switched-off banner never flashes. Debug builds use Google's sample banner unit.

## 10. Analytics and crash reporting

`Funnel` milestones, each logged once per install, following Secret Calculator:

| Event | When |
|-------|------|
| `setup_complete` | All three access steps granted for the first time. Starts the funnel. |
| `onboarding_complete` | First run finished, with `try_flip` = done or skipped |
| `first_flip` | The first call silenced or vibrated by a flip |
| `return_d1`, `return_d7` | App opened one day and seven days after setup |

Other events, logged through `AnalyticsLogger`:

| Event | Parameters | Purpose |
|-------|------------|---------|
| `access_result` | `type` (phone, sound, notifications), `granted` | Which access loses users |
| `service_state_changed` | `state` (on, off, paused), `source` (home, tile, notification, boot, update, health) | Activation and control surfaces |
| `flip_applied` | `action` (silence, vibrate), `flat_only`, `pocket_protection` | Core value delivered |
| `service_interrupted` | `reason`, `source` | Reliability in the field |
| `auto_resume` | `source` (boot, update, health), `success` | Whether section 4.1 works |
| `battery_guidance` | `action` (shown, opened_settings, exempted) | Whether guidance helps |
| `feature_toggled` | `feature`, `enabled` | Adoption |
| `discovery_card` | `card`, `action` (shown, accepted, dismissed) | Whether discovery works |
| `rate_us_tapped` | none | Manual rating use |
| `ad_event` | as in Secret Calculator | Ad health |

No phone numbers, contact names, call times or call durations are ever logged.
Crashlytics receives crashes plus one non-fatal per `service_interrupted`.

## 11. Technical approach

### Architecture

See `ARCHITECTURE.md`. It defines the target structure, the package layout and
every deviation from Secret Calculator.

### New components

| Component | Package | Source |
|-----------|---------|--------|
| `AppModule`, `RepositoryModule`, `ApplicationScope` | `di/` | Secret Calculator pattern |
| `MainActivity` shell, `OnboardingActivity` | `ui/activities/` | Secret Calculator pattern |
| `Routes`, `AppNavHost` | `ui/navigation/` | Secret Calculator pattern |
| `NovaDesign`, `States`, `TopBars` | `ui/components/` | Copied from Secret Calculator |
| `Appearance`, `AppTheme`, `ThemeMode`, fonts | `ui/theme/` | Copied from Secret Calculator |
| `HintStore` | `ui/common/` | Copied from Secret Calculator |
| `AnalyticsLogger`, `Funnel` | `data/analytics/` | Copied, new milestones |
| `InAppReview`, `ReviewPolicy` | `data/review/` | Copied unchanged |
| `IntentUtil`, `OtherApps`, `PackageManagerExt` | `utils/about_utils/` | Copied, plus guards |
| `AdConfig`, remote switches, consent | `utils/ads/` | Copied pattern, plus consent |
| `HealthCheckWorker` | `workers/` | Secret Calculator worker pattern |
| `PackageReplacedReceiver` | `boot/` | New, shares `BootMonitoringCoordinator` |
| Alerts channel and "stopped" alert | `notification/` | New |
| Battery status and manufacturer steps | `data/reliability/` | New |
| Flip stats store | `data/stats/` | New |

### Dependencies

| Dependency | Why | Same as Secret Calculator |
|------------|-----|---------------------------|
| Hilt, Hilt navigation compose, KSP | Dependency injection | Yes |
| `firebase-crashlytics` and plugin | Crash visibility | Yes |
| `firebase-config` | Remote switches | Yes |
| `play-review-ktx` | In-app review | Yes |
| `ui-text-google-fonts` | Outfit font | Yes |
| `androidx.appcompat` | `AppCompatActivity` shell | Yes |
| `work-runtime-ktx` | Health check | Yes |
| `user-messaging-platform` | Ad consent | No, added for X5 |
| Robolectric, Turbine, `androidx.test:core` | Tests | Yes |
| App Distribution plugin | Tester builds | Yes |
| `play-services-ads-lite` | Ads | No, kept for X6 |
| `app-update-ktx` | In-app update | Stretch only |

### Data and migration for existing users

| Item | Rule |
|------|------|
| Existing settings | All current DataStore keys keep their names and meaning (X2). |
| First run | Skipped when access is already complete or the service flag is on. `onboarding_completed` is set to true for these users. |
| Action choice | Stored as today. Any saved selection with vibrate ticked shows as Vibrate. Mute only shows as Silence. The service logic is untouched. Check on a device that the single Vibrate option behaves like today's vibrate selections. |
| Start after restart | Absent key now reads as true. An explicit false is respected. |
| New stores | Small injected stores, Secret Calculator style: appearance, hints, review, funnel, flip stats, what's new version, pause end time. |
| Update moment | The package replaced receiver restarts the service without the user opening the app. On next open, the "what's new" dialog appears once. |

## 12. Release strategy

| Topic | Plan |
|-------|------|
| Order | As in Secret Calculator: reliability fixes on today's code (M1, can ship as 1.8), then migration with behaviour unchanged (M2), then redesign and features (M3 to M7). |
| Version | versionName `2.0.0`, versionCode `9`. A 1.8 release would use versionCode 9 and v2.0 would move to 10. |
| Branch | `release/v2.0`, cut from `master` at `a39167c`. Pull requests target this branch. It merges to `master` at release and is tagged `v2.0.0`. |
| Testers | Every milestone from M1 onward goes to the App Distribution tester group and the internal track. |
| Closed beta | At the end of M7, for at least one week. The WhatsApp and Instagram community is the natural tester pool. |
| Production | Staged: 5%, 20%, 50%, 100%. Hold at least two days per stage. |
| Gate to advance a stage | Crash-free users at 99.5% or better, ANR under the Play bad behaviour threshold, `auto_resume` success above 90%, no new cluster of one star reviews. |
| Rollback | Halt the rollout in Play Console, turn off risky ad placements through Remote Config, and ship `2.0.1`. Android cannot downgrade users, so the gate matters. |
| Store listing | New screenshots, feature graphic and short description that match the redesign. Data safety form updated for Crashlytics and Remote Config. Privacy policy link live and reachable from Settings. |

## 13. Decisions needed from the owner

| # | Decision | Default if no answer | Blocks |
|---|----------|----------------------|--------|
| D1 | Rating: keep "Rate us" unchanged and add Secret Calculator's in-app review policy, or bring back the old star card | "Rate us" unchanged plus the review policy (section 8) | M6-07 |
| D2 | Production AdMob App ID | Release is blocked | M7-01 |
| D3 | Add Crashlytics and update Data safety | Add it, as in Secret Calculator | M0-07 |
| D4 | Reduced ad placement in section 9 | **Decided 2026-10-06 by the owner: banner at the bottom of every screen** | M7-03 |
| D5 | Start after restart on by default | On by default | M1-02 |
| D6 | Play Console baseline numbers for section 2 | Targets stay as proposals | M0-11 |
| D7 | Privacy policy URL | **Decided 2026-10-06: https://sites.google.com/view/fliptomute/home** | M6-06 |
| D8 | Hindi translation in v2.0 | Not included | M7-08 |
| D9 | Build on Secret Calculator's architecture | **Decided 2026-10-06 by the owner: yes** | |
| D10 | Bring back Firebase Remote Config for ad switches and the update dialog, as in Secret Calculator. 1.x removed it in commit `fcf4f3a`. | Bring it back | M7-04 |

## 14. Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| The Hilt migration breaks the service, receivers or tile | Medium | High | M2 changes structure only, keeps behaviour, and must pass all 187 tests plus the regression script before M3 starts. |
| Two process-wide state holders after the migration | Low | High | The monitoring state repository must be a Hilt `@Singleton`. A test in M2-05 proves the service, tile and screens share one instance. |
| Existing users dislike a changed interface | Medium | Medium | Keep the main switch in the same place, preserve every setting, show "what's new", staged rollout with review monitoring. |
| Reliability changes introduce a new service bug | Medium | High | Reliability lands first in M1 with unit tests per start source. Long beta. |
| Background start limits differ by Android version and manufacturer | High | Medium | The design assumes the start can be refused and always falls back to the alert. Device matrix in M1, M2 and M8. |
| Target SDK 37 changes service behaviour | Medium | High | Done last, as its own task, with the device matrix run again (X11). |
| Play policy review of the foreground service or new notification | Low | High | The service type and its declaration are unchanged. The alert is user benefit only and not promotional. |
| Consent flow lowers ad revenue in the EEA | Medium | Low | Required by AdMob policy in any case. |
| Scope creep | High | Medium | Everything new goes to `FUTURE_FEATURES.md`. v2.0 scope is frozen at the end of M0. |
| Single developer capacity | Medium | Medium | M0 and M1 can ship alone as 1.8 if the rest slips. |

## 15. Fallback release

If the migration or redesign takes longer than planned, M0 and M1 can ship
alone as version 1.8. That release carries the reliability fixes, crash
reporting and the intent guards with the current interface. The decision point
is the end of M1.
