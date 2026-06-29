# Minimal — a text-only Android launcher

A minimalist home screen: clock + date, your favourite apps as plain text, and a
swipe-up drawer that lists every app by name with fast, Greek-aware search. Built
with Kotlin + Jetpack Compose, styled with JetBrains Mono, and themeable with 38
built-in colour schemes.

## Features

### Home (page 1)
- Large clock with the date underneath. The date capitalises its first letter so
  it reads naturally in Greek and other locales.
- Favourite apps listed as plain text, in the order you add them.
- Tap the time to open the clock app; tap the date to open the calendar.
- Long-press anywhere to open **Settings**.

### Drawer (page 2)
- Swipe **up** from home. The search box **auto-focuses** and the keyboard opens,
  so you can start typing immediately.
- **Greek-aware search:** case- and accent-insensitive, working in Greek and
  Latin alike — `αθηνα` matches `Αθήνα`, `cafe` matches `Café`. The Greek final
  sigma (`ς`/`σ`) is folded too.
- **Single-result auto-launch:** when your query narrows to exactly one app, it
  launches automatically (after a short debounce so it never fires mid-word).
  Pressing the keyboard's **Search/Enter** key launches the top result at once.
- Full app list otherwise, alphabetical, names only.
- **Long-press an app** for: add/remove favourite, hide, app info, uninstall.

### Settings
- **Theme picker:** 38 schemes (Warm Dark, Paper, Tokyo Night, Nord, Gruvbox,
  Dracula, Catppuccin, Rosé Pine, Solarized, and many more — light and dark).
  Tap a swatch to apply it instantly; the choice is saved.
- 12h / 24h clock toggle.
- "Set as default launcher" button.
- Pick favourites (tap the star); hide / unhide apps.

### Under the hood
- **Live app list:** installs, uninstalls, and updates appear immediately via a
  package-change broadcast receiver — no manual refresh, no need to leave the app.
- **JetBrains Mono** is bundled (OFL licence in `/licenses`) and applied globally,
  including Greek glyphs.
- **Edge-to-edge** with transparent system bars; the status/navigation icon
  colour flips automatically so it stays readable on both light and dark themes.
- Pressing Home (or swiping back to page 1) clears search focus and dismisses the
  keyboard.

## How to build
1. Open the `MinimalLauncher` folder in **Android Studio** (Ladybug or newer).
2. Let it sync — it downloads the Gradle 8.9 wrapper and dependencies on first run.
3. Run on a device or emulator (minSdk 26 / Android 8.0+).
4. Press the system Home button and choose **Minimal**, or use Settings →
   "set as default launcher".

> If install fails with `INSTALL_FAILED_USER_RESTRICTED` (common on Xiaomi /
> MIUI / HyperOS), enable **Install via USB** in Developer options.
>
> Command line: once Android Studio has generated the Gradle wrapper jar you can
> also run `./gradlew assembleDebug`.

## Project layout
```
app/src/main/
├── AndroidManifest.xml          # HOME intent-filter makes it a launcher
├── java/com/example/minimallauncher/
│   ├── data/                    # app querying, settings (DataStore), search normaliser
│   │   ├── AppInfo.kt
│   │   ├── AppLauncher.kt       # launch / app-info / uninstall / clock / calendar intents
│   │   ├── AppRepository.kt     # queries launchable apps, locale-sorted
│   │   ├── SettingsRepository.kt# favourites, hidden, 24h, theme  (DataStore)
│   │   └── TextNormalizer.kt    # Greek + Latin accent/case folding for search
│   └── ui/
│       ├── MainActivity.kt      # vertical pager, edge-to-edge, Home/Back handling
│       ├── LauncherViewModel.kt # app/favourite/search state + live package updates
│       ├── HomeScreen.kt        # clock, date, favourites
│       ├── DrawerScreen.kt      # search, app list, auto-focus, auto-launch
│       ├── SettingsScreen.kt    # theme picker, toggles, favourites/hidden
│       ├── Common.kt            # shared SearchField + helpers
│       └── theme/
│           ├── Color.kt         # LauncherPalette, all 38 themes, colour accessors
│           ├── Theme.kt         # provides the active palette + Material scheme
│           └── Type.kt          # JetBrains Mono typography
└── res/font/                    # JetBrains Mono ttf files
```

## Customising
- **Colours / themes:** every theme lives in `ui/theme/Color.kt`. Each is built
  from seven base colours (bg, surface, border, text, text-2, accent,
  accent-soft); the rest are derived with the same `color-mix` maths as the source
  CSS. Add a new `ThemeOption(...)` to the `AppThemes` list and it appears in the
  picker automatically.
- **The colour names** used across the UI (`Bg`, `Accent`, `TextPrimary`, …) are
  composable accessors that read the currently selected theme, so screens never
  reference a fixed colour.

## Possible next steps
- Drag-to-reorder favourites (currently shown in the order added).
- Per-app rename / alias.
- Hidden-apps unlock (reveal a hidden app only by typing its exact name).
- Home gestures (swipe-down for notifications, double-tap to lock).
- Work-profile / multi-user apps via `LauncherApps`.
- A unit test for `TextNormalizer`, and R8 + a signing config for release builds.