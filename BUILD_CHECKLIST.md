# Build checklist

Everything ReelCount ships, and everything you have to do by hand.

## Files produced

### Root

| File | Purpose |
| --- | --- |
| `App.tsx` | Consent gate, onboarding gate, tab navigator |
| `index.js` | React Native entry point |
| `app.json` | App registration name (`ReelCount`) |
| `package.json` | Dependencies and scripts |
| `tsconfig.json` | TypeScript config (`npx tsc --noEmit`) |
| `metro.config.js` | Metro bundler |
| `babel.config.js` | Babel preset |
| `.eslintrc.js`, `.prettierrc.js` | Lint and formatting |
| `.editorconfig` | Kotlin style used by ktlint (`android_studio`) |
| `.watchmanconfig` | Watchman |
| `.gitignore` | React Native + Android ignores |
| `README.md` | Description, build steps, permissions, limitations |
| `CONTRIBUTING.md` | How to add a new tracked app |
| `PRIVACY.md` | No-data-leaves-device policy |
| `CHANGELOG.md` | v0.1.0 entry |
| `LICENSE` | GNU GPL-3.0 full text |
| `BUILD_CHECKLIST.md` | This file |
| `.github/workflows/ci.yml` | ESLint, tsc, ktlint, debug APK build, artifact upload |

### TypeScript

| File | Purpose |
| --- | --- |
| `src/native/ReelCount.ts` | Typed bridge to the native module and event subscriptions |
| `src/theme.ts` | Colours, spacing, brain-state brackets, limit clamping |
| `src/storage/firstRun.ts` | Consent and onboarding flags in AsyncStorage |
| `src/components/Charts.tsx` | SVG bar chart, line chart, donut chart |
| `src/navigation/RootNavigator.tsx` | Bottom tabs: Dashboard, Stats, Settings |
| `src/screens/OnboardingScreen.tsx` | Permission explanation with live checks |
| `src/screens/DashboardScreen.tsx` | Animated counter, brain state, per-app list, pull to refresh, live events |
| `src/screens/SettingsScreen.tsx` | Limit slider, toggles, per-app switches, all-day lock, revoke link |
| `src/screens/StatsScreen.tsx` | 7-day bars, 30-day line, per-app donut |

### Android Gradle and resources

| File | Purpose |
| --- | --- |
| `android/build.gradle` | SDK levels, Kotlin 1.9.22, AGP 8.3.2 |
| `android/settings.gradle` | RN autolinking, app module |
| `android/gradle.properties` | AndroidX, Hermes, New Architecture |
| `android/gradle/wrapper/gradle-wrapper.properties` | Gradle 8.6 |
| `android/app/build.gradle` | Room + kapt, security-crypto, coroutines, RN |
| `android/app/proguard-rules.pro` | Keeps modules, Room, coroutines |
| `android/app/src/main/AndroidManifest.xml` | All permissions except INTERNET |
| `android/app/src/debug/AndroidManifest.xml` | Debug-only INTERNET for Metro |
| `android/app/src/main/res/values/strings.xml` | App, accessibility and notification strings |
| `android/app/src/main/res/values/styles.xml` | `AppTheme` |
| `android/app/src/main/res/values/colors.xml` | Overlay and theme colours |
| `android/app/src/main/res/drawable/ic_launcher.xml` | Launcher icon |
| `android/app/src/main/res/drawable/ic_stat_reelcount.xml` | Notification icon |
| `android/app/src/main/res/xml/accessibility_service_config.xml` | Event types, `canRetrieveWindowContent`, view-id flags |

### Kotlin (`com.reelcount`)

