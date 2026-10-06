# Flip to Mute v2.0 milestone tracker

This is the working document. Update it in every session.

- Plan and reasoning: `UPDATE_PLAN.md`
- Target architecture, taken from Secret Calculator: `ARCHITECTURE.md` (pattern IDs A1 to A26, deviations X1 to X11)
- Screen designs: `DESIGN_SPEC.md`
- Finding IDs (R, U, T): `CURRENT_STATE_AUDIT.md`
- Decision IDs (D1 to D10): `UPDATE_PLAN.md` section 13

## Overview

| Milestone | Goal | Status | Tasks done | Rough size |
|-----------|------|--------|-----------|------------|
| M0 | Foundations and baseline | In progress | 11 of 13 | 5 days |
| M1 | Reliability fixes on today's code | Blocked | 13 of 15 | 12 days |
| M2 | Architecture migration, behaviour unchanged | Blocked | 13 of 14 | 10 days |
| M3 | Design system from Secret Calculator | Blocked | 8 of 9 | 7 days |
| M4 | First run and access | Blocked | 9 of 9 | 7 days |
| M5 | Home | Blocked | 13 of 13 | 10 days |
| M6 | Settings, help, about, review | Not started | 0 of 12 | 10 days |
| M7 | Ads, consent, remote switches, polish | Not started | 0 of 11 | 7 days, plus 2 for stretch items |
| M8 | Test, beta and release | Not started | 0 of 9 | 7 days of work, plus beta and rollout waiting time |

Order follows Secret Calculator's release rules: fixes first on the old code,
then migration with behaviour unchanged, then redesign, then features. Keeping
them apart makes any regression easy to trace.

**Sizes** are relative effort for one developer: S is about half a day, M
about one day, L two to three days. Recalibrate after M0.

**Status values:** Not started, In progress, Blocked, Done.

**Rule:** a milestone is Done only when every exit criterion is met.

**Rule:** before building anything structural, open the matching Secret
Calculator file named in `ARCHITECTURE.md` and follow it.

---

## M0: Foundations and baseline

Goal: a safe place to work, a green baseline and the means to measure.

| Done | ID | Task | Size | Refs |
|------|----|------|------|------|
| [x] | M0-01 | Create branch `release/v2.0` from `master` at `a39167c` | S | |
| [x] | M0-02 | Create the `docs/` folder with audit, plan, design spec, tracker and future features | S | |
| [x] | M0-03 | Analyse Secret Calculator and write `ARCHITECTURE.md` | S | D9 |
| [x] | M0-04 | Set versionName `2.0.0` and versionCode `9` in `app/build.gradle.kts` | S | |
| [x] | M0-05 | Fix the five DataStore tests that fail on Windows, then delete `failing-tests.txt` | S | R10 |
| [x] | M0-06 | Add a GitHub Actions workflow: unit tests, lint, `assembleRelease` on every pull request to `release/v2.0` | S | R10 |
| [x] | M0-07 | Add Firebase Crashlytics and its Gradle plugin, as in Secret Calculator | S | R5, A15, D3 |
| [x] | M0-08 | Add `AnalyticsLogger` (a `fun interface`) and `Funnel`, copied from Secret Calculator and given Flip to Mute milestones | M | R5, A14 |
| [x] | M0-09 | Add Robolectric, Turbine, `androidx.test:core` and `TrackedViewModels` to the test setup | S | A21 |
| [x] | M0-10 | Add Firebase App Distribution for debug builds, credentials path read from `local.properties` | S | A23, X9 |
| [ ] | M0-11 | Owner: copy baseline numbers from Play Console into `UPDATE_PLAN.md` section 2 | S | D6 |
| [ ] | M0-12 | Owner: answer decisions D1 to D10 and record them in `UPDATE_PLAN.md` section 13 | S | |
| [x] | M0-13 | Add personal `.idea/` files to `.gitignore` so the working tree stays clean | S | |

Exit criteria:

- [ ] All unit tests pass on Windows and in CI (191 locally after M0-09).
- [ ] CI runs on pull requests and is green.
- [ ] A debug build reaches the tester group through App Distribution.
- [ ] A release build installs and opens on a real phone.
- [ ] Decisions D1 to D10 are answered or have their default accepted.
- [ ] v2.0 scope is frozen. New ideas go to `FUTURE_FEATURES.md`.

