# Quote Widget

A small Android home-screen widget that shows **your own quotes**. It's free, has no ads, needs no account, and your quotes never leave the phone.

- **Paste a whole list.** Put one quote per line, or leave a blank line between multi-line quotes. Bullets like `-`, `•` and `1.` are removed.
- **Pin one, several, or none.** If you pin quotes, the widget rotates only through those. With no pins, it rotates through all of them.
- **Choose how often it changes.** Any number of minutes, hours or days. You can also shuffle the order.
- **Tap the quote** on the widget to skip to the next one. Tap the small corner text to open the app.
- **Material You.** On Android 12 and newer, the widget and app use your wallpaper colours. You can pick from four styles (tinted, neutral, bold accent, transparent), plus text size and alignment, with a live preview.
- **Updates itself from this repo.** Each push to `main` builds a signed APK and publishes it as a release. The app checks for a newer release when you open it and installs it after you confirm.

## Install

1. On your phone, open the [latest release](../../releases/latest) and download `quote-widget.apk`.
2. Open the file. Android will ask you to allow installs from your browser or file manager, so allow it once.
3. Long-press your home screen, choose **Widgets**, then **Quote Widget**, and drag it out.
4. Open the app, tap **Paste quotes**, and you're done.

The first time you install an update, Android asks you to let *Quote Widget* install apps. Turn that on once. After that, updates are one tap.

## How releases work

`.github/workflows/release.yml` runs on every push to `main`:

- It builds with Gradle 8.11 and AGP 8.7, using `minSdk 26` (Android 8).
- It signs the APK with the key in the repository secrets `KEYSTORE_B64` and `KEYSTORE_PASSWORD`. Every build must use the **same key**, or Android will refuse to update the app.
- It publishes release `v1.0.<run number>`. The run number is also the app's `versionCode`, which is how the app tells that an update is newer.
