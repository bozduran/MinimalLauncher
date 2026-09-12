# Architecture

This document records the structural decisions behind Minimal Launcher: what the
layers are, which way dependencies point, and — just as importantly — **what was
deliberately not built**. It exists so a future contributor can tell an intentional
omission from an oversight.

See [`../CODE_AUDIT.md`](../CODE_AUDIT.md) for the findings that drove these
decisions and [`../USER_STORIES.md`](../USER_STORIES.md) for the work items.

---

## 1. Layers

```
ui/                     MainActivity, screens, LauncherViewModel, LauncherUiState
  └─ LauncherAction.kt  typed outgoing actions + failure messages
  └─ ClockFormatter.kt  pure time/date formatting
  └─ HomeFavoritesLayout.kt  pure layout arithmetic
data/                   interfaces + Android-backed implementations
  ├─ AppRepository              (interface)  ─ PackageManagerAppRepository
  ├─ SettingsRepository         (interface)  ─ DataStoreSettingsRepository
  ├─ AppChangeSource            (interface)  ─ PackageChangeSource
  ├─ LauncherGateway            (interface)  ─ AndroidLauncherGateway
  └─ AppLogger                  (interface)  ─ LogcatAppLogger
domain (pure, no Android)  AppInfo, TextNormalizer, AppListOrdering
```

`ui/` owns behaviour and state. `data/` owns I/O. There is no `domain/` package:
the Android-free logic is three small files (`AppInfo`, `TextNormalizer`,
`AppListOrdering`) and giving them a package of their own would add a directory
without adding a rule.

### The dependency rule

`ui` → `data` interfaces → Android implementations. Concretely:

- **A composable must not touch `Context`, `Intent`, `PackageManager` or a
  database.** It reads state and dispatches a method on the ViewModel.
- **`AppInfo` / `TextNormalizer` / `AppListOrdering` must not import `android.*`.**
  They are the only units testable without a device or Robolectric, so this is the
  most valuable boundary in the project.
- Dependencies are chosen in exactly one place: `LauncherViewModelFactory`.

Violating the first rule is what the audit found nine times over (S3): launching
was scattered across composables, so nothing was testable, loggable or gateable.

---

## 2. Why not Clean Architecture, and why not a DI framework

This is a ~4k-LOC single-module app. A full `domain`/`data`/`presentation` split
with a use-case class per action would add roughly 20 files and indirection, and
would buy nothing that the four interfaces above do not already buy. At this size
that is architecture for its own sake.

**The trigger to revisit:** when a second entry point appears that needs the same
behaviour without the launcher UI — a widget, a Quick Settings tile, a test
harness driving real use cases — the use-case layer starts earning its keep.

Likewise, no Hilt/Dagger/Koin. Constructor injection plus a hand-written
`ViewModelProvider.Factory` is ~20 lines and gives every test seam this project
needs (`LauncherViewModelTest` runs entirely on the JVM with hand-written fakes).
A DI framework becomes worth its build-time and cognitive cost when *graph
construction* is the problem — many bindings, scopes, or generated components.
It is not the problem here.

When a third real implementation of an interface appears, prefer widening the
factory over introducing a framework.

---

## 3. State, concurrency and ownership

### The ViewModel owns behaviour

`LauncherViewModel` holds all launcher state and performs all side effects. The
composables are pure functions of collected state plus dispatched intents.

### One state object, not nine flows

The UI collects exactly one `StateFlow<LauncherUiState>`. Screens read fields from
that snapshot; none of the internal flows is public. This matters for correctness,
not tidiness: with nine independently-collected flows a screen could derive its
favorites list from one app list and its drawer from another, and there was nowhere
to express "loading" or "this failed".

`LauncherUiState.appList` is a sealed `AppListState`:

- `Loading` — no attempt has finished yet, so the UI must not claim "no favorites
  yet". This is what stopped the onboarding copy appearing on every cold start.
- `Ready(apps)` — a usable list.
- `Error(cause)` — the *first* load failed and there is nothing to show.