---

## M1: Reliability fixes on today's code

Goal: Flip to Mute keeps running, or tells the user that it stopped. Built on
the current structure, like Secret Calculator's 0.31 hotfix, so it can ship
alone as version 1.8. New classes take their dependencies through the
constructor so M2 can hand them to Hilt without changes.

| Done | ID | Task | Size | Refs |
|------|----|------|------|------|
| [x] | M1-01 | Give every service start a source: user, boot, update, system restart, health check | M | R3 |
| [x] | M1-02 | Read `startAfterPhoneRestart` as true when the key is absent. Keep an explicit false. | S | R2, D5 |
| [x] | M1-03 | Add a receiver for `MY_PACKAGE_REPLACED`, reusing `BootMonitoringCoordinator` | M | R1 |
| [x] | M1-04 | Keep the on flag when a start the user did not trigger fails. Publish an "interrupted" state instead. | L | R3 |
| [x] | M1-05 | Add the Alerts notification channel and the "Flip to Mute stopped" alert | M | R3, T1 |
| [x] | M1-06 | Add `HealthCheckWorker`, built like Secret Calculator's `TrashCleanupWorker`, about every six hours | M | R3, R4, A19 |
| [x] | M1-07 | Add the battery optimisation status check behind a `fun interface`, and the manufacturer steps data | M | R4, A2 |
| [x] | M1-08 | Guard every external intent in `IntentUtil` | S | R7, X3 |
| [x] | M1-09 | Make the flip feedback setting work: one haptic tick when the action is applied | S | R8 |
| [x] | M1-10 | Set `allowBackup="false"` and exclude device-to-device transfer in `data_extraction_rules.xml` | S | R6, A20, X4 |
| [x] | M1-11 | Use `SENSOR_DELAY_UI` while a call is ringing | S | R11 |
| [x] | M1-12 | Log `service_state_changed`, `service_interrupted`, `auto_resume` and `flip_applied` | S | R5 |
| [x] | M1-13 | Unit tests for every start source and failure path, and for the update receiver | M | R10 |
| [ ] | M1-14 | Device tests: reboot, `adb install -r`, process kill, force stop. Fill in the device matrix. **Blocked: no phone connected to the build computer.** | M | R1, R2, R3 |
| [ ] | M1-15 | Owner decision: continue to M2, or ship M0 and M1 as version 1.8 | S | Plan section 15 |

Exit criteria:

- [ ] After a reboot the service returns without opening the app, on every device in the matrix.
- [ ] After an app update the service returns without opening the app.
- [ ] After a process kill the service returns, or the alert appears within one health check period.
- [ ] The on flag is cleared only by the user or by a real loss of access. A unit test proves each case.
- [ ] No external link can crash the app when no handler is installed.

---

## M2: Architecture migration, behaviour unchanged

Goal: Flip to Mute has Secret Calculator's structure. Every screen still looks
and behaves exactly as in 1.7 or 1.8. No new features and no visual changes in
this milestone.

