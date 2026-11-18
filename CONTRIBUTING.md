# Contributing to ReelCount

Thanks for helping. ReelCount is deliberately small and offline; changes that
add network access, analytics or ads will not be accepted.

## Adding support for a new app

Detection is driven by one native table. Two places need to agree:

1. **`android/app/src/main/java/com/reelcount/util/TrackedApps.kt`** — add the
   package name constant, include it in `ALL`, add a label in `displayName`, and
   add its view ids to `VIEW_IDS`:

   ```kotlin
   NEW_APP to listOf("reel_player_page_container"),
   ```

   Use the shortest stable suffix of the view id, without the `package:id/`
   prefix. Any id that starts with `reel_player_` is accepted automatically, so a
   new app that follows that convention needs only the package name. The per-app
   toggles, the JSON blobs and the charts pick the new app up automatically.

2. **`src/native/ReelCount.ts`** — add the app to `TRACKED_APPS` so the UI lists
   it. The donut chart colours cycle automatically.

The accessibility service reads `TrackedApps.VIEW_IDS`; it does not keep its own
copy, and a new view id must not be added anywhere else.

### Finding the view ids on a device

```bash
adb shell settings put secure enabled_accessibility_services com.reelcount/.service.ReelCountAccessibilityService
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml
```

Open the Reels/Shorts surface, dump again and diff the dumps. The container that
appears when a reel is on screen is the candidate. Add the shortest suffix that
is still unique (for example `clips_viewer_view_pager`).

Never add ids that begin with `reel_viewer_`: those are Stories and counting them
would inflate every number.

## Rules for detection changes

The six anti-false-positive rules in `detect/ReelDetector.kt` exist because these
apps emit many events that look like a reel but are not. Any change must keep all
of them, and each one has a unit test in
`android/app/src/test/.../ReelDetectorTest.kt`:

1. Reject zero-sized nodes.
2. Require `isVisibleToUser`.
3. Debounce one increment per view id per 1000 ms.
4. Require 2000 ms of visibility before counting.
5. Never count `reel_viewer_*` (Stories).
6. Ignore events from apps that are not in the foreground.

## Local checks

```bash
npm install
npx tsc --noEmit
npx eslint . --ext .js,.jsx,.ts,.tsx
curl -sSLO https://github.com/pinterest/ktlint/releases/download/1.2.1/ktlint && chmod +x ktlint
./ktlint "android/app/src/**/*.kt"
cd android && ./gradlew testDebugUnitTest assembleDebug
```

A behaviour change to detection or settings needs a failing test first. The tests
are plain JVM JUnit; no device or emulator is involved.

## Commit style

Short imperative subjects, one logical change per commit. Include the reason in
the body when the change is not obvious. Please run the checks above before
opening a pull request.

## License

By contributing you agree that your contribution is licensed under GPL-3.0,
matching the rest of the project.
