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

## 3b. Install the APK from the CI artifact

1. Open the **Actions** run on github.com in a **mobile browser** — the GitHub
   mobile app cannot download artifacts.
2. Scroll to **Artifacts** at the bottom and tap **reelcount-apks**.
3. Unzip it (Files by Google, or ZArchiver) and install **`release/app-release.apk`**.
   The debug APK needs Metro running on a computer, so it shows a red screen on
   its own.
4. Allow "install unknown apps" for the browser or files app when asked, and pass
   the Play Protect warning with **More details → Install anyway**. The warning
   appears because the app asks for the Accessibility permission and is not
   distributed through Play; it is not a malware report.
5. On Android 13+, Android blocks the Accessibility switch for sideloaded apps.
   If the service will not turn on, open **Settings → Apps → ReelCount →** the ⋮
   menu **→ Allow restricted settings**, then enable it again.
6. Upgrading over an older ReelCount build: builds from before 0.1.2 each had a
   different signature, so uninstall the old one first. From 0.1.2 on the
   committed side-load key keeps the signature stable and upgrades install
   normally.

## 4. Device quirks worth knowing

- Some OEM skins (Xiaomi, Oppo, Samsung) kill background work. Add ReelCount to
  the battery-optimisation allowlist.
- The notification's **Stop** action ends the foreground service. To stop
  counting entirely, switch the Accessibility Service off in Android settings.
- If a count silently stops for one app, that app renamed its view ids — see
  `CONTRIBUTING.md`.

## 5. Before publishing your own build

- Replace the committed side-load key (`android/app/reelcount-dev.jks.base64`,
  alias `reelcount`, password `reelcountdev`) with a real private release keystore
  kept outside the repository. That key exists only so repeated CI builds share
  one signature.
- Bump `versionCode` and `versionName`.
- Commit the Room schema that the first build writes to `android/app/schemas/`.
- Capture screenshots and add them to the README if you want them there.

## What CI verifies

- `npx tsc --noEmit` and `npx eslint` (TypeScript, React Native preset).
- ktlint 1.2.1 over all Kotlin sources.
- `./gradlew testDebugUnitTest` — detection rules, blocking policy, settings logic,
  JSON helpers.
- `./gradlew assembleDebug` and `./gradlew assembleRelease` — compilation,
  resource linking, manifest merging and the R8/ProGuard path.
- Both APKs are uploaded as the `reelcount-apks` artifact.

What CI cannot verify, because there is no device: that the view ids match the
current Instagram/YouTube/Facebook/Snapchat builds, and that the overlay, blocker
and notification behave as intended in use.