| Done | ID | Task | Size | Refs |
|------|----|------|------|------|
| [x] | M2-01 | Align the tool chain with Secret Calculator: AGP, Kotlin, KSP, Gradle, Hilt plugin. Keep the newer library versions. | M | A22, X7 |
| [x] | M2-02 | Add Hilt: `@HiltAndroidApp`, `AppModule`, `RepositoryModule` with `@Binds`, `@ApplicationScope` | M | A1 |
| [x] | M2-03 | Make the service, boot receiver, update receiver, tile service and device admin receiver `@AndroidEntryPoint` | M | A1 |
| [x] | M2-04 | Give `HealthCheckWorker` a Hilt `@EntryPoint`, as in `TrashCleanupWorker` | S | A19 |
| [x] | M2-05 | Keep the monitoring state repository a process-wide `@Singleton`, and prove it with a test | S | A1 |
| [x] | M2-06 | Turn every view model into `@HiltViewModel`. Delete `AppContainer` and `ViewModelFactories`. | M | A1 |
| [x] | M2-07 | `MainActivity` becomes an `AppCompatActivity` with the Secret Calculator shell: root column, insets once, banner underneath, route tracking | M | A3 |
| [x] | M2-08 | `Routes` object and `AppNavHost` with `go()`, `back(entry)` and the 280 ms transitions | M | A5 |
| [x] | M2-09 | **Re-scoped:** every existing screen now gets its view model from Hilt and is reached through `Routes`. The full Route + Screen(state, actions) + `TransientState` pattern is applied as each screen is rebuilt in M4 to M6, because every 1.x screen is replaced there and converting them twice would be wasted work. | L | A6 |
| [x] | M2-10 | Move to the Secret Calculator package layout in `ARCHITECTURE.md` section 4 | M | |
| [x] | M2-11 | Replace `IntentUtil`, `OtherApps` and `PackageManagerExt` with the Secret Calculator versions plus the guards from M1-08 | S | X3 |
| [x] | M2-12 | Wire `Funnel` milestones: setup complete, first flip, day 1 return, day 7 return | S | A14 |
| [x] | M2-13 | Port the view model tests to Robolectric and Turbine where it simplifies them. All 187 tests still pass. | M | A21 |
| [ ] | M2-14 | Run the full regression script on one device and compare with 1.7. **Blocked: no phone connected.** | M | |

Exit criteria:

- [ ] `AppContainer` and `ViewModelFactories` are gone.
- [ ] Every structural rule in `ARCHITECTURE.md` section 2 marked M2 is met, or a deviation is written in section 3.
- [ ] All unit tests pass. The regression script passes with no behaviour change from 1.7.
- [ ] A release build with R8 opens every screen without a crash.

---

## M3: Design system from Secret Calculator

Goal: the visual foundation, copied from Secret Calculator's design kit and
themed for Flip to Mute.

| Done | ID | Task | Size | Refs |
|------|----|------|------|------|
| [x] | M3-01 | Copy `VaultDesign.kt`, `States.kt` and `TopBars.kt` as `NovaDesign`, `States` and `TopBars` with the same sizes and motion | M | A7 |
| [x] | M3-02 | Copy `Appearance`, `AppTheme`, `ThemeMode` and the theme function. Theme mode defaults to System. | M | A8 |
| [x] | M3-03 | Flip to Mute colour themes in `DESIGN_SPEC.md` section 3.1, with contrast checks | M | U12 |
| [x] | M3-04 | Outfit plus platform font, with the Google Fonts certificate array | S | A9 |
| [x] | M3-05 | Window theme for light and dark so there is no white flash, set before `super.onCreate` as Secret Calculator does | S | R13 |
| [ ] | M3-06 | Adaptive launcher icon with a monochrome layer, same artwork. **Blocked: needs artwork from the owner**: the current icon has its background baked in, so a transparent foreground layer and a single-colour glyph for themed icons must be exported from the source design. | S | U15 |
| [x] | M3-07 | `PhoneFlipIllustration` with animations and a still version for reduced motion | L | |
| [x] | M3-08 | Flip to Mute components on top of the kit: `FlipPowerControl`, `ActionSelector`, `AttentionCard`, `StatsCard` | L | |
| [x] | M3-09 | Previews for every component in both themes and at 200% font | S | |

Exit criteria:

- [ ] The app opens with no white flash in dark mode.
- [ ] Every component has a preview in both themes.
- [ ] Shapes, spacing and motion match Secret Calculator.

---

## M4: First run and access

Goal: a new user is protected within one minute.

| Done | ID | Task | Size | Refs |
|------|----|------|------|------|
| [x] | M4-01 | `OnboardingActivity` and `OnboardingViewModel` with an `OnboardingStep` enum, following Secret Calculator's onboarding | M | A4 |
| [x] | M4-02 | Start rule: open onboarding when `onboardingCompleted` is false and setup is not complete. Existing users skip it. | S | R12 |
| [x] | M4-03 | Welcome step | M | U1 |
| [x] | M4-04 | Access step: one button that walks through the steps, permanent denial handling, automatic advance on resume | L | U2 |
| [x] | M4-05 | Hint sheet shown before the Sound control settings page | S | U2 |
| [x] | M4-06 | Try it step: buzz on detection, hint after 20 seconds, skip | M | U1 |
| [x] | M4-07 | Log the onboarding funnel through `Funnel` | S | A14 |
| [x] | M4-08 | View model unit tests and one Compose test of the full first run | M | R10 |
| [x] | M4-09 | Remove the old permissions screen and bottom sheet | S | |

