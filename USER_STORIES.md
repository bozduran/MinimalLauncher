# Minimal Launcher — Product Backlog

**Source:** [`CODE_AUDIT.md`](./CODE_AUDIT.md) (revision `2bb87a0`)
**Scope:** app stability, correctness, performance and maintainability of the existing feature set. **No net-new features.**
**Format:** GitHub / GitLab issue-importable Markdown; story IDs are epic-prefixed so they survive re-ordering and can be used directly as Jira keys.
**Total:** 41 stories across 9 epics — 16 High, 18 Medium, 7 Low.

---

## Implementation status

Implemented on branch `refactor/user-stories-implementation` (33 stories complete, 8 partial — 41 of 41 touched).
Every commit below is atomic and was verified with `./gradlew assembleDebug assembleRelease testDebugUnitTest`
plus a green test run before committing. **236 unit tests, 0 failures**, lint clean and blocking.

| Story | Status | Commit |
| --- | --- | --- |
| PERF-1 Gradle wrapper | ✅ done | `cd101f1` |
| QA-1 TextNormalizer tests | ✅ done | `cddea69` |
| ARCH-1 dependency injection | ✅ done | `5aed7ea` |
| QA-2 ViewModel test fakes + baseline | ✅ done | `23a390b` |
| STAB-1 load failures must not crash | ✅ done | `a42adaf` |
| STAB-2 settings storage degradation | ✅ done | `a5387f8` |
| STAB-3 single-flight conflated refresh | ✅ done | `3ff08fe` |
| STAB-4 home overflow | ✅ done | `4f3d5d1` |
| STAB-5 visible intent failures | ✅ done | `496f2ae` |
| STAB-6 refresh on locale change | ✅ done | `85083c0` |
| STAB-7 no false empty state | ✅ done | `ee19776` |
| DATA-1 package identity | ✅ done | `6fe1edf` |
| DATA-2 hiding removes from home | ✅ done | `3a20788` |
| DATA-3 prune uninstalled packages | ✅ done | `67c976c` |
| DATA-4 backup rules | ⚠️ done, unverified on device | `dbf60d5` |
| DRAW-1 auto-launch exactly once | ✅ done | `dbbd39d` |
| DRAW-4 precomputed search / off-main filter | ✅ done | `2793102` |
| PERF-2 R8 + shrinking | ⚠️ done, release smoke test outstanding | `996f73a` |
| PERF-3 baseline profile infrastructure | ⚠️ partial — module + CI generation wired; profile not yet generated | `0f5a837`, `77ca89a` |
| A11Y-3 palette contrast audit + themed icon | ✅ done (contrast debt recorded below) | `6330349`, `e181c3b` |
| PLAT-4 hygiene / warning-free build / lint gate | ✅ done | `cdb5740`, `e181c3b` |
| QA-4 CI pipeline (incl. emulator job) | ✅ done | `6dd4d34`, `b3df761` |
| ARCH-3 gateway for all outgoing actions | ✅ done | `4335dde` |
| I18N-1 externalise strings + Greek | ✅ done | `bd87efb` |
| ARCH-4 testable clock formatter | ✅ done | `fdb1af3` |
| DRAW-3 consistent return-to-home | ✅ done | `59fbff7` |
| DRAW-2 deterministic search focus | ✅ done | `1f2aae9` |
| I18N-2 design tokens | ✅ done | `4fe244e` |
| ARCH-5 document architecture | ✅ done | `c34a138` |
| PLAT-3 process death + lifecycle contract | ✅ done | `b6b2c9b` |
| ARCH-6 observable failure seam | ✅ done | `a42be40` |
| ARCH-2 single immutable UI state | ✅ done | `5fdb538` |
| PERF-4 launch window matches the theme | ✅ done | `a2151bb` |
| PLAT-2 predictive back + back semantics | ✅ done | `9c733d7` |
| I18N-3 resolve clock/calendar by capability | ✅ done | `bd4f193` |
| PERF-6 version scheme | ⚠️ partial — versioning done, applicationId needs an owner decision | `ee82ccc` |
| PLAT-1 LauncherApps / profiles | ✅ done (work-profile *rendering* unverified on device) | `4c0be4c` |
| QA-3 instrumented UI suite | ⚠️ partial — written and compiles, never executed | `3229699` |
| A11Y-2 touch targets | ⚠️ partial — tap targets fixed, font-scale rendering unverified | `31614ba` |
| PERF-5 lifecycle-aware UI work | ⚠️ partial — changes in place; recomposition counts unmeasured | `9238155` |
| A11Y-1 screen-reader routes | ⚠️ partial — semantics added; TalkBack behaviour unverified | `085ff1a`

**Open (0 stories) — all 41 are now touched.**

**Partial (8):** DATA-4, PERF-2, PERF-3, PERF-5, PERF-6, QA-3, A11Y-1, A11Y-2.

**PERF-6 decision (owner, round 11):** the `applicationId` stays
`com.example.minimallauncher` for now. It is therefore *not* publishable to Google
Play, and that is a deliberate, recorded choice rather than an oversight. Revisit
before any distribution; README "Releasing" states the consequence (existing installs
become a fresh install with no settings migration) and the alternative (add a
migration first).
Each partial's remaining criterion is listed in the device-verification table below;
seven of the eight need a device or emulator run, and PERF-6 needs an owner decision
on the applicationId.

### Device verification policy

An Android 15 (API 35) device is reachable over `adb` from this environment, but the
project owner has explicitly scoped its use to **read-only / non-invasive**: no APK
install, no instrumented test execution, no launcher or backup changes.

Consequences for the open stories — these are the reasons they are not closed:

| Story | Why a device is needed | Status under this policy |
| --- | --- | --- |
| QA-3 instrumented UI tests | Tests must run on a device | ⚠️ written + compiles; runs in CI's emulator job (QA-4) |
| A11Y-1 TalkBack reach | Needs a real accessibility service | ⚠️ semantics added; announcements unverified |
| A11Y-2 font scale / tap targets | Needs a device at 1.3x–2.0x font scale | ⚠️ tap targets fixed; font scale unverified |
| PERF-5 recomposition counts | Needs Layout Inspector / JankStats on a device | ⚠️ changes landed; counts unmeasured |
| PERF-3 baseline profiles | Profiles are generated and measured on a device | ⚠️ infrastructure done; CI generates the profile |
| PLAT-1 `LauncherApps` / work profiles | Needs a work profile or second user | ✅ done — enumeration is behind an interface and unit-tested; only the on-device rendering is unverified |

PLAT-1 was the exception and is now complete: the profile-aware enumeration sits
behind `LaunchableSource` and is unit-tested with fakes. The five stories above it
remain open because none of them can be completed to that standard here.

**Partial (4):** DATA-4 and PERF-2 are implemented but need on-device verification
(`bmgr backupnow/restore`; an R8 release smoke test). QA-4's CI pipeline is live but
its instrumented emulator job is not wired up until QA-3 exists. PERF-6 has the
version scheme in place but the `applicationId` still needs a domain the project
owns — see README "Releasing".

**A11Y-3 debt (measured, not yet fixed):** secondary text fails WCAG AA on 25 of 38
palettes and tertiary text on 37; the accent fails AA-large on 3 (`ayu-light`,
`rose-pine-dawn`, `everforest-light`). Primary text is compliant everywhere. Closing
this means changing the palette derivation, i.e. visual design, so it was recorded
as executable debt in `PaletteContrastTest` rather than silently loosened.

---

## How to use this backlog

