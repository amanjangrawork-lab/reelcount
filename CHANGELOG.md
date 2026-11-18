# Changelog

All notable changes to ReelCount are documented here. This project follows
[Semantic Versioning](https://semver.org/).

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
