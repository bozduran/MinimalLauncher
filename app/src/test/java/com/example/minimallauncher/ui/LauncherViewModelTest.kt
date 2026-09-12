package com.example.minimallauncher.ui

import com.example.minimallauncher.testutil.FakeAppChangeSource
import com.example.minimallauncher.testutil.FakeAppRepository
import com.example.minimallauncher.testutil.FakeSettingsRepository
import com.example.minimallauncher.testutil.RecordingAppLogger
import com.example.minimallauncher.testutil.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [LauncherViewModel] against hand-written fakes.
 *
 * Runs on the JVM with no Robolectric and no device: every dependency is injected
 * (see ARCH-1) and the dispatcher is controlled by the test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        appRepo: FakeAppRepository = FakeAppRepository(),
        settingsRepo: FakeSettingsRepository = FakeSettingsRepository(),
        changeSource: FakeAppChangeSource = FakeAppChangeSource(),
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = dispatcher,
        logger: com.example.minimallauncher.data.AppLogger = RecordingAppLogger(),
    ) = LauncherViewModel(appRepo, settingsRepo, changeSource, ioDispatcher, logger)

    // ── loading ─────────────────────────────────────────────────────────────

    @Test
    fun `init loads the app list`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val vm = viewModel(appRepo = repo)

        advanceUntilIdle()

        assertEquals(listOf("Chrome", "Maps"), vm.allApps.value.map { it.label })
        assertEquals(1, repo.loadCount)
    }

    @Test
    fun `app list load is dispatched through the injected io dispatcher`() = runTest(dispatcher) {
        // Main runs eagerly; the io dispatcher is a StandardTestDispatcher, so if
        // the ViewModel honours the injected dispatcher the load cannot have run yet.
        val io = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo, ioDispatcher = io)

        assertEquals("load ran inline on the main thread", 0, repo.loadCount)

        advanceUntilIdle()
        assertEquals(1, repo.loadCount)
        assertEquals(1, vm.allApps.value.size)
    }

    @Test
    fun `app list is empty before the first load completes`() = runTest(dispatcher) {
        val vm = viewModel(appRepo = FakeAppRepository(listOf(app("Chrome"))))
        assertTrue(vm.allApps.value.isEmpty())
    }

    @Test
    fun `package change events reload the app list`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val changes = FakeAppChangeSource()
        val vm = viewModel(appRepo = repo, changeSource = changes)
        advanceUntilIdle()
        assertEquals(1, repo.loadCount)

        repo.apps = listOf(app("Chrome"), app("Maps"))
        changes.emitChange()
        advanceUntilIdle()

        assertEquals(2, repo.loadCount)
        assertEquals(listOf("Chrome", "Maps"), vm.allApps.value.map { it.label })
    }

    @Test
    fun `manual refresh reloads the app list`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        repo.apps = listOf(app("Chrome"), app("Maps"))
        vm.refresh()
        advanceUntilIdle()

        assertEquals(2, vm.allApps.value.size)
    }

    // ── STAB-3: single-flight, conflated reloads ────────────────────────────

    @Test
    fun `the initial load is not delayed by the package-event debounce`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)

        testScheduler.runCurrent()

        assertEquals("first load must start immediately", 1, repo.loadCount)
        assertEquals(listOf("Chrome"), vm.allApps.value.map { it.label })
    }

    @Test
    fun `a burst of package events collapses into a single reload`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val changes = FakeAppChangeSource()
        viewModel(appRepo = repo, changeSource = changes)
        advanceUntilIdle()
        assertEquals(1, repo.loadCount)

        repeat(8) { changes.emitChange() }
        advanceUntilIdle()

        assertEquals("burst must be conflated, not one query per event", 2, repo.loadCount)
    }

    @Test
    fun `rapid manual refreshes collapse into a single reload`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertEquals(1, repo.loadCount)

        repeat(5) { vm.refresh() }
        advanceUntilIdle()

        assertEquals(2, repo.loadCount)
    }

    @Test
    fun `a slow earlier load can never overwrite a faster later one`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Stale")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertEquals(listOf("Stale"), vm.allApps.value.map { it.label })

        // Load A starts after its debounce and blocks on the gate.
        val gateA = repo.gateNextLoad()
        repo.apps = listOf(app("Old"))
        vm.refresh()
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()
        assertEquals(2, repo.loadCount)

        // Load B arrives, supersedes A, snapshots newer data and completes first.
        repo.apps = listOf(app("Fresh"))
        vm.refresh()
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()
        assertEquals(3, repo.loadCount)

        // Releasing the superseded load afterwards must not clobber the new state.
        gateA.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            "the superseded load must not publish over the newer one",
            listOf("Fresh"),
            vm.allApps.value.map { it.label },
        )
    }

    @Test
    fun `a superseded load is not reported as a failure`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Stale")))
        val logger = RecordingAppLogger()
        val vm = viewModel(appRepo = repo, logger = logger)
        advanceUntilIdle()

        val gate = repo.gateNextLoad()
        vm.refresh()
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()

        repo.apps = listOf(app("Fresh"))
        vm.refresh() // supersedes the gated load
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()

        gate.complete(Unit)
        advanceUntilIdle()

        assertNull("cancellation is not a failure", vm.appListError.value)
        assertTrue(logger.records.isEmpty())
        assertEquals(listOf("Fresh"), vm.allApps.value.map { it.label })
    }

    @Test
    fun `a reload while one is in flight replaces it rather than running two`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        val gate = repo.gateNextLoad()
        vm.refresh()
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()
        assertEquals(2, repo.loadCount)

        vm.refresh() // supersedes the in-flight load
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        assertEquals("no second concurrent load while debouncing", 2, repo.loadCount)

        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()
        assertEquals("exactly one replacement load", 3, repo.loadCount)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    // ── STAB-1: load failures must not crash and must be surfaced ───────────

    @Test
    fun `a failing app-list load does not crash the launcher`() = runTest(dispatcher) {
        val repo = FakeAppRepository().apply { failure = RuntimeException("PackageManager failed") }
        val vm = viewModel(appRepo = repo)

        advanceUntilIdle()

        assertNotNull("load failure must be surfaced to the UI", vm.appListError.value)
    }

    @Test
    fun `a failed refresh keeps the last known good app list`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertEquals(1, vm.allApps.value.size)

        repo.failure = RuntimeException("PackageManager failed")
        vm.refresh()
        advanceUntilIdle()

        assertEquals(
            "the previously loaded list must survive a failed refresh",
            listOf("Chrome"),
            vm.allApps.value.map { it.label },
        )
    }

    @Test
    fun `retry after a failure clears the error and loads the new list`() = runTest(dispatcher) {
        val repo = FakeAppRepository().apply { failure = RuntimeException("PackageManager failed") }
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertNotNull(vm.appListError.value)

        repo.failure = null
        repo.apps = listOf(app("Chrome"), app("Maps"))
        vm.retryLoad()
        advanceUntilIdle()

        assertNull(vm.appListError.value)
        assertEquals(listOf("Chrome", "Maps"), vm.allApps.value.map { it.label })
    }

    @Test
    fun `a successful load clears a previously surfaced error`() = runTest(dispatcher) {
        val repo = FakeAppRepository().apply { failure = RuntimeException("boom") }
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertNotNull(vm.appListError.value)

        repo.failure = null
        repo.apps = listOf(app("Chrome"))
        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.appListError.value)
    }

    @Test
    fun `load failures are recorded through the logger seam`() = runTest(dispatcher) {
        val logger = RecordingAppLogger()
        val repo = FakeAppRepository().apply { failure = IllegalStateException("boom") }
        viewModel(appRepo = repo, logger = logger)

        advanceUntilIdle()

        assertEquals(1, logger.records.size)
        assertTrue(logger.records.single().throwable is IllegalStateException)
    }

    @Test
    fun `a package change event that fails to load does not crash`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val changes = FakeAppChangeSource()
        val vm = viewModel(appRepo = repo, changeSource = changes)
        advanceUntilIdle()

        repo.failure = RuntimeException("boom")
        changes.emitChange()
        advanceUntilIdle()

        assertNotNull(vm.appListError.value)
        assertEquals(listOf("Chrome"), vm.allApps.value.map { it.label })
    }

    // ── favorites ───────────────────────────────────────────────────────────

    @Test
    fun `favorites preserve the saved order, not the app-list order`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps"), app("Photos")))
        val settings = FakeSettingsRepository().apply { setFavorites("photos", "chrome") }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(listOf("Photos", "Chrome"), vm.favorites.value.map { it.label })
    }

    @Test
    fun `favorites skip packages that are not installed`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val settings = FakeSettingsRepository().apply { setFavorites("chrome", "GhostApp") }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(listOf("chrome"), vm.favorites.value.map { it.packageName })
    }

    @Test
    fun `favoriteSet is derived from the favorites list`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { setFavorites("a", "b") }
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()

        assertEquals(setOf("a", "b"), vm.favoriteSet.value)
    }

    @Test
    fun `toggleFavorite writes through to the repository`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()

        vm.toggleFavorite("com.example.chrome")
        advanceUntilIdle()

        assertEquals(listOf("com.example.chrome"), settings.currentFavorites)
    }

    // ── hidden ──────────────────────────────────────────────────────────────

    @Test
    fun `drawer excludes hidden packages`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply { setHidden("maps") }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(listOf("Chrome"), vm.drawerApps.value.map { it.label })
    }

    @Test
    fun `hiddenSet reflects the repository`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { setHidden("maps") }
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()

        assertEquals(setOf("maps"), vm.hiddenSet.value)
    }

    @Test
    fun `toggleHidden writes through to the repository`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()

        vm.toggleHidden("maps")
        advanceUntilIdle()

        assertEquals(setOf("maps"), settings.currentHidden)
    }

    // ── search ──────────────────────────────────────────────────────────────

    @Test
    fun `blank query returns every non-hidden app`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply { setHidden("maps") }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)
        advanceUntilIdle()

        vm.setQuery("")
        advanceUntilIdle()

        assertEquals(listOf("Chrome"), vm.drawerApps.value.map { it.label })
    }

    @Test
    fun `query filters the drawer case-insensitively`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        vm.setQuery("CHRO")
        advanceUntilIdle()

        assertEquals(listOf("Chrome"), vm.drawerApps.value.map { it.label })
    }

    @Test
    fun `query filters the drawer accent-insensitively for Greek labels`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Αθήνα"), app("Maps")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        vm.setQuery("αθηνα")
        advanceUntilIdle()

        assertEquals(listOf("Αθήνα"), vm.drawerApps.value.map { it.label })
    }

    @Test
    fun `query that matches nothing yields an empty drawer`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        vm.setQuery("zzz")
        advanceUntilIdle()

        assertTrue(vm.drawerApps.value.isEmpty())
    }

    @Test
    fun `onHomePressed clears the query`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        vm.setQuery("chr")
        advanceUntilIdle()
        assertEquals(1, vm.drawerApps.value.size)

        vm.onHomePressed()
        advanceUntilIdle()

        assertEquals("", vm.query.value)
        assertEquals(2, vm.drawerApps.value.size)
    }

    @Test
    fun `onHomePressed emits a single home signal`() = runTest(dispatcher) {
        val vm = viewModel()
        val received = mutableListOf<Unit>()
        val job = launch { vm.goHome.collect { received += it } }
        advanceUntilIdle()

        vm.onHomePressed()
        advanceUntilIdle()

        assertEquals(1, received.size)
        job.cancel()
    }

    // ── STAB-2: settings storage must degrade, not crash or lie ─────────────

    @Test
    fun `a failed favorite write does not crash the launcher`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { writeFailure = java.io.IOException("disk full") }
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()

        vm.toggleFavorite("com.example.chrome")
        advanceUntilIdle()

        assertNotNull("write failure must be surfaced", vm.settingsError.value)
    }

    @Test
    fun `a failed favorite write leaves the stored value unchanged`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { setFavorites("existing") }
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()

        settings.writeFailure = java.io.IOException("disk full")
        vm.toggleFavorite("com.example.chrome")
        advanceUntilIdle()

        assertEquals(
            "state must still show what was actually persisted",
            listOf("existing"),
            settings.currentFavorites,
        )
        assertEquals(setOf("existing"), vm.favoriteSet.value)
    }

    @Test
    fun `a failed theme write is logged through the seam`() = runTest(dispatcher) {
        val logger = RecordingAppLogger()
        val settings = FakeSettingsRepository().apply { writeFailure = java.io.IOException("disk full") }
        val vm = viewModel(settingsRepo = settings, logger = logger)
        advanceUntilIdle()

        vm.setTheme("dracula")
        advanceUntilIdle()

        assertEquals(1, logger.recordsFor("settings-write").size)
        assertTrue(logger.recordsFor("settings-write").single().throwable is java.io.IOException)
    }

    @Test
    fun `a settings read error is surfaced to the user`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { failReads(java.io.IOException("corrupt")) }
        val vm = viewModel(settingsRepo = settings)

        advanceUntilIdle()

        assertNotNull(vm.settingsError.value)
    }

    @Test
    fun `dismissing the settings error clears the warning`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { failReads(java.io.IOException("corrupt")) }
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()
        assertNotNull(vm.settingsError.value)

        vm.dismissSettingsError()
        advanceUntilIdle()

        assertNull(vm.settingsError.value)
    }

    @Test
    fun `a settings read failure still allows the app list to load`() = runTest(dispatcher) {
        // The launcher's core action (enumerate + open an app) must not depend on
        // settings storage being healthy.
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val settings = FakeSettingsRepository().apply { failReads(java.io.IOException("corrupt")) }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(listOf("Chrome"), vm.drawerApps.value.map { it.label })
    }

    // ── settings pass-through ───────────────────────────────────────────────

    @Test
    fun `use24h defaults to true and follows the repository`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()
        assertTrue(vm.use24h.value)

        vm.setUse24h(false)
        advanceUntilIdle()

        assertEquals(false, vm.use24h.value)
        assertEquals(false, settings.currentUse24h)
    }

    @Test
    fun `themeKey defaults to the default theme and follows the repository`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()
        assertEquals(com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY, vm.themeKey.value)

        vm.setTheme("dracula")
        advanceUntilIdle()

        assertEquals("dracula", vm.themeKey.value)
        assertEquals("dracula", settings.currentTheme)
    }
}
