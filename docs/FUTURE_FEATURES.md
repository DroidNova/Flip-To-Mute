# Future features: after v2.0

Ideas for the big updates that follow v2.0. Nothing here is in v2.0 scope.
When a v2.0 task tempts us to add something, it is written here instead.

Written 2026-10-06. Review this list at task M8-09, when the v2.1 scope is chosen
with real v2.0 data.

## How to read this list

| Column | Meaning |
|--------|---------|
| Value | High, Medium or Low. What it does for retention, daily use, new installs or revenue. |
| Effort | S (under 2 days), M (up to a week), L (up to 3 weeks), XL (more) |
| New access | Any permission or special access the user must grant beyond v2.0 |
| Confidence | How sure we are it can be built well. "Needs research" items have a spike listed in section 6. |

A feature moves into a release only when its row has an owner decision, a
written acceptance test and, where marked, a finished research spike.

## 1. Proposed release themes

| Release | Theme | Main goal | Headline features |
|---------|-------|-----------|-------------------|
| v2.1 | More ways to flip | Used more often | F1 Flip to Shhh, F2 internet calls, F4 flip back to ring, F7 sensitivity |
| v2.2 | Smart and automatic | Retention | F9 schedule, F11 widget, F12 app shortcuts, F10 weekly recap, F13 history |
| v2.3 | Yours | Revenue and reach | F19 remove ads, F21 themes, F23 more languages, F16 settings backup |
| Later | Experiments | Learn | F8 fold to mute, F24 automation, F25 watch |

The order follows one rule: first make people use the gesture every day, then
make the app manage itself, then ask for money.

## 2. v2.1: more ways to flip

Today the gesture only matters when a cellular call rings. For many users that
is a few times a week. These features give the same gesture more moments.

| ID | Feature | Description | Value | Effort | New access | Confidence |
|----|---------|-------------|-------|--------|------------|------------|
| F1 | **Flip to Shhh** | Place the phone face down at any time to switch to Do Not Disturb. Pick it up to switch back. Ideal for meetings, meals and sleep. | High, daily use | L | None. Reuses Sound control access. | Needs research |
| F2 | **Internet calls** | Flip to silence WhatsApp, Telegram, Meet and similar calls. This is the first limitation listed in the 1.x README. | High, removes the top limitation | L | Possibly Notification access | Needs research |
| F3 | **Flip to pause media** | Face down while music or video plays sends a pause command. | Medium | M | None | Good |
| F4 | **Flip back to ring again** | If the phone is turned face up while the call is still ringing, the ringtone returns. Fixes accidental flips. | Medium | S | None | Good |
| F5 | **Pick up to quiet** | Lifting the phone while it rings lowers the ring volume, before any flip. | Medium | M | None | Good |
| F6 | **Flip to decline** | Optional: face down for two seconds declines the call instead of silencing it. | High for some users | M | Answer phone calls | Needs research |
| F7 | **Sensitivity** | Choose Quick, Normal or Careful. Changes the hold time and angle. | Medium, fewer complaints | S | None | Good |
| F8 | **Fold to mute** | On flip and fold phones, closing the device silences the call. | Low, niche but a strong store headline | M | None | Fair |

Technical notes:

- **F1** needs to notice face down and pick up while the screen is off. Ordinary accelerometer events can stop when the processor sleeps. The likely design starts detection while the screen is on and uses a wake-up or significant-motion sensor for pick up. Battery cost must be measured before committing.
- **F2** has two candidate designs. The audio mode listener, available from Android 12, reports ringing without any new access but may not fire for every app. The notification listener sees call notifications from every app but needs Notification access, which is a heavy request for a privacy focused app. It is also unknown which apps obey a ringer mode change.
- **F6** relies on `TelecomManager.endCall`, deprecated since Android 10 but still present. Behaviour on Android 14 to 16 and Play policy must be checked first.
- **F3** uses a media key event through `AudioManager`. It needs the sensor while media plays, so it should be limited to screen on.

## 3. v2.2: smart and automatic

The app should need less attention, and should remind people that it is useful.

| ID | Feature | Description | Value | Effort | New access | Confidence |
|----|---------|-------------|-------|--------|------------|------------|
| F9 | **Schedule** | Active hours and days, for example weekdays 9 to 6. Outside them the app pauses itself. | High | M | None | Good |
| F10 | **Weekly recap** | An optional weekly notification: "You silenced 9 calls this week." Off by default. | Medium, reminds users of value | S | None | Good |
| F11 | **Home screen widget** | A small widget with on, off and "pause 1 hour". | Medium, visibility on the home screen | M | None | Good |
| F12 | **App shortcuts** | Long press the app icon: Pause 1 hour, Turn off, Turn on. | Medium | S | None | Good |
| F13 | **Flip history** | A simple list of recent flips: time and action only. Never caller details. | Low to medium | S | None | Good |
| F14 | **Headset aware** | Pause automatically while a Bluetooth headset or car is connected. | Low | M | Nearby devices | Fair |
| F15 | **Meeting mode** | Turn on stricter behaviour during calendar events. | Medium | L | Calendar | Fair. The permission is heavy. |
| F16 | **Settings backup** | Keep settings when moving to a new phone. Needs the settings and the runtime state split into two files. v2.0 turns backup off, as Secret Calculator does. Secret Calculator's own backup and restore code is the starting point. | Medium | M | None | Good |
| F17 | **Per SIM choice** | On dual SIM phones, react only to the chosen SIM. | Medium in dual SIM markets | M | None | Good. The call monitor already tracks subscriptions. |
| F18 | **Tips** | A rotating "Did you know" card for features the user has not tried. | Low | S | None | Good |
| F26 | **In-app update prompt** | Only if it did not ship as a v2.0 stretch item. | Medium | M | None | Good |

