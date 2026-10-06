# Flip to Mute v2.0 design spec

This document describes the redesigned interface. It is the reference for
milestones M3 to M6. Finding IDs refer to `CURRENT_STATE_AUDIT.md`.

The visual language is Secret Calculator's (decision D9, `ARCHITECTURE.md`
patterns A7 to A10). Sizes, shapes, fonts and motion are copied from
`ui/components/VaultDesign.kt`, `ui/theme/` and `ui/navigation/AppNavHost.kt`
in that project. Only the colours, the illustrations and the screens
themselves are new. Colour values are starting points, tuned and contrast
checked in M3.

## 1. Design principles

1. **One screen, one job.** Home answers one question: is Flip to Mute protecting me right now?
2. **Show, then ask.** Explain the gesture with a moving picture before asking for any access.
3. **Plain words.** The user never reads "monitoring", "service" or "state".
4. **Never fail silently.** If the app cannot work, Home says why and offers one fix.
5. **Calm by default.** No ads in the first minute, no pop-ups stacked on each other, at most one card asking for something on Home at a time.
6. **Reachable with one thumb.** Primary actions sit in the lower two thirds of the screen.
7. **Same family as Secret Calculator.** A user of both apps should recognise the cards, rows, sheets and motion.

## 2. Navigation map

```
OnboardingActivity (new users only)
  Welcome ─► Access ─► Try it ─► MainActivity

MainActivity
  Home
    ├── Pause sheet
    ├── Update available dialog, then What's new dialog (once each)
    └── Settings  (gear icon, top right)
          ├── Keep it running
          ├── Check my setup
          └── About
```

- The first run is its own activity, as in Secret Calculator. It finishes and starts `MainActivity` with a cleared task.
- Home is the start destination of `MainActivity`.
- Navigation uses `go()`, which ignores a repeated tap, and `back(entry)`, which ignores a second Back tap.
- The Quick Settings tile and both notifications open Home.
- The overflow menu is removed (U6).

## 3. Design system

### 3.1 Colour

The colour system is Secret Calculator's: an `AppTheme` enum where each entry
has a light and a dark `ColorScheme`, and a `ThemeMode` of System, Light or
Dark. Both are held by `Appearance` as Compose state, so a change in Settings
redraws every screen at once. Wallpaper based dynamic colour is not used, the
same as Secret Calculator, so the brand is always visible (U12).

Flip to Mute ships three colour themes. Theme mode defaults to System.

| Theme | Role | Light | Dark |
|-------|------|-------|------|
| Indigo (default) | Primary | `#3457D5` | `#B8C4FF` |
| | Primary container | `#DDE1FF` | `#173BAB` |
| | Background | `#FBF8FF` | `#131318` |
| Teal | Primary | `#006A64` | `#4FDBD0` |
| | Primary container | `#9EF2E8` | `#00504B` |
| | Background | `#F4FBF9` | `#0E1514` |
| Sunset | Primary | `#A3410F` | `#FFB596` |
| | Primary container | `#FFDBCD` | `#7F2B00` |
| | Background | `#FFF8F6` | `#1A110E` |

The full Material roles for each theme are generated from these seeds in M3-03.

State colours are separate from the themes, so the state reads the same in
every theme.

| State | Meaning | Light | Light container | Dark | Dark container |
|-------|---------|-------|-----------------|------|----------------|
| On | Protecting | `#1B8A4B` | `#D5F5E0` | `#6FDB9A` | `#0F5131` |
| Paused | Temporarily off | `#8A5A00` | `#FFE2B5` | `#FFB95C` | `#5C3F00` |
| Off | Not running | `#74777F` | `#E1E2EC` | `#8E9099` | `#2B2D34` |
| Attention | Needs a fix | `#BA1A1A` | `#FFDAD6` | `#FFB4AB` | `#93000A` |

State is never shown by colour alone. Every state also has an icon and a sentence.