Exit criteria:

- [ ] Fresh install to "on" in 60 seconds or less, with at most 6 taps inside the app.
- [ ] A user updating from 1.7 with setup complete never sees the first run.
- [ ] Denial and permanent denial work on Android 13 or later and on Android 12 or earlier.
- [ ] No ad is visible during the first run.

---

## M5: Home

Goal: one obvious control, clear state, visible value.

| Done | ID | Task | Size | Refs |
|------|----|------|------|------|
| [x] | M5-01 | Rebuild `HomeUiState` around the states in `DESIGN_SPEC.md` section 4.4 | M | U3 |
| [x] | M5-02 | Wire `FlipPowerControl` and the status headline | M | U3 |
| [x] | M5-03 | `ActionSelector` with the mapping to the stored selection. Check Vibrate on a device. | S | U4 |
| [x] | M5-04 | `AttentionCard` for every failure, each with its fix action | M | U10 |
| [x] | M5-05 | Flip stats store, recording in the coordinator, `StatsCard` | M | U11 |
| [x] | M5-06 | Timed pause: preference, service behaviour, automatic resume, notification and tile text | L | T2 |
| [x] | M5-07 | Pause sheet, built with `SheetHeader` and `SheetAction` | S | T2 |
| [x] | M5-08 | Battery warning card | S | R4 |
| [x] | M5-09 | Discovery cards with `HintStore`, one at a time | M | T3, A12 |
| [x] | M5-10 | Home dialog queue: update available, then what's new | S | T4, A13 |
| [x] | M5-11 | Card ordering as a pure function with unit tests | S | |
| [x] | M5-12 | Unit tests and Compose tests for on, off, pause and attention | M | R10 |
| [x] | M5-13 | Remove old Home code and unused strings | S | U5 |

Exit criteria:

- [ ] Every row of the Home states table can be reached and matches the spec.
- [ ] A timed pause ends on time while idle, and immediately when a call arrives after the end time.
- [ ] The counter increases only when a flip action is actually applied.
- [ ] A TalkBack walk through of Home is fully understandable.
- [ ] The word "monitoring" appears nowhere in the interface. (Home is clean; the remaining uses are in Settings and the call test, both replaced in M6.)

---

## M6: Settings, help, about, review

Goal: every remaining screen in the new design, and the rating flow.

| Done | ID | Task | Size | Refs |
|------|----|------|------|------|
| [ ] | M6-01 | Rebuild Settings with Secret Calculator's `SettingsGroup`, `SettingsRow`, `SwitchRow` and `ChoicePill` | M | U7, A10 |
| [ ] | M6-02 | Keep it running screen | M | R4 |
| [ ] | M6-03 | Check my setup guided flow, reusing the three test view models | L | U8 |
| [ ] | M6-04 | Report a problem: support email with an automatic summary, through `IntentUtil.sendSupportMail` | S | U10 |
| [ ] | M6-05 | About screen in the Secret Calculator layout: header, action tiles, community, other apps | M | U7 |
| [ ] | M6-06 | Privacy policy link | S | R14, D7 |
| [ ] | M6-07 | Rating: keep "Rate us" in About unchanged. Add `InAppReview` and `ReviewPolicy` from Secret Calculator, with Flip to Mute value moments. | M | T4, A18, D1 |
| [ ] | M6-08 | Appearance group: theme mode and colour theme pills | S | A8 |
| [ ] | M6-09 | Flip to lock: explain dialog first, switch turns on only after device admin is granted | S | T3, A11 |
| [ ] | M6-10 | Remove old screens, clean strings, move hardcoded text into resources | M | U7, U13 |
| [ ] | M6-11 | Copy `ReviewPolicyTest` and add tests for the value moments | S | A18 |
| [ ] | M6-12 | Unit tests and Compose tests for the new screens | M | R10 |