## 4. v2.3: yours

| ID | Feature | Description | Value | Effort | New access | Confidence |
|----|---------|-------------|-------|--------|------------|------------|
| F19 | **Remove ads** | A one time purchase through Play Billing, built with Secret Calculator's `PremiumBillingManager`, `Security.verifyPurchase` and `PremiumController` unchanged (`ARCHITECTURE.md` A25). | High, revenue and goodwill | M | None | Good |
| F20 | **Pro bundle** | If F19 sells: schedule, Flip to Shhh, widget styles and themes as a paid tier. Decide only with sales data. | Unknown | L | None | Low until F19 data exists |
| F21 | **More themes** | More colour themes and a true black theme, added to the `AppTheme` enum. Secret Calculator marks some themes as Pro, and the same `ProBadge` can be used if F20 happens. | Medium | S | None | Good |
| F22 | **Confirmation styles** | Choose the buzz pattern that confirms a flip. | Low | S | None | Good |
| F23 | **More languages** | Translate the app and the store listing. Choose languages from the Play Console install countries. | High, new installs | M per batch | None | Good |
| F24 | **Automation hooks** | Let Tasker, Samsung Routines and similar apps turn the app on, off or pause it. | Low, loved by power users | M | None | Good. Needs a protected receiver. |
| F25 | **Watch companion** | A Wear OS tile to pause and resume. | Low | XL | None | Fair |
| F27 | **Store listing experiments** | Test icons, screenshots and descriptions in Play Console. No code. | Medium, new installs | S | None | Good |
| F28 | **Rating flow review** | After 30 days of v2.0 data, compare review prompts shown with ratings received. Tune `ReviewPolicy` in both apps together. | Medium | S | None | Good |
| F29 | **Protect settings with fingerprint** | Optional: require a fingerprint before Flip to Mute can be turned off or its settings changed, for example on a child's or shared phone. Built with Secret Calculator's `BiometricUnlock`, `BiometricHardware` and `showBiometricPrompt()` unchanged: strong biometrics only, turned on only after a successful check, refreshed on every resume (`ARCHITECTURE.md` A26). | Low | S | None | Good |

## 5. Suggested order

If nothing in the v2.0 data changes the picture, build in this order:

1. F4 Flip back to ring again. Small, and it removes a real annoyance.
2. F7 Sensitivity. Small, and it answers "too slow" and "too sensitive" reviews.
3. F12 App shortcuts. Small.
4. F1 Flip to Shhh, after its research spike. This is the feature most likely to make the app a daily habit.
5. F2 Internet calls, after its research spike. This is the feature most likely to appear in reviews as a request.
6. F9 Schedule.
7. F19 Remove ads.
8. F23 More languages.

## 6. Research spikes

Each spike is a time boxed experiment that ends with a written answer in this file.

| Spike | Question | How to answer | Time box |
|-------|----------|---------------|----------|
| S1, for F1 | Can face down and pick up be detected with the screen off, at an acceptable battery cost? | Prototype with a wake-up sensor and with significant motion. Measure battery over 24 hours on three phones. | 3 days |
| S2, for F2 | Which internet calling apps report ringing through the audio mode, and which obey a ringer mode change? | Test WhatsApp, Telegram, Messenger, Meet, Teams, Signal on Android 12 to 16. Record a table. | 3 days |
| S3, for F6 | Does `endCall` still work on Android 14 to 16, and does Play policy allow this use? | Device test, then read the current Play permissions policy. | 1 day |
| S4, for F8 | Which hinge signals are available on Samsung and Motorola flip phones? | Test with Jetpack WindowManager on one device of each. | 2 days |
| S5, general | Does the sticky service restart reliably on Android 12 to 16? | Covered by M1-14. Copy the result here. | In v2.0 |

## 7. Considered and rejected

Kept here so the same ideas are not debated twice.

| Idea | Why not |
|------|---------|
| Auto reply by SMS after a flip | Sending SMS is limited by Play policy to default messaging apps. High risk of rejection. |
| Favourite contacts that are never silenced | Needs the caller's number and the contact list. Both are sensitive, and the call log permission is restricted by Play. It also breaks the promise that the app never sees who is calling. |
| Silence alarms and timers with a flip | Android gives no way to control another app's alarm. |
| Shake to silence | Too many false triggers in pockets and bags, and it is not what the app is known for. |
| Anything built on an Accessibility service | Play policy allows that API only for accessibility purposes. |
| Remote Config for detection thresholds | Remote Config returns in v2.0 for ad switches and the update dialog only (decision D10). Detection thresholds stay in the app, because a bad remote value could stop calls being silenced for everyone at once. |
| Secret Calculator's old star rating card | Removed from Secret Calculator in commit `038a72e`. It asked for the user's opinion before the Play review, which Google's review guidelines advise against. |
| Asking for the battery optimisation exemption with a direct system prompt | The permission behind that prompt is limited by Play policy to narrow cases. v2.0 guides the user to the settings page instead. |

## 8. Idea inbox

New ideas go here first, one line each, with a date. They are sorted into the
sections above when the next release is planned.

| Date | Idea | From |
|------|------|------|
| | | |
