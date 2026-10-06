# Flip to Mute: v2.0 documentation

This folder is the single source of truth for the v2.0 update of Flip to Mute
(`com.droidnova.fliptomute`). Everything here was written against the code on
`master` at commit `a39167c` (version 1.7, versionCode 8) on 2026-10-06.

v2.0 is built on the architecture of Secret Calculator
(`D:\Softwares\PlayStoreApps\Secret-Calculator`, version 2.0.0), which is
already released and tested.

## Documents

| # | Document | What it is for | When to update it |
|---|----------|----------------|-------------------|
| 1 | [CURRENT_STATE_AUDIT.md](CURRENT_STATE_AUDIT.md) | What the app is today and every problem found in the code, with file references. | Only when a finding is proven wrong. It is a snapshot. |
| 2 | [ARCHITECTURE.md](ARCHITECTURE.md) | The target architecture taken from Secret Calculator: what is adopted, what deviates and why, and the package layout. | When a pattern is adopted or a deviation is added. |
| 3 | [UPDATE_PLAN.md](UPDATE_PLAN.md) | The whole v2.0 plan: goals, stability work, architecture, redesign, retention, rating, ads, analytics, release strategy, decisions, risks. | When scope or a decision changes. |
| 4 | [DESIGN_SPEC.md](DESIGN_SPEC.md) | The redesign in Secret Calculator's visual language: navigation, every screen, colours, type, motion, wording, accessibility. | When a screen design changes. |
| 5 | [MILESTONES.md](MILESTONES.md) | The tracker. Milestones M0 to M8, task checklists, exit criteria, device matrix, regression script and progress log. | Every working session. |
| 6 | [FUTURE_FEATURES.md](FUTURE_FEATURES.md) | Features for the big updates after v2.0, with value, effort and feasibility notes. | When an idea is added, promoted or rejected. |

## How we work with these docs

1. Pick the next unchecked task in `MILESTONES.md`. Tasks are ordered so each one builds on the last.
2. Before building anything structural, open the matching Secret Calculator file named in `ARCHITECTURE.md` and follow it.
3. Do the task on `master`, or on a short branch cut from it (the old `release/v2.0` branch was merged and deleted on 2026-10-07). Reference the task ID in the commit message, for example `M1-03: resume monitoring after app update`.
4. Tick the task, and add one line to the progress log at the bottom of `MILESTONES.md`.
5. A milestone is done only when every exit criterion is met, not when every box is ticked.
6. Anything out of scope that comes up goes into `FUTURE_FEATURES.md`, not into v2.0.

## v2.0 in one paragraph

Version 2.0 makes Flip to Mute dependable first and attractive second. The
service must keep working after reboots, app updates and aggressive battery
managers, and must tell the user when it cannot. These fixes ship first, on
today's code. The app then moves to Secret Calculator's architecture with no
behaviour change, and only after that gets its new interface: a three step
first run, a home screen with one obvious control, plain language instead of
"monitoring" jargon, and visible proof that the app is doing its job. "Rate
us" keeps today's behaviour.

## Decisions

Listed in full in `UPDATE_PLAN.md` section 13. None of them blocks milestone M0.

- **D9, decided:** build on Secret Calculator's architecture.
- **Open:** D1 rating, D2 AdMob App ID, D3 Crashlytics, D4 ad placement, D5 start after restart by default, D6 Play Console baselines, D7 privacy policy URL, D8 Hindi, D10 Remote Config.