Status bar and navigation bar icons follow the light or dark choice, set in the
theme function exactly as Secret Calculator does.

### 3.2 Typography

Copied from Secret Calculator, which shares it with Speedometer.

| Styles | Font | Weight |
|--------|------|--------|
| Display | Outfit | Bold |
| Headline, title large, title medium | Outfit | Semi-bold |
| Title small | Outfit | Medium |
| Body | Platform sans | Regular |
| Labels and buttons | Platform sans | Medium |

Outfit is a downloadable Google Font, so it adds nothing to the download size.
Until it arrives, or on phones without Play services, the platform font is used
at the same weights. Body text stays in the platform font, which also covers
Hindi and other scripts.

### 3.3 Shape, spacing, elevation

Values from Secret Calculator's design kit.

| Token | Value |
|-------|-------|
| Cards (`NovaCard`) | 22 dp corners, filled with a faint tint of the theme accent, no shadow |
| Tiles and icon badges on a card | 14 dp corners |
| Text fields in sheets and dialogs | 14 dp corners |
| Icon badge | Round, leads every settings row |
| Section label | Small uppercase text above each group |
| Main control on Home | Circle, 168 dp, 120 dp on short screens |
| Buttons | Fully rounded, at least 48 dp tall |
| Bottom sheets | Material 3 modal sheet, fully expanded, surface container colour |
| Grid | 4 dp |
| Screen side padding | 16 dp |
| Elevation | Flat. Tint, not shadow, separates layers. |

### 3.4 Motion

| Moment | Motion | Source |
|--------|--------|--------|
| Screen change, forward | Slide in from the right by one sixth, fade in, 280 ms | Secret Calculator nav host |
| Screen change, back | Slide by one tenth the other way, fade, 280 ms | Secret Calculator nav host |
| Lists and card groups appearing | Fade and lift, each item 35 ms after the previous (`appearIn`) | Secret Calculator kit |
| Pressing a card or tile | Shrinks to 96% while pressed (`pressScale`) | Secret Calculator kit |
| Hero icon | Slow up and down drift, 2.2 s (`floating`) | Secret Calculator kit |
| Turning on | Phone illustration rotates from face up to face down, 400 ms | New |
| While on | A slow ring pulses once around the control every 3 seconds | New |
| Turning off | Phone rotates back to face up, colour fades to Off, 300 ms | New |
| Welcome step | The phone flips, a sound wave icon shrinks to nothing, then it loops, 2.4 s | New |

When the system animator scale is zero, looping motion is replaced by still
images.

### 3.5 Components

| Component | Used in | Source |
|-----------|---------|--------|
| `NovaCard`, `IconBadge`, `SectionLabel` | Everywhere | Copied from `VaultDesign.kt` |
| `SettingsGroup`, `SettingsRow`, `SwitchRow`, `ChoicePill` | Settings, Keep it running | Copied from `VaultDesign.kt` |
| `SheetHeader`, `SheetAction`, `ActionTile` | Pause sheet, access hint sheet, About | Copied from `VaultDesign.kt` |
| `GradientBanner` | The one highlighted action on a screen, for example "Turn back on" | Copied from `VaultDesign.kt` |
| `EmptyState`, `LoadingState` | Check my setup, stats | Copied from `States.kt` |
| `NovaTopBar` | Every screen except Home | Copied from `TopBars.kt` |
| `FlipPowerControl` | Home | New, built on the kit |
| `PhoneFlipIllustration` | Home, Welcome, Try it | New |
| `ActionSelector` (Silence or Vibrate) | Home, Settings | New, built from two `ChoicePill`s |
| `AttentionCard` | Home | New, built on `NovaCard` |
| `StatsCard` | Home | New, built on `NovaCard` |
| `DiscoveryCard` | Home | New, built on `NovaCard`, state in `HintStore` |
| Banner ad | Home, Settings, About | Owned by `MainActivity`, as in Secret Calculator |

## 4. Screens

