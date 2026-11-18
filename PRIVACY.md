# Privacy

ReelCount collects nothing and sends nothing. There is no server to send it to.

## What ReelCount can see

The Accessibility Service reads two things from the supported apps:

- the package name of the app in the foreground, and
- whether a known video-container view id is present and visible on screen.

It does **not** read text, passwords, messages, photos, search history, account
names or post content. The code never calls `getText()`, never inspects the
clipboard, and never takes screenshots.

## What ReelCount stores

Two things, on this device, in `reelcount.db` (a Room/SQLite database inside the
app's private storage):

- `daily_counts`: one row per day with the total and a per-app count.
- `settings`: your limit, toggles and any all-day lock.

Settings are additionally mirrored into `EncryptedSharedPreferences` so the
accessibility service can read them without touching the disk on every scroll
event.

## What leaves the device

Nothing. The release build declares no `INTERNET` permission, so it is not
capable of making a network request even if it tried. There is no analytics SDK,
no crash reporter, no advertising identifier and no `AD_ID` permission.

The only network permission in this repository is in the **debug** manifest,
where React Native's Metro bundler needs it to serve JavaScript to your phone
while you are developing. Debug builds are never published.

## Deleting your data

Uninstalling ReelCount removes the database, the encrypted preferences and every
count. Nothing is backed up: `android:allowBackup` is `false`.

## Third parties

None. ReelCount has no accounts, no sign-in and no third-party SDKs.

Questions or concerns: open an issue on the repository.