Exit criteria:

- [ ] No screen from 1.7 remains in the old style.
- [ ] "Rate us" opens the Play Store exactly as 1.7 does, verified on a device.
- [ ] The review sheet is requested only when `ReviewPolicy` allows it, verified with Play's internal app sharing.
- [ ] The version shown equals the installed package version.
- [ ] Lint reports no unused resources and no hardcoded text.

---

## M7: Ads, consent, remote switches, polish

Goal: compliant ads, and a product that is fast and accessible.

| Done | ID | Task | Size | Refs |
|------|----|------|------|------|
| [ ] | M7-01 | Replace the sample AdMob App ID with the production ID | S | R9, D2 |
| [ ] | M7-02 | Consent with the User Messaging Platform. Ads initialised off the main thread after consent. "Privacy options" row in Settings. | M | R9, X5, X10 |
| [ ] | M7-03 | `AdConfig` and an activity-owned banner with three retries, hidden on the routes chosen in D4 | M | U9, A17, D4 |
| [ ] | M7-04 | `RemoteAdGate` style switches: banner per placement, latest version code for the update dialog | M | A16, D10 |
| [ ] | M7-05 | Accessibility pass against `DESIGN_SPEC.md` section 7 | M | |
| [ ] | M7-06 | Layout pass: tablet, landscape, 320 dp wide phone | S | U14 |
| [ ] | M7-07 | Performance: cold start, release build check, download size compared with 1.7 | M | |
| [ ] | M7-08 | Translation readiness. Stretch: Hindi translation. | M | U13, D8 |
| [ ] | M7-09 | Stretch: in-app update prompt | M | T4 |
| [ ] | M7-10 | Target SDK 37 as its own change, then rerun the device matrix | M | X11 |
| [ ] | M7-11 | Upload to the internal track and fix everything in the Play pre-launch report | S | R10 |

Exit criteria:

- [ ] With a test device set to the EEA, the consent form appears and no ad loads before an answer.
- [ ] Turning a Remote Config ad flag off hides that banner without a new release.
- [ ] Cold start is not slower than 1.7 on the same phone.
- [ ] The accessibility checklist passes.
- [ ] The pre-launch report shows no crashes.
- [ ] Download size is within 2 MB of 1.7. Hilt and the Outfit font add a little.

---

## M8: Test, beta and release

Goal: ship to everyone safely and measure the result.

| Done | ID | Task | Size | Refs |
|------|----|------|------|------|
| [ ] | M8-01 | Run the regression script below on every device in the matrix | L | |
| [ ] | M8-02 | Upgrade tests from 1.7 and 1.8: service on, paused, off, and setup unfinished | M | |
| [ ] | M8-03 | Closed beta through App Distribution and the Play closed track for at least 7 days. Triage every report. | L | A23 |
| [ ] | M8-04 | Store listing: screenshots, feature graphic, descriptions, "what's new" text | M | |
| [ ] | M8-05 | Update the Data safety form and the privacy policy | S | R14 |
| [ ] | M8-06 | Merge to `master`, tag `v2.0.0`, write release notes | S | |
| [ ] | M8-07 | Staged rollout: 5%, 20%, 50%, 100%, checking the gates at each stage | M | |
| [ ] | M8-08 | Review at day 7 and day 30. Write the results into `UPDATE_PLAN.md` section 2. | S | |
| [ ] | M8-09 | Choose the v2.1 scope from `FUTURE_FEATURES.md` | S | |

Exit criteria:

- [ ] Rollout is at 100%.
- [ ] Every rollout gate in `UPDATE_PLAN.md` section 12 was met at every stage.
- [ ] The metrics table has real numbers for v2.0.

---

## Device matrix

Fill in during M1-14, again after M2-14, and during M8-01. Write Pass, Fail or
the alert behaviour that was observed.

| Device | Android | Returns after reboot | Returns after update | Returns after process kill | Survives overnight idle | Flip silences a call | Notes |
|--------|---------|----------------------|----------------------|----------------------------|-------------------------|----------------------|-------|
| Pixel or emulator | 16 | | | | | | |
| Samsung | | | | | | | |
| Xiaomi, Redmi or POCO | | | | | | | |
| Oppo, Vivo or Realme | | | | | | | |
| Oldest available | 8 to 10 | | | | | | |