### 4.1 Welcome (OnboardingActivity, step 1)

```
┌────────────────────────────────────┐
│                                    │
│        [ phone flip animation ]    │
│                                    │
│   Silence calls with a flip        │
│                                    │
│   When your phone rings, place it  │
│   face down. The ringing stops.    │
│                                    │
│   🔒 No access to your contacts,   │
│      numbers or call history.      │
│                                    │
│   ┌──────────────────────────────┐ │
│   │         Get started          │ │
│   └──────────────────────────────┘ │
└────────────────────────────────────┘
```

No ad. No skip, because the next screen has one.

### 4.2 Access (OnboardingActivity, step 2)

```
┌────────────────────────────────────┐
│ ←                          1 of 3  │
│                                    │
│  Three quick steps                 │
│  Flip to Mute needs these to work. │
│                                    │
│  ✓  Phone                          │
│     To know when a call is ringing │
│                                    │
│  ●  Notifications                  │
│     To show that it is running     │
│                                    │
│  ○  Sound control                  │
│     To silence the ringtone        │
│                                    │
│   ┌──────────────────────────────┐ │
│   │     Allow notifications      │ │
│   └──────────────────────────────┘ │
│            Do this later           │
└────────────────────────────────────┘
```

Behaviour:

- One primary button. Its label names the next missing step. Tapping it goes straight to the system dialog. The one-line reason on the row replaces the explanation dialog (U2).
- Order is Phone, Notifications, Sound control. Sound control is last because it leaves the app for a system settings page.
- Before leaving for Sound control, a short sheet shows what to tap: "Find Flip to Mute in the list and switch it on."
- On return, the screen refreshes and advances by itself. When all three are done it moves to step 3 with no "Finish" button.
- If a permission is permanently denied, the button changes to "Open settings".
- The Notifications row is hidden on Android 12 and lower when notifications are already enabled.
- "Do this later" goes to Home in the "setup needed" state.
- The existing rules in `PermissionsViewModel` and `resolveRuntimePermissionAction` are reused.

### 4.3 Try it (OnboardingActivity, step 3)

```
┌────────────────────────────────────┐
│ ←                             Skip │
│                                    │
│        [ live phone illustration ] │
│                                    │
│   Try it now                       │
│   Turn your phone face down, wait  │
│   for the buzz, then pick it up.   │
│                                    │
└────────────────────────────────────┘

        after a detected flip

┌────────────────────────────────────┐
│              ✓                     │
│   It works                         │
│   Do the same when a call rings.   │
│                                    │
│   ┌──────────────────────────────┐ │
│   │     Turn on Flip to Mute     │ │
│   └──────────────────────────────┘ │
└────────────────────────────────────┘
```

- Uses the existing orientation monitor through `SensorTestViewModel`.
- The buzz is needed because the screen cannot be seen while face down.
- If the device has no usable sensor, this step is skipped and Home explains the limitation.
- After 20 seconds without a flip, a hint appears: "Place it flat on a table, screen down."
- "Turn on Flip to Mute" starts the service and opens Home in the On state.

### 4.4 Home

```
┌────────────────────────────────────┐
│ Flip to Mute                    ⚙  │
│                                    │
│            ╭──────────╮            │
│           │  phone,    │           │
│           │  face down │           │
│            ╰──────────╯            │
│                                    │
│        Flip to Mute is on          │
│   Place your phone face down when  │
│   it rings.                        │
│                                    │
│          [  Pause  ▾ ]             │
│                                    │
│  When I flip my phone              │
│  ┌───────────────┬───────────────┐ │
│  │ ✓ Silence     │    Vibrate    │ │
│  └───────────────┴───────────────┘ │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ 14 calls silenced this month │  │
│  │ Last flip: today, 2:10 PM    │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ Lock your screen with a flip │  │
│  │ [ Try it ]        [ Not now ]│  │
│  └──────────────────────────────┘  │
│ ─────────── banner ad ──────────── │
└────────────────────────────────────┘
```

