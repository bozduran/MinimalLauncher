# Minimal — a text-only Android launcher

A minimalist home screen: clock + date, your favourite apps as plain text, and a
swipe-up drawer listing every app by name with search (Greek-aware). Built with
Kotlin + Jetpack Compose, styled with JetBrains Mono and a warm dark palette.

## Features
- **Home (page 1):** large clock, date underneath, then favourite apps as text.
  Tap the time to open the clock app, tap the date for the calendar.
- **Drawer (page 2):** swipe **up** from home. Full app list, alphabetical, names
  only. Search box at the top.
- **Search** is case- and accent-insensitive and works in **Greek** as well as
  Latin: typing `αθηνα` matches `Αθήνα`, `cafe` matches `Café`. Greek final sigma
  (`ς`/`σ`) is folded too.
- **Settings:** long-press anywhere on the home screen.
  - Pick favourites (tap the star).
  - 12h / 24h clock toggle.
  - "Set as default launcher" button.
  - Hide / unhide apps.
- **Long-press an app** in the drawer for: add/remove favourite, hide, app info,
  uninstall.
- **JetBrains Mono** is bundled (OFL licence in `/licenses`) and applied globally.

## How to build
1. Open the `MinimalLauncher` folder in **Android Studio** (Ladybug or newer).
2. Let it sync — it will download the Gradle 8.9 wrapper and dependencies.
3. Run on a device or emulator (minSdk 26 / Android 8.0+).
4. Press the system Home button and choose **Minimal** to set it as your launcher,
   or use Settings → "set as default launcher".

> Command line: once Android Studio has generated the Gradle wrapper jar you can
> also run `./gradlew assembleDebug`.

## Project layout
```
app/src/main/
├── AndroidManifest.xml          # HOME intent-filter makes it a launcher
├── java/com/example/minimallauncher/
│   ├── data/                    # app querying, settings (DataStore), search normaliser
│   └── ui/                      # MainActivity + Compose screens + theme
└── res/font/                    # JetBrains Mono ttf files
```

## Customising the colours
All colours live in `ui/theme/Color.kt`, mapped 1:1 from the supplied palette
(`--bg`, `--accent`, etc.). Change them there and the whole UI updates.

## Notes / possible next steps
- Drag-to-reorder favourites (currently they appear in the order you add them).
- Optional auto-focus of the search box when entering the drawer.
- Light theme variant.
- Home-screen gestures (double-tap to lock, swipe-down for notifications).