| File | Purpose |
| --- | --- |
| `MainApplication.kt` | Registers `ReelCountPackage`, creates channels, warms the DB |
| `MainActivity.kt` | Hosts the React root view |
| `ReelCountModule.kt` | `getTodayCount`, `setLimit`, `getLimit`, `getPerAppCounts`, `isAccessibilityServiceEnabled`, `openAccessibilitySettings`, `requestOverlayPermission`, `setBlockEnabled`, settings and stats helpers; emits `onReelCounted`, `onLimitReached`, `onBlockStateChanged` |
| `ReelCountPackage.kt` | `ReactPackage` registration |
| `ReelCountEvents.kt` | Bridges counted reels to JS and to an app-internal broadcast |
| `service/ReelCountAccessibilityService.kt` | Detection heuristics and the six anti-false-positive rules |
| `service/ReelCountForegroundService.kt` | Persistent "Today: X reels" notification with Stop action |
| `service/BootReceiver.kt` | Restart after boot or app update |
| `ui/OverlayManager.kt` | Draggable pill, tap-to-expand breakdown, 5 s auto-hide, blocker overlay |
| `ui/ReelBlocker.kt` | Limit and all-day-lock enforcement, `GLOBAL_ACTION_BACK`, 5 minute dismissal |
| `db/DailyCount.kt`, `db/DailyCountDao.kt` | Daily totals entity and transactional increment |
| `db/Settings.kt`, `db/SettingsDao.kt` | Settings entity and DAO |
| `db/ReelCountDatabase.kt` | Room singleton, version 1, `exportSchema = false` |
| `data/ReelCountSettings.kt` | Immutable settings snapshot |
| `data/SettingsRepository.kt` | Single write path: Room + EncryptedSharedPreferences mirror |
| `data/CountRepository.kt` | Count reads and increments |
| `util/TrackedApps.kt` | The four package names and labels |
| `util/PerAppCounts.kt` | JSON encode/decode for per-app counts and toggles |
| `util/TimeUtils.kt` | Epoch day and clock helpers |
| `util/AccessibilityUtils.kt` | Accessibility/overlay permission checks and settings intents |
| `notifications/NotificationChannels.kt` | `reelcount_service` (LOW) and `reelcount_blocker` channels |

### Not in the repository

- `android/gradlew`, `android/gradlew.bat`,
  `android/gradle/wrapper/gradle-wrapper.jar` — the wrapper is generated, not
  committed. Create it with `cd android && gradle wrapper --gradle-version 8.6`
  (CI does this too), or let Android Studio create it on first sync.
- The debug signing key — Android Gradle Plugin creates one at
  `~/.android/debug.keystore` on the first build, so nothing needs committing.
  Replace it with a real release keystore before distributing.
- `node_modules/` — `npm install`.
- `package-lock.json` — recreate with `npm install` if you want a pinned tree.

## Manual steps you must perform

### 1. Get the code onto your machine

```bat
git clone https://github.com/amanjangrawork-lab/reelcount.git C:\reelcount
cd C:\reelcount
npm install
cd android
gradle wrapper --gradle-version 8.6
cd ..
```

(Or unzip the copy you downloaded into `C:\reelcount`.)

### 2. Build and install

```bat
npx react-native run-android
```

or open the `android` folder in Android Studio and press Run. A debug APK alone:

```bat
cd android && gradlew assembleDebug
```

### 3. On the phone, first launch

1. **Read the consent screen.** It states that ReelCount uses the Accessibility
   Service, cannot read passwords or messages, and keeps all data on the device.
   Tap **I understand, continue**.
2. **Enable the Accessibility Service.** On the onboarding screen tap **Enable
   Accessibility Service**, find **ReelCount** in Android's list and switch it on.
   Come back — the row turns into a green check.
3. **Allow the overlay** (optional but recommended). Tap **Allow Overlay** and
   grant "Display over other apps" for ReelCount.
4. **Allow notifications** when Android asks (needed for the persistent
   "Today: X reels" notification on Android 13+).
5. Tap **Continue** once Accessibility is on. The tabs open.
6. **Open Instagram Reels** and scroll one or two reels. Return to ReelCount: the
   counter and the notification should show the new total.

### 4. Optional device tuning

- Some OEM skins (Xiaomi, Oppo, Samsung) kill background services. Add ReelCount
  to the battery-optimisation allowlist so the counter survives.
- The notification's **Stop** action ends the foreground service; to stop
  counting entirely, switch the Accessibility Service off in Android settings.

### 5. Before publishing your own build

- Replace `signingConfigs.debug` in `android/app/build.gradle` with your own
  release keystore.
- Bump `versionCode` and `versionName`.
- Capture screenshots into `docs/screenshots/` and link them in `README.md`.

## What was and was not verified here

Verified locally:

- `npx tsc --noEmit` — passes.
- `npx eslint . --ext .js,.jsx,.ts,.tsx` — 0 errors (19 `no-void` warnings remain;
  they are style warnings from the React Native ESLint preset, not failures).
- ktlint 1.2.1 against all Kotlin sources — clean.
- All 8 Android XML files parse as well-formed.
- GitHub Actions: the `TypeScript and ESLint` and `Kotlin formatting` jobs pass on
  every push. The `Debug APK` job runs `./gradlew assembleDebug` on a machine
  with a full Android SDK, so compilation, resource linking and manifest
  merging are covered there.

Not verified:

- Nothing has been run on a phone or emulator yet: detection against the real
  Instagram/YouTube/Facebook/Snapchat view ids, the overlay, the blocker and the
  notification are untested in practice.
- The Gradle wrapper jar is not committed (binary); CI generates it.