A failure while a list is **already** on screen keeps `Ready` and reports through
`LauncherUiState.loadFailure` instead, so a failed refresh never replaces the user's
apps with an error or an empty state.

One-shot events are deliberately excluded: `goHome` stays a `SharedFlow` because
"scroll to page 0" is an event, and replaying it after a configuration change would
be wrong.

**Actions are typed methods, not a sealed intent class.** `setQuery`,
`toggleFavorite`, `openClock` and the rest are the action surface. Introducing a
`LauncherIntent` hierarchy would add a dispatch layer that maps one-to-one onto
these methods without adding a rule to enforce — the property AC4 actually needs is
that *no caller can write state directly*, which is achieved by keeping every flow
private and exposing only read-only `StateFlow`s.

Note that derived state settles one scheduler pass after an action, not
synchronously. That is inherent to a combined snapshot and is imperceptible in the
UI, but a test asserting on `uiState` after a direct call must let the scheduler run
first.

Two decisions are load-bearing and easy to break:

- **Auto-launch is decided in the ViewModel, not in a composable effect.** The
  guard depends on whether the user edited the query *in this session*
  (`userEditGeneration`) and on which app was already launched
  (`lastAutoLaunchedPackage`). Both must outlive the composition — when they lived
  in a `remember`, anything that recreated the composition launched an app the user
  never asked for.
- **Outgoing actions go through `LauncherGateway`.** Every method returns a
  `Result`, which the ViewModel turns into logged evidence and user-visible
  feedback. No composable calls `startActivity`.

### One reload pipeline

App-list reloads are requested through a conflated `MutableSharedFlow` (replay = 1,
DROP_OLDEST) driving `collectLatest`, debounced except for the first load. This
gives three properties that were previously absent:

1. a burst of `PACKAGE_*` broadcasts collapses into one enumeration;
2. a newer request cancels an in-flight load, so a slow earlier load can never
   publish over a faster later one;
3. `CancellationException` from a superseded load is rethrown, never recorded as a
   failure.

`refresh()` is safe to call as often as needed — that is a deliberate contract, so
callers do not have to coordinate.

### Dispatchers are injected

`ioDispatcher` (default `Dispatchers.IO`) and `computationDispatcher` (default
`Dispatchers.Default`) are constructor parameters. Tests substitute
`TestDispatcher`s, which is what lets a test *prove* that work leaves the main
thread rather than asserting it in a comment. Flow filtering (`drawerApps`) runs on
`computationDispatcher`; the `PackageManager` query runs on `ioDispatcher`.

### Failure handling

There is no "ignore errors" path. The seam is `AppLogger`:

- an uncaught exception inside `viewModelScope` kills the process, and this app is
  the device's home screen, so every suspend entry point that touches the platform
  or disk has an explicit failure branch;
- user-visible failures are typed state (`appListError`, `settingsError`,
  `actionFailure`), not bare `Throwable`s;
- the last known good app list is retained across a failed refresh, and settings
  failures degrade to defaults behind a visible warning.

`runCatching { … }` with a discarded result is treated as a defect, not a style
choice.

### Persistence

`DataStoreSettingsRepository` takes a `DataStore<Preferences>`, not a `Context`,
so it is exercised in JVM tests over a temporary file. All four preference flows
derive from one `catch`ed flow: an `IOException` degrades to defaults and raises a
sticky `readError` instead of silently freezing the UI on initial values forever.

Settings are pruned after a successful enumeration, with two guards: never against
an empty app list (that would wipe the configuration), and never when nothing is
stale (no write on the common path).

---

## 4. UI conventions

- **Theme.** `LauncherPalette` (12 colours) is the single source of truth for
  colour. `LocalPalette` is a `compositionLocalOf` so a theme change recomposes
  the colour readers rather than the whole tree, and `LauncherPalette` is
  `@Immutable`. `Theme.kt` additionally maps the palette onto a Material
  `ColorScheme` so Material components inherit the theme; screens read the palette
  accessors (`Bg`, `TextPrimary`, …) rather than the Material scheme. **This
  duality is intentional**: the palette remains authoritative for custom
  components, and the Material scheme exists so any Material component we adopt
  later is themed without extra work.