The circular control is a toggle with the accessibility role Switch. Tapping it
turns Flip to Mute on or off.

#### Home states

| Interface state | Service state in code | Control | Headline | Supporting line | Buttons |
|-----------------|-----------------------|---------|----------|-----------------|---------|
| Setup needed | Access incomplete, service off | Off colour, disabled look | Finish setting up | Three quick steps and you are protected. | Primary: Continue setup |
| Off | `Stopped` | Off colour, phone face up | Flip to Mute is off | Tap to silence calls with a flip. | None, the control is the action |
| Turning on | `Starting`, `Resuming` | Progress ring | Turning on… | None | Control disabled |
| On | `Active` | On colour, phone face down, slow pulse | Flip to Mute is on | Place your phone face down when it rings. | Pause |
| Pausing, turning off | `Pausing`, `Stopping` | Progress ring | One moment… | None | Control disabled |
| Paused, timed | `Paused` with an end time | Paused colour | Paused until 3:30 PM | Calls will ring normally until then. | Resume now, Turn off |
| Paused, open ended | `Paused` | Paused colour | Flip to Mute is paused | Calls will ring normally. | Resume, Turn off |
| Checking | `Unresolved`, `Recovering` | Progress ring, at most two seconds | Checking… | None | Control disabled |
| Needs attention | `Error` | Attention colour, warning icon | See the table below | See the table below | One fix button |

#### Needs attention messages

| Failure in code | Headline | Fix button |
|-----------------|----------|------------|
| `SETUP_REQUIRED` | Access was removed | Fix access |
| `NOTIFICATION_UNAVAILABLE` | Notifications are turned off | Allow notifications |
| `SERVICE_START_NOT_ALLOWED`, interrupted by the system | Android stopped Flip to Mute | Turn back on |
| `CALL_MONITOR_FAILED` | Could not listen for calls | Try again |
| `SOUND_CONTROL_FAILED` | Could not change the sound mode | Check sound access |
| `SENSOR_UNAVAILABLE` | The motion sensor is not responding | Check my setup |
| `TELEPHONY_UNAVAILABLE` | This device cannot receive cellular calls | None |
| `CLEANUP_FAILED` | Please check your sound mode | Got it |
| `UNKNOWN` | Something went wrong | Try again, with "Report a problem" below |

When the service is on but battery restrictions are detected, a softer card
appears under the control: "Your phone may stop Flip to Mute in the
background" with the button "Keep it running".

#### Order of cards below the action selector

Only the first applicable card that asks for something is shown.

1. Attention or battery card, when relevant. Always first.
2. Stats card, once at least one flip has been recorded.
3. One discovery card, from `HintStore`. The review sheet is never a card; see section 4.10.

#### Discovery cards

| Card | Shown when | Primary action |
|------|-----------|----------------|
| Lock your screen with a flip | Service on for 2 days, Flip to lock off, device supports it | Opens the Flip to lock setting |
| Add the Quick Settings tile | Service on for 4 days, tile not added, Android 13 or later | Requests the tile |
| Keep it on after a restart | Start after restart is explicitly off | Turns the setting on |

Each card has "Not now", which hides that card permanently.

### 4.5 Pause sheet

```
┌────────────────────────────────────┐
│  Pause Flip to Mute                │
│                                    │
│   For 30 minutes                   │
│   For 1 hour                       │
│   For 2 hours                      │
│   Until I turn it back on          │
└────────────────────────────────────┘
```

One tap selects and closes. The notification "Pause" action keeps its current
behaviour, which is the open ended pause.

### 4.6 Settings

Built exactly like Secret Calculator's settings screen: labelled
`SettingsGroup` cards of one-line `SettingsRow`s, each led by an icon badge,
with a switch, a value or a chevron at the end. Access and device states are
refreshed every time the screen resumes.