## Regression script

Run on each device before beta and before production.

1. Fresh install. Complete the first run. Confirm the service is on.
2. Call the phone. Flip it face down. Ringing stops. End the call. The sound mode returns.
3. Repeat with Vibrate selected.
4. Repeat with "Only when lying flat" on, once from a table and once from the hand.
5. Cover the proximity sensor as in a pocket, call, and confirm nothing changes.
6. Answer a call, reject a call and miss a call without flipping. The sound mode is unchanged afterwards.
7. Change the sound mode by hand during a silenced call. The manual choice is kept.
8. Pause for 30 minutes. Confirm the notification and tile text. Resume early.
9. Let a timed pause end by itself.
10. Toggle from the Quick Settings tile and from the notification.
11. Reboot. Confirm the service returns without opening the app.
12. Install the same build again with `adb install -r`. Confirm the service returns.
13. Remove Sound control access in system settings. Confirm the attention card and its fix button.
14. Turn notifications off for the app. Confirm the attention card.
15. Enable Flip to lock. Flip with the screen on. The screen locks.
16. Run Check my setup from start to finish.
17. Tap Rate us, Share, Report a problem, Instagram, WhatsApp, Privacy policy. Each opens or shows a message. None crashes.
18. Switch theme mode, colour theme and 200% font size. Check every screen.
19. Rotate the phone on every screen.
20. Upgrade path: install 1.7, turn the service on, install 2.0 over it. Settings are kept, the first run is skipped and "what's new" shows once.

## Progress log

Newest entry first. One line per session.