- **Spacing and type are tokens.** `Dimens` and `AppTextStyles` hold the values
  that appear in more than one place. Genuinely one-off measurements stay inline.
- **Strings.** All user-visible text lives in `res/values/strings.xml` with a
  Greek translation in `values-el`. `TranslationParityTest` enforces that the two
  files agree on keys, plural quantities and format arguments. Glyph-only strings
  and theme names are marked `translatable="false"` — they are symbols and product
  names, not language.
- **Layout.** The home screen is a fixed, centred column; the only vertical
  gesture belongs to the `VerticalPager` that opens the drawer. Favorites are
  therefore capped at what fits the measured height, with an overflow row routing
  to the drawer. Do **not** add `verticalScroll` to the home screen: it will fight
  the pager.

---

## 5. Testing strategy

Everything runs on the JVM — `./gradlew testDebugUnitTest` — with no Robolectric
and no device. That is a constraint, not a coincidence: it is the only way the
suite stays fast enough to run on every change.

| Concern | How it is tested |
| --- | --- |
| Pure logic (`TextNormalizer`, `AppListOrdering`, `ClockFormatter`, `HomeFavoritesLayout`) | direct unit tests, including locale and boundary cases |
| `DataStoreSettingsRepository` | a **real** DataStore over a `TemporaryFolder` file |
| `LauncherViewModel` | hand-written fakes + `TestDispatcher`, with FIFO load gates to reproduce out-of-order completion |
| Resources | `TranslationParityTest` parses both `strings.xml` files |
| Theming | `PaletteContrastTest` computes WCAG contrast for all 38 palettes |

Fakes are hand-written rather than generated: they are small, explicit, and the
gate helpers needed to reproduce real ordering hazards do not exist in a mocking
framework.

**Not covered by this suite** (each needs a device): Compose UI behaviour,
Activity lifecycle wiring, broadcast delivery, and backup/restore. These are
tracked as QA-3 and the on-device verification items in `USER_STORIES.md`.

---

## 6. Build

- The Gradle wrapper is committed (Gradle 8.9, `distributionSha256Sum` pinned).
  CI invokes `./gradlew` — no system Gradle, no Android Studio.
- Release builds enable R8 and resource shrinking (`isMinifyEnabled`,
  `isShrinkResources`), which takes the APK from ~10.8 MB to ~1.7 MB. Keep rules
  are added only with a stated reason; `proguard-rules.pro` documents that none are
  currently needed.
- Release signing reads `keystore.properties` (gitignored). When it is absent the
  release build still runs so R8 can be verified, producing an unsigned artifact.
- Lint runs with `abortOnError` and promotes the checks this project is actively
  fixing (`HardcodedText`, `UnusedResources`, `ObsoleteSdkInt`,
  `MissingTranslation`, `MonochromeLauncherIcon`). Dependency-upgrade notices stay
  advisory.

---

### Repository hygiene

Every module needs its own `build/` entry in the root `.gitignore`. `/app/build` was
listed explicitly, so adding `:baselineprofile` without an entry for it let a single
`git add -A` commit 989 build artifacts (intermediates, generated manifests and the
bundled `trace_processor`/`tracebox` binaries). They have since been untracked, but
they **remain in git history** — before this branch is pushed anywhere, consider
rewriting it out:

```
git filter-repo --path baselineprofile/build --invert-paths
```

Prefer `git add <paths>` over `git add -A` when a build has run.

## 7. When to split into modules

Not yet, and not at this size. Split when one of these becomes true:

1. build times become the bottleneck (a `:core`/`:data` module with no Compose
   dependency would cut incremental Kotlin compile time);
2. more than one entry point must share behaviour without the launcher UI;
3. a second app variant needs a different subset (e.g. a paid build without
   themes).

The current package boundaries were chosen so that (1) and (2) are mechanical
extractions rather than rewrites: `domain` is already Android-free, `data` already
hides behind interfaces, and `ui` already depends on those interfaces rather than
on implementations.
