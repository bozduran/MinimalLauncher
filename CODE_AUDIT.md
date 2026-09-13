# Minimal Launcher — Code & Architecture Audit

**Audited revision:** `master` @ `2bb87a0` ("Merge pull request #2 from bozduran/feature/improve-allaround")
**Scope:** `:app` (single module, ~1,500 LOC Kotlin, 35 source/resource files)
**Focus:** stability, correctness, performance, maintainability — not visual redesign
**Companion document:** [`USER_STORIES.md`](./USER_STORIES.md) (every finding below maps to at least one story there)

---

## 0. Baseline & how to read this document

| Property | Value |
| --- | --- |
| Module | `:app` only, no `:core`/`:domain` split |
| Package | `com.example.minimallauncher` (applicationId is still `com.example.*`) |
| min / target / compile SDK | 26 / 35 / 35 |
| Kotlin / AGP / Compose BOM | 2.0.21 / 8.7.2 / 2024.10.01 |
| Dependencies | core-ktx, lifecycle-runtime-ktx, lifecycle-viewmodel-compose, activity-compose, compose-ui/foundation/material3, datastore-preferences |
| Not present | DI framework, coroutines-test, Robolectric, `ui-test-junit4`, Macrobenchmark, Baseline Profile plugin, lint config, CI, signing config, Gradle wrapper scripts |
| Structure | `data/` (5 files) + `ui/` (6 files) + `ui/theme/` (3 files) |
| Tests | **none** — `app/src/` contains only `main/` |

Findings are referenced by ID everywhere in this repository's backlog:

- `F#` — potential bug or edge case (§1)
- `S#` — bad practice or code smell (§2)
- `R#` — robustness/resilience recommendation (§3)

Severity here means *user-visible blast radius on a launcher*, which is stricter than usual: this app is the user's **HOME** activity, so a crash or a stalled first frame is not "one screen broke", it is "the phone has no home screen until the user recovers manually".

---

## 1. Potential bugs & edge cases

### F1 — Uncaught exceptions in `viewModelScope` crash the whole launcher 🔴 Critical

**Evidence**

- `LauncherViewModel.kt:105-110` — `refresh()` calls `appRepo.loadApps()` inside `viewModelScope.launch` with no `try/catch`.
- `LauncherViewModel.kt:121-124` — `toggleFavorite`, `toggleHidden`, `setUse24h`, `setTheme` all `launch` DataStore `edit` calls with no `try/catch`.
- `AppRepository.kt:19-30` — `packageManager.queryIntentActivities(...)`, `ri.loadLabel(pm)` and `activityInfo` access are unguarded.

**Why it fails.** `viewModelScope` is backed by `SupervisorJob() + Dispatchers.Main.immediate`. `SupervisorJob` only prevents *sibling cancellation* — it does **not** install an exception handler, so an uncaught exception in a `launch` block propagates to the thread's uncaught-exception handler and **kills the process**. The realistic triggers are not exotic:

- `SecurityException` / `RuntimeException` from `queryIntentActivities` when a package is being installed or removed concurrently (the receiver fires at exactly that moment — see F12),
- `DeadObjectException` / `TransactionTooLargeException` from `loadLabel` under memory pressure,
- `IOException` from DataStore `edit` (disk full, corrupt file) on every settings toggle,
- OEM `PackageManager` implementations that throw where AOSP returns empty lists.

**Blast radius.** A launcher crash does not just close an app: the system has no HOME activity, so the user is dropped to a blank screen / fallback launcher, and because `stateNotNeeded="true"` is set (F8/S8) recovery can land in an unexpected state.

---

### F2 — A DataStore read failure silently freezes all settings forever 🟠 High

**Evidence**

- `SettingsRepository.kt:25-34` — `context.dataStore.data.map { … }` with no `.catch { }` on any of the four flows.
- `LauncherViewModel.kt:42-58` — each flow is `stateIn(viewModelScope, SharingStarted.Eagerly, <default>)`.

**Why it fails.** `DataStore.data` emits an `IOException` (and registers it in the `DataStore` instance) if the backing file is unreadable or corrupt. With no `catch`, the upstream flow throws, the `stateIn` coroutine dies, and the exposed `StateFlow` **keeps its initial value permanently**. There is no crash, no log, and no retry — the user simply sees "24-hour clock: on", "theme: warm dark", "no favorites" forever, and every subsequent toggle appears to do nothing (writes also fail, per F1). This is the worst failure mode in the app: silent, total, and persistent until app data is cleared.

**Correct pattern.** `catch { if (it is IOException) emit(emptyPreferences()) else throw it }` per flow, plus a surfaced one-shot error event so the UI can tell the user rather than lying to them.

---

### F3 — Concurrent, un-conflated refreshes can publish a stale app list 🟠 High

**Evidence**

- `MainActivity.kt:61-65` — `onResume()` calls `vm.refresh()` on every resume.
- `LauncherViewModel.kt:76-80` — the `packageReceiver` calls `refresh()` on every `PACKAGE_*` broadcast.
- `LauncherViewModel.kt:105-110` — each call spawns a fresh coroutine and then does `allAppsFlow.value = apps`.

**Why it fails.** `refresh()` has no de-duplication, no `Mutex`, and no ordering. Two calls issued in quick succession hop to `Dispatchers.IO` (a multi-threaded pool) and can complete **out of order**. The classic sequence is: resume → load #1 starts; a package broadcast → load #2 starts and finishes; load #1 then finishes and overwrites the state with the pre-change list. The UI shows a list that does not match the device until something else triggers a refresh. The same mechanism makes a Play Store batch update (or `adb install-multiple`) issue one full `PackageManager` query + label load *per broadcast*, on the main thread's coroutine scope, during some of the busiest moments of the device's life (F12).

---