```
Settings
────────────────────────────────────
FLIP BEHAVIOUR
  (◎) When I flip my phone     Silence ›
  (◎) Only when lying flat        [ off ]
  (◎) Pocket protection           [ on  ]
  (◎) Buzz when a flip is detected [ on ]

MORE GESTURES
  (◎) Flip to lock screen         [ off ]

KEEP IT RUNNING
  (◎) Start after phone restart   [ on  ]
  (◎) Battery restrictions   Restricted ›
  (◎) Quick Settings tile         [ Add ]

APPEARANCE
  Theme      ( System ) ( Light ) ( Dark )
  Colour     ( ● Indigo ) ( ● Teal ) ( ● Sunset )

HELP AND ABOUT
  (◎) Check my setup                   ›
  (◎) Report a problem                 ›
  (◎) About                            ›
  (◎) Privacy options                  ›   (only when required)
────────────────────────────────────
banner ad
```

Rules taken from Secret Calculator:

- A feature that needs special access shows a short explain dialog first.
- The switch turns on only after the system confirms. Flip to lock turns on only after device admin access is granted, the same way Secret Calculator turns fingerprint unlock on only after a successful fingerprint check.
- If access is removed in system settings, the row turns back off on the next resume.
- One-off confirmations appear as a snackbar from a `@StringRes` message.

Removed from Settings: the "Monitoring status" row, the access status list,
the three diagnostics rows, the hardcoded version row and the placeholder
privacy row (U7). Access status now lives in "Check my setup".

### 4.7 Keep it running

| Element | Content |
|---------|---------|
| Header | `SheetHeader` style: icon badge, title, one line |
| Status | "Battery restrictions: Restricted" or "Not restricted" |
| Explanation | "Some phones stop apps in the background to save battery. Allow Flip to Mute to keep running so it never misses a call." |
| Primary action | `GradientBanner`: "Open battery settings" |
| Manufacturer steps | Three numbered steps chosen by phone brand. Generic steps for unknown brands. |
| Secondary action | "Open app info" |
| Switch | Start after phone restart |

### 4.8 Check my setup

One guided flow replaces the three test screens (U8). The existing view models
supply the logic.

| Step | Type | Pass condition | If it fails |
|------|------|----------------|-------------|
| 1. Access | Automatic | All three granted | "Fix access" button |
| 2. Battery | Automatic | Not restricted | "Keep it running" button, marked as a warning not a failure |
| 3. Flip sensor | User flips the phone | Face down detected | Tips, then "Report a problem" |
| 4. Sound switch | User taps "Test" | Mode changes and is restored | "Check sound access" button |
| 5. Call detection | Optional. User calls the phone from another phone | Ringing detected | Tips about cellular calls only |

The flow ends with "Everything works" or "1 problem found", and a "Send
report" button that prepares the support email with the results attached.
A clean run counts as a value moment for the review policy. No ad on this screen.

### 4.9 About

Same layout as Secret Calculator's About screen.

```
        [ app icon ]
        Flip to Mute
        Version 2.0.0

   ( ★ )        ( ⇪ )        ( ! )
  Rate us       Share     Report a problem

  Privacy policy                       ›

  COMMUNITY
  (◎) Follow on Instagram              ›
  (◎) Join the WhatsApp community      ›

  MORE FROM DROIDNOVA
  [ Background Video Recorder ]
  [ one more app, rotating ]
  More apps on Play Store              ›
```

- The three round actions are `ActionTile`s.
- The version comes from the package, never from a string resource.
- Each link goes through the guarded `IntentUtil` (X3).

### 4.10 Rating

Decision D1. Full rules in `UPDATE_PLAN.md` section 8.

| Surface | Behaviour |
|---------|-----------|
| "Rate us" tile in About | Opens the Play Store listing, exactly as today |
| Play In-App Review sheet | Requested by Secret Calculator's `ReviewPolicy` after value moments, on every 3rd launch, at most 3 times, never soon after a full-screen ad. Drawn by Google Play, not by the app. |

