# Ad placements in Flip to Mute

Written 2026-10-07 from a walk through the user journey. It says where ads show, where they never
show, and how often. Change the limits here and in `InterstitialPolicy`, `AdConfig` and
`RemoteAdGate` together.

## 1. The user journey

| Step | What the user is doing | Ad |
|------|------------------------|----|
| First run (3 steps) | Granting access, trying a flip | None. A new user has seen no value yet. |
| Home | Checking that it is on, pausing, switching Silence or Vibrate | Bottom banner |
| A flip (the app is closed) | Nothing: the phone is face down | None. Ads in notifications are against Play policy. |
| Tapping the flip or recap notification | Opening "Your flips" | Native ad in the list. No banner while it shows. |
| Leaving "Your flips" for Home | Finished looking | Interstitial, within the limits in section 3 |
| "Check my setup" | Running the checks | Bottom banner. No full-screen ad during the checks. |
| Leaving "Check my setup" for Home | Finished | Interstitial, within the limits |
| Settings, About, Keep it running | Changing something, or fixing a problem | Bottom banner only |
| Tapping a locked theme | Choosing to unlock it | Rewarded ad, only if the user taps "Watch ad" |

## 2. Placements

| Placement | Format | Where | Remote Config switch | Unit ID |
|-----------|--------|-------|----------------------|---------|
| Bottom banner | Adaptive banner, collapsible | Under every screen of `MainActivity` | `ad_banner_<screen>_enabled`, one per screen | Set |
| Your flips | Native | After the milestones card, before the recent flips | `ad_native_activity_enabled` | Set |
| Natural break | Interstitial | Going back to Home from "Your flips" or "Check my setup" | `ad_interstitial_enabled`, `ad_interstitial_cooldown_hours` | Set |
| Earned theme | Rewarded | Settings, on a locked theme, after "Watch ad" | `ad_rewarded_theme_enabled` | Set |

Debug builds use Google's sample units for every format, so testers never click real ads.

## 3. Limits

| Rule | Value | Where |
|------|-------|-------|
| One ad per screen | The banner hides while the native ad shows on "Your flips" | `shouldShowBanner` |
| No full-screen ad for a new user | Not before the 3rd app launch | `InterstitialPolicy.MIN_LAUNCHES` |
| Time between two interstitials | 12 hours, changeable remotely, never under 1 hour | `InterstitialPolicy`, `ad_interstitial_cooldown_hours` |
| The user never waits for an ad | An interstitial that has not loaded is skipped | `InterstitialAds.show` |
| No ad before consent | Nothing is requested until the consent step allows it | `AdConsent`, `MainActivity.startAds` |
| No review prompt after an ad | 3 minutes of quiet after any full-screen ad | `ReviewPolicy.AD_QUIET_MS` |
| Buyers see no ads | Banner, native and interstitial are off after "Remove ads". The rewarded ad stays, because the user asks for it. | `MainActivity` |

## 4. Where ads never show

- The first run and the access steps.
- Over Home's power control, the pause sheet or an attention card: these are the actions the app exists for.
- On the way into a screen. The interstitial only follows a finished task.
- In notifications, on the lock screen, or over the incoming call. Play policy forbids the first two, and the third would break the app's purpose.
- On app open. Flip to Mute is opened for a few seconds to do one thing, often from a notification; an ad at that moment is the interruption this plan avoids. If revenue data later argues for it, add it with a cap of once a day and never on a launch from a notification, the tile or a shortcut.

## 5. What to watch after release

- Banner and native impressions per active user (AdMob), against `activity_opened` (Analytics).
- Uninstalls and 1-star reviews mentioning ads in the week after the interstitial unit goes live. Turn it off with `ad_interstitial_enabled` if either rises.
- `return_d7` before and after: ads must not cost retention.
