package com.example.minimallauncher.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.minimallauncher.data.AppChangeSource
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppLogger
import com.example.minimallauncher.data.AppRepository
import com.example.minimallauncher.data.LauncherGateway
import com.example.minimallauncher.data.LogcatAppLogger
import com.example.minimallauncher.data.SettingsRepository
import com.example.minimallauncher.data.TextNormalizer
import com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the launcher's app/favorite/search state.
 *
 * Dependencies arrive through the constructor (see [LauncherViewModelFactory]) so
 * every behaviour here can be exercised in a JVM unit test with fakes and a
 * `TestDispatcher` — no Robolectric, no device.
 */
@OptIn(FlowPreview::class) // Flow.debounce is still a preview API in coroutines 1.7.3
class LauncherViewModel(
    private val appRepo: AppRepository,
    private val settingsRepo: SettingsRepository,
    appChangeSource: AppChangeSource,
    private val gateway: LauncherGateway,
    /**
     * Survives process death where `rememberSaveable` cannot.
     *
     * Defaulted so tests can construct a ViewModel directly; the factory supplies a
     * real handle backed by the Activity's saved state.
     */
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: AppLogger = LogcatAppLogger(),
) : ViewModel() {

    /**
     * A scope with a [CoroutineExceptionHandler].
     *
     * `scope` is backed by a `SupervisorJob`, which contains sibling
     * cancellation but installs no exception handler: an unguarded throw inside a
     * `launch` still reaches the thread's uncaught handler and kills the process.
     * Every suspend entry point in this class handles its own failures, but this
     * handler means a future one that forgets cannot take the device's home screen
     * down with it — it is logged instead.
     */
    private val scope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, error ->
                logger.record(TAG_UNCAUGHT, error, "uncaught coroutine failure")
            },
    )

    private val allAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = allAppsFlow.asStateFlow()

    /**
     * Last app-list load failure, or null when the list is current.
     *
     * Surfaced instead of thrown: an uncaught exception inside `scope`
     * (which only has a `SupervisorJob`, not an exception handler) would kill the
     * process, and this app *is* the device's home screen.
     */
    private val _appListError = MutableStateFlow<Throwable?>(null)
    val appListError: StateFlow<Throwable?> = _appListError.asStateFlow()

    /**
     * True until the first app-list load attempt finishes.
     *
     * Without this the "no favorites yet" onboarding copy was shown during every
     * cold start, because "0 favorites" and "not loaded yet" were the same value.
     * Deliberately only true for the *first* load: a later refresh keeps the
     * previous list on screen rather than flashing an empty state.
     */
    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    /**
     * Non-null while stored settings could not be read (defaults are in use) or a
     * settings write failed. Dismissible by the user.
     */
    private val _settingsError = MutableStateFlow<Throwable?>(null)
    val settingsError: StateFlow<Throwable?> = _settingsError.asStateFlow()

    /** Last failed outgoing action (STAB-5). */
    private val _actionFailure = MutableStateFlow<ActionFailure?>(null)

    /** Why an app-list reload was requested; only [ReloadReason.Initial] skips the debounce. */
    private enum class ReloadReason { Initial, Change, Manual }

    /**
     * Conflated reload channel (`replay = 1` so the initial request survives until
     * the collector starts). A burst of requests keeps only the newest.
     */
    private val reloadRequests = MutableSharedFlow<ReloadReason>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * The search query.
     *
     * Seeded from [savedState] so a search survives process death. Restoring it must
     * never open an app, which is why [userEditGeneration] starts at 0 for a
     * restored value: only a user edit in this session can trigger auto-launch.
     */
    val query = MutableStateFlow(savedState.get<String>(KEY_QUERY) ?: "")

    // Emits when the user presses Home so the UI scrolls back to page 0.
    private val _goHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val goHome = _goHome

    /**
     * Apps the launcher should open on its own: the single result of a search the
     * user is still typing, or the top result of an explicit search submission.
     *
     * The decision lives here rather than in a composable effect because it depends
     * on state that outlives the composition. Previously the guard was a `remember`
     * in the drawer, so anything recreating the composable (process death, a
     * system-initiated recreate, "don't keep activities") left a restored
     * single-match query with no guard and launched an app the user never asked for.
     */
    private var userEditGeneration = 0

    /** Last package auto-launched for the current search, so re-narrowing cannot relaunch it. */
    private var lastAutoLaunchedPackage: String? = null

    private val favoritePkgs = settingsRepo.favorites
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val hiddenPkgs = settingsRepo.hidden
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val use24h: StateFlow<Boolean> = settingsRepo.use24h
        .stateIn(scope, SharingStarted.Eagerly, true)

    val themeKey: StateFlow<String> = settingsRepo.themeKey
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_THEME_KEY)

    val favoriteSet: StateFlow<Set<String>> = favoritePkgs
        .map { it.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val hiddenSet: StateFlow<Set<String>> = hiddenPkgs

    /**
     * Apps shown on the home screen, in the saved favorite order.
     *
     * Hidden apps are excluded: "hide app" in the drawer means the app should not
     * be reachable from the launcher, and leaving it on the home screen made the
     * action mean two different things. The favorite *flag* is deliberately kept
     * (see [favoriteSet]) so the settings screen still shows the app as favorited
     * and unhiding restores its previous position.
     */
    val favorites: StateFlow<List<AppInfo>> =
        combine(allAppsFlow, favoritePkgs, hiddenPkgs) { apps, favs, hidden ->
            val byPkg = apps.associateBy { it.packageName }
            favs.mapNotNull { byPkg[it] }.filter { it.packageName !in hidden }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Apps shown in the drawer: not hidden, filtered by the (Greek-aware) query. */
    val drawerApps: StateFlow<List<AppInfo>> =
        combine(allAppsFlow, hiddenPkgs, query) { apps, hidden, q ->
            apps.asSequence()
                .filter { it.packageName !in hidden }
                // Precomputed keys: only the query is normalised per keystroke.
                .filter { TextNormalizer.matchesKey(it.searchKey, q) }
                .toList()
        }
            // Filtering must not run on the frame-producing thread.
            .flowOn(computationDispatcher)
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        // Single load pipeline. `collectLatest` cancels the in-flight load when a
        // newer request arrives, which is what guarantees a slow earlier load can
        // never publish over a faster later one (the stale-list bug), and makes a
        // burst of package broadcasts collapse into one enumeration.
        scope.launch {
            reloadRequests.collectLatest { reason ->
                // The first load must not wait; everything else is debounced so a
                // Play Store "update all" does not trigger one query per event.
                if (reason != ReloadReason.Initial) delay(PACKAGE_EVENT_DEBOUNCE_MS)
                loadAppsOnce()
            }
        }
        reloadRequests.tryEmit(ReloadReason.Initial)

        scope.launch {
            appChangeSource.changes.collect { reloadRequests.tryEmit(ReloadReason.Change) }
        }

        // Surface a settings store that cannot be read, instead of presenting
        // defaults as if they were the user's configuration.
        scope.launch {
            settingsRepo.readError.collect { error ->
                if (error != null) _settingsError.value = error
            }
        }

        // Auto-launch: when a search the user typed narrows to exactly one app,
        // open it — once. `combine` keeps the query and the matches consistent, and
        // the debounce keeps it from firing mid-word.
        scope.launch {
            combine(query, drawerApps) { q, matches -> q to matches }
                .debounce(SEARCH_DEBOUNCE_MS)
                .collect { (q, matches) -> considerAutoLaunch(q, matches) }
        }
    }

    private fun considerAutoLaunch(q: String, matches: List<AppInfo>) {
        if (q.isBlank()) {
            // A fresh search: allow the next single-match result to launch again.
            lastAutoLaunchedPackage = null
            return
        }
        // No user edit in this session => the query was restored, not typed. Never
        // launch on restore.
        if (userEditGeneration == 0) return

        val match = matches.singleOrNull() ?: return
        // Typing more characters can keep the same single match; do not relaunch it.
        if (match.packageName == lastAutoLaunchedPackage) return

        lastAutoLaunchedPackage = match.packageName
        launchApp(match)
    }

    /**
     * Requests a reload of the app list.
     *
     * Safe to call as often as needed: requests are conflated and supersede any
     * load still in flight.
     */
    fun refresh() {
        reloadRequests.tryEmit(ReloadReason.Manual)
    }

    private suspend fun loadAppsOnce() {
        try {
            val apps = withContext(ioDispatcher) { appRepo.loadApps() }
            allAppsFlow.value = apps
            _appListError.value = null
            _isLoadingApps.value = false
            pruneStaleSettings(apps)
        } catch (cancelled: CancellationException) {
            // Superseded by a newer request — not a failure, and not "loaded":
            // leave the loading flag alone for the replacement load to clear.
            throw cancelled
        } catch (error: Exception) {
            // Keep the last known good list on screen and report the failure.
            logger.record(TAG_APP_LIST, error, "app list load failed")
            _appListError.value = error
            _isLoadingApps.value = false
        }
    }

    /**
     * Forgets favorites/hidden entries for packages that are no longer installed,
     * so reinstalling an app does not silently resurrect old choices.
     *
     * Two safety properties:
     *  - never prunes against an empty enumeration, which would wipe the user's
     *    entire configuration on a transient empty result;
     *  - skips the store entirely when nothing is stale, so the common path does no
     *    write and cannot cause a flicker.
     */
    private suspend fun pruneStaleSettings(apps: List<AppInfo>) {
        if (apps.isEmpty()) return
        val installed = apps.mapTo(mutableSetOf()) { it.packageName }
        val stored = favoritePkgs.value + hiddenPkgs.value
        if (stored.none { it !in installed }) return

        try {
            settingsRepo.pruneMissing(installed)
        } catch (error: Exception) {
            logger.record(TAG_SETTINGS, error, "pruning stale settings failed")
            _settingsError.value = error
        }
    }

    /** Clears the app-list error without retrying. */
    fun dismissAppListError() {
        _appListError.value = null
    }

    /** Retries the app list load after a failure. */
    fun retryLoad() = refresh()

    /** Clears the settings warning. */
    fun dismissSettingsError() {
        _settingsError.value = null
    }

    fun setQuery(q: String) {
        if (q == query.value) return
        userEditGeneration++
        if (q.isBlank()) lastAutoLaunchedPackage = null
        persistQuery(q)
    }

    /**
     * Explicit search submission (keyboard Search/Enter).
     *
     * Opens the top result and records it as launched, so the pending auto-launch
     * for the same app cannot fire a second time. A blank query is a no-op: the
     * "top result" of an empty search is just whatever app happens to be first.
     */
    fun submitSearch() {
        if (query.value.isBlank()) return
        val top = drawerApps.value.firstOrNull() ?: return
        if (top.packageName == lastAutoLaunchedPackage) return
        lastAutoLaunchedPackage = top.packageName
        launchApp(top)
    }

    // ── outgoing actions (ARCH-3 / STAB-5) ──────────────────────────────────

    fun launchApp(app: AppInfo) = dispatch(LauncherAction.OpenApp, app.label) { gateway.launch(app) }

    fun openAppInfo(packageName: String) =
        dispatch(LauncherAction.OpenAppInfo, packageName) { gateway.openAppInfo(packageName) }

    fun uninstall(packageName: String) =
        dispatch(LauncherAction.Uninstall, packageName) { gateway.uninstall(packageName) }

    fun openHomeSettings() =
        dispatch(LauncherAction.OpenHomeSettings) { gateway.openHomeSettings() }

    fun openClock() = dispatch(LauncherAction.OpenClock) { gateway.openClock() }

    fun openCalendar() = dispatch(LauncherAction.OpenCalendar) { gateway.openCalendar() }

    /** The last outgoing action that could not be performed, or null. */
    val actionFailure: StateFlow<ActionFailure?> = _actionFailure.asStateFlow()

    /**
     * Records that the search field could not be focused.
     *
     * The drawer asks for focus after waiting a frame and retries once; if both
     * attempts fail it reports here so the failure is diagnosable instead of being
     * swallowed by a `runCatching`.
     */
    fun onSearchFocusFailed(error: Throwable) {
        logger.record(TAG_UI, error, "search field could not be focused")
    }

    /** Clears the failure once the user has seen it. */
    fun dismissActionFailure() {
        _actionFailure.value = null
    }

    /**
     * Runs a gateway action, recording and surfacing a failure instead of dropping
     * it: a dead intent used to make a menu tap do nothing at all.
     */
    private fun dispatch(
        action: LauncherAction,
        subject: String? = null,
        block: () -> Result<Unit>,
    ) {
        block().onFailure { error ->
            logger.record(TAG_GATEWAY, error, "launcher action failed: ${action.name}")
            _actionFailure.value = ActionFailure(action, error, subject)
        }
    }

    /**
     * Returns the launcher to a predictable state: page 0, no search, no pending
     * auto-launch.
     *
     * Wired to every way the user can arrive back at the launcher — the Home
     * button, Back from a launched app, or the system recreating the activity —
     * because the previous implementation only reset on `onNewIntent` (i.e. the
     * Home button). Returning via Back left the drawer open on a stale query.
     */
    fun onReturnToHome() {
        // Cleared through the same path as an edit so the persisted value does not
        // restore a stale search next time the launcher is recreated.
        persistQuery("")
        lastAutoLaunchedPackage = null
        _goHome.tryEmit(Unit)
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun persistQuery(value: String) {
        query.value = value
        savedState[KEY_QUERY] = value
    }

    fun toggleFavorite(pkg: String) = settingsWrite { settingsRepo.toggleFavorite(pkg) }
    fun toggleHidden(pkg: String) = settingsWrite { settingsRepo.toggleHidden(pkg) }
    fun setUse24h(v: Boolean) = settingsWrite { settingsRepo.setUse24h(v) }
    fun setTheme(key: String) = settingsWrite { settingsRepo.setTheme(key) }

    /**
     * Runs a settings mutation, turning a storage failure into user-visible state
     * instead of an uncaught exception. Because the persisted flows are the single
     * source of truth there is no optimistic copy to roll back: a failed write
     * simply never appears, and the observable state still shows the last value
     * that was actually stored.
     */
    private fun settingsWrite(action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
            } catch (error: Exception) {
                logger.record(TAG_SETTINGS, error, "settings write failed")
                _settingsError.value = error
            }
        }
    }

    private companion object {
        const val KEY_QUERY = "query"

        const val TAG_APP_LIST = "app-list-load"
        const val TAG_SETTINGS = "settings-write"
        const val TAG_GATEWAY = "launcher-gateway"
        const val TAG_UI = "ui"
        const val TAG_UNCAUGHT = "uncaught"

        /** Collapses a burst of package-change broadcasts into one enumeration. */
        const val PACKAGE_EVENT_DEBOUNCE_MS = 250L

        /** How long typing must pause before a single search result auto-launches. */
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