### F4 — The home screen becomes unreachable with enough favorites 🔴 Critical

**Evidence**

- `HomeScreen.kt:65-75` — root is a plain `Column` with `.fillMaxSize()`, `.padding(horizontal = 30.dp)` and `verticalArrangement = Arrangement.Center`. There is **no** `verticalScroll` or `LazyColumn`.
- `HomeScreen.kt:95-122` — fixed-height content: 66sp/70sp clock, 15sp date, `Spacer(54.dp)`, one row per favorite at ~50dp (23sp text + `padding(vertical = 11.dp)`), `Spacer(40.dp)`, "↑ apps".
- `HomeScreen.kt:97-104` — empty state replaces the list, so a user never notices the limit until they pass it.

**Why it fails.** On a typical 800dp-tall phone the fixed block (clock 70 + date ~20 + spacers 6/54/40 + footer ~16 ≈ 206dp) plus 50dp per favorite overflows at roughly **12 favorites**. When a `Column` with `Arrangement.Center` has *negative* free space, content is pushed symmetrically off **both** edges: the clock is clipped at the top *and* the last favorites are clipped at the bottom, with no scroll gesture available (the only vertical gesture is the `VerticalPager`, which is the opposite direction to what the user needs). Those apps are simply unopenable from Home.

**Complication for the fix.** The obvious fix (`Modifier.verticalScroll`) competes with the `VerticalPager`'s vertical drag on `MainActivity.kt:99-109`. The recommended direction is to keep Home as a single viewport and make the *favorites list* the scrollable/overlaid layer (or move overflow into the drawer), not to add a nested scroll container that fights the pager. See STAB-4 for the two acceptable resolutions.

---

### F5 — Two different notions of "an app" cause duplicate and mis-keyed entries 🟠 High

**Evidence**

- `AppInfo.kt:8` — `val key = "$packageName/$activityName"`.
- `AppRepository.kt:29` — `.distinctBy { it.key }` (i.e. dedupe per *activity*).
- `SettingsRepository.kt:19-20` — favorites and hidden are keyed by **package name** only (`List<String>` / `Set<String>`).
- `LauncherViewModel.kt:60-64` — `favorites` resolves favorites by `apps.associateBy { it.packageName }`, so one package key fans out to *every* activity of that package.
- `LauncherViewModel.kt:121-122` — `toggleFavorite(pkg)` / `toggleHidden(pkg)` take the package name.

**Why it fails.** An app that publishes more than one `MAIN`/`LAUNCHER` activity is a normal case (some vendors ship a separate "Settings"/"Camera" launcher alias, and many apps ship one launcher activity per product variant). Consequences today:

1. The drawer and the settings list show the **same app label multiple times** (`distinctBy { it.key }` does not collapse them).
2. Starring *one* of those rows adds the package, which makes `favorites` return **all** of that package's activities — so Home shows two or three identical lines.
3. `LazyColumn` keys (`it.key`) are activity-based while the menu's `isFavorite` check (`DrawerScreen.kt:132`) is package-based, so the UI can be internally inconsistent.
4. "Hide app" on one row hides every row for that package (probably desirable) while "add to favorite" adds every row (probably not).

This is a single root cause — an inconsistent identity model — with four symptoms, which is why it is one story (DATA-1) rather than four.

---

### F6 — Hiding an app leaves it on the home screen 🟠 High

**Evidence**

- `LauncherViewModel.kt:60-64` — `favorites` combines `allAppsFlow` with `favoritePkgs` only; `hiddenPkgs` is not consulted.
- `LauncherViewModel.kt:67-73` — `drawerApps` *does* filter by `hiddenPkgs`.
- `SettingsRepository.kt:36-52` — `toggleFavorite` and `toggleHidden` are independent; neither cleans up the other.
- `LauncherViewModel.kt:62-63` — `favs.mapNotNull { byPkg[it] }` silently drops favorites for packages that no longer resolve.

**Why it fails.** "Hide app" is offered in the drawer's long-press menu (`DrawerScreen.kt:134`) and reads to the user as "remove from my launcher". The app disappears from the drawer but stays visible and launchable on Home. The reverse is also true: hiding an app does not clear its favorite flag, so it remains in the ordered favorites list forever; uninstalling an app leaves both a favorite entry and a hidden entry stranded in DataStore, and if the package is later reinstalled (`mapNotNull` now resolves it again) the app **silently reappears on Home** in its original position. Settings state accumulates indefinitely with no pruning.

---

### F7 — Single-result auto-launch can fire without the user asking 🟠 High

**Evidence**

```kotlin
// DrawerScreen.kt:55-86
var menuApp by remember { mutableStateOf<AppInfo?>(null) }
...
var lastLaunched by remember { mutableStateOf<String?>(null) }

LaunchedEffect(query) {
    if (query.isBlank()) { lastLaunched = null; return@LaunchedEffect }
    delay(350)
    val results = latestApps
    if (results.size == 1 && query != lastLaunched) {
        lastLaunched = query
        AppLauncher.launch(context, results.first())
    }
}
```

**Two distinct defects:**

1. **Spontaneous launch on recreation.** `lastLaunched` is a plain `remember`, so it is `null` on every Activity (re)creation, while `query` lives in the ViewModel and is restored. If the user left the launcher while a single-result query was showing, then anything that recreates the Activity — process death and restore, the "don't keep activities" developer option, a system-initiated recreate, an OEM task-killer scenario — recomposes the drawer with a non-blank `query`, `lastLaunched == null`, and results == 1. The app launches itself. Launchers are killed and restored constantly; this is a real "my phone opened an app by itself" bug.
2. **Re-launch while narrowing.** `query != lastLaunched` only suppresses the *identical* query. If the user typed `chro` and Chrome was the only match, Chrome launched. On return, typing one more character (`chrom`) changes the key, `lastLaunched` differs, results are still 1 → **Chrome launches again mid-typing**. Any narrowing search that stays at one result re-launches on every keystroke.