There is no rating card on Home. Secret Calculator removed its star card in
favour of the Play review sheet, and Flip to Mute follows that.

### 4.11 Update and what's new dialogs

Secret Calculator's home dialog queue. At most one dialog per Home visit, in
this order:

1. **Update available.** Shown when Remote Config reports a newer version code, at most once per launch. Buttons: "Update", "Later".
2. **What's new.** Shown once per version to users who updated. Buttons: "Got it".

What's new text for 2.0:

- A fresh, simpler look
- Keeps working after restarts and updates
- Pause for 30 minutes, 1 hour or 2 hours

## 5. Wording changes

| Today | v2.0 |
|-------|------|
| Monitoring | Never shown to the user |
| Flip to Mute is on | Flip to Mute is on (kept) |
| Checking monitoring | Checking… |
| Restoring your monitoring state… | Removed |
| Preparing call monitoring. | Turning on… |
| Set up app | Continue setup |
| App setup | Access |
| Phone Access | Phone |
| Sound Control Access | Sound control |
| Mute the ringtone | Silence |
| Vibrate phone | Vibrate |
| Flip Feedback | Buzz when a flip is detected |
| Only When Lying Flat | Only when lying flat |
| Diagnostics | Check my setup |
| Report bugs | Report a problem |
| Flip to Mute could not start. | A specific headline from section 4.4 |

Style rules: sentence case everywhere, no exclamation marks, no blame ("You
denied…"), and every error names the next step.

## 6. Notifications and tile

| Surface | Title | Text | Actions |
|---------|-------|------|---------|
| Ongoing, on | Flip to Mute is on | Place your phone face down when it rings. | Pause, Turn off |
| Paused, timed | Paused until 3:30 PM | Calls will ring normally until then. | Resume, Turn off |
| Paused, open ended | Flip to Mute is paused | Calls will ring normally. | Resume, Turn off |
| Alert, new | Flip to Mute stopped | Tap to turn it back on. | Tap opens Home |
| Tile | Flip to Mute | On, Off, Paused, Paused until 3:30, Setup needed | Tap toggles pause, as today |

Channels: "Status" (low importance, existing channel ID kept so user settings
survive) and "Alerts" (default importance, new).

## 7. Accessibility

- Touch targets at least 48dp.
- The main control announces "Flip to Mute, switch, on" and its state changes.
- State is carried by icon and text as well as colour.
- Text contrast at least 4.5 to 1, large text and icons at least 3 to 1, in light and dark and in every colour theme.
- Layouts hold at 200% font scale. Long headlines wrap, they are never cut.
- The flip animation has a text alternative and stops when system animations are off.
- Every screen scrolls, so nothing is unreachable on small or landscape screens.
- The "Try it" step can be skipped, for users who cannot perform the gesture during setup.

## 8. Adaptive layout

| Width | Layout |
|-------|--------|
| Under 600dp | Single column, full width with side padding |
| 600dp and wider | Single column, content limited to 560dp and centred |
| Landscape phone | Same column, scrolling; the main control shrinks to 120dp |

Edge to edge drawing stays on. Content respects system bars, the display
cutout and the banner height.

## 9. Assets to produce

| Asset | Format | Milestone |
|-------|--------|-----------|
| Phone flip illustration, face up and face down | Compose vector | M3 |
| Adaptive launcher icon with monochrome layer (U15), same artwork as today | `mipmap-anydpi-v26` XML plus layers | M3 |
| Seed colours for Indigo, Teal and Sunset, expanded to full Material schemes | Kotlin colour file, as `Color.kt` in Secret Calculator | M3 |
| Google Fonts certificate array for Outfit | `values/font_certs.xml`, copied from Secret Calculator | M3 |
| Icons for the access steps, settings rows and state colours | Material icons, as Secret Calculator uses | M3 |
| Store screenshots, 6 to 8, light and dark | PNG | M8 |
| Feature graphic | PNG, 1024 by 500 | M8 |
