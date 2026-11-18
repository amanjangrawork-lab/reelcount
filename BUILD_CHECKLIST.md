# Build checklist

The manual steps. What each file does lives in `README.md` and in the code's own
comments; this file deliberately does not keep a second copy of the file tree.

## 1. Get the code and dependencies

```bash
git clone https://github.com/amanjangrawork-lab/reelcount.git
cd reelcount
npm install
cd android && gradle wrapper --gradle-version 8.6 && cd ..
```

The wrapper scripts and `gradle-wrapper.jar` are generated, not committed. CI does
the same step.

## 2. Build and install

```bash
npx react-native run-android
```

or, for an APK only:

```bash
cd android && ./gradlew assembleDebug
```

## 3. On the phone, first launch

1. Read the consent screen and tap **I understand, continue**.
2. **Enable the Accessibility Service**: tap the button, find **ReelCount** in
   Android's list and switch it on. Come back; the row turns green.
3. **Allow the overlay** if you want the floating pill and the full-screen
   reminder.
4. Allow notifications when Android asks (the persistent "Today: X reels"
   notification needs it on Android 13+).
5. Tap **Continue**, open Instagram Reels, scroll two reels, return: the counter
   and the notification should both have moved.

## 4. Device quirks worth knowing

- Some OEM skins (Xiaomi, Oppo, Samsung) kill background work. Add ReelCount to
  the battery-optimisation allowlist.
- The notification's **Stop** action ends the foreground service. To stop
  counting entirely, switch the Accessibility Service off in Android settings.
- If a count silently stops for one app, that app renamed its view ids — see
  `CONTRIBUTING.md`.

## 5. Before publishing your own build

- Replace `signingConfigs.debug` in `android/app/build.gradle` with a real release
  keystore; the release variant currently uses the debug key.
- Bump `versionCode` and `versionName`.
- Commit the Room schema that the first build writes to `android/app/schemas/`.
- Capture screenshots and add them to the README if you want them there.

## What CI verifies

- `npx tsc --noEmit` and `npx eslint` (TypeScript, React Native preset).
- ktlint 1.2.1 over all Kotlin sources.
- `./gradlew testDebugUnitTest` — detection rules, settings logic, JSON helpers.
- `./gradlew assembleDebug` and `./gradlew assembleRelease` — compilation,
  resource linking, manifest merging and the R8/ProGuard path.
- Both APKs are uploaded as the `reelcount-apks` artifact.

What CI cannot verify, because there is no device: that the view ids match the
current Instagram/YouTube/Facebook/Snapchat builds, and that the overlay, blocker
and notification behave as intended in use.