Additionally the effect fires purely on `query` changes, so it has no notion of *who* changed the query (user vs. restore vs. `onHomePressed`'s `query.value = ""`), which is the underlying design gap. The fix is to model intent explicitly (a monotonically increasing "user edit" token / a resolved-result state), not to patch the string comparison.

---

### F8 — Search auto-focus relies on a timing hack, and fails silently 🟡 Medium

**Evidence**

```kotlin
// DrawerScreen.kt:63-71
LaunchedEffect(active) {
    if (active) {
        delay(100) // let the field finish laying out before we focus it
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    } else {
        keyboard?.hide()
    }
}
```

**Why it fails.** The 100 ms sleep is a guess about frame timing. On a cold-start-to-drawer path (slow first composition, low-end device, system under load) the `BasicTextField` node may not be attached when the request lands; `FocusRequester.requestFocus()` then throws, and the `runCatching` converts a user-visible defect ("the search box never focused") into a silent no-op with no log and no retry. `LocalSoftwareKeyboardController.show()` is also unreliable when called in the same frame as a focus change. There is also no `inputMode`/`captureKeyboard` guard, so the keyboard may be shown over an unfocused field.

**Correct pattern.** Drive focus from layout/state instead of time: `LaunchedEffect(active) { if (active) { awaitFrame(); focusRequester.requestFocus() } }` combined with `Modifier.focusRequester(...)`/`onPlaced`, and let the IME follow the focus change rather than requesting it independently.

---

### F9 — Home's long-press gesture is overloaded and competes with child clicks 🟡 Medium

**Evidence**

- `HomeScreen.kt:70-72` — the root `Column` installs `pointerInput(Unit) { detectTapGestures(onLongPress = { onOpenSettings() }) }`.
- `HomeScreen.kt:83, 92` — the clock and date are children with `Modifier.clickable`.
- `HomeScreen.kt:106-119` — every favorite row is a child with `Modifier.clickable` (launch) and `clip`/`padding`.

**Why it fails.** Pointer input is delivered child-first, but `detectTapGestures` in the parent still observes the same gesture stream, and whether the child's `clickable` also fires `onClick` for a long press depends on consumption ordering that is *not* specified by the `Modifier.clickable` contract. Practically this means a long press on a favorite can (a) open Settings, (b) launch the app, or (c) both, with the outcome varying between Compose versions. Even when it works, "long-press anywhere" is the *only* route to Settings (`HomeScreen.kt:71`, documented in `README.md:16`), so it doubles as an undiscoverable primary navigation affordance (see S12/A11Y-1) and it makes a destructive action (opening a full-screen settings surface) reachable by accidentally resting a finger on the clock.

**Status:** flagged as *verify first* — the exact consumption outcome should be pinned down with a UI test before changing behaviour, then the gesture should be made explicit with `combinedClickable`/a dedicated affordance rather than a parent-level catch-all.

---

### F10 — Cold start flashes the wrong theme and unreadable system-bar icons 🟡 Medium

**Evidence**

- `themes.xml:3` and `themes.xml:6` — `android:windowBackground = @color/bg` (= `#1C1A17`, the *warm-dark* background) and `android:windowLightStatusBar = false`, both static.
- `Color.kt:80-86` — `Paper` and 18 other light palettes (19 of the 38 total) have light backgrounds (e.g. `#F4F1EA`).
- `MainActivity.kt:41-53` — the real theme is read from DataStore asynchronously and applied in a `LaunchedEffect(palette)` that calls `enableEdgeToEdge(...)` *after* the first frame.
- `MainActivity.kt:39` — `enableEdgeToEdge()` is called a second time, unconditionally, before `setContent`.

**Why it fails.** A light-theme user gets a **dark launch window** and, because `windowLightStatusBar=false` is baked into the theme, light (white) status-bar icons on a light background until the effect runs — a visible flash of both colour *and* contrast on every cold start, i.e. on every press of Home. There is no `values-night` variant, no `windowSplashScreen*` attributes and no `core-splashscreen` dependency. Calling `enableEdgeToEdge` twice also means the window contract is applied two different ways in the same lifecycle.

**Why it's not trivially fixable:** the theme key lives in DataStore, which cannot be read synchronously on the main thread. Acceptable resolutions are (a) mirror the theme key into a synchronously-readable store for the launch window and call `setTheme` before `super.onCreate`, or (b) adopt `androidx.core:core-splashscreen` and keep the splash window until the palette resolves. Both are covered by PERF-4.

---

### F11 — Backup rules do not cover the only data the app has 🟡 Medium

**Evidence**

- `AndroidManifest.xml:5-7` — `allowBackup="true"`, `dataExtractionRules="@xml/data_extraction_rules"`, `fullBackupContent="@xml/backup_rules"`.
- `backup_rules.xml` — `<include domain="sharedpref" path="." />` only.
- `data_extraction_rules.xml` — `<cloud-backup><include domain="sharedpref" path="." /></cloud-backup>` only; **no `<device-transfer>` section at all**.
- `SettingsRepository.kt:13` — `preferencesDataStore(name = "launcher_settings")`, which persists to `files/datastore/launcher_settings.preferences_pb` — i.e. the **`file` domain**, not `sharedpref`.

**Why it fails.** Because an `<include>` list is *exclusive*, declaring only `sharedpref` means "back up the `shared_prefs/` directory and nothing else". The launcher writes nothing to `shared_prefs/` — its entire state (favorites order, hidden set, 24h toggle, theme) lives in DataStore under `files/datastore/`. The net effect is that **nothing is backed up or restored**: a user who migrates to a new phone with Google backup gets a fresh launcher with no favorites and the default theme. `<device-transfer>` being undeclared also leaves device-to-device migration semantics undefined and dependent on platform defaults rather than an explicit decision.

**Verification note:** the concrete restored/excluded set should be confirmed with `adb shell bmgr backupnow <pkg>` + a restore on a second image before and after the fix, since the include/exclude precedence rules are easy to get subtly wrong.

---

### F12 — Refresh storms during exactly the events that stress the device 🟡 Medium

**Evidence**

- `LauncherViewModel.kt:85-97` — `IntentFilter` with `PACKAGE_ADDED`, `PACKAGE_REMOVED`, `PACKAGE_CHANGED`, `PACKAGE_REPLACED` + `addDataScheme("package")`.
- `LauncherViewModel.kt:76-80` — *any* of those events → `refresh()` → full `queryIntentActivities` + `loadLabel` per app.
- `MainActivity.kt:64` — `onResume()` → `refresh()` as well.

**Why it fails.** A Play Store "update all" of 40 apps, or a device restore, delivers dozens of broadcasts in a burst. Each one triggers a full package enumeration and label load (the expensive part: one IPC round-trip per app) and each one spawns an independent coroutine (F3). The same burst also raises the probability of the concurrent-package-removal races described in F1. Additionally `onResume` duplicates work the receiver already did, and nothing at all refreshes the list when the device locale changes (F14).

---

### F13 — Failed intent dispatches are indistinguishable from successful ones 🟡 Medium

**Evidence**

- `AppLauncher.kt:27` — `openAppInfo` : `runCatching { context.startActivity(intent) }`, failure discarded.
- `AppLauncher.kt:33` — `uninstall` : same.
- `AppLauncher.kt:38-48` — `openHomeSettings` : `runCatching`, and the fallback `runCatching` is *also* discarded.
- `AppLauncher.kt:54` — `openClock` : same.
- `AppLauncher.kt:61` — `openCalendar` : same.
- `AppLauncher.kt:18-19` — `launch` : the only one that reports, via a `Toast` with a hardcoded English string.

**Why it fails.** `Settings.ACTION_HOME_SETTINGS` is absent on some OEM builds; `ACTION_APPLICATION_DETAILS_SETTINGS` can be filtered; `ACTION_SHOW_ALARMS` is not guaranteed to resolve on AOSP builds without a clock app. In all of those cases the user taps a menu row and *nothing happens* — no toast, no log, no state change. From the user's perspective the app is broken; from the developer's perspective there is no evidence at all (`runCatching` with a discarded result is also a `lint`-visible anti-pattern). This directly undermines the "set as default launcher" path, which is the app's primary onboarding action.

---

### F14 — The app list is never re-sorted when the locale changes ⚪ Low

**Evidence**

- `AppRepository.kt:17` — `Collator.getInstance(Locale.getDefault())` is created **per call**.
- `LauncherViewModel.kt:85-97` — the refresh `IntentFilter` listens to package events only.

**Why it fails.** Per-call `Collator` creation is correct (it picks up locale changes) but useless without a trigger: changing the system language re-sorts nothing until the next `onResume`/package event. For an app whose selling point is locale-correct Greek handling (`README.md:20-22`), a stale, wrongly-collated app list is a visible defect. `ACTION_LOCALE_CHANGED` (and `ACTION_TIMEZONE_CHANGED` for the clock, F15) are not observed.

---

### F15 — A false "no favorites yet" empty state appears on every launch ⚪ Low

**Evidence**

- `LauncherViewModel.kt:60-64` — `favorites` has `initialValue = emptyList()` under `SharingStarted.Eagerly`.
- `LauncherViewModel.kt:33-34` — `allAppsFlow` starts as `emptyList()` and is only populated after the IO load completes.
- `HomeScreen.kt:97-104` — `if (favorites.isEmpty())` renders "no favorites yet / long-press anywhere to open settings".

**Why it fails.** There is no loading state in the model, so "0 favorites" and "not loaded yet" are the same value. On every cold start (and on every resume that re-triggers a refresh, since the list is replaced) the user with 8 favorites briefly sees the onboarding message telling them to long-press to configure the launcher. It is a cosmetic defect with a real cost: it teaches the user something false about their own configuration, and it is indistinguishable from the genuine "my favorites were wiped" bug in F2. The fix belongs in the state model (ARCH-2: an explicit `Loading` state), not in the view.

---

### F16 — The search query is publicly mutable state that drives a side effect ⚪ Low

**Evidence**

- `LauncherViewModel.kt:36` — `val query = MutableStateFlow("")` (exposed as a mutable type).
- `LauncherViewModel.kt:112-119` — `setQuery`/`onHomePressed` are the intended writers.
- `DrawerScreen.kt:53` — the UI collects it; `DrawerScreen.kt:75-86` — auto-launch is driven off it.

**Why it fails.** Any composable can write `query.value` directly, bypassing `setQuery`, and because a *value change in `query` is the entire trigger for auto-launch* (F7), an unrelated future write becomes an app launch. Exposing `MutableStateFlow` from a ViewModel is a well-known smell precisely because it removes the ability to reason about who can change state — which is exactly the reasoning F7 needs.

---

### F17 — Returning to the launcher without pressing Home leaves stale search state 🟡 Medium

**Evidence**

- `MainActivity.kt:61-65` — reset behaviour (`vm.onHomePressed()`) is wired **only** to `onResume`-driven refresh and `onNewIntent`.
- `MainActivity.kt:68-71` — `onNewIntent` → `vm.onHomePressed()`; the intent is not stored via `setIntent`.
- `LauncherViewModel.kt:116-119` — `onHomePressed()` clears `query` and emits `_goHome`.

**Why it fails.** The reset path assumes the user always returns via the **Home button**, which re-delivers the `MAIN`/`HOME` intent (hence `launchMode="singleTask"` at `AndroidManifest.xml:17`). If the user launches an app from the drawer and comes back with **Back** or via Recents, no reset happens: the launcher resumes on page 1 with the old query, the old keyboard state and a `lastLaunched` value that suppresses re-launch, so the user is staring at a filtered single-item drawer (or an already-launched app's stale query) instead of their home screen. Symmetrically, `onNewIntent` fires for *any* intent — including the `CATEGORY_LAUNCHER` intent delivered when the app is opened from another launcher's app list — so an explicit "open Minimal" resets the user's position. There is no documented, intentional rule for "what state should the launcher be in when the user comes back to it", which is why this is a design story (DRAW-3) rather than a one-line fix.

---

## 2. Bad practices & code smells

### S1 — No dependency injection; the ViewModel hard-wires concrete repositories

`LauncherViewModel.kt:28-31` is an `AndroidViewModel` that constructs `AppRepository(app)` and `SettingsRepository(app)` directly from `Application`. There are no interfaces anywhere in `data/`, and no `ViewModelProvider.Factory`. Consequences: `LauncherViewModel` cannot be unit-tested without Robolectric or an instrumented device; the `Application` dependency makes the VM's constructor signature framework-bound; and swapping an implementation (e.g. `LauncherApps`-backed enumeration, PLAT-1) requires editing the VM. For an app this size a DI *framework* is not warranted — a hand-written `ViewModelProvider.Factory` with constructor injection is the proportionate fix (ARCH-1).

### S2 — `Context` is the data layer's constructor argument

`AppRepository.kt:8` and `SettingsRepository.kt:15` both take `Context`. Since these are the only two "repositories", every piece of app logic transits through a framework type. The domain types that *are* clean — `AppInfo.kt`, `TextNormalizer.kt` — prove the codebase can separate concerns; the boundary just hasn't been drawn. The pragmatic fix is to keep the concrete classes Android-aware but hide them behind interfaces so callers depend on behaviour, not on `Context` (ARCH-1/ARCH-5).

### S3 — Composables perform side effects directly through a global singleton

`AppLauncher` is a stateless `object` (`AppLauncher.kt:9`) called straight from the UI with `LocalContext.current`:

| Call site | Effect |
| --- | --- |
| `HomeScreen.kt:83` | open clock app |
| `HomeScreen.kt:92` | open calendar |
| `HomeScreen.kt:116` | launch app |
| `DrawerScreen.kt:84` | launch app (auto-launch) |
| `DrawerScreen.kt:100` | launch app (IME action) |
| `DrawerScreen.kt:120` | launch app (list tap) |
| `DrawerScreen.kt:135` | open app info |
| `DrawerScreen.kt:136` | uninstall |
| `SettingsScreen.kt:93` | open home settings |

The ViewModel — the component that owns "what happens when the user acts" — is bypassed for every one of these. Nothing here is testable, loggable, rate-limitable or gated on app state, and the auto-launch in `DrawerScreen.kt:75-86` shows the cost: a lifecycle-sensitive decision (F7) is made inside a composable effect. `AppLauncher` also mixes five unrelated responsibilities (launch, app-info, uninstall, clock, calendar) and mutates the `Intent` it receives from `getLaunchIntentForPackage` (`AppLauncher.kt:12-17`). See ARCH-3.

### S4 — Scattered `StateFlow`s instead of one UI state model

`LauncherViewModel.kt:34-73` publishes: `allApps`, `query`, `goHome`, `use24h`, `themeKey`, `favoriteSet`, `hiddenSet`, `favorites`, `drawerApps` — nine independent observable surfaces. Screens subscribe piecemeal (`HomeScreen.kt:57-58`, `DrawerScreen.kt:52-54`, `SettingsScreen.kt:54-58`), so:

- there is no atomic snapshot (the UI can render a favorites list built from a new app list combined with an old favorites list),
- there is no place to put `Loading`/`Error`/empty distinction (F15, F2),
- each subscription is its own recomposition scope, and `SharingStarted.Eagerly` keeps every derived flow hot even when only one screen is visible (PERF-5),
- `MutableStateFlow` leaks mutability outward (F16).

The MVI/MVVM-shaped fix (one `data class LauncherUiState` + typed intents) is a moderate refactor, not a rewrite, and it is the enabling change for F2/F7/F15 (ARCH-2).

### S5 — Work on the main thread inside a flow operator

`LauncherViewModel.kt:67-73` — `drawerApps` filters and matches inside `combine(...) { }`, and `stateIn(viewModelScope, …)` collects that in `viewModelScope`, i.e. **`Dispatchers.Main`**. The predicate is `TextNormalizer.matches` (`TextNormalizer.kt:25-28`), which per call does a `lowercase()`, an NFD decomposition (`Normalizer.normalize`), a `Regex` replacement and a `String.replace`. That is ~5 string allocations and a normalisation pass **per installed app, per keystroke**, on the frame-producing thread. With 150 apps that is hundreds of allocations on every character typed, and it recomputes labels that never change. The fix is twofold: precompute a `searchKey` once per app when the list is built (the labels only change on package events), and move the filter to `Dispatchers.Default` (DRAW-4).

### S6 — Every user-visible string and most dimensions are hardcoded

`strings.xml` (3 lines) contains exactly one entry: `app_name`. Everything else is an inline literal:

- `HomeScreen.kt:99` — `"no favorites yet\nlong-press anywhere to open settings"`
- `HomeScreen.kt:124` — `"↑ apps"`
- `Common.kt:68` — `"search…"`
- `Common.kt:77` — `"×"`
- `DrawerScreen.kt:170-173` — `"remove from favorites"`, `"add to favorites"`, `"hide app"`, `"app info"`, `"uninstall"`
- `SettingsScreen.kt:73` — `"‹"`, `:81` — `"settings"`, `:92` — `"24-hour clock"`, `:93` — `"set as default launcher"`, `:94` — `"theme"`, `:100` — `"favorites — tap to toggle"`, `:113` — `"★"`/`"☆"`, `:128` — `"unhide"`
- `AppLauncher.kt:19` — `"Can't open ${app.label}"`
- `Color.kt:92-131` — 38 theme display names

For an app that explicitly targets Greek users (`README.md:20-22`, `TextNormalizer.kt:5-13`) and renders a locale-formatted date, shipping zero localisation is the largest maintainability/i18n gap in the project. The same pattern applies to layout constants (`66.sp`, `70.sp`, `54.dp`, `30.dp`, `28.dp`, `22.dp`, `11.dp`, `98.dp`, `250.dp` …) which are re-typed per composable instead of being tokens, so a spacing change is a multi-file search. See I18N-1/I18N-2.

### S7 — Outdated or inappropriate platform APIs

| Where | Issue |
| --- | --- |
| `AppRepository.kt:19` | `pm.queryIntentActivities(intent, 0)` — the `Int`-flags overload is deprecated at API 33 in favour of `ResolveInfoFlags.of(0)`. |
| `AppRepository.kt:11-31` | Enumerates apps with `PackageManager` rather than `LauncherApps`. `LauncherApps.getActivityList` is the API intended for launchers: it is profile-aware (work profile / secondary users), reports enabled/disabled state, and pairs with `LauncherApps.Callback` for change notifications instead of four raw broadcasts. |
| `AppRepository.kt:19-28` | No filtering on `activityInfo.enabled` / `exported`. |
| `AppLauncher.kt:57-62` | `openCalendar` hardcodes `com.google.android.calendar` and falls back to `content://com.android.calendar/time`; on any non-Google or renamed provider both paths fail silently (F13). `AlarmClock.ACTION_SHOW_ALARMS` (`AppLauncher.kt:52`) is likewise assumed to resolve. |
| `AndroidManifest.xml:32-37` | `<queries>` is declared correctly for MAIN/LAUNCHER (good), which is what makes the `PackageManager` path work at all — but the hardcoded calendar package is *not* covered by that declaration, so `getLaunchIntentForPackage("com.google.android.calendar")` depends on that app independently matching the declared intent. |
| `AndroidManifest.xml` | No `android:enableOnBackInvokedCallback`. With targetSdk 35 the legacy back path still works, but the app participates in no predictive-back animation, and the legacy path is deprecated/platform-defaulted for targetSdk 36+. |

### S8 — The Activity's lifecycle contract is implicit and undocumented

`AndroidManifest.xml:17-20` combines three declarations with non-obvious interactions:

- `android:launchMode="singleTask"` — required so that pressing Home re-delivers through `onNewIntent` (F17), but it also means the launcher is a task root with unusual recents behaviour.
- `android:stateNotNeeded="true"` — the system may recreate the Activity *without* `onSaveInstanceState`, so `rememberSaveable` values (`showSettings` at `MainActivity.kt:76`, the pager page at `:77`) may legitimately not survive.
- `android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout|smallestScreenSize|uiMode"` — eight absorbed changes. Absorbing `orientation|screenSize|uiMode` is defensible for a HOME app (no recreate flicker), but it is undecided and unrecorded: nobody reading the code can tell whether the Compose state survived *by design* or *by accident*, and anything that does recreate (process death, developer options, `density`/`fontScale` changes which are *not* absorbed) is untested. `MainActivity` also never uses `SavedStateHandle`.

The result is that the app's most important guarantee — "returning to Home always looks and behaves the same" — is asserted nowhere and violated in at least one path (F17).

### S9 — The release build is unoptimised and unshippable

`app/build.gradle.kts:19-27`: `isMinifyEnabled = false`, no `isShrinkResources`, no signing config, `proguard-rules.pro` is empty, no `baselineProfile` plugin, no `androidx.profileinstaller`, no `buildConfigField`/`versionName` discipline (`versionCode = 1`, `versionName = "1.0"` at `:15-16`), and no `debug`/`release` differentiation beyond the default. For a **launcher**, cold-start latency is the headline performance metric and release-mode startup is where R8 + Baseline Profiles pay off most; shipping unminified also means the Compose runtime and all 38 palettes are delivered unshrunk. `applicationId = "com.example.minimallauncher"` (`:12`) also cannot be published to Google Play, so any distribution plan (including the `set as default launcher` flow) is blocked on renaming.

### S10 — The Gradle wrapper is incomplete, so the project cannot be built from the CLI

`gradle/wrapper/gradle-wrapper.properties` exists, but:

- `gradlew`, `gradlew.bat` and `gradle-wrapper.jar` are **absent from the working tree and from `git ls-files`** (verified),
- no `gradle` binary is on `PATH` (verified), though the Gradle 8.9 distribution and a warm dependency cache (98 module groups) *are* present under `~/.gradle`,
- `README.md:56-58` acknowledges this ("once Android Studio has generated the Gradle wrapper jar you can also run `./gradlew assembleDebug`"),
- `app/.gitignore` even contains a comment asserting the wrapper should be kept — but it was never committed.

Consequence: no CI can be set up, no contributor can reproduce a build, and the audit itself cannot be re-validated by compiling. This is the cheapest high-impact fix in the whole document (PERF-1).

### S11 — Theme layer ergonomics invite silent errors

- `Color.kt:48-51` — `private fun p(bg, surface, border, text, text2, accent, accentSoft)` takes **seven positional `Long` values**. It is called 36 times with completely unlabelled arguments (`Color.kt:96-131`); transposing `text2` and `accent` yields a plausible-looking, silently wrong palette that no compiler or reviewer will catch.
- `Color.kt:144` — `LocalPalette = staticCompositionLocalOf { WarmDark }`. A `static` local has no fine-grained tracking, so changing the theme invalidates the **entire** composition rather than the ~12 colour readers. With `AppThemes` being 38 entries and the picker changing the palette on every tap, this is a jank source on low-end devices.
- `Color.kt:13-26` builds a `LauncherPalette` *and* `Theme.kt:17-41` maps it onto a Material `ColorScheme` — two parallel colour models where the palette accessors (`Bg`, `TextPrimary`, `SurfaceCol`, `Accent`, …) are used by screens instead of `MaterialTheme.colorScheme`. Any Material component that reads the scheme is unlikely to receive the same value the custom components use.
- The palette colours are also defined as top-level `val`s with `@Composable @ReadOnlyComposable` getters (`Color.kt:146-157`), which reads like constants but behaves like ambient state — an easy source of "why is this colour wrong outside a composable".

### S12 — Accessibility is essentially unaddressed

- **No `contentDescription` anywhere** in the codebase (verified by search): every interactive element is a bare `Text` with a `clickable` modifier.
- The "×" clear-search control (`Common.kt:75-86`) and the theme swatches (`SettingsScreen.kt:150-185`) announce as text/graphics with no role or label; the `★`/`☆` favorite indicators (`SettingsScreen.kt:112-117`) convey state through a glyph only, with no `stateDescription`.
- **The only route to Settings is an unlabelled long-press** (`HomeScreen.kt:70-72`, `README.md:16`). There is no `Semantics` custom action, no hint, and no visible affordance, so TalkBack users have no discoverable way to reach it.
- The app menu in the drawer is also long-press-only (`DrawerScreen.kt:119-122`) with no custom action.
- Typography is small by design (11sp theme labels `SettingsScreen.kt:179`, 12sp footer `HomeScreen.kt:127`, 13sp section labels `:203`) and several targets are below the 48dp minimum (the "×" text, the "unhide" text at `SettingsScreen.kt:127-133`, `MenuRow` at ~44dp).
- There is no test asserting any of this, and no `lint` baseline to keep it from regressing (S13).

### S13 — Dead code, warnings and drifted conventions

- `MainActivity.kt:20` and `MainActivity.kt:30` import `androidx.compose.runtime.remember` **twice**; `MainActivity.kt:11` imports `ExperimentalFoundationApi` without using it (`@OptIn` appears only in `DrawerScreen.kt:48`).
- `HomeScreen.kt:29` imports `Accent` without using it.
- `MainActivity.kt:39` and `MainActivity.kt:52` both call `enableEdgeToEdge()` (F10).
- `themes.xml:2` parents on `android:Theme.Material.NoActionBar` — an API-21-era platform theme — rather than a Material3/Compose-appropriate base, which is what the app's `Dialog` window and any future system-drawn surface inherit.
- No `lint { abortOnError = true }`, no baseline file, no detekt/ktlint, no `allWarningsAsErrors`; unresolved Kotlin warnings are invisible.
- `AppLauncher.kt:17` mutates the `Intent` instance returned by `PackageManager.getLaunchIntentForPackage`, mixing ownership of framework objects with presentation concerns.

---

## 3. Robustness & resilience

### R1 — Make the existing seams real, not a Clean Architecture rewrite

At ~1,500 LOC, a full `domain` / `data` / `presentation` layering with a use-case class per action would be net-negative: it adds indirection and files without adding testability that a single well-placed interface doesn't already provide. The proportionate target shape is:

```
ui/            MainActivity, screens, LauncherViewModel, LauncherUiState, LauncherIntent
ui/gateway/    LauncherGateway (interface) + AndroidLauncherGateway (Intent/LauncherApps work)
data/          AppRepository / SettingsRepository as *interfaces* + Android-backed impls
domain/        pure: TextNormalizer, AppInfo, sorting, clock formatting  (already ~true today)
di/            a hand-written ViewModelProvider.Factory
```

This is roughly a 6-file delta that unlocks unit tests for every decision in the app (ARCH-1/2/3/5).

### R2 — "Offline-first" is not applicable; *failure isolation* is the equivalent requirement

There is no network, so the resilience analogue for this app is: **a single failing subsystem must never blank the UI or crash the process, and the core action must survive.** Concretely:

1. Every suspend/flow entry point that touches the platform or disk has a defined failure behaviour — `catch` → last-known-good state → an explicit, visible retry path (F1, F2, F3).
2. **Opening an app never depends on settings storage.** Favorites/hidden/theme can all fail and the drawer must still enumerate and launch apps (today a DataStore failure is survivable for the drawer list but fatal for favorites — F2/F6).
3. Failures become **observable** rather than swallowed: one `CoroutineExceptionHandler` on the VM scope plus a small `Logger`/`Recorder` seam so that `runCatching {}` (`AppLauncher.kt`) and the empty-keyboard-no-focus case (F8) leave evidence. Without this, every item in this audit is re-introduced by the first developer who can't reproduce a bug (ARCH-6, QA-4).
4. **Degraded enumeration is a first-class state**: zero launchable apps returned (package-visibility quirk, OEM filter, first boot) must render an explicit empty/error state, not the "no favorites yet" onboarding copy (F15).

### R3 — Startup is the primary performance contract; protect it with a measurement

A HOME activity is on the critical path of the user's most frequent gesture. The measurable budget should be set with:

```
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME
```

plus a Macrobenchmark `StartupTimingMetric` (cold + warm) once a stable release build exists. The protections that matter, in order: the missing Gradle wrapper and a real release build (S9/S10) → R8 + resource shrinking → Baseline Profiles/`ProfileInstaller` → removing the launch-window flash (F10) by resolving the theme synchronously or via `core-splashscreen` → avoiding a first-frame package enumeration on the main thread (already correct: `Dispatchers.IO` at `LauncherViewModel.kt:107`, keep it).

### R4 — Quality gates must land *before* the refactors

There is no `app/src/test`, no `app/src/androidTest`, no CI and no lint configuration, so nothing in this document can regress-detect. Three units are testable today with essentially no production change: `TextNormalizer` (pure), `SettingsRepository` (in-memory/temp-file DataStore), and `AppInfo`/sorting. After ARCH-1, `LauncherViewModel` becomes testable with fakes, which is what makes F3/F7/F15 fixable with confidence rather than by inspection. Recommended order: PERF-1 (wrapper) → QA-1/QA-2 → CI (QA-4) → then the High fixes, each accompanied by the test that proves it.

---

## 4. Traceability index

| Finding | Stories |
| --- | --- |
| F1 uncaught VM exceptions | STAB-1, ARCH-6, QA-2 |
| F2 DataStore silent freeze | STAB-2, ARCH-2, ARCH-6 |
| F3 concurrent/stale refresh | STAB-3 |
| F4 home overflow | STAB-4 |
| F5 identity mismatch | DATA-1 |
| F6 hidden app stays on Home | DATA-2, DATA-3 |
| F7 spontaneous / repeated auto-launch | DRAW-1, ARCH-2 |
| F8 focus timing hack | DRAW-2 |
| F9 overloaded long-press | A11Y-1 |
| F10 cold-start theme flash | PERF-4 |
| F11 backup rules exclude DataStore | DATA-4 |
| F12 refresh storms | STAB-3 |
| F13 silent intent failures | STAB-5, ARCH-6, PLAT-2 |
| F14 no locale-change refresh | STAB-6 |
| F15 false empty state | STAB-7, ARCH-2 |
| F16 mutable public query | ARCH-2, DRAW-1 |
| F17 stale state on non-Home return | DRAW-3, PLAT-3 |
| S1 no DI | ARCH-1, QA-2 |
| S2 Context in data layer | ARCH-1, ARCH-5 |
| S3 UI side effects via singleton | ARCH-3 |
| S4 scattered StateFlows | ARCH-2, PERF-5 |
| S5 main-thread search work | DRAW-4, QA-1 |
| S6 hardcoded strings/dimensions | I18N-1, I18N-2 |
| S7 outdated platform APIs | PLAT-1, PLAT-2, PLAT-4, I18N-3 |
| S8 implicit lifecycle contract | PLAT-3 |
| S9 unoptimised release | PERF-2, PERF-3, PERF-6 |
| S10 missing Gradle wrapper | PERF-1, QA-4 |
| S11 theme ergonomics | I18N-2, ARCH-5, PERF-5 |
| S12 accessibility gaps | A11Y-1, A11Y-2, A11Y-3 |
| S13 dead code / lint drift | PLAT-4, QA-4, PERF-4 |
| R1 target architecture shape | ARCH-1, ARCH-2, ARCH-3, ARCH-4, ARCH-5 |
| R2 failure isolation | STAB-1, STAB-2, STAB-5, STAB-7, ARCH-6 |
| R3 startup contract | PERF-1, PERF-2, PERF-3, PERF-4 |
| R4 quality gates first | PERF-1, QA-1, QA-2, QA-3, QA-4 |

---

## 5. What the code already gets right

An audit that only lists defects is unusable for prioritisation. These are deliberate, correct choices that should be preserved by the backlog:

- **DataStore instead of `SharedPreferences`**, with a single `preferencesDataStore` delegate (`SettingsRepository.kt:13`) — the modern, async, transactional choice.
- **`ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`** (`LauncherViewModel.kt:92-97`) — the explicit export flag required from API 33/34; many apps still ship the implicit version. The receiver is also correctly unregistered in `onCleared` inside `runCatching` (`:100-103`).
- **`<queries>` declared for `MAIN`/`LAUNCHER`** (`AndroidManifest.xml:32-37`) — the correct, Play-policy-safe way to see other launchable apps on API 30+ instead of requesting `QUERY_ALL_PACKAGES`.
- **`PackageManager` enumeration runs off the main thread** (`LauncherViewModel.kt:107` — `withContext(Dispatchers.IO)`), so the expensive label load never blocks a frame on the happy path.
- **Locale-correct sorting via `Collator`** (`AppRepository.kt:17,30`) rather than `String.compareTo` — necessary for Greek ordering.
- **`TextNormalizer` is a pure, dependency-free object** with thorough documentation of its Greek/Latin folding rules (`TextNormalizer.kt:5-28`), and it uses locale-independent `lowercase()` — the right choice for a search key.
- **`stateIn` + `asStateFlow` for UI state**, with `Eagerly` sharing — appropriate for a single-activity app whose ViewModel lives as long as the UI.
- **Edge-to-edge with contrast-aware system bars** (`MainActivity.kt:39,45-53`) rather than hardcoded bar colours, plus `systemBarsPadding()`/`imePadding()` in the right places.
- **No `Context` retained in domain types** (`AppInfo`, `TextNormalizer`) and no obvious leak path: no long-lived `Activity` references, no static holdings.
- **Build hygiene that exists:** Kotlin/Gradle version catalog (`gradle/libs.versions.toml`), `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, `android.nonTransitiveRClass=true`, adaptive launcher icons only (fine at minSdk 26), and a bundled OFL font with its licence file.
- **A README that documents the intended behaviour** and even anticipates several findings (live app list, locale-aware search, "R8 + a signing config for release builds" and "a unit test for `TextNormalizer`" are already listed at `README.md:100-101`).

---

*Generated from a read-only inspection of revision `2bb87a0`. Every `file:line` reference above was taken from the working tree at the time of the audit; see [`USER_STORIES.md`](./USER_STORIES.md) for the actionable backlog derived from these findings.*