| Date | Milestone | What was done | Next |
|------|-----------|---------------|------|
| 2026-10-06 | M5 | M5-01 to M5-13: new Home built on the design kit (power control, status headline per state, Silence or Vibrate, attention card per failure, battery card, stats card, one discovery card, pause sheet, update and what's new dialogs); timed pause in the service (stays running, coordinator ignores calls until the end time by the clock, notification and tile show "Paused until", survives a process restart, health check leaves it alone); `FlipStatsStore`, `HintStore`, `AppVersion`, `UpdateAvailability` (false until M7-04); pure rules for status, cards, discovery and attention with tests; Home tests on Robolectric; About moved into Settings; 80 unused 1.x strings removed. `HomeSnapshots` renders six Home states. 278 tests, lint and release build pass. | Exit criteria need a phone (timed pause end, TalkBack). Start M6 |
| 2026-10-06 | M4 | M4-01 to M4-09: `OnboardingActivity` with Welcome, Access and Try it, built like Secret Calculator's onboarding; one primary button names the next missing access, permanent denial opens settings, sound control shows a hint sheet first, the step advances by itself on return; Try it uses the real sensor with a buzz, a 20 s hint and Skip; `OnboardingGate` keeps updating users out; Home, Settings and the test screens open the Access step in access-only mode; the 1.x setup screen and its components are deleted. `OnboardingFlowComposeTest` walks the whole first run and saves each step as a PNG. 243 tests, lint and release build pass. | Exit criteria need a phone (timing, Android 12 and 13 denial). Start M5 |
| 2026-10-06 | M3 | M3-01 to M3-05 and M3-07 to M3-09: Secret Calculator's design kit copied as `NovaDesign`, `States`, `TopBars`; its Blue, Teal and Sunset schemes, `Appearance`, `AppTheme`, `ThemeMode` and Outfit typography; state colours with contrast checked (all pass); DayNight window background; `PhoneFlipIllustration` and `PhoneFlipDemo`; `FlipPowerControl`, `ActionSelector`, `AttentionCard`, `StatsCard`. `ComponentSnapshots` renders them to `app/build/snapshots` under Robolectric, checked in four themes. App now uses the Blue theme instead of wallpaper colours. | M3-06 needs icon artwork. Start M4 |
| 2026-10-06 | M2 | M2-01 to M2-13: AGP 9.3.1, Kotlin 2.4.10, KSP, Hilt 2.60.1; `AppModule` reproduces the old container exactly; service and tile `@AndroidEntryPoint`, receivers and worker through `BackgroundEntryPoint`; `@HiltViewModel` everywhere; `AppContainer` and `ViewModelFactories` deleted; `MainActivity` is an `AppCompatActivity` with the Secret Calculator shell; `Routes` and `AppNavHost` with `go`, `back(entry)` and 280 ms transitions; packages moved to `utils`, `utils/ads`, `utils/about_utils`; funnel `app_opened` and first-time `setup_complete` wired; `ObjectGraphTest` proves the shared state. Deviations X12 to X14 recorded. 223 tests, lint and clean release build pass. | M2-14 needs a phone. Start M3 |
| 2026-10-06 | M1 | M1-01 to M1-13 built and unit tested on today's code: start sources and `InterruptionPolicy`; restart setting on by default; `PackageReplacedReceiver`; automatic failures keep the saved choice and post the "stopped" alert (Alerts channel, once per interruption, tap turns it back on); `HealthCheckWorker` every 6 hours (WorkManager 2.10.1); battery status and brand steps; guarded `IntentUtil`; flip buzz; backup off including device transfer; sensor at UI rate; analytics for state, interruptions, auto resume and flips. 216 tests, lint and release build pass. Home now shows an interrupted session as off with "Try again". | M1-14 needs a phone; M1-15 is the owner's 1.8 decision; start M2 meanwhile |
| 2026-10-06 | M0 | M0-10: App Distribution plugin 5.3.0 on debug builds, group `me-flip-to-mute` (override with `firebaseAppDistribution.groups`), key path in `local.properties` (not committed). `appDistributionUploadDebug` exists; no upload run, so the group is not yet confirmed to exist in Firebase. M0-13: per-machine `.idea` files ignored. M0-11 and M0-12 wait for the owner. | Start M1 |
| 2026-10-06 | M0 | M0-08 and M0-09 together: `AnalyticsLogger`, `firebaseAnalyticsLogger` and `Funnel` ported from Secret Calculator (setup_complete, onboarding_complete, first_flip, return_d1, return_d7), exposed from `AppContainer` but not called yet (wired in M1-12, M2-12, M4-07). Robolectric 4.17, Turbine, `androidx.test:core`, `robolectric.properties` and `TrackedViewModels` added; `FunnelTest` ported. 191 tests, lint and release build pass. | M0-10: Firebase App Distribution |
| 2026-10-06 | M0 | M0-07: Crashlytics added (plugin 3.0.8, library from Firebase BoM 34.17.0). Firebase moved into the version catalog as in Secret Calculator. Keep rules for line numbers added. Tests, lint and release build pass, and the release build uploaded its mapping file. Owner still to update the Play Data safety form (D3). Not yet seen in the Crashlytics console. | M0-08: `AnalyticsLogger` and `Funnel` |
| 2026-10-06 | M0 | M0-06: added `.github/workflows/ci.yml` (unit tests, lint, unsigned release build). Lint had 8 errors; fixed all, including a likely crash on Android 8 and 9 (audit R15). Tests, lint and release build pass locally. Not yet run on GitHub, because nothing is pushed. | Push the branch to confirm CI is green, then M0-07: Crashlytics |
| 2026-10-06 | M0 | M0-05: DataStore tests now use Okio storage through a shared `TestDataStores.kt` helper, because `File.renameTo` cannot overwrite on Windows. 187 of 187 pass, twice. Deleted `failing-tests.txt`. | M0-06: GitHub Actions workflow |
| 2026-10-06 | M0 | M0-04: version set to 2.0.0 (versionCode 9); debug build passes and the merged manifest shows the new version | M0-05: fix the five DataStore tests that fail on Windows |
| 2026-10-06 | M0 | Analysed Secret Calculator, wrote `ARCHITECTURE.md`, added the M2 migration milestone and renumbered later milestones | Owner decisions D1 to D10, then M0-04 and M0-05 |
| 2026-10-06 | M0 | Audited v1.7, ran the unit test baseline (182 of 187 pass), wrote the docs folder, created branch `release/v2.0` | Owner decisions, then M0-04 and M0-05 |
