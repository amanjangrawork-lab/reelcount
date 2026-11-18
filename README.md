# ReelCount

A privacy-first Android app that counts how many short-form videos you scroll
through — Instagram Reels, YouTube Shorts, Facebook Reels and Snapchat Spotlight —
and optionally blocks access when you hit a daily limit.

No internet permission. No analytics. No ads. GPL-3.0.

## Why

Short-form video is designed to be uncountable. ReelCount makes the number
visible, on your own device, and gives you a way to stop when you said you would.

## Screenshots

| Dashboard | Stats | Settings | Floating pill |
| --- | --- | --- | --- |
| _placeholder_ | _placeholder_ | _placeholder_ | _placeholder_ |

Screenshots live in `docs/screenshots/` once captured on a device.

## Features

- Live counter for today's Reels/Shorts with a brain-state colour
  (green 0–50, yellow 51–100, orange 101–200, red 200+).
- Per-app breakdown (Instagram, YouTube, Facebook, Snapchat).
- Optional daily limit with a full-screen reminder and automatic Back press.
- Optional all-day lock.
- Draggable floating pill showing the count, tappable for the per-app breakdown.
- 7-day bar chart, 30-day line chart, 30-day per-app donut.
- Persistent notification with today's total and a Stop action.

## Prerequisites

- Node.js 18 or newer, npm 9 or newer.
- JDK 17 and the Android SDK (platform 34, build-tools 34.0.0).
- A physical Android 10+ (API 29) device. Emulators do not have Instagram or
  YouTube installed, so counting can only be tested on a real phone.
- React Native 0.74.5, Kotlin 1.9.22, Gradle 8.6, AGP 8.3.2, Room 2.6.1.

## Build

```bash
npm install
npm run tsc          # TypeScript check
cd android && ./gradlew assembleDebug
```

The debug APK lands in `android/app/build/outputs/apk/debug/app-debug.apk`.

To install and run with Metro:

```bash
npx react-native run-android
```

Release build (signed with the debug key — replace before distributing):

```bash
cd android && ./gradlew assembleRelease
```

### Gradle wrapper

`android/gradle/wrapper/gradle-wrapper.properties` pins Gradle 8.6. The wrapper
scripts (`gradlew`, `gradlew.bat`) and the binary `gradle-wrapper.jar` are
generated rather than committed, so create them once before the first build:

```bash
cd android && gradle wrapper --gradle-version 8.6
```

Alternatively, open the `android/` folder once in Android Studio, which creates
the wrapper during its first Gradle sync. CI does this automatically
(`gradle wrapper` step in `.github/workflows/ci.yml`).

## Permissions and why they exist

| Permission | Why |
| --- | --- |
| `BIND_ACCESSIBILITY_SERVICE` (the Accessibility Service) | The only public Android API that reports which screen is showing and which view ids are on it. Used to detect a reel container and to press Back at the limit. |
| `SYSTEM_ALERT_WINDOW` | Optional. Draws the floating counter pill and the limit reminder. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the counter and its notification alive while you use other apps. |
| `POST_NOTIFICATIONS` | Shows the "Today: X reels" notification. |
| `RECEIVE_BOOT_COMPLETED` | Restarts the counter after a reboot. |

**There is no `INTERNET` permission in the release manifest.** The debug variant
adds it (in `android/app/src/debug/AndroidManifest.xml`) solely so Metro can
serve JavaScript to your phone during development.

## How detection works

The accessibility service looks for known reel container view ids
(`reel_player_page_container`, `clips_viewer_view_pager`,
`shorts_player_container`) in the window tree of the four supported apps, then
applies six rules before anything is counted:

1. Zero-sized nodes are rejected.
2. `isVisibleToUser` must be true.
3. The same view id cannot increment twice within 1000 ms.
4. The container must have been visible for at least 2000 ms.
5. Stories (`reel_viewer_*` ids) are never counted.
6. Events are ignored unless the tracked app is the foreground app.

Only view ids and visibility are read — never text, so passwords, messages and
post content are never captured.

## Where settings are stored

Room is the authoritative store: `daily_counts` and a single-row `settings`
table in `reelcount.db`, on the device only.

Because the accessibility service must read settings synchronously on every
scroll event, `SettingsRepository` keeps an `EncryptedSharedPreferences`
(`androidx.security:security-crypto`) mirror that is loaded once, cached in
memory, and updated by the same single write path as the Room row. If the
Android keystore is unavailable on a device, the mirror falls back to private
preferences and Room remains authoritative.

## Project layout

```
App.tsx                       consent gate, onboarding gate, tabs
src/native/ReelCount.ts       typed bridge to the native module
src/screens/                  Dashboard, Stats, Settings, Onboarding
src/components/Charts.tsx     SVG bar, line and donut charts
android/app/src/main/java/com/reelcount/
  MainActivity.kt, MainApplication.kt, ReelCountModule.kt, ReelCountPackage.kt
  service/                    accessibility, foreground service, boot receiver
  ui/                         OverlayManager, ReelBlocker
  db/                         Room entities, DAOs, database
  data/                       repositories
  util/                       tracked apps, JSON helpers, time helpers
```

## Known limitations

- **App UI updates break detection.** Instagram, YouTube, Facebook and Snapchat
  can rename or restructure their view ids at any time. When that happens the
  count silently stops for that app until the ids in
  `ReelCountAccessibilityService.viewIdsByPackage` (and `CONTRIBUTING.md`) are
  updated. ReelCount cannot detect anything it has no ids for.
- Counting is heuristic: a reel is counted when a recognised container is
  visible for 2 seconds and the view scrolls. Videos watched without a scroll
  event may be undercounted.
- The accessibility service cannot be stopped from inside the app on modern
  Android; the notification's Stop action ends the foreground service, and full
  removal requires switching the service off in Android settings.
- Reels opened from the Instagram/Facebook in-app browser surfaces, or via
  Android's system share sheet previews, may not be recognised.
- Tested against the view ids current at v0.1.0 (2026). No device automation is
  possible in CI, so the accessibility heuristics are not covered by automated
  tests.
- The Gradle release build is signed with the debug key. Anyone shipping this
  needs their own signing config.

## License

ReelCount is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

ReelCount is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

The full text is in [LICENSE](LICENSE). Copyright (C) 2026 ReelCount
contributors.
