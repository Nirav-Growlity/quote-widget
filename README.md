# Quote Widget

A small Android home-screen widget that shows **your own quotes**. It's free, has no ads, needs no account, and your quotes never leave the phone.

- **Paste a whole list.** Put one quote per line, or leave a blank line between multi-line quotes. Bullets like `-`, `•` and `1.` are removed.
- **Pin one, several, or none.** If you pin quotes, the widget rotates only through those. With no pins, it rotates through all of them.
- **Choose how often it changes.** Any number of minutes, hours or days. You can also shuffle the order.
- **Tap the quote** on the widget to skip to the next one. Tap the small corner text to open the app.
- **Text that fits itself.** Each widget measures its real size and uses the largest text that shows the whole quote. Resize the widget and the text re-fits. Text never shrinks below a readable 13sp. A quote too long for that becomes scrollable, with a scrollbar and a "Scroll for more" hint.
- **Light, dark, or follow the system.** This applies to both the app and the widget.
- **27 widget themes, all flat colours with no gradients.**
  - *Classic palettes*: Catppuccin, Rosé Pine, Nord, Gruvbox, Dracula, Solarized, Tokyo Night, Everforest, Kanagawa and One Dark, in their light and dark variants.
  - *Originals*: Matcha, Lilac, Apricot, Citrus, Midnight and Mono.
  - *Match my wallpaper*: four Material You styles that take their colours from your wallpaper on Android 12 and newer.
  - Each theme has one accent detail: a quote mark, a side stripe or a corner dot. A quote ending in "— Name" shows the author on their own line.
- **36 fonts for the quote**, all from Google Fonts:
  - *Serif*: Playfair Display, Cormorant, EB Garamond, Lora, Libre Baskerville, Fraunces, Instrument Serif, DM Serif Display, Bodoni Moda and Crimson Pro, with italic versions of several.
  - *Sans*: Manrope, Outfit, Space Grotesk, Josefin Sans, Poppins, Quicksand and Syne.
  - *Handwritten*: Caveat, Dancing Script, Great Vibes, Sacramento, Homemade Apple, Patrick Hand and Kalam.
  - *Typewriter*: Special Elite, Courier Prime, IBM Plex Mono and JetBrains Mono.
  - *Display*: Abril Fatface, Bebas Neue, Alfa Slab One and Righteous.
  - Every font works with every theme, and *Theme font* keeps each theme's own font.
- **Built to be calm.** The app has three tabs. *Quotes* holds your collection, with the quote currently on your widget at the top. *Widget* controls how the widget looks and how often it changes. *Settings* holds app-wide options: light or dark theme, updates, and links to the source and licences.
- **Updates itself from this repo.** Each push to `main` builds a signed APK and publishes it as a release. The app checks for a newer release when you open it and installs it after you confirm.

## Install

1. On your phone, open the [latest release](../../releases/latest) and download `quote-widget.apk`.
2. Open the file. Android will ask you to allow installs from your browser or file manager, so allow it once.
3. Long-press your home screen, choose **Widgets**, then **Quote Widget**, and drag it out.
4. Open the app, tap **Add quotes**, and paste your list.

After this first install, **all updates happen inside the app**, so you never need to come back here. The first time, Android asks you to let *Quote Widget* install apps. Turn that on, press back, and the update carries on by itself. On Android 12 and newer, later updates usually install without even a confirmation screen.

## Battery

The app is built so it does almost nothing in the background:

- **It never wakes the phone.** The rotation timer is a single non-wakeup alarm. If it comes due while the screen is off, it waits and fires the next time you turn the screen on.
- **One timer, only while a widget exists.** If no widget is on the home screen, no timer is set at all.
- **The shortest rotation is 15 minutes**, the same minimum Android uses for its own background jobs.
- **Nothing keeps running.** There are no background services, wake locks or polling. Each quote change takes a few milliseconds, after which Android can close the app completely.
- **It only uses the network in the foreground.** The update check runs only when you open the app (at most once every 10 minutes) or tap **Check now** in Settings. You can turn off the automatic check there. If a newer version exists, a popup offers to install it. The widget never uses the network.

## How releases work

`.github/workflows/release.yml` runs on every push to `main`:

- It builds with Gradle 8.11 and AGP 8.7, using `minSdk 26` (Android 8).
- It signs the APK with the key in the repository secrets `KEYSTORE_B64` and `KEYSTORE_PASSWORD`. Every build must use the **same key**, or Android will refuse to update the app.
- It publishes release `v1.0.<run number>`. The run number is also the app's `versionCode`, which is how the app tells that an update is newer.

## Editing widget styles and fonts

Widget layouts, colours and fonts are generated, so don't edit the output by hand.

- **Themes:** edit `tools/gen_widget_res.py`, then run `python tools/gen_widget_res.py`. It rewrites `res/layout/widget_*`, `res/drawable/wbg_*`, the colour files, `WidgetStyle.kt` and `QuoteFont.kt`.
- **Fonts:** edit the list in `tools/fonts.py`, run `python tools/fetch_fonts.py` (it needs `pip install fonttools`), then run the generator.

Launchers can't load an app's own fonts. So when a library font is chosen, the widget draws the quote as an image in that font. The layout tints the image with the theme's text colour, so wallpaper colours and light/dark still apply.

## Credits

The classic palettes use the published colour values of these open-source themes. They are all MIT licensed:
[Catppuccin](https://github.com/catppuccin/catppuccin), [Rosé Pine](https://github.com/rose-pine/rose-pine-theme), [Nord](https://github.com/nordtheme/nord), [Gruvbox](https://github.com/morhetz/gruvbox), [Dracula](https://github.com/dracula/dracula-theme), [Solarized](https://github.com/altercation/solarized), [Tokyo Night](https://github.com/folke/tokyonight.nvim), [Everforest](https://github.com/sainnhe/everforest), [Kanagawa](https://github.com/rebelot/kanagawa.nvim) and [One Dark](https://github.com/atom/atom/tree/master/packages/one-dark-ui).

The fonts come from [Google Fonts](https://fonts.google.com) under the SIL Open Font License or the Apache License. Each licence is in [FONT_LICENSES.txt](FONT_LICENSES.txt). Fonts whose licence allows it are trimmed to Latin characters to keep the app small. Fonts with a Reserved Font Name are included unmodified.
