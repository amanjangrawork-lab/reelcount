# Contributing to ReelCount

Thanks for helping. ReelCount is deliberately small and offline; changes that
add network access, analytics or ads will not be accepted.

## Adding support for a new app

Detection is driven by one table. Three places need to agree:

1. **`android/app/src/main/java/com/reelcount/util/TrackedApps.kt`** — add the
   package name constant and include it in `ALL`, plus a display name in
   `displayName`. The per-app toggles, the JSON blobs and the charts pick the new
   app up automatically.

2. **`android/app/src/main/java/com/reelcount/service/ReelCountAccessibilityService.kt`**
   — add an entry to `viewIdsByPackage`:

   ```kotlin
   TrackedApps.NEW_APP to listOf("reel_player_page_container"),
   ```

   Use the shortest stable suffix of the view id, without the `package:id/`
   prefix. Any id that starts with `reel_player_` is accepted automatically, so a
   new app that follows that convention needs only the package name.

3. **`src/native/ReelCount.ts`** — add the app to `TRACKED_APPS` so the UI lists
   it. The donut chart colours cycle automatically.

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

The six anti-false-positive rules in
`ReelCountAccessibilityService` exist because these apps emit many events that
look like a reel but are not. Any change must keep all of them:

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
cd android && ./gradlew assembleDebug
```

## Commit style

Short imperative subjects, one logical change per commit. Include the reason in
the body when the change is not obvious. Please run the checks above before
opening a pull request.

## License

By contributing you agree that your contribution is licensed under GPL-3.0,
matching the rest of the project.
