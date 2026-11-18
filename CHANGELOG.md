# Changelog

All notable changes to ReelCount are documented here. This project follows
[Semantic Versioning](https://semver.org/).

## [0.1.1] - 2026-11-18

Correctness and pruning pass. No new features; the counting behaviour is
unchanged apart from the fixes below.

### Fixed

- The persistent notification kept its tap target and its Stop action after a
  count update. Previously both were silently dropped by a second, separate
  notification builder.
- The foreground package is now cleared when an untracked app comes forward, so
  Rule 6 ("ignore background apps") holds while you are in another app.
- The full-screen blocker is removed when the accessibility service is torn
  down, so disabling the service while blocked can no longer leave an overlay
  covering the screen.
- The dashboard reports the actual failure instead of describing every error as
  "Accessibility Service is off".
- The consent screen no longer offers a "Not now" button that did nothing.

### Changed

- Detection rules moved into `detect/ReelDetector.kt` as pure functions and are
  covered by JVM unit tests (`./gradlew testDebugUnitTest`).
- The settings mirror is a plain app-private `SharedPreferences` cache instead of
  `EncryptedSharedPreferences`; the alpha `androidx.security:security-crypto`
  dependency is gone and the privacy documentation now matches the code.
- Tracked apps, their labels and their view ids live in one native table
  (`util/TrackedApps.kt`); overlay colours live in `colors.xml`.
- Room exports its schema to `android/app/schemas`.
- CI runs the unit tests and builds the release variant (R8/ProGuard), not just
  the debug APK.

### Removed

- Unused code: the app-internal broadcast channel that had no receiver, four
  bridge methods no screen called (`getLimit`, `getSettingsFromDatabase`,
  `openOverlaySettings`, `dismissBlockForFiveMinutes`), four DAO methods, three
  time helpers, the unused `reelcount_blocker` notification channel, an unused
  string, and the hardcoded overlay hex colours.

## [0.1.0] - 2026-11-18

First public source release.

### Added

- Accessibility Service that counts Instagram Reels, YouTube Shorts, Facebook
  Reels and Snapchat Spotlight using view-id heuristics with six
  anti-false-positive rules (zero-size rejection, visibility requirement,
  1000 ms debounce per view id, 2000 ms dwell time, Stories excluded, foreground
  app only).
- Room persistence: `daily_counts` (one row per day with a per-app JSON
  breakdown) and a single-row `settings` table, plus an EncryptedSharedPreferences
  mirror for synchronous reads on the accessibility path.
- Foreground service with a persistent "Today: X reels" notification and a Stop
  action, restarted after reboot by `BootReceiver`.
- Draggable floating counter pill with a tap-to-expand per-app breakdown that
  auto-hides after five seconds.
- Daily limit enforcement: automatic `GLOBAL_ACTION_BACK` plus a full-screen
  reminder with a five-minute dismissal, and an all-day lock.
- Native TurboModule bridge (`ReelCount`) exposing counts, settings and
  permission helpers, with `onReelCounted`, `onLimitReached` and
  `onBlockStateChanged` events.
- React Native UI: first-run consent, onboarding with live permission checks,
  dashboard with animated counter and brain-state colour, 7-day and 30-day stats
  with SVG charts, and a settings screen.
- GitHub Actions CI: ESLint, TypeScript, ktlint, debug APK build and artifact
  upload.

### Security

- No `INTERNET`, `ACCESS_NETWORK_STATE` or `AD_ID` permissions in the release
  manifest; `allowBackup` disabled.