1. Each `### <ID> — <title>` heading is one issue. Copy the body verbatim into Jira/GitLab/GitHub; the `Priority`, `Epic`, `Findings` and `Estimate` lines map to fields/labels.
2. `Findings` links back to the audit. Every audit finding has at least one story, and every story has a finding (see [Traceability](#traceability-matrix)).
3. Acceptance criteria are written as **Given / When / Then** so they can be pasted into a test class (unit or Compose UI) with minimal editing.
4. Estimates are relative t-shirt sizes: **S** ≈ ≤ half a day, **M** ≈ 1–2 days, **L** ≈ 3–5 days.

### Priority definitions

| Priority | Meaning for this app |
| --- | --- |
| **High** | User-visible breakage, data loss, crash, unreachable core action, or a blocker for any release/CI. Ship before anything cosmetic. |
| **Medium** | Correctness, resource, lifecycle or robustness defect with a workaround, or a prerequisite that unlocks a High item. |
| **Low** | Maintainability, accessibility polish, hygiene, or future-proofing. |

### Definition of Done (applies to every story)

- [ ] Code compiles (`./gradlew assembleDebug`) — **requires PERF-1 first**.
- [ ] New/changed logic is covered by a unit test, or the story explicitly states why it is not testable.
- [ ] Manual verification performed on **API 26** (minSdk floor) *and* **API 35** (target).
- [ ] No new lint/compiler warnings introduced; deprecations resolved rather than suppressed.
- [ ] Any new user-visible text is in `res/values/strings.xml` (per I18N-1).
- [ ] Behaviour verified across: cold start, process death + restore, locale change, light *and* dark theme, and a package install/uninstall while the launcher is running.
- [ ] `CODE_AUDIT.md` finding status updated if the story changes the underlying behaviour.

### Story template

```
### ID — Title
**Priority:** High | Medium | Low · **Epic:** … · **Findings:** … · **Estimate:** S | M | L

**As a** <persona>, **I want** <capability>, **so that** <value>.

**Acceptance Criteria**
1. **Given** … **When** … **Then** …

**Technical Implementation Notes**
- Files, APIs, libraries, tests.
```

---

# EPIC-STAB — Stability & Crash Prevention

> A launcher crash does not merely close an app — the device is left without a home screen. This epic is the highest priority in the backlog.

---

### STAB-1 — App-list loading failures must never crash the launcher
**Priority:** High · **Epic:** STAB · **Findings:** F1, R2 · **Estimate:** S

**As a** launcher user, **I want** the launcher to keep running and tell me when the app list cannot be read, **so that** a flaky `PackageManager` response never leaves me without a home screen.

**Acceptance Criteria**
1. **Given** `PackageManager.queryIntentActivities` throws (`SecurityException`, `DeadObjectException`, any `RuntimeException`), **When** the launcher loads or refreshes its app list, **Then** the process does not crash and the previously loaded list (or a defined empty state) remains on screen.
2. **Given** the app list fails to load, **When** the failure is handled, **Then** a one-shot error event is emitted so the UI can show a non-blocking message and a retry affordance.
3. **Given** a load failure has been reported, **When** the user retries or the next package-change event arrives, **Then** loading is attempted again and success clears the error.
4. **Given** the failure path executes, **When** it is observed, **Then** it has been recorded through the failure seam (ARCH-6) with the exception type — not silently discarded.

**Technical Implementation Notes**
- `LauncherViewModel.kt:105-110` — wrap the body of `refresh()` in `runCatching`/`try-catch` and model the outcome as a `Result`.
- `AppRepository.kt:19-30` — also guard per-resolve failures inside the loop (`mapNotNull` + `runCatching`) so one bad package does not fail the whole enumeration.
- Expose the error as part of `LauncherUiState` (ARCH-2) rather than a raw `Throwable` in the UI.
- Coroutine safety: `viewModelScope` uses `SupervisorJob` but has **no** exception handler, so an uncaught throw still terminates the process — catching is the fix, not the scope.
- Test: `LauncherViewModelTest` with a `FakeAppRepository` that throws (QA-2).

---

### STAB-2 — Settings storage failures must degrade gracefully instead of freezing silently
**Priority:** High · **Epic:** STAB · **Findings:** F2, R2 · **Estimate:** M

**As a** launcher user, **I want** a corrupt or unreadable settings store to fall back to defaults with a visible warning, **so that** the launcher doesn't silently ignore every toggle and appear broken.

**Acceptance Criteria**
1. **Given** the DataStore file raises `IOException` on read, **When** any settings flow is collected, **Then** that flow emits defaults (empty favorites/hidden, 24h on, `DEFAULT_THEME_KEY`) instead of terminating, and the UI stays interactive.
2. **Given** a read failure occurred, **When** the launcher renders settings, **Then** a user-visible, dismissible warning states that settings could not be loaded — the user is not shown a fabricated default as if it were their configuration.
3. **Given** a settings **write** fails (`IOException`, disk full), **When** the user toggles a favorite/theme/hidden app, **Then** the failure is caught, reported through the failure seam, and the optimistically-updated UI reverts to the persisted value.
4. **Given** a failed read is later retried, **When** the store becomes readable, **Then** the real values replace the defaults without an app restart.
5. **Given** settings storage is completely unavailable, **When** the user opens the drawer, **Then** app enumeration and launching still work (the launcher's core action never depends on settings).

**Technical Implementation Notes**
- `SettingsRepository.kt:25-34` — add `.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }` to each of the four flows; this is the documented DataStore guidance, and without it each `stateIn` at `LauncherViewModel.kt:42-58` dies permanently while keeping its initial value.
- `SettingsRepository.kt:36-60` — return `Result<Unit>` (or throw a typed error) from the `edit` helpers; do not let the exception escape into `viewModelScope.launch` (`LauncherViewModel.kt:121-124`, see F1).
- Prefer exposing a single `SettingsState` + `SettingsError` rather than four independent flows (ARCH-2).
- Tests: in-memory/temporary-file DataStore; assert defaults-on-IOException and revert-on-write-failure.

---

### STAB-3 — Single-flight, conflated app-list refresh
**Priority:** High · **Epic:** STAB · **Findings:** F3, F12 · **Estimate:** M

**As a** launcher user, **I want** the app list to always reflect the current state of the device, **so that** freshly installed or removed apps are shown correctly and never flicker back to a stale list.

**Acceptance Criteria**
1. **Given** two refreshes are requested in quick succession, **When** both complete, **Then** only the most recent result is published to the UI (the older result can never overwrite the newer one).
2. **Given** a burst of package-change broadcasts (e.g. a Play Store "update all" of 40 apps), **When** the events arrive within a short window, **Then** the launcher performs a bounded number of `PackageManager` enumerations (debounced/conflated) rather than one per event.
3. **Given** the launcher is resumed while a refresh is already in flight, **When** `onResume` requests another refresh, **Then** the in-flight work is joined or superseded — not duplicated.
4. **Given** a refresh is superseded, **When** the superseded load completes, **Then** it does not clear or partially overwrite the newer state.
5. **Given** the receiver is registered, **When** unregistration happens in `onCleared`, **Then** no further refreshes are triggered and no coroutine leaks into the next ViewModel instance.

**Technical Implementation Notes**
- `LauncherViewModel.kt:105-110` — replace the ad-hoc `launch` with a conflated pipeline: a `MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST)` (or `MutableStateFlow<Unit>`) feeding `refreshRequests.debounce(250).mapLatest { appRepo.loadApps() }` collected once in `init`, or serialise with a `Mutex` + `latest`-check.
- Do **not** simply add a `Mutex`: mutual exclusion fixes ordering but not the redundant work; conflation fixes both.
- `LauncherViewModel.kt:85-97` — keep the four actions, but consider narrowing to `PACKAGE_ADDED|REMOVED|REPLACED` only, since `PACKAGE_CHANGED` (component enable/disable, permissions) fires far more often without changing the launcher set.
- `MainActivity.kt:64` — keep the `onResume` refresh only if the conflated pipeline makes it cheap; otherwise rely on the receiver + a staleness timestamp.
- Test: fake repository with controllable delays; assert publish order and enumeration count for a burst (QA-2).

---

### STAB-4 — The home screen must remain fully usable with many favorites
**Priority:** High · **Epic:** STAB · **Findings:** F4 · **Estimate:** M

**As a** launcher user with many favorite apps, **I want** every favorite and the clock to stay reachable, **so that** I never lose access to an app because my list grew too long.

**Acceptance Criteria**
1. **Given** 30 favorites configured, **When** the home screen is shown on a 5-inch 720p device, **Then** the clock, the date and **all 30** favorites are reachable (visible, or reachable by an explicit, working gesture).
2. **Given** the content exceeds the viewport, **When** it is laid out, **Then** nothing is clipped at *both* edges and the first content item is never pushed off-screen.
3. **Given** a long favorites list, **When** the user swipes vertically on the home screen, **Then** the pager transitions to the drawer (existing behaviour) **or** the list scrolls — never both simultaneously, and never neither.
4. **Given** the device is rotated or the font scale is increased, **When** the layout recomputes, **Then** reachability from criterion 1 still holds.
5. **Given** fewer favorites than fit, **When** the screen is rendered, **Then** the existing centred visual composition is preserved (this story must not regress the current aesthetic).

**Technical Implementation Notes**
- `HomeScreen.kt:65-75` — root `Column` with `Arrangement.Center` and no scroll; `HomeScreen.kt:106-119` — one ~50dp row per favorite.
- **The obvious `Modifier.verticalScroll()` conflicts with the `VerticalPager`** (`MainActivity.kt:99-109`): both want vertical drag. Choose one of:
  1. **Preferred:** keep Home non-scrolling; make overflow explicit — cap the visible list and route the rest into the drawer (e.g. "…and 14 more" row that switches to the drawer page), or
  2. give the favorites block its own scrollable sub-viewport with `Modifier.nestedScroll` + a pager-aware `NestedScrollConnection` so a drag at the list's edge hands off to the pager.
- Do not use `Arrangement.Center` with potentially overflowing content; if the list stays centred, switch to a `LazyColumn` with `verticalArrangement = Arrangement.Center` semantics that degrade gracefully.
- Add a "many favorites" screenshot/UI test (QA-3) at 3 sizes.

---

### STAB-5 — Failed system-intent dispatches must be visible to the user
**Priority:** Medium · **Epic:** STAB · **Findings:** F13, R2 · **Estimate:** S

**As a** launcher user, **I want** a clear message when a launcher action cannot be performed, **so that** I know whether to retry or configure something instead of assuming the app is broken.

**Acceptance Criteria**
1. **Given** no activity can handle `Settings.ACTION_HOME_SETTINGS`, **When** the user taps "set as default launcher", **Then** a message explains that the system settings screen is unavailable (and the existing generic-settings fallback is attempted first).
2. **Given** "uninstall" or "app info" cannot be dispatched (`ActivityNotFoundException`, `SecurityException`), **When** the user selects it, **Then** a message naming the app is shown.
3. **Given** no clock app resolves `AlarmClock.ACTION_SHOW_ALARMS`, **When** the user taps the time, **Then** a message is shown rather than nothing happening.
4. **Given** any dispatch fails, **When** it is handled, **Then** the failure is recorded through the failure seam (ARCH-6) — `runCatching` with a discarded result is not acceptable.
5. **Given** the dispatch succeeds, **When** the transition occurs, **Then** no toast or message is shown.

**Technical Implementation Notes**
- `AppLauncher.kt:27,33,38-48,54,61` — replace discarded `runCatching {}` with `Result`-returning functions; `AppLauncher.kt:18-19` already reports for `launch`, so unify on that pattern.
- Route reporting through the ViewModel (ARCH-3) so the message is state-driven and testable rather than a `Toast` from inside a singleton.
- `AppLauncher.kt:57-62` — `openCalendar` hardcodes `com.google.android.calendar`; replace with capability-based resolution (I18N-3) before adding a failure message for it.
- Tests: fake `PackageManager`/`Context` returning no resolver; assert the reported error type.

---

### STAB-6 — Refresh the app list when the device locale changes
**Priority:** Low · **Epic:** STAB · **Findings:** F14 · **Estimate:** S

**As a** bilingual launcher user, **I want** the app list to be re-sorted when I change the system language, **so that** apps appear in the correct alphabetical order for that locale.

**Acceptance Criteria**
1. **Given** the launcher is running, **When** the user changes the system language, **Then** the app list is re-sorted using a `Collator` for the new locale without restarting the launcher.
2. **Given** the locale changes, **When** the list is re-sorted, **Then** favorites on the home screen keep their user-defined order (favorites are explicitly ordered, not collated).
3. **Given** the launcher is backgrounded when the locale changes, **When** it is resumed, **Then** the list is already correct.
4. **Given** the locale changes, **When** the clock/date re-renders, **Then** the date uses the new locale's formatting — and, if the system is set to a 12-hour locale, the display honours the user's explicit 12h/24h preference over the system default.

**Technical Implementation Notes**
- `AppRepository.kt:17` — `Collator.getInstance(Locale.getDefault())` is already created per call, so only a trigger is missing.
- Add `Intent.ACTION_LOCALE_CHANGED` to the filter at `LauncherViewModel.kt:85-97` (this is a protected broadcast; `RECEIVER_NOT_EXPORTED` is correct for it).
- Consider also `ACTION_TIMEZONE_CHANGED`/`ACTION_TIME_CHANGED` for the clock, or rely on the minute tick (`HomeScreen.kt:41-49`) which self-corrects.
- Test: inject a `Locale` provider, assert sort order changes for Greek vs English collation.

---

### STAB-7 — No false "no favorites yet" state while the app list is loading
**Priority:** Medium · **Epic:** STAB · **Findings:** F15, R2 · **Estimate:** S

**As a** launcher user with favorites configured, **I want** the launcher to distinguish "still loading" from "nothing configured", **so that** I am never told my configuration is empty when it is not.

**Acceptance Criteria**
1. **Given** the launcher has just cold-started and the app list is still loading, **When** the home screen renders, **Then** the "no favorites yet — long-press anywhere to open settings" onboarding text is **not** shown.
2. **Given** loading completes with favorites present, **When** the screen re-renders, **Then** the favorites appear without an intermediate empty-state flash.
3. **Given** loading completes and the user genuinely has no favorites, **When** the screen re-renders, **Then** the onboarding text is shown.
4. **Given** loading fails (STAB-1), **When** the screen re-renders, **Then** an error/retry state is shown — not the onboarding text.
5. **Given** the launcher is resumed and the list is refreshed, **When** the refresh runs, **Then** the previous favorites remain visible (no flashing empty state) until the new list replaces them.

**Technical Implementation Notes**
- Root cause: `favorites`/`drawerApps` are `stateIn(..., initialValue = emptyList())` (`LauncherViewModel.kt:60-73`) with no loading flag, and `allAppsFlow` starts empty (`:33-34`).
- Model it in `LauncherUiState` (ARCH-2) as a sealed `AppListState { Loading, Ready(apps), Error(cause) }`; keep the last `Ready` payload visible during a re-load so refreshes never flash.
- Test: assert the onboarding text is absent in the `Loading` state (QA-3).

---

# EPIC-DATA — Data Correctness & Durability

> The launcher's persisted state is tiny but entirely user-owned: favorites order, hidden set, theme. Getting its identity model and lifecycle right is cheap now and expensive later.

---

### DATA-1 — One drawer and home entry per app package
**Priority:** High · **Epic:** DATA · **Findings:** F5 · **Estimate:** M

**As a** launcher user, **I want** each app to appear exactly once and to be favorited/hidden as a whole, **so that** the list isn't cluttered with duplicates and my actions apply consistently.

**Acceptance Criteria**
1. **Given** an installed app declares two `MAIN`/`LAUNCHER` activities, **When** the drawer and settings list render, **Then** that app appears exactly **once**, using its default launcher activity.
2. **Given** such an app is favorited, **When** the home screen renders, **Then** it appears once — not once per activity.
3. **Given** an app is hidden, **When** the drawer renders, **Then** every entry for that package is hidden.
4. **Given** the settings list and the drawer, **When** the same app is rendered in both, **Then** both use the same identity key and the same favorite/hidden state.
5. **Given** an app publishes a single launcher activity (the common case), **When** the list renders, **Then** behaviour is unchanged from today (no regression).
6. **Given** two distinct apps share a display label, **When** either is favorited, **Then** only that app is affected.

**Technical Implementation Notes**
- Today: `AppInfo.key = "$packageName/$activityName"` (`AppInfo.kt:8`) and `.distinctBy { it.key }` (`AppRepository.kt:29`) versus package-keyed settings (`SettingsRepository.kt:19-20`, `LauncherViewModel.kt:121-122`).
- **Decision to make in this story:** unify on `packageName` as the identity, keep `activityName` as a launch detail. `AppInfo` becomes keyed by package; the drawer's `key = { it.key }` (`DrawerScreen.kt:110`) follows.
- Dedupe by choosing a deterministic representative activity per package: prefer the one returned for the explicit package launch intent, else the first resolve result, ordered deterministically (label, then activity name) so the choice is stable across refreshes.
- **Migration:** existing installs may hold favorites/hidden values that are already package names (no migration needed) — verify this before shipping, since any change to the stored key format requires a DataStore migration. Document the guarantee in the repository.
- Tests: repository test with a fake resolver returning duplicate packages; assert one entry, correct chosen activity, and stability across two loads.

---

### DATA-2 — Hiding an app removes it from the home screen
**Priority:** High · **Epic:** DATA · **Findings:** F6 · **Estimate:** S

**As a** launcher user, **I want** "hide app" to remove the app from my launcher everywhere, **so that** the action means what it says.

**Acceptance Criteria**
1. **Given** a favorite app, **When** the user chooses "hide app" from the drawer menu, **Then** it disappears from the drawer **and** from the home screen.
2. **Given** an app is hidden, **When** the user opens Settings, **Then** the app is listed as hidden and can be unhidden.
3. **Given** an app is unhidden, **When** the drawer renders, **Then** the app reappears in its correct alphabetical position.
4. **Given** an app is hidden and later unhidden, **When** it returns to the drawer, **Then** its previous favorite status is applied deliberately (choose and document one: restore the favorite, or require re-favoriting).
5. **Given** an app is hidden, **When** the user searches for its exact name in the drawer, **Then** it stays hidden (no accidental exposure) — unless the "hidden-app unlock" follow-up is explicitly implemented later.

**Technical Implementation Notes**
- `LauncherViewModel.kt:60-64` — `favorites` combines only `allAppsFlow` and `favoritePkgs`; add the `hiddenPkgs` filter (or, better, filter once in a shared derived state).
- `SettingsRepository.kt:46-52` — decide whether `toggleHidden` also mutates the favorites list; recommendation: **keep the favorite flag and filter it in the view** so unhiding restores the previous state, and make that explicit in the story's acceptance test (criterion 4).
- Guard for a hidden-then-uninstall path via DATA-3.
- Test: `LauncherViewModelTest` — toggle hidden, assert `favorites` and `drawerApps` both exclude the package.

---

### DATA-3 — Prune settings for uninstalled packages
**Priority:** Medium · **Epic:** DATA · **Findings:** F6 · **Estimate:** S

**As a** launcher user, **I want** stale favorites and hidden entries removed when an app is uninstalled, **so that** reinstalling an app doesn't silently resurrect old choices or bloat my settings file.

**Acceptance Criteria**
1. **Given** an app is favorited, **When** the package is uninstalled, **Then** its favorites entry is removed from DataStore.
2. **Given** an app is hidden, **When** the package is uninstalled, **Then** its hidden entry is removed.
3. **Given** an app was uninstalled and later reinstalled, **When** the drawer/home render, **Then** the app appears in the default (unfavorited, unhidden) state unless the user opts in.
4. **Given** a package is uninstalled while a work profile still exposes it, **When** pruning runs, **Then** the entry is retained for the profile that still has it installed.
5. **Given** pruning runs, **When** it makes no changes, **Then** no write occurs and no UI flicker results.

**Technical Implementation Notes**
- Trigger from the existing `ACTION_PACKAGE_REMOVED` handling (`LauncherViewModel.kt:85-97`) — note the broadcast carries `EXTRA_REPLACING`; skip pruning when replacing, or the entry is dropped on every app update.
- Also prune opportunistically after each successful enumeration: `(favorites + hidden) - installedPackages`, diffed before writing.
- Use `LauncherApps`/profile-aware enumeration where available (PLAT-1) so criterion 4 holds.
- Test: toggle favorite → fake uninstall → assert DataStore content; and assert replace-update does **not** prune.

---

### DATA-4 — Backup and restore must actually include launcher settings
**Priority:** Medium · **Epic:** DATA · **Findings:** F11 · **Estimate:** S

**As a** launcher user replacing my phone, **I want** my favorites, hidden apps and theme restored automatically, **so that** I don't spend 20 minutes reconfiguring my home screen.

**Acceptance Criteria**
1. **Given** a user with favorites, hidden apps and a non-default theme, **When** Auto Backup runs and is restored on a second device/emulator, **Then** all three settings are restored exactly.
2. **Given** a device-to-device transfer, **When** it completes, **Then** the launcher settings are transferred (behaviour is explicitly declared rather than inherited by default).
3. **Given** a backup/restore round-trip, **When** the restore completes, **Then** no stale or conflicting settings remain from the target device's previous install.
4. **Given** the backup rules, **When** reviewed, **Then** the included domains match where the data actually lives, with a comment stating the exact file path, so a future storage change doesn't silently break backups again.
5. **Given** the app has no other stateful data, **When** the rules are finalised, **Then** they do not include unrelated domains (no `database`/`external`/`root` over-inclusion).

**Technical Implementation Notes**
- `backup_rules.xml` and `data_extraction_rules.xml` currently include only `domain="sharedpref"`, but `preferencesDataStore(name = "launcher_settings")` (`SettingsRepository.kt:13`) writes to `files/datastore/launcher_settings.preferences_pb` — the **`file`** domain. Add `<include domain="file" path="datastore/" />` (or move the DataStore file under a dedicated `sharedpref`-backed path if you prefer the narrower domain).
- `data_extraction_rules.xml` has **no `<device-transfer>` section** — declare one explicitly so D2D behaviour is a decision, not a default.
- Because an `<include>` list is exclusive, the current rules effectively back up **nothing** the app writes. See the Android [Auto Backup](https://developer.android.com/identity/data/autobackup) documentation for domain semantics.
- **Verification is mandatory for this story:** `adb shell bmgr backupnow com.example.minimallauncher`, then `adb shell bmgr restore` on a second AVD, and assert the restored preferences. Include the exact commands in the PR description.
- Cross-check the Android 12 behaviour note that `allowBackup="false"` would not disable D2D transfer ([behaviour changes](https://developer.android.com/about/versions/12/behavior-changes-12)) — the reverse of what most people assume.

---

# EPIC-DRAW — Drawer & Search Behaviour

---

### DRAW-1 — Auto-launch fires only on deliberate input, exactly once
**Priority:** High · **Epic:** DRAW · **Findings:** F7, F16, ARCH-2 · **Estimate:** M

**As a** launcher user, **I want** a single search match to open only when I actually finished typing, **so that** apps never open by themselves or twice.

**Acceptance Criteria**
1. **Given** the user types a query that narrows to exactly one app, **When** they stop typing for the debounce interval, **Then** that app launches **once**.
2. **Given** an app was auto-launched from query `chro`, **When** the user returns and types one more character (`chrom`), **Then** no second launch occurs for an unchanged single-match result.
3. **Given** the Activity is recreated (process death, restore, system recreate) while a non-blank single-match query is present, **When** the drawer is recomposed, **Then** **no** app launches — auto-launch only follows a user edit made in this session.
4. **Given** the user clears the query with the "×" control, **When** the query becomes blank, **Then** the "already launched" guard resets so a subsequent search behaves as a fresh search.
5. **Given** the user presses Home, **When** the query is cleared (`onHomePressed`), **Then** the guard resets as in criterion 4.
6. **Given** the query matches zero apps, **When** the debounce elapses, **Then** nothing launches and no error is shown.
7. **Given** auto-launch is armed, **When** the user instead presses the IME Search action, **Then** the top result launches exactly once (no double launch from the effect plus the action).

**Technical Implementation Notes**
- Current defect source: `DrawerScreen.kt:55-86` — `lastLaunched` is `remember` (null after recreation) and compared against the raw query string, so both criterion 2 and criterion 3 fail.
- Model intent in the ViewModel, not the composable: an incrementing `editGeneration` (or a `SearchState(query, matches, autoLaunchConsumed)` value in `LauncherUiState`, ARCH-2). An effect may only consume state that was produced by a user edit since the last consumption.
- `LauncherViewModel.kt:36` — stop exposing `MutableStateFlow`; expose `StateFlow` + `setQuery` so nothing else can trigger a launch (S4/F16).
- Consider moving the debounce into the ViewModel (`query` flow → `debounce(350) → matches`) so the guard and the debounce live together and are unit-testable.
- Tests (QA-2): simulate a user edit → revert to a restored state → assert zero launches; simulate narrowing → assert one launch per distinct *user* action.

---

### DRAW-2 — Deterministic search-field auto-focus
**Priority:** High · **Epic:** DRAW · **Findings:** F8 · **Estimate:** S

**As a** launcher user, **I want** the search box focused and the keyboard open every time I swipe up to the drawer, **so that** I can always start typing immediately.

**Acceptance Criteria**
1. **Given** the drawer page becomes active, **When** the search field is composed, **Then** it receives focus and the IME opens — on a cold start and on a warm swipe-up, on a low-end device, with no timing dependency.
2. **Given** the drawer becomes inactive (swiped back to home, or Home pressed), **When** the transition occurs, **Then** the IME is dismissed and the field loses focus.
3. **Given** focus is requested, **When** the field is not yet attached, **Then** the request is retried after the next frame instead of throwing and being swallowed.
4. **Given** the request fails twice, **When** it finally gives up, **Then** the failure is recorded through the failure seam (ARCH-6) — no silent `runCatching`.
5. **Given** the drawer is active and the user taps a different page then returns, **When** the page becomes active again, **Then** the field is focused again.

**Technical Implementation Notes**
- `DrawerScreen.kt:63-71` — replace `delay(100)` with `LaunchedEffect(active) { if (active) { awaitFrame(); focusRequester.requestFocus() } }`; pair with `Modifier.focusRequester(...)` and, if needed, trigger from `onPlaced`/`onGloballyPositioned` so the node existence is observed rather than guessed.
- Drive the IME from the focus change (`BasicTextField` + `KeyboardOptions`) rather than calling `LocalSoftwareKeyboardController.show()` immediately; use `keyboardController.hide()` on deactivation.
- Consider `WindowInsets.ime` handling: `imePadding()` is already applied (`DrawerScreen.kt:93`).
- Test: `createAndroidComposeRule` — swipe to the drawer, assert the field `isFocused()` and the IME is visible (QA-3).

---

### DRAW-3 — Consistent return-to-home behaviour
**Priority:** Medium · **Epic:** DRAW · **Findings:** F17 · **Estimate:** M

**As a** launcher user, **I want** the launcher to return to a predictable state whenever I come back to it, **so that** I always land on my home screen instead of a stale search.

**Acceptance Criteria**
1. **Given** the user launched an app from the drawer and returns via the **Home button**, **When** the launcher resumes, **Then** the app shows page 0 (home) with an empty query and no IME.
2. **Given** the same scenario but the user returns via **Back or Recents**, **When** the launcher resumes, **Then** the outcome is identical to criterion 1.
3. **Given** the user opens Minimal explicitly from another launcher's app list, **When** the launcher is already running, **Then** it returns to page 0 with a cleared query (documented behaviour).
4. **Given** the user is on page 1 with a query and presses Back, **When** Back is handled, **Then** it returns to page 0 (existing behaviour) and then, on page 0, Back does not exit the launcher to a black screen.
5. **Given** the launcher resumes after any transition, **When** the UI is rendered, **Then** the IME is not visible over the home screen.
6. **Given** an app menu dialog is open, **When** the Home button is pressed, **Then** the dialog is dismissed and the launcher returns to page 0.

**Technical Implementation Notes**
- `MainActivity.kt:61-71` — reset currently hangs off `onNewIntent` only; add the equivalent on `onResume`/`onStart` (or a `LifecycleEventObserver`) so Back/Recents returns behave the same, and call `setIntent` to keep the framework contract consistent.
- Keep the query/pager reset in the ViewModel (`LauncherViewModel.kt:116-119`) so it is testable, and expose a single `onReturnToHome()` entry point rather than splitting it between `onResume` and `onNewIntent`.
- Interact with `LauncherRoot`'s `goHome` collector (`MainActivity.kt:83-88`) and the dialog state at `DrawerScreen.kt:55` (consider hoisting `menuApp` so the launcher (not the page) owns dialog dismissal).
- Depends on PLAT-3 for the process-death definition of "return to a predictable state".
- Test: instrumentation test that launches an app and uses `ActivityScenario.recreate()` + Back; assert page 0 and empty query.

---

### DRAW-4 — Search filtering off the main thread with precomputed keys
**Priority:** Medium · **Epic:** DRAW · **Findings:** S5 · **Estimate:** S

**As a** launcher user, **I want** typing in the drawer to stay smooth on a device with 200 apps, **so that** search never stutters while I type.

**Acceptance Criteria**
1. **Given** 200 installed apps, **When** the user types a character in the search field, **Then** no normalisation or matching work runs on the main thread.
2. **Given** the app list has been loaded, **When** the user filters it repeatedly, **Then** each app's search key is computed **once per list load**, not once per keystroke.
3. **Given** the filtered result changes, **When** the UI renders, **Then** the visible list updates within the same debounce window and no frame exceeds 16 ms due to filtering (verify with a Macrobenchmark or `JankStats` trace).
4. **Given** the app list is reloaded (package event), **When** search keys are rebuilt, **Then** results remain correct for the new list.
5. **Given** the existing matching semantics, **When** this story lands, **Then** all `TextNormalizer` behaviours are preserved (Greek accents, final sigma, Latin accents, blank query) — proven by QA-1.

**Technical Implementation Notes**
- `LauncherViewModel.kt:67-73` — the `combine` lambda runs in the `stateIn` collector context (`viewModelScope` → Main). Move the predicate off Main with `.flowOn(Dispatchers.Default)` on an upstream flow, or compute matches in a `mapLatest { withContext(Dispatchers.Default) { … } }`.
- Precompute the normalised label once per app: add `val searchKey: String` to `AppInfo` (or a side map built when the list is assembled in `AppRepository.loadApps`), computed in `AppRepository.kt:23-27` while already on `Dispatchers.IO`.
- Keep `TextNormalizer` pure and unchanged (`TextNormalizer.kt:14-28`) — it is the reference implementation and the test target.
- Injection: use a dispatcher parameter (ARCH-1) so `Dispatchers.Default` can be replaced with a test dispatcher.
- Benchmarks: assert filtering 200 apps completes well under the 350 ms debounce with margin.

---

# EPIC-ARCH — Architecture & Testability

> The target is explicitly **not** a full Clean Architecture rewrite (~1,500 LOC does not justify use-case-per-action). It is to make the existing two-package boundary real enough to test and extend.

---

### ARCH-1 — Inject repositories through a ViewModel factory
**Priority:** High · **Epic:** ARCH · **Findings:** S1, S2, R1 · **Estimate:** M

**As a** developer, **I want** `LauncherViewModel` to receive its dependencies through its constructor, **so that** I can unit-test every behaviour with fakes instead of running on a device.

**Acceptance Criteria**
1. **Given** the new constructor, **When** the ViewModel is created in production, **Then** a `ViewModelProvider.Factory` supplies the real repository implementations and no `new`-ing happens inside the ViewModel.
2. **Given** the ViewModel under test, **When** it is constructed with a fake repository, **Then** all existing behaviours (favorites, hidden, refresh, query) can be exercised in a plain JVM unit test with `kotlinx-coroutines-test`.
3. **Given** a dispatcher parameter, **When** tests inject a `TestDispatcher`, **Then** no real dispatching occurs.
4. **Given** `MainActivity`, **When** the ViewModel is obtained, **Then** it is still scoped to the Activity's lifecycle with no behaviour change.
5. **Given** the interfaces, **When** an alternative implementation is needed (e.g. a `LauncherApps`-backed enumerator, PLAT-1), **Then** it can be swapped at the factory without touching the ViewModel.
6. **Given** the change, **When** the app runs, **Then** observable behaviour is identical to before (this is a pure refactor).

**Technical Implementation Notes**
- `LauncherViewModel.kt:28-31` — replace `AndroidViewModel(app)` + inline construction with `ViewModel(deps)`; add `AppRepository`/`SettingsRepository` interfaces in `data/` and keep the current classes as the Android implementations (rename to `PackageManagerAppRepository`, `DataStoreSettingsRepository` for clarity).
- Prefer `ViewModelProvider.Factory` with `CreationExtras` over `AndroidViewModel`, so `Context` disappears from the ViewModel's signature (S2). If `Application` is still required, keep it as a constructor parameter of the *implementation*, not the VM.
- Optional alternative: `androidx.lifecycle.viewmodel.compose.viewModel { LauncherViewModel(...) }` with a `viewModelFactory { initializer { … } }` — no DI framework needed at this size.
- Add test dependencies: `kotlinx-coroutines-test`, `junit`, `app/src/test`.
- This story unblocks QA-2 and is a prerequisite for ARCH-2's tests.

---

### ARCH-2 — One immutable UI state with intent-style actions
**Priority:** Medium · **Epic:** ARCH · **Findings:** S4, F2, F7, F15, F16, R1 · **Estimate:** L

**As a** developer, **I want** a single, immutable, testable UI state for the launcher, **so that** screens render one consistent snapshot and impossible states (loading-but-empty, hidden-but-favorite) cannot occur.

**Acceptance Criteria**
1. **Given** the ViewModel, **When** the UI subscribes, **Then** it collects exactly one `StateFlow<LauncherUiState>` for rendering the launcher surface, replacing the current per-screen ad-hoc subscriptions.
2. **Given** `LauncherUiState`, **When** it is inspected, **Then** it distinguishes at least `Loading`, `Ready` and `Error` for the app list, so no screen can render a fabricated empty state (STAB-7).
3. **Given** an already-loaded list, **When** a refresh runs, **Then** the previous `Ready` payload remains in the state (no `Loading` flash on every resume).
4. **Given** user actions (search edited, app clicked, favorite toggled, hidden toggled, theme selected, 24h toggled, home pressed), **When** they are dispatched, **Then** they are expressed as typed intents rather than direct `MutableStateFlow` writes.
5. **Given** the state object and its collections, **When** it is compared for equality, **Then** it is stable and immutable (no `MutableList`/`MutableSet` in the public type, `@Immutable` where useful) so Compose can skip recomposition.
6. **Given** the state, **When** a test drives a sequence of intents, **Then** the resulting states can be asserted deterministically (no `delay`-based assertions).
7. **Given** `query`, **When** it is exposed, **Then** it is read-only and reachable only through a search intent (F16).

**Technical Implementation Notes**
- Consolidate `LauncherViewModel.kt:34-73` (nine observable surfaces) into a state holder; keep `favorites`, `drawerApps` and `favoriteSet` as *derived* properties of the state rather than separate `stateIn` pipelines (`SharingStarted.Eagerly` on six flows also keeps everything hot when only one screen is visible — see PERF-5).
- Screens change from `vm.favorites.collectAsStateCompat()` to reading fields of the single collected state (`HomeScreen.kt:57-58`, `DrawerScreen.kt:52-54`, `SettingsScreen.kt:54-58`), and from direct `vm.x()` calls to `vm.dispatch(LauncherIntent.X)`.
- Keep the intent set minimal and named after user actions; do not introduce a generic event bus.
- Sequencing: land **after** ARCH-1 (testability) and **before** ARCH-3, so gateway results flow into the state naturally.
- Tests: intent-sequence tests for search/favorite/hidden/theme, plus the F7 auto-launch guard and the F15 loading distinction.

---

### ARCH-3 — Route app launching and system intents through the ViewModel
**Priority:** Medium · **Epic:** ARCH · **Findings:** S3, R1 · **Estimate:** M

**As a** developer, **I want** all side effects to go through the ViewModel and a gateway abstraction, **so that** launch behaviour is testable, loggable and lifecycle-safe.

**Acceptance Criteria**
1. **Given** the UI, **When** an app row, the clock, the date, "app info", "uninstall" or "set as default launcher" is activated, **Then** the composable dispatches an intent — it does not call `AppLauncher` or `startActivity` itself.
2. **Given** the gateway interface, **When** a test injects a fake, **Then** no real `Intent` is dispatched and the launched package/activity can be asserted.
3. **Given** an auto-launch decision, **When** it is made, **Then** it is made in the ViewModel (a pure function of state) rather than in a composable `LaunchedEffect`.
4. **Given** any dispatch failure, **When** it occurs, **Then** the gateway returns a typed result that the ViewModel turns into visible feedback (STAB-5) and a recorded event (ARCH-6).
5. **Given** `AppLauncher`'s five responsibilities (`launch`, `openAppInfo`, `uninstall`, `openHomeSettings`, `openClock`, `openCalendar`), **When** the gateway is defined, **Then** each is an explicit method with a documented contract, and no method mutates an `Intent` it did not create.
6. **Given** the composables, **When** they no longer need `LocalContext.current` for side effects, **Then** `context` is removed from those call sites.

**Technical Implementation Notes**
- Call sites to convert: `HomeScreen.kt:83, 92, 116`; `DrawerScreen.kt:84, 100, 120, 135-136`; `SettingsScreen.kt:93`.
- `AppLauncher.kt:9-63` becomes the implementation behind `interface LauncherGateway { fun launch(app: AppInfo): Result<Unit>; fun openAppInfo(pkg: String): Result<Unit>; fun uninstall(pkg: String): Result<Unit>; fun openHomeSettings(): Result<Unit>; fun openClock(): Result<Unit>; fun openCalendar(): Result<Unit> }`.
- `AppLauncher.kt:12-17` — stop mutating the `Intent` returned by `getLaunchIntentForPackage`; build a fresh intent from the returned one's component, or add the flag on a copy.
- The gateway still needs a `Context`; hold it in the application-scoped implementation, never in a composable.
- Tests: fake gateway asserting the exact `(pkg, activity)` pair for each tap and for auto-launch.

---

### ARCH-4 — Testable, remembered clock and date formatting
**Priority:** Medium · **Epic:** ARCH · **Findings:** R1, S6 · **Estimate:** S

**As a** developer, **I want** clock/date formatting to be pure and injectable, **so that** the 12h/24h behaviour and locale formatting can be unit-tested without a device.

**Acceptance Criteria**
1. **Given** a fixed `LocalDateTime` and `use24h = true`, **When** the time is formatted, **Then** the output is `HH:mm` for the active locale.
2. **Given** a fixed `LocalDateTime` and `use24h = false`, **When** the time is formatted, **Then** the output is 12-hour without a locale-inappropriate suffix (decide and document: `h:mm`, no AM/PM, matching today's `HomeScreen.kt:61`).
3. **Given** a locale (Greek, English), **When** the date is formatted, **Then** the pattern and capitalisation are locale-correct and unit-tested.
4. **Given** the formatters, **When** the user toggles 24h or the locale changes, **Then** new formatters are created — and when neither changes, **Then** formatters are **not** re-created on every recomposition.
5. **Given** the clock tick, **When** the device time or timezone changes manually, **Then** the displayed time is correct within one minute.
6. **Given** the app is backgrounded, **When** it is foregrounded, **Then** the clock shows the correct current time immediately (not after the remaining tick interval).

**Technical Implementation Notes**
- `HomeScreen.kt:61-63` — `DateTimeFormatter.ofPattern(...)` is currently allocated on every recomposition and uses a hardcoded pattern with `Locale.getDefault()`; extract to a pure `TimeFormatter` object (or a small class with injected `Clock`/locale) and `remember(use24h, locale)` the compiled formatters.
- `HomeScreen.kt:41-49` — `produceState` minute loop is sound; consider recomputing the delay after a time change and exposing the tick as a flow so it can be tested with a virtual clock.
- Use `Clock`/`ZoneId` injection (java.time is available at minSdk 26) so tests are deterministic.
- Tests: `TimeFormatterTest` covering both modes, Greek/English, DST boundary and a leap-day date.

---

### ARCH-5 — Decide and document the module/package boundaries
**Priority:** Low · **Epic:** ARCH · **Findings:** S2, S11, R1 · **Estimate:** S

**As a** developer, **I want** the current architectural decision recorded, **so that** future contributors don't either (a) over-engineer a small launcher or (b) erode the boundaries we do want.

**Acceptance Criteria**
1. **Given** the repository, **When** a new contributor reads the docs, **Then** the intended layering (`ui` → gateway → `data` interfaces → Android impls; `domain` pure) is stated with a rationale for *not* adopting full Clean Architecture or a DI framework at this size.
2. **Given** the dependency rule, **When** a change introduces a compile-time dependency in the wrong direction (e.g. `domain` importing `android.*`), **Then** the document states it is a violation and how to fix it.
3. **Given** the theme layer, **When** the `LauncherPalette` ↔ Material `ColorScheme` duplication is reviewed, **Then** one source of truth is chosen and documented (candidates: drive Material from the palette everywhere, or drop custom accessors in favour of `MaterialTheme.colorScheme`).
4. **Given** `LocalPalette` being a `staticCompositionLocalOf`, **When** the tradeoff is documented, **Then** the reason and the accepted cost (whole-composition invalidation on theme change) are recorded.
5. **Given** the README, **When** it is read, **Then** the project layout section matches the actual tree.

**Technical Implementation Notes**
- Files: extend `README.md` (which already has a "Project layout" and "Customising" section) and/or add `docs/ARCHITECTURE.md`; reference `CODE_AUDIT.md` §R1.
- Document the theme decision concretely: `Theme.kt:17-41` maps the palette onto a Material scheme while screens use the raw palette accessors (`Color.kt:146-157`), so Material components and custom components can disagree.
- Keep it short — a page, not a manifesto. The value is the rationale (why *not* full Clean Architecture), which prevents both failure modes in criterion 1.

---

### ARCH-6 — Observable failure seam instead of silent `runCatching`
**Priority:** Low · **Epic:** ARCH · **Findings:** F1, F2, F13, R2 · **Estimate:** S

**As a** developer, **I want** every handled failure to leave a record, **so that** bugs reported as "nothing happened" can be diagnosed without a reproduction.

**Acceptance Criteria**
1. **Given** a failure is caught anywhere in the app, **When** it is handled, **Then** it is reported through one shared seam with a context tag (what was being attempted) and the throwable.
2. **Given** the seam, **When** the app runs in release, **Then** reports are still produced (destined for a crash/analytics backend later) without depending on that backend existing today.
3. **Given** `viewModelScope`, **When** an uncaught exception somehow escapes, **Then** a `CoroutineExceptionHandler` records it before the process-level handler runs.
4. **Given** any `runCatching { }` whose result is discarded, **When** the code is reviewed, **Then** it has been replaced — a lint/detekt rule or CI grep prevents regressions.
5. **Given** the seam, **When** a test injects a recording logger, **Then** expected failure paths assert their records (e.g. STAB-1, STAB-5, DRAW-2).

**Technical Implementation Notes**
- Define `interface AppLogger { fun record(tag: String, throwable: Throwable? = null, message: String? = null) }` with a `LogcatAppLogger` implementation; inject it (ARCH-1).
- Call sites to convert: `AppLauncher.kt:18-19, 27, 33, 38-48, 54, 61`; `LauncherViewModel.kt:102` (`runCatching` around `unregisterReceiver`); `DrawerScreen.kt:66`.
- Add `CoroutineExceptionHandler` to the ViewModel scope creation when ARCH-1 lands.
- Keep the seam tiny and dependency-free so it doesn't block the logger decision; wire an analytics backend later without touching call sites.

---

# EPIC-I18N — Resources, Localisation & Tokens

---

### I18N-1 — Extract every user-facing string and ship a Greek translation
**Priority:** High · **Epic:** I18N · **Findings:** S6 · **Estimate:** M

**As a** Greek-speaking launcher user, **I want** the launcher UI in my language, **so that** it reads naturally on a device set to Greek.

**Acceptance Criteria**
1. **Given** the codebase, **When** strings are searched, **Then** no user-visible literal remains in Kotlin — everything is a `stringResource(...)`/`context.getString(...)` from `res/values/strings.xml`.
2. **Given** `res/values-el/strings.xml`, **When** the device locale is Greek, **Then** every screen (home onboarding copy, drawer placeholder, app menu, settings labels, theme names, error messages) renders in Greek.
3. **Given** a string containing the app label (`AppLauncher.kt:19`), **When** it is localised, **Then** a format placeholder (`%1$s`) is used and the translation can reorder it.
4. **Given** any count-dependent string (e.g. "and N more" from STAB-4, or "N hidden apps"), **When** it is localised, **Then** it uses `plurals` with at least `one`/`other` for the relevant languages.
5. **Given** the 38 theme display names, **When** they are localised, **Then** names that are proper nouns (e.g. "Tokyo Night", "Nord", "Dracula") are marked `translatable="false"` and the rest are translatable.
6. **Given** a device locale with no translation, **When** the app renders, **Then** English is used as the fallback without layout breakage.
7. **Given** pseudo-localisation is enabled, **When** the app runs, **Then** no text is clipped or overflowing in the settings list, drawer rows or app menu.

**Technical Implementation Notes**
- Literals to extract: `HomeScreen.kt:99, 124`; `Common.kt:68, 77`; `DrawerScreen.kt:170-173`; `SettingsScreen.kt:73, 81, 92-94, 100, 113, 128`; `AppLauncher.kt:19`; theme labels at `Color.kt:92-131`.
- Also fix accessibility labels in the same pass (A11Y-1) so `contentDescription` values are localised from day one.
- Use `stringResource` in composables (it is recomposition-safe) rather than caching `context.getString`.
- Add an `android:localeConfig` if per-app language selection is wanted later; not required by this story.
- Consider a lint check ("HardcodedText" is built-in and will now be enforceable — see PLAT-4/QA-4).

---

### I18N-2 — Extract design tokens; stop hardcoding dimensions per composable
**Priority:** Medium · **Epic:** I18N · **Findings:** S6, S11 · **Estimate:** M

**As a** developer, **I want** spacing, sizing and typography expressed as named tokens, **so that** a design change is one edit instead of a 6-file search.

**Acceptance Criteria**
1. **Given** the UI code, **When** dimensions are searched, **Then** repeated magic numbers (`66.sp`, `70.sp`, `30.dp`, `28.dp`, `22.dp`, `54.dp`, `98.dp`, `11.dp`, `250.dp`, `16.dp`) are replaced by named tokens (an `object Spacing`/`Sizes` or `MaterialTheme` extensions).
2. **Given** the typography scale, **When** a text element is styled, **Then** it uses a named style from `AppTypography`/`MaterialTheme.typography` (or an explicit local text style) rather than open-coded `fontSize`/`lineHeight`/`fontWeight` triples.
3. **Given** a token value changes, **When** the app is rebuilt, **Then** all screens that use it update consistently.
4. **Given** the palette factory `p(...)` (`Color.kt:48-51`), **When** the 36 call sites invoke it, **Then** all arguments are named, so transposing two `Long` values is impossible without editing the parameter name.
5. **Given** `LocalPalette`, **When** the theme changes, **Then** only colour readers recompose (either switch to `compositionLocalOf` or document the accepted cost in ARCH-5).
6. **Given** the app renders in each of the 38 themes, **When** the UI is inspected, **Then** no screen uses a hardcoded colour that ignores the active palette.

**Technical Implementation Notes**
- Introduce `ui/theme/Dimens.kt` (or `LocalSpacing` like `LocalPalette`) with the ~12 values actually in use; do not invent a scale the design doesn't have.
- `Color.kt:96-131` — convert `p(0x…, 0x…, …)` to named arguments; the call sites are already one-per-line so this is mechanical and reviewable.
- `Type.kt:19-35` already builds a full `AppTypography` — screens largely bypass it by setting `fontFamily`/`fontSize` directly. Consolidate.
- `HomeScreen.kt:87` — the `replaceFirstChar { it.titlecase(...) }` date capitalisation interacts with locale; keep it but move it into the testable formatter (ARCH-4).
- Verify with a screenshot test across a dark and a light theme.

---

### I18N-3 — Resolve clock and calendar intents by capability, not package name
**Priority:** Low · **Epic:** I18N · **Findings:** S7, F13 · **Estimate:** S

**As a** launcher user on a device without Google Calendar, **I want** the clock and calendar shortcuts to open whatever app handles them, **so that** tapping the time or date is never a dead end.

**Acceptance Criteria**
1. **Given** a device with a non-Google calendar app, **When** the user taps the date, **Then** that calendar opens.
2. **Given** a device with no calendar app at all, **When** the user taps the date, **Then** a visible message is shown (STAB-5) instead of silence.
3. **Given** a device with several clock apps, **When** the user taps the time, **Then** a resolvable alarms intent is used, or the user is offered a chooser.
4. **Given** a device with a work profile, **When** the calendar is opened, **Then** the intent resolves across profiles (or fails visibly).
5. **Given** the codebase, **When** intents are searched, **Then** no third-party package name is hardcoded.

**Technical Implementation Notes**
- `AppLauncher.kt:57-62` — replace `getLaunchIntentForPackage("com.google.android.calendar")` and the `content://com.android.calendar/time` fallback with `Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)` (and/or `CalendarContract.CONTENT_URI` with `ACTION_VIEW`) resolved via `PackageManager.resolveActivity` before dispatch.
- `AppLauncher.kt:51-55` — `AlarmClock.ACTION_SHOW_ALARMS` is correct in principle; resolve it first and fall back to `ACTION_SET_ALARM`/a chooser, then to the visible-message path.
- Remember that package visibility applies: an app is only resolvable if it matches the declared `<queries>` (`AndroidManifest.xml:32-37`). `CATEGORY_APP_CALENDAR` does **not** match a MAIN/LAUNCHER-only declaration, so either extend `<queries>` for the calendar category or resolve through the existing launcher-activity list (preferable — the app's own list already contains the calendar entry).
- Fold into the gateway contract (ARCH-3) so the fallback chain is unit-testable.

---

# EPIC-PERF — Performance, Startup, Build & Release

---

### PERF-1 — Commit the Gradle wrapper so the project builds from the CLI
**Priority:** High · **Epic:** PERF · **Findings:** S10, R3, R4 · **Estimate:** S

**As a** developer or CI system, **I want** `./gradlew` to work from a clean checkout, **so that** builds are reproducible and automatable.

**Acceptance Criteria**
1. **Given** a clean clone, **When** `./gradlew tasks` is run, **Then** it succeeds using the pinned Gradle 8.9 distribution without a pre-installed Gradle.
2. **Given** `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar`, **When** `git ls-files` is inspected, **Then** all three are tracked.
3. **Given** `gradle-wrapper.properties`, **When** it is inspected, **Then** `distributionSha256Sum` is present so the distribution is verified.
4. **Given** a clean clone with network access, **When** `./gradlew assembleDebug` is run, **Then** it produces a debug APK.
5. **Given** CI, **When** it runs the same command, **Then** no manual Gradle installation or Android Studio is required.
6. **Given** `.gitignore` files, **When** they are reviewed, **Then** they no longer imply the wrapper is optional (the `app/.gitignore` comment already asserts it should be kept).

**Technical Implementation Notes**
- The distribution and dependency cache already exist locally: `~/.gradle/wrapper/dists/gradle-8.9-bin/…/gradle-8.9/bin/gradle` and 98 cached module groups, so the wrapper can be generated offline:
  `~/.gradle/wrapper/dists/gradle-8.9-bin/*/gradle-8.9/bin/gradle wrapper --gradle-version 8.9 --distribution-type bin`
- `gradle-wrapper.properties` pins `gradle-8.9-bin.zip` (`gradle/wrapper/gradle-wrapper.properties:3`); keep the version pinned — do not float to `latest`.
- Add `distributionSha256Sum` to the properties file.
- Verify from a *clean* clone (not just the working tree) to prove the wrapper jar is actually tracked.
- **This is the prerequisite for every other story's Definition of Done and for QA-4.**

---

### PERF-2 — Enable R8 minification and resource shrinking for release
**Priority:** High · **Epic:** PERF · **Findings:** S9, R3 · **Estimate:** M

**As a** launcher user, **I want** the optimised release build, **so that** the launcher starts faster and uses less storage.

**Acceptance Criteria**
1. **Given** the release build type, **When** it is built, **Then** `isMinifyEnabled = true` and `isShrinkResources = true` are applied, and the build produces a signed-or-testable artifact.
2. **Given** the release APK, **When** it is installed, **Then** Home renders correctly, all 38 themes apply, favorites/hidden/theme persist across restarts, and app enumeration still finds every launchable app (Reflection/keep-rule regressions checked explicitly).
3. **Given** the release APK, **When** its size is compared to the unminified build, **Then** the reduction is recorded in the PR.
4. **Given** a keep rule is needed (e.g. for a serialised or reflectively-loaded type), **When** it is added, **Then** `proguard-rules.pro` documents *why*.
5. **Given** Compose + DataStore (no reflection-heavy deps), **When** the app is exercised end-to-end on release, **Then** no `ClassNotFoundException`/`NoSuchMethodError` occurs — verified by a smoke test rather than assumed.
6. **Given** the app is not yet published, **When** the signing config is added, **Then** it reads from `keystore.properties` (already gitignored) and the build fails clearly if it's missing, rather than silently producing an unsigned artifact.

**Technical Implementation Notes**
- `app/build.gradle.kts:19-27` — currently `isMinifyEnabled = false` with an empty `proguard-rules.pro`.
- Add a `release` signing config sourced from `keystore.properties` (see `app/.gitignore`, which already anticipates it).
- Replace deprecated `getDefaultProguardFile(...)` usage pattern only if needed; it is current for AGP 8.7.
- Do **not** enable full mode/aggressive obfuscation without a smoke test; enumerate the risk surfaces explicitly (DataStore serialisation, Compose, `PackageManager` interactions).
- Consider `androidResources { }`/`packaging` options only if measurements justify them.
- Depends on PERF-1 for the build command; feeds PERF-3.

---

### PERF-3 — Protect launcher cold start with Baseline Profiles
**Priority:** Medium · **Epic:** PERF · **Findings:** S9, R3 · **Estimate:** M

**As a** launcher user, **I want** the launcher to paint promptly after I press Home, **so that** there's no perceptible delay before I can open an app.

**Acceptance Criteria**
1. **Given** a release build installed on a device, **When** `adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME` is run, **Then** the `TotalTime` is recorded as the pre-change baseline and after the change.
2. **Given** the Baseline Profile, **When** it is installed, **Then** `ProfileInstaller` is wired so profiles are applied on install/update without manual steps.
3. **Given** a Macrobenchmark `StartupTimingMetric` (cold + warm), **When** it is run on a release build, **Then** the cold-start improvement is measured and recorded — not assumed from the profile being present.
4. **Given** a device that ignores profiles, **When** the app starts, **Then** behaviour is unaffected (no dependency on profiles for correctness).
5. **Given** the app is repackaged with R8 (PERF-2), **When** the profile is regenerated, **Then** it is regenerated as part of that build rather than drifting stale.

**Technical Implementation Notes**
- Add the `androidx.baselineprofile` Gradle plugin and a `:baselineprofile` (macrobenchmark) module, or start with a manually authored `app/src/main/baseline-prof.txt` if the toolchain setup is too heavy for this iteration — record the choice.
- Add `androidx.profileinstaller` (pulled in transitively by the plugin; make it explicit).
- Measure before/after with identical device state and report numbers in the PR — this story's value is the measurement, not the plugin.
- Prerequisite: PERF-2 (profiles are largely wasted on an unminified, unoptimised build).

---

### PERF-4 — Remove the cold-start theme flash and wrong system-bar icons
**Priority:** Medium · **Epic:** PERF · **Findings:** F10, S13 · **Estimate:** M

**As a** launcher user on a light theme, **I want** the launcher to appear in my theme immediately, **so that** I don't see a dark flash and unreadable status-bar icons every time I press Home.

**Acceptance Criteria**
1. **Given** a light theme (e.g. "Paper") is selected, **When** the launcher cold-starts, **Then** no dark launch window is visible and the status-bar icons are dark from the first frame.
2. **Given** a dark theme is selected, **When** the launcher cold-starts, **Then** the launch window matches the dark background and icons are light from the first frame.
3. **Given** the theme is applied, **When** the first frame renders, **Then** `enableEdgeToEdge` has been configured exactly once with the correct `SystemBarStyle`.
4. **Given** a very slow read of the persisted theme, **When** the splash/launch window is shown, **Then** it holds the correct background until the theme is known (no intermediate flash of the wrong colour).
5. **Given** the device changes UI mode or the user switches themes in-app, **When** the theme changes, **Then** system-bar icons remain readable.
6. **Given** the codebase, **When** `enableEdgeToEdge` is searched, **Then** it appears in exactly one place.

**Technical Implementation Notes**
- `themes.xml:3,6` hardcodes the warm-dark background and `windowLightStatusBar=false`; `colors.xml` defines only two colours. Either:
  1. make the launch window theme-neutral and resolve the real theme synchronously (mirror the theme key into a small synchronous store for launch, e.g. a dedicated `SharedPreferences`/file read in `onCreate` before `super.onCreate` + `setTheme(...)`), or
  2. add `androidx.core:core-splashscreen` and hold the splash window until the palette resolves.
- `MainActivity.kt:39, 45-53` — consolidate to a single `enableEdgeToEdge` call; the second call (post-composition, keyed on `palette`) should become a targeted `WindowCompat`/controller update rather than a re-application.
- `SystemBarStyle.light(transparent, transparent)` vs `dark(...)` at `MainActivity.kt:47-51` is already the correct mechanism — the problem is *when* it runs.
- Provide a `values-night`/themed launch background only if the synchronous-read approach isn't taken.
- Verify with a cold-start screen recording on both a light and a dark theme.

---

### PERF-5 — Reduce recomposition and keep state collection lifecycle-aware
**Priority:** Medium · **Epic:** PERF · **Findings:** S4, S11 · **Estimate:** M

**As a** launcher user, **I want** scrolling, typing and theme switching to stay smooth, **so that** the launcher feels instant rather than janky.

**Acceptance Criteria**
1. **Given** the app collects UI state, **When** the composables read it, **Then** collection is lifecycle-aware (`collectAsStateWithLifecycle`) so no work happens while the launcher is stopped.
2. **Given** the theme changes, **When** the palette updates, **Then** only the components that read the changed colours recompose (verify with Compose recomposition counts or a layout inspector trace).
3. **Given** the settings list of ~200 apps, **When** the user toggles a favorite, **Then** the affected row recomposes and the list does not recompose wholesale (use stable keys, `@Immutable` state, and lambda-stable callbacks).
4. **Given** the `VerticalPager`, **When** the user drags between pages, **Then** the drawer's `active` flag does not cause the full drawer to recompose on every pixel of movement (use the settled page rather than `currentPage`).
5. **Given** the drawer's `LazyColumn`, **When** it is scrolled, **Then** items are keyed correctly and no `key`-collision warning appears in logs.
6. **Given** the launcher is backgrounded, **When** it is stopped, **Then** no continuous ticker or flow collection keeps running needlessly.

**Technical Implementation Notes**
- Add `androidx.lifecycle:lifecycle-runtime-compose` to the version catalog (`gradle/libs.versions.toml`) for `collectAsStateWithLifecycle`; replace `Common.kt:35`'s wrapper.
- `MainActivity.kt:107` — `active = pagerState.currentPage == 1` recomposes on scroll; use `pagerState.settledPage` (or a `snapshotFlow`-driven value) so page content changes only when settled.
- `HomeScreen.kt:41-49` — the `produceState` minute loop keeps a coroutine alive; it is cancelled on dispose (correct), but confirm it does not tick while stopped once lifecycle-aware collection lands.
- `SettingsScreen.kt:102-136` — pass stable lambdas (`remember`-wrapped) to the row composables so `LazyColumn` items can skip.
- Measure with the Layout Inspector recomposition counters and record before/after counts.

---

### PERF-6 — Adopt a publishable applicationId and a version scheme
**Priority:** Low · **Epic:** PERF · **Findings:** S9 · **Estimate:** S

**As a** product owner, **I want** a real application ID and versioning, **so that** the app can be distributed, updated and migrated off the current identity.

**Acceptance Criteria**
1. **Given** the build config, **When** `applicationId` is inspected, **Then** it is a controlled, reverse-DNS identifier owned by the project (not `com.example.*`).
2. **Given** an existing install of `com.example.minimallauncher`, **When** the decision is made, **Then** the migration story is explicit: either a fresh install is acceptable (documented), or a documented data-migration path exists for favorites/hidden/theme.
3. **Given** `versionCode`/`versionName`, **When** a release is prepared, **Then** both are set deliberately (not left at `1`/`"1.0"`) and CI or the release process enforces incrementing `versionCode`.
4. **Given** `namespace` and `applicationId`, **When** they are inspected, **Then** their relationship is intentional (the Kotlin namespace may stay stable even if the ID changes).
5. **Given** the README, **When** it is read, **Then** the build/install instructions reference the correct ID and there is no stale `com.example` reference.

**Technical Implementation Notes**
- `app/build.gradle.kts:8, 12, 15-16` — `namespace = "com.example.minimallauncher"`, `applicationId = "com.example.minimallauncher"`, `versionCode = 1`, `versionName = "1.0"`.
- Note that `com.example.*` is not publishable to Google Play, which also blocks App Bundle distribution and any Play-based update path — relevant because "set as default launcher" is the app's onboarding funnel.
- Data migration consideration: the launcher's settings live in the app's own DataStore, so an ID change is a fresh install; decide and document rather than discovering it at release time.
- Consider `buildConfigField`/`resValue` for version display if the settings screen ever shows it.

---

# EPIC-PLAT — Platform Modernisation

---

### PLAT-1 — Enumerate launchable apps with `LauncherApps` (PackageManager as fallback)
**Priority:** High · **Epic:** PLAT · **Findings:** S7, F5, DATA-3 · **Estimate:** L

**As a** launcher user with a work profile or a second user, **I want** all my launchable apps to appear, **so that** the launcher is actually complete.

**Acceptance Criteria**
1. **Given** a device with an active work profile, **When** the launcher enumerates apps, **Then** work-profile apps appear with a distinguishable indication (documented decision: a profile badge or a section) and can be launched.
2. **Given** a device with no work profile, **When** the launcher enumerates apps, **Then** behaviour is identical to today (no regression in counts, ordering or labels).
3. **Given** `LauncherApps` is unavailable or returns no activities (device policy, unusual OEM), **When** enumeration runs, **Then** the launcher falls back to the current `PackageManager` path and this is recorded (ARCH-6) rather than silently returning nothing.
4. **Given** app install/update/remove events — including in other profiles — **When** they occur, **Then** the list updates without a manual refresh (consider `LauncherApps.Callback` in place of the four raw broadcasts).
5. **Given** the deprecated flags overload, **When** the `PackageManager` fallback resolves activities, **Then** it uses `ResolveInfoFlags.of(...)` on API 33+ with an API-26-compatible branch, and no deprecated API warning remains.
6. **Given** enumeration results, **When** they are mapped, **Then** only enabled, resolvable, launchable activities for the current profile group are included, and the launcher excludes itself.
7. **Given** a suspended or disabled app, **When** the list renders, **Then** the documented behaviour is applied consistently (`enabled`/`isPackageSuspended` checked explicitly rather than implied).

**Technical Implementation Notes**
- `AppRepository.kt:11-31` — this is the file to extend; keep `AppInfo` as the output type so the UI is unaffected (plus an optional `profile` field for criterion 1).
- `LauncherApps.getActivityList(null, user)` + `UserManager.getUserProfiles()`/`userManager.myUserProfile` for profiles; each `LauncherActivityInfo` provides label, component, and `getUser()`.
- Package visibility: the `<queries>` declaration at `AndroidManifest.xml:32-37` already matches MAIN/LAUNCHER, which keeps the fallback working; `LauncherApps` itself is the sanctioned launcher API and does not require `QUERY_ALL_PACKAGES`.
- Profile-awareness interacts with DATA-1 (identity per package **per profile**) and DATA-3 (pruning must consider all profiles) — coordinate those stories, and make the identity key include the profile where necessary.
- Interface it behind ARCH-1's `AppRepository` so the fallback logic is unit-testable with a fake `LauncherApps`.
- Test manually on an AVD with a work profile (or a second user) before closing: assert counts, labels and successful launches from the work profile.

---

### PLAT-2 — Adopt predictive back and define back semantics for a HOME app
**Priority:** Medium · **Epic:** PLAT · **Findings:** S7, F13, F17 · **Estimate:** S

**As a** launcher user on Android 13+, **I want** the back gesture to behave predictably and preview correctly, **so that** navigating the launcher feels consistent with the rest of the system.

**Acceptance Criteria**
1. **Given** the app targets API 35+ and declares predictive-back support, **When** the user begins a back gesture on the drawer page, **Then** the system back animation is previewed (not a hard cut).
2. **Given** the user is on page 1 (drawer), **When** Back completes, **Then** the launcher returns to page 0.
3. **Given** the user is on page 0 (home), **When** Back is pressed, **Then** the documented behaviour is applied consistently — decide and implement one of: Back does nothing (do **not** finish the HOME activity, which would leave the user without a launcher), or it opens a defined target. This must not be left to the platform default.
4. **Given** Settings is open, **When** Back is pressed, **Then** Settings closes and the user returns to the previous page and query state.
5. **Given** an app menu dialog is open, **When** Back is pressed, **Then** the dialog dismisses and back does not propagate to page navigation in the same gesture.
6. **Given** the launcher runs on Android 16 (API 36), **When** the back gesture is used, **Then** it still works — the app must not depend on the removed legacy back path.
7. **Given** all of the above, **When** the user navigates with gesture navigation and with 3-button navigation, **Then** behaviour is identical.

**Technical Implementation Notes**
- `AndroidManifest.xml:14-21` — add `android:enableOnBackInvokedCallback="true"`; `MainActivity.kt:91-94` already uses `BackHandler`, which participates in the platform dispatcher once opted in.
- `MainActivity.kt:91-94` — the two `BackHandler`s are ordered by `enabled`; verify the dialog case (`DrawerScreen.kt:129-139`) is reached first, since a `Dialog` window owns back for its own window and may not route through the Activity handler.
- Criterion 3 is the important decision: a HOME activity that finishes itself leaves a blank screen, so "no-op on page 0" is almost always correct — document it.
- Verify behaviour changes explicitly against the [Android 16 predictive-back behaviour change](https://developer.android.com/about/versions/16/behavior-changes-16) before closing.
- Test with both navigation modes on API 33 and API 35 emulators.

---

### PLAT-3 — Survive process death and document the config-change contract
**Priority:** Medium · **Epic:** PLAT · **Findings:** S8, F17, F7 · **Estimate:** M

**As a** launcher user, **I want** the launcher to come back in a sensible, predictable state after the system kills it, **so that** I never see stale or unexpected UI.

**Acceptance Criteria**
1. **Given** the launcher is killed in the background and restored, **When** it reopens, **Then** it shows page 0 with an empty query and no IME (per DRAW-3) — and, critically, launches nothing (per DRAW-1 criterion 3).
2. **Given** the manifest's `configChanges` list is absorbed, **When** the device rotates, changes UI mode or changes screen size, **Then** the launcher maintains its position and no visual glitch occurs.
3. **Given** a configuration change that is **not** absorbed (e.g. `fontScale`, `density`), **When** it occurs, **Then** the Activity recreates and the documented restore behaviour is correct and tested.
4. **Given** `stateNotNeeded="true"`, **When** the Activity is recreated without saved instance state, **Then** the app still renders correctly and the absence of state is handled deliberately (no `NullPointerException`, no empty-name UI).
5. **Given** the manifest, **When** it is read, **Then** every one of `launchMode`, `stateNotNeeded` and `configChanges` has an inline comment (or a documented rationale) explaining why it is set — so a future contributor does not remove or extend it by accident.
6. **Given** `SavedStateHandle`, **When** the pager page and the query need to survive recreation, **Then** they are stored there (or explicitly declared not to survive), with the choice documented.

**Technical Implementation Notes**
- `AndroidManifest.xml:17-20` — `launchMode="singleTask"`, `stateNotNeeded="true"`, and eight absorbed `configChanges`. Each is currently load-bearing but unexplained.
- `MainActivity.kt:76-77` — `rememberSaveable` for `showSettings` and `rememberPagerState`; with `stateNotNeeded="true"` these are best-effort. Decide the retention contract and enforce it.
- Add `SavedStateHandle` to the ViewModel (works with the ARCH-1 factory) for the query/page if they should survive; keep the auto-launch guard (`lastLaunched`/generation) explicitly **non**-saved so restore can never launch (DRAW-1).
- Verify with "Don't keep activities" enabled and with `adb shell am kill` + restore; also test a `fontScale` change (Settings → Display size/font).
- Test: `ActivityScenario.recreate()` plus a killed-process restore assertion.

---

### PLAT-4 — Clear deprecations, unused imports and enable lint as a gate
**Priority:** Low · **Epic:** PLAT · **Findings:** S13, S7 · **Estimate:** S

**As a** developer, **I want** a warning-free build with lint enforcing the basics, **so that** new code can't quietly reintroduce the issues this backlog just fixed.

**Acceptance Criteria**
1. **Given** `MainActivity.kt`, **When** it is compiled, **Then** the duplicate `remember` import and the unused `ExperimentalFoundationApi` import are gone, and `HomeScreen.kt`'s unused `Accent` import is gone.
2. **Given** `enableEdgeToEdge`, **When** it is searched, **Then** it appears exactly once (see PERF-4).
3. **Given** `themes.xml`, **When** the base theme is reviewed, **Then** it is a deliberate choice for a Compose app (documented), not an inherited API-21 platform theme by default.
4. **Given** `./gradlew lint`, **When** it runs, **Then** it completes with `abortOnError = true` and a reviewed baseline for any pre-existing issue; new `HardcodedText`, `UnusedResources`, `MissingContentDescription` and `ObsoleteSdkInt` findings fail the build.
5. **Given** deprecated API usage, **When** the build runs, **Then** deprecation warnings are either fixed (e.g. `ResolveInfoFlags`, PLAT-1) or explicitly suppressed with a comment that states the API-level constraint.
6. **Given** `lint` results, **When** they are reviewed, **Then** the `NewApi`/`ObsoleteSdkInt` checks confirm every `minSdk 26` branch is correct.

**Technical Implementation Notes**
- `app/build.gradle.kts` — add a `lint { abortOnError = true; warningsAsErrors = false; baseline = file("lint-baseline.xml") }` block; commit the baseline and require it to shrink, not grow.
- `MainActivity.kt:11, 20, 30`; `HomeScreen.kt:29`; `themes.xml:2`.
- Enable `allWarningsAsErrors` for Kotlin **after** the tree is clean, not before (it will otherwise block every other story).
- Consider ktlint/detekt only if the team wants formatting enforcement; lint alone covers the correctness checks listed here.
- This story is the enforcement mechanism for I18N-1 (`HardcodedText`), A11Y-1 (`ContentDescription`) and PLAT-1 (`NewApi`), so it should land alongside them.

---

# EPIC-QA — Quality Gates & Observability

---

### QA-1 — Unit-test `TextNormalizer`
**Priority:** High · **Epic:** QA · **Findings:** S5, R4 · **Estimate:** S

**As a** developer, **I want** the Greek/Latin search normalisation pinned by tests, **so that** the app's signature feature can't silently regress.

**Acceptance Criteria**
1. **Given** the test suite, **When** it runs, **Then** `app/src/test/java/.../TextNormalizerTest.kt` exists and passes with no device or Robolectric.
2. **Given** `normalize`, **When** tested with Greek accented input (`"Αθήνα"`), **Then** the result is `"αθηνα"`.
3. **Given** `normalize`, **When** tested with Latin accents (`"Café"`), **Then** the result is `"cafe"`.
4. **Given** `normalize`, **When** tested with final sigma (`"Οδυσσεύς"`), **Then** all sigmas are folded to `σ` so the two forms match.
5. **Given** `matches` with a blank or whitespace-only query, **When** evaluated, **Then** it returns `true` (the empty query matches everything).
6. **Given** `matches` with mixed case and accents on both sides, **When** evaluated, **Then** matching is symmetric for case and accents.
7. **Given** the Turkish dotless-i edge case, **When** tested, **Then** the documented behaviour is asserted (locale-independent `lowercase()`), so the choice is intentional rather than accidental.
8. **Given** an empty label, **When** `normalize` is called, **Then** it returns an empty string and does not throw.

**Technical Implementation Notes**
- `TextNormalizer.kt:14-28` is already pure and Android-free — no production change should be needed; if a change is needed, that itself is a finding.
- Use parameterised tests (`@ParameterizedTest`/JUnit4 `Parameterized`) for the character cases so new pairs are one line.
- Include a matching test for the "blank query matches all" contract used by `LauncherViewModel.kt:71`.
- Prerequisite: none (pure JVM test), but PERF-1 makes it runnable in CI.

---

### QA-2 — Unit-test `LauncherViewModel` with fake repositories
**Priority:** High · **Epic:** QA · **Findings:** F1, F3, F7, F15, S1, R4 · **Estimate:** M

**As a** developer, **I want** the launcher's state and auto-launch rules covered by fast JVM tests, **so that** the stability and correctness stories are verified rather than hand-checked.

**Acceptance Criteria**
1. **Given** `LauncherViewModelTest`, **When** it runs, **Then** it uses `FakeAppRepository`, `FakeSettingsRepository` and `FakeLauncherGateway` with `StandardTestDispatcher` — no `AndroidViewModel`, no Robolectric.
2. **Given** a failing app-repository load, **When** `refresh` runs with `runTest`, **Then** no exception escapes the test scope and the error state is asserted (STAB-1).
3. **Given** two overlapping refreshes completing out of order, **When** the state settles, **Then** the newer result is the published one (STAB-3).
4. **Given** a package-change burst, **When** the pipeline settles, **Then** the fake repository's load count is bounded (STAB-3).
5. **Given** a single-match query derived from a **user edit**, **When** the debounce elapses, **Then** exactly one launch intent is observed; **Given** the same query arriving from a restored state, **When** the state is re-emitted, **Then** zero launches occur (DRAW-1).
6. **Given** a hidden app that is also a favorite, **When** state is computed, **Then** it is absent from both `favorites` and `drawerApps` (DATA-2).
7. **Given** the app list is still loading, **When** state is computed, **Then** the state is `Loading`, not `Ready(emptyList())` (STAB-7).
8. **Given** a settings read failure, **When** state is computed, **Then** defaults are used and an error is surfaced (STAB-2).
9. **Given** a settings write failure, **When** a favorite toggle is dispatched, **Then** the optimistic value reverts (STAB-2).

**Technical Implementation Notes**
- Depends on ARCH-1 (constructor injection + displacement of `AndroidViewModel`) and benefits from ARCH-2 (typed intents make assertions readable).
- Add to `gradle/libs.versions.toml` and `app/build.gradle.kts`: `junit`, `kotlinx-coroutines-test`, `app.cash.turbine` (optional but makes flow assertions far clearer), `truth`/`kotest` only if already preferred — keep the dependency set minimal.
- Use `runTest` + `advanceTimeBy` for the debounce rather than real delays; ban `Thread.sleep` in tests.
- Fake repositories should be hand-written (small, explicit) rather than a mocking framework at this size.
- This story is the proof mechanism for STAB-1/2/3/7, DATA-2, DRAW-1 and F16; write it before those fixes in each PR if possible (test-first makes the bugs concrete).

---

### QA-3 — Compose UI tests for the critical launcher flows
**Priority:** Medium · **Epic:** QA · **Findings:** F4, F9, R4 · **Estimate:** M

**As a** developer, **I want** instrumented tests for the paths a user actually takes, **so that** layout, focus and gesture regressions are caught automatically.

**Acceptance Criteria**
1. **Given** `app/src/androidTest`, **When** the suite runs, **Then** it covers: cold start → home renders; swipe to drawer → search field focused and IME visible (DRAW-2); type a query → single match auto-launches exactly once (DRAW-1); long-press an app → menu appears with all four actions; settings theme change applies immediately; 24h toggle persists across recreation.
2. **Given** the home screen with 30 favorites (seeded fake/state), **When** it renders on a small device profile, **Then** the clock and every favorite are reachable (STAB-4).
3. **Given** a long press on a favorite row, **When** the gesture completes, **Then** exactly one outcome occurs — Settings opens *or* the app launches, never both (F9's ambiguity is resolved and pinned).
4. **Given** the settings screen, **When** the theme is switched to a light palette, **Then** the UI reflects the new palette (guards the `staticCompositionLocalOf` behaviour).
5. **Given** any test failure, **When** it is reported, **Then** the output includes a screenshot/semantics-tree dump sufficient to diagnose without a local repro.
6. **Given** the tests, **When** they run in CI, **Then** they run on an API 26 and an API 35 emulator (or a documented subset if CI capacity is limited).

**Technical Implementation Notes**
- Add `androidx.compose.ui:ui-test-junit4` (androidTest) and `ui-test-manifest` (debug), plus `androidx.test:runner`/`rules`/`ext:junit`.
- Prefer `createAndroidComposeRule<MainActivity>()` for the end-to-end flows and `createComposeRule()` with direct state injection for the layout/gesture cases (faster, no device state coupling).
- `@get:Rule val composeRule` + `composeRule.onNodeWithText(...)`; add `testTag`s where semantics are ambiguous — and prefer `contentDescription`/semantics (A11Y-1) over test-only tags.
- Use `ComposeTestRule.mainClock.advanceTimeBy` for the 350 ms search debounce rather than real waiting.
- For criterion 3, wrap in a `SemanticsNodeInteraction.performTouchInput { longClick() }` and assert that exactly one of the two outcomes fired.
- Run on two emulator API levels in CI (QA-4).

---

### QA-4 — CI pipeline: build, unit tests and lint on every change
**Priority:** Medium · **Epic:** QA · **Findings:** S10, S13, R2, R4 · **Estimate:** M

**As a** developer, **I want** every push to be built, tested and linted automatically, **so that** regressions are caught before review and the backlog's fixes stay fixed.

**Acceptance Criteria**
1. **Given** a push or pull request, **When** CI runs, **Then** it executes `./gradlew assembleDebug testDebugUnitTest lintDebug` and fails the check on any failure.
2. **Given** a lint violation covered by the baseline policy (PLAT-4), **When** CI runs, **Then** the build fails.
3. **Given** the Gradle dependency cache, **When** CI runs repeatedly, **Then** it is cached so a typical run is a few minutes, not tens of minutes.
4. **Given** a failing instrumented test on the emulator job (QA-3), **When** CI runs, **Then** the job is required for merge (or explicitly marked non-blocking with a documented reason).
5. **Given** a pull request, **When** CI completes, **Then** the unit-test report and lint results are attached/visible on the PR.
6. **Given** a contributor with no local Android environment, **When** they push, **Then** CI is the source of truth for build/test status.
7. **Given** the workflow file, **When** it is reviewed, **Then** it uses a pinned JDK 17 and the wrapper from PERF-1 (no system Gradle).

**Technical Implementation Notes**
- Prerequisite: **PERF-1** — without the wrapper there is nothing for CI to invoke.
- GitHub Actions example shape: `actions/checkout` → `actions/setup-java` (temurin 17) → `gradle/actions/setup-gradle` (handles wrapper caching) → the Gradle commands above; a separate job with `reactivecircus/android-emulator-runner` for QA-3.
- Keep the first iteration to the three Gradle commands; add the emulator job once QA-3 exists.
- Publish `app/build/reports/lint-results-debug.html` and the JUnit XML as artifacts on failure.
- Secrets: the release signing config (PERF-2) is **not** needed for `assembleDebug`; keep the pipeline simple and free of secrets until a release job is actually wanted.

---

# EPIC-A11Y — Accessibility

---

### A11Y-1 — TalkBack can reach Settings and every app action
**Priority:** High · **Epic:** A11Y · **Findings:** S12, F9 · **Estimate:** M

**As a** screen-reader user, **I want** every launcher action to have a discoverable, announced affordance, **so that** I can open settings, hide, favorite and uninstall apps without guessing at gestures.

**Acceptance Criteria**
1. **Given** TalkBack is enabled, **When** the user focuses the home screen, **Then** a custom accessibility action (or an announced, focusable affordance) provides access to Settings — long-press is not the only route.
2. **Given** TalkBack is enabled, **When** the user focuses an app in the drawer, **Then** a custom action announces and opens the app menu (favorite/hide/info/uninstall).
3. **Given** the search field has text, **When** TalkBack focuses the "×" control, **Then** it announces a clear-search label and role, and activating it clears the query.
4. **Given** a favorite row in Settings, **When** TalkBack focuses it, **Then** it announces the app name **and** its favorited state (e.g. `stateDescription = "favorited"`), and activation toggles it.
5. **Given** the "unhide" control, **When** it is focused, **Then** it is announced as a separate, actionable control rather than an unlabelled text node.
6. **Given** the theme picker, **When** a swatch is focused, **Then** it announces the theme name and whether it is selected.
7. **Given** the long-press gesture on the home screen, **When** a long press is performed on a favorite or the clock, **Then** exactly one outcome occurs (the F9 ambiguity is resolved and covered by a UI test).
8. **Given** the whole app, **When** a semantics audit is run, **Then** no interactive element is announced as plain text and no `Clickable` lacks a label.

**Technical Implementation Notes**
- Add `Modifier.semantics { contentDescription = …; role = …; stateDescription = …; customActions = listOf(...) }` at the call sites: `HomeScreen.kt:106-119` (rows), `DrawerScreen.kt:110-124` (rows + menu), `Common.kt:75-86` ("×"), `SettingsScreen.kt:102-136` (favorite/unhide rows), `SettingsScreen.kt:148-187` (swatches).
- Localise every new string via I18N-1 (do them in the same pass).
- For F9, prefer an explicit affordance over an invisible gesture: e.g. make Settings reachable from the drawer or a focusable area, and scope long-press with `combinedClickable` instead of a parent-level `detectTapGestures`. `combinedClickable` documents the interaction in one place and removes the parent/child consumption ambiguity.
- Add `lint`'s `ContentDescription` check (PLAT-4) so this cannot regress.
- Test: `composeRule.onNodeWithContentDescription(...)` assertions and an `AccessibilityChecks`/Espresso a11y scan if the dependency is acceptable.

---

### A11Y-2 — Respect font scale and meet tap-target minimums
**Priority:** Medium · **Epic:** A11Y · **Findings:** S12, S6 · **Estimate:** S

**As a** user with large text or limited dexterity, **I want** the launcher to scale and to have generous tap targets, **so that** I can read and operate it comfortably.

**Acceptance Criteria**
1. **Given** the system font scale is set to 130%, **When** each screen renders, **Then** no text is clipped, no row overlaps and all controls remain reachable.
2. **Given** the system font scale is at maximum, **When** the settings list renders, **Then** rows grow and the list remains scrollable.
3. **Given** the smallest interactive elements ("×" clear control, `★`/`☆` favorite toggle, "unhide", the back chevron `‹`), **When** measured, **Then** each has a touch target of at least 48×48dp (padding the hit area if the glyph must stay small).
4. **Given** a text-only UI with 11–13sp labels, **When** reviewed, **Then** body/secondary text uses at least the minimum recommended size, or the deviation is documented as an intentional aesthetic with a rationale.
5. **Given** large font scale combined with many favorites, **When** the home screen renders, **Then** STAB-4's reachability guarantee still holds.
6. **Given** the drawer rows, **When** font scale increases, **Then** `LazyColumn` row heights adapt and no text is truncated without an ellipsis.

**Technical Implementation Notes**
- Use `Modifier.minimumInteractiveComponentSize()` (Material3) or explicit `sizeIn(minWidth = 48.dp, minHeight = 48.dp)` on the small controls: `Common.kt:75-86`, `SettingsScreen.kt:112-133`, `SettingsScreen.kt:72-78`.
- Sizes currently come from hardcoded `sp`/`dp` literals; the token extraction in I18N-2 makes the audit mechanical.
- `MainActivity.kt` absorbs `uiMode` but **not** `fontScale`, so font-scale changes recreate the Activity — verify restore behaviour (PLAT-3).
- Test at 1.0×, 1.3× and 2.0× font scale with screenshot assertions (QA-3).

---

### A11Y-3 — Themed icon and palette contrast audit
**Priority:** Low · **Epic:** A11Y · **Findings:** S12, S11 · **Estimate:** S

**As a** launcher user, **I want** a launcher icon that matches my theme and text that is always readable, **so that** the app looks intentional and I can read it in any of the 38 themes.

**Acceptance Criteria**
1. **Given** a device on Android 13+ with themed icons enabled, **When** the launcher icon is displayed, **Then** a monochrome variant is used.
2. **Given** each of the 38 palettes, **When** text is rendered on its background, **Then** `text`/`text2`/`text3` meet WCAG AA contrast for their size (or the specific failing palettes are listed and corrected).
3. **Given** the accent colour on each palette's background, **When** used for large text (the clock) or indicators (the `★`, the toggle track), **Then** it meets at least 3:1 contrast.
4. **Given** a failing palette, **When** it is adjusted, **Then** the change is visible in the settings swatch and documented.
5. **Given** the audit results, **When** they are recorded, **Then** a table of palette → contrast ratio is committed so future palette additions can be checked against it.
6. **Given** the two hand-tuned palettes and the 36 derived ones, **When** the audit runs, **Then** both derivation paths are checked (the derived `p(...)` palettes compute `text3`/`accentRing` via `mix`, which can drift).

**Technical Implementation Notes**
- Add `<monochrome android:drawable="@drawable/ic_launcher_monochrome" />` to `mipmap-anydpi-v26/ic_launcher.xml` and provide the drawable.
- Write a small JVM unit test that iterates `AppThemes` (`Color.kt:92-131`) and computes relative luminance contrast ratios using the same maths as `androidx.compose.ui.graphics.luminance`; assert thresholds and fail with the palette name. This is the cheapest way to keep 38 palettes honest and it runs in CI.
- `Color.kt:34-67` — note `c(rgb)` forces alpha to `FF` while `accentRing` uses explicit alpha (`:66,77,85`); the audit must use composited colours, not raw values, everywhere alpha < 1.
- `Theme.kt:15` already derives dark/light from `palette.bg.luminance() < 0.5f`; reuse that boundary rather than re-inventing it.

---

# Traceability matrix

Every audit finding maps to at least one story, and every story has an audit anchor.

| Audit finding | Story IDs |
| --- | --- |
| F1 uncaught exceptions in `viewModelScope` | **STAB-1**, ARCH-6, QA-2 |
| F2 DataStore read failure freezes settings | **STAB-2**, ARCH-2, ARCH-6 |
| F3 concurrent/stale app list | **STAB-3** |
| F4 home screen overflow | **STAB-4**, QA-3 |
| F5 app identity mismatch | **DATA-1**, PLAT-1 |
| F6 hidden app remains on home | **DATA-2**, DATA-3 |
| F7 auto-launch without intent | **DRAW-1**, ARCH-2, PLAT-3 |
| F8 focus timing hack | **DRAW-2**, QA-3 |
| F9 overloaded long-press | **A11Y-1**, QA-3 |
| F10 cold-start theme flash | **PERF-4** |
| F11 backup rules exclude DataStore | **DATA-4** |
| F12 refresh storms | **STAB-3** |
| F13 silent intent failures | **STAB-5**, ARCH-3, I18N-3, PLAT-2 |
| F14 no locale-change refresh | **STAB-6** |
| F15 false empty state | **STAB-7**, ARCH-2, QA-2 |
| F16 publicly mutable query | **ARCH-2**, DRAW-1 |
| F17 stale state on non-Home return | **DRAW-3**, PLAT-3 |
| S1 no dependency injection | **ARCH-1**, QA-2 |
| S2 `Context` in the data layer | **ARCH-1**, ARCH-5 |
| S3 UI side effects via singleton | **ARCH-3** |
| S4 scattered `StateFlow`s | **ARCH-2**, PERF-5 |
| S5 main-thread search work | **DRAW-4**, QA-1 |
| S6 hardcoded strings/dimensions | **I18N-1**, I18N-2, A11Y-2 |
| S7 outdated platform APIs | **PLAT-1**, PLAT-2, PLAT-4, I18N-3 |
| S8 implicit lifecycle contract | **PLAT-3** |
| S9 unoptimised release build | **PERF-2**, PERF-3, PERF-6 |
| S10 missing Gradle wrapper | **PERF-1**, QA-4 |
| S11 theme layer ergonomics | **I18N-2**, ARCH-5, A11Y-3, PERF-5 |
| S12 accessibility gaps | **A11Y-1**, A11Y-2, A11Y-3 |
| S13 dead code / lint drift | **PLAT-4**, QA-4, PERF-4 |
| R1 right-sized architecture | ARCH-1, ARCH-2, ARCH-3, ARCH-4, ARCH-5 |
| R2 failure isolation | STAB-1, STAB-2, STAB-5, STAB-7, ARCH-6 |
| R3 startup performance contract | PERF-1, PERF-2, PERF-3, PERF-4 |
| R4 quality gates before refactors | PERF-1, QA-1, QA-2, QA-3, QA-4 |

**Story → epic → priority summary**

| Epic | Stories | IDs |
| --- | --- | --- |
| STAB — Stability & Crash Prevention | 7 | STAB-1…7 |
| DATA — Data Correctness & Durability | 4 | DATA-1…4 |
| DRAW — Drawer & Search Behaviour | 4 | DRAW-1…4 |
| ARCH — Architecture & Testability | 6 | ARCH-1…6 |
| I18N — Resources, Localisation & Tokens | 3 | I18N-1…3 |
| PERF — Performance, Startup, Build & Release | 6 | PERF-1…6 |
| PLAT — Platform Modernisation | 4 | PLAT-1…4 |
| QA — Quality Gates & Observability | 4 | QA-1…4 |
| A11Y — Accessibility | 3 | A11Y-1…3 |

| Priority | Count | Stories |
| --- | --- | --- |
| **High** | 16 | STAB-1, STAB-2, STAB-3, STAB-4, DATA-1, DATA-2, DRAW-1, DRAW-2, ARCH-1, I18N-1, PERF-1, PERF-2, PLAT-1, QA-1, QA-2, A11Y-1 |
| **Medium** | 18 | STAB-5, STAB-7, DATA-3, DATA-4, DRAW-3, DRAW-4, ARCH-2, ARCH-3, ARCH-4, I18N-2, PERF-3, PERF-4, PERF-5, PLAT-2, PLAT-3, QA-3, QA-4, A11Y-2 |
| **Low** | 7 | STAB-6, ARCH-5, ARCH-6, I18N-3, PERF-6, PLAT-4, A11Y-3 |

---

# Suggested delivery slicing

The ordering is dependency-driven, not priority-sorted: the wrapper and the test harness must exist before anything else can be verified.

### Sprint 0 — Unblock and measure (do first, ~1 sprint)
**PERF-1** (wrapper) → **QA-1** (pure tests) → **ARCH-1** (injection) → **QA-2** (ViewModel tests) → **QA-4** (CI) → **PLAT-4** (lint gate).
*Outcome:* the project builds from the CLI, the tree is warning-free, and every subsequent fix can be proven by a test in CI. Nothing else in this backlog can be verified without this.

### Sprint 1 — Stop the bleeding (High stability)
**STAB-1**, **STAB-2**, **STAB-3**, **STAB-4**, **ARCH-6**.
*Outcome:* no crash path from a platform or storage failure; no stale app list; the home screen can't strand an app. ARCH-6 lands here so the new failure paths leave evidence.

### Sprint 2 — Correctness of state and search (High)
**DATA-1**, **DATA-2**, **DRAW-1**, **DRAW-2**, plus **ARCH-2** as the enabling refactor.
*Outcome:* one entry per app; hiding means hiding; search launches only on deliberate input, once. ARCH-2 is what makes DRAW-1 and STAB-7 testable and removes the mutable-public-state hazard.

### Sprint 3 — Resources and distribution readiness (High/Medium)
**I18N-1** (strings + Greek), **PERF-2** (R8), **PERF-4** (cold-start flash), **DATA-4** (backup), **STAB-5** (visible failures).
*Outcome:* the app is localisable, shippable in release mode, and restorable on a new device.

### Sprint 4 — Platform reach and performance (High/Medium)
**PLAT-1** (`LauncherApps` + work profile), **PERF-3** (Baseline Profiles), **PERF-5** (recompositions), **DRAW-4** (off-main-thread search), **ARCH-3** (gateway), **STAB-7/DATA-3**.
*Outcome:* the launcher is complete for multi-profile users and its startup/scroll cost is measured and protected.

### Sprint 5 — Quality, accessibility and polish (Medium/Low)
**QA-3** (UI tests), **A11Y-1/2**, **PLAT-2**, **PLAT-3**, **I18N-2**, **STAB-6**, **ARCH-4/5**, **PERF-6**, **A11Y-3**, **I18N-3**.
*Outcome:* the launcher is accessible, its lifecycle contract is documented, and no known finding remains open.

### Critical path
`PERF-1 → QA-4 → STAB-1 → ARCH-2 → DRAW-1/STAB-7 → QA-3`
Anything that skips PERF-1 cannot satisfy the Definition of Done, because the build cannot be run.

---

# Out of scope / explicitly deferred

These are net-new features, not defects. They are recorded so they are not silently absorbed into the stories above, and they are **not** part of this backlog:

| Deferred item | Why deferred |
| --- | --- |
| Drag-to-reorder favorites | New feature; the current insertion-order behaviour is documented and correct. |
| Per-app rename / alias | New feature; interacts with DATA-1's identity model — revisit *after* DATA-1 lands. |
| Hidden-app unlock by typing the exact name | New feature; DATA-2 criterion 5 deliberately pins the current (hidden stays hidden) behaviour first. |
| Home gestures (swipe-down for notifications, double-tap to lock) | New feature requiring privileged permissions (`BIND_NOTIFICATION_LISTENER_SERVICE`, `DEVICE_ADMIN`). |
| Widgets / notification badges / search-provider integration | New feature surface with substantial permission and lifecycle cost. |
| Multiple launcher layouts (tabs, folders, dock) | New feature; would change the product's premise. |
| Splitting into multiple Gradle modules | Not justified at ~1,500 LOC; ARCH-5 documents the rationale and the trigger point for revisiting. |
| Analytics/crash-reporting backend integration | ARCH-6 provides the seam; choosing and wiring a vendor is a product decision. |
| Play Store listing, store assets, privacy policy | Process work, tracked outside engineering. |
| Per-app language selection (`android:localeConfig`) | Depends on I18N-1; cheap follow-up once translations exist. |

---

*Derived from [`CODE_AUDIT.md`](./CODE_AUDIT.md) at revision `2bb87a0`. Every `file:line` reference in the implementation notes was taken from that revision; re-verify line numbers if the tree has moved.*
