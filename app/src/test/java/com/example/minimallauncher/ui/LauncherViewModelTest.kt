package com.example.minimallauncher.ui

import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.testutil.FakeAppChangeSource
import com.example.minimallauncher.testutil.FakeAppRepository
import com.example.minimallauncher.testutil.FakeLauncherGateway
import com.example.minimallauncher.testutil.FakeSettingsRepository
import com.example.minimallauncher.testutil.RecordingAppLogger
import com.example.minimallauncher.testutil.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
        gateway: FakeLauncherGateway = FakeLauncherGateway(),
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = dispatcher,
        computationDispatcher: kotlinx.coroutines.CoroutineDispatcher = dispatcher,
        logger: com.example.minimallauncher.data.AppLogger = RecordingAppLogger(),
        savedState: androidx.lifecycle.SavedStateHandle = androidx.lifecycle.SavedStateHandle(),
    ) = LauncherViewModel(
        appRepo,
        settingsRepo,
        changeSource,
        gateway,
        savedState,
        ioDispatcher,
        computationDispatcher,
        logger,
    )

    // ── loading ─────────────────────────────────────────────────────────────

    @Test
    fun `init loads the app list`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val vm = viewModel(appRepo = repo)

        advanceUntilIdle()

        assertEquals(listOf("Chrome", "Maps"), vm.uiState.value.apps.map { it.label })
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
        assertEquals(1, vm.uiState.value.apps.size)
    }

    @Test
    fun `app list is empty before the first load completes`() = runTest(dispatcher) {
        val vm = viewModel(appRepo = FakeAppRepository(listOf(app("Chrome"))))
        assertTrue(vm.uiState.value.apps.isEmpty())
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
        assertEquals(listOf("Chrome", "Maps"), vm.uiState.value.apps.map { it.label })
    }

    @Test
    fun `manual refresh reloads the app list`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        repo.apps = listOf(app("Chrome"), app("Maps"))
        vm.refresh()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.apps.size)
    }

    // ── DATA-3: prune settings for uninstalled packages ─────────────────────

    @Test
    fun `a favorite for an uninstalled package is pruned`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val settings = FakeSettingsRepository().apply { setFavorites("chrome", "ghost") }
        viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(listOf("chrome"), settings.currentFavorites)
    }

    @Test
    fun `a hidden entry for an uninstalled package is pruned`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply { setHidden("ghost", "maps") }
        viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(setOf("maps"), settings.currentHidden)
    }

    @Test
    fun `nothing is pruned when every stored package is installed`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply {
            setFavorites("chrome")
            setHidden("maps")
        }
        viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals("the common path must not write to the store", 0, settings.pruneCalls)
        assertEquals(listOf("chrome"), settings.currentFavorites)
        assertEquals(setOf("maps"), settings.currentHidden)
    }

    @Test
    fun `an empty app list never wipes the stored configuration`() = runTest(dispatcher) {
        // A transient empty enumeration must not be mistaken for "everything was
        // uninstalled", which would discard all favorites and hidden entries.
        val repo = FakeAppRepository(emptyList())
        val settings = FakeSettingsRepository().apply {
            setFavorites("chrome", "maps")
            setHidden("photos")
        }
        viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(0, settings.pruneCalls)
        assertEquals(listOf("chrome", "maps"), settings.currentFavorites)
        assertEquals(setOf("photos"), settings.currentHidden)
    }

    @Test
    fun `a failed load does not prune`() = runTest(dispatcher) {
        val repo = FakeAppRepository().apply { failure = RuntimeException("boom") }
        val settings = FakeSettingsRepository().apply { setFavorites("ghost") }
        viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(0, settings.pruneCalls)
        assertEquals(listOf("ghost"), settings.currentFavorites)
    }

    @Test
    fun `a reinstall does not resurrect the old favorite state`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply { setFavorites("chrome", "maps") }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)
        advanceUntilIdle()

        // Maps is uninstalled, then reinstalled.
        repo.apps = listOf(app("Chrome"))
        vm.refresh()
        advanceUntilIdle()
        assertEquals(listOf("chrome"), settings.currentFavorites)

        repo.apps = listOf(app("Chrome"), app("Maps"))
        vm.refresh()
        advanceUntilIdle()

        assertEquals(
            "the old favorite must not come back with the package",
            listOf("Chrome"),
            vm.uiState.value.favorites.map { it.label },
        )
    }

    @Test
    fun `a prune failure is surfaced rather than crashing`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val settings = FakeSettingsRepository().apply {
            setFavorites("ghost")
            writeFailure = java.io.IOException("disk full")
        }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertNotNull(vm.uiState.value.settingsError)
    }

    // ── STAB-5: failed actions are visible, not silent ──────────────────────

    @Test
    fun `a successful action reports no failure`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway()
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()

        vm.launchApp(app("Chrome"))
        vm.openClock()

        assertNull(vm.uiState.value.actionFailure)
    }

    @Test
    fun `a failed launch surfaces the app it could not open`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway().apply { failEverything = true }
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()

        vm.launchApp(app("Chrome", packageName = "com.example.chrome"))
        advanceUntilIdle()

        val failure = vm.uiState.value.actionFailure
        assertNotNull(failure)
        assertEquals(LauncherAction.OpenApp, failure!!.action)
        assertEquals("Chrome", failure.subject)
    }

    @Test
    fun `each failed action is reported with its own action type`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway().apply { failEverything = true }
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()

        val cases = listOf<Pair<() -> Unit, LauncherAction>>(
            ({ vm.openAppInfo("pkg") } to LauncherAction.OpenAppInfo),
            ({ vm.uninstall("pkg") } to LauncherAction.Uninstall),
            ({ vm.openHomeSettings() } to LauncherAction.OpenHomeSettings),
            ({ vm.openClock() } to LauncherAction.OpenClock),
            ({ vm.openCalendar() } to LauncherAction.OpenCalendar),
        )

        for ((trigger, expected) in cases) {
            vm.dismissActionFailure()
            trigger()
            // uiState is derived, so it settles on the next scheduler pass.
            advanceUntilIdle()
            assertEquals(expected, vm.uiState.value.actionFailure?.action)
        }
    }

    @Test
    fun `dismissing a failure clears it`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway().apply { failEverything = true }
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()
        vm.openClock()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.actionFailure)

        vm.dismissActionFailure()
        advanceUntilIdle()

        assertNull(vm.uiState.value.actionFailure)
    }

    @Test
    fun `every action maps to its own message resource`() {
        val ids = LauncherAction.entries.map { action ->
            ActionFailure(action, IllegalStateException()).messageResId()
        }

        assertEquals(
            "each action needs a distinct message or the user cannot tell what failed",
            LauncherAction.entries.size,
            ids.toSet().size,
        )
    }

    @Test
    fun `an auto-launch failure is surfaced too`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = FakeLauncherGateway().apply { failEverything = true }
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        typeAndWait(vm, "chr")
        advanceUntilIdle()

        assertEquals(LauncherAction.OpenApp, vm.uiState.value.actionFailure?.action)
    }

    // ── ARCH-3: all outgoing actions go through the gateway ─────────────────

    @Test
    fun `tapping an app routes through the gateway`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = FakeLauncherGateway()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        vm.launchApp(vm.uiState.value.drawerApps.first { it.packageName == "maps" })

        assertEquals(listOf("maps"), gateway.launchedPackages)
    }

    @Test
    fun `app menu actions route through the gateway`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway()
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()

        vm.openAppInfo("com.example.maps")
        vm.uninstall("com.example.maps")

        assertEquals(
            listOf(
                FakeLauncherGateway.Call.Details("com.example.maps"),
                FakeLauncherGateway.Call.Uninstall("com.example.maps"),
            ),
            gateway.calls,
        )
    }

    @Test
    fun `clock, calendar and home settings route through the gateway`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway()
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()

        vm.openClock()
        vm.openCalendar()
        vm.openHomeSettings()

        assertEquals(
            listOf(
                FakeLauncherGateway.Call.Clock,
                FakeLauncherGateway.Call.Calendar,
                FakeLauncherGateway.Call.HomeSettings,
            ),
            gateway.calls,
        )
    }

    @Test
    fun `a failing launch is recorded through the logger`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway().apply { failEverything = true }
        val logger = RecordingAppLogger()
        val vm = viewModel(gateway = gateway, logger = logger)
        advanceUntilIdle()

        vm.launchApp(app("Chrome"))

        assertEquals(1, logger.recordsFor("launcher-gateway").size)
        assertTrue(
            logger.recordsFor("launcher-gateway").single().throwable
                is android.content.ActivityNotFoundException,
        )
    }

    @Test
    fun `a failing outgoing action does not crash the launcher`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway().apply { failEverything = true }
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()

        vm.openClock()
        vm.openCalendar()
        vm.openHomeSettings()
        vm.openAppInfo("com.example.maps")
        vm.uninstall("com.example.maps")

        assertEquals(5, gateway.calls.size)
    }

    // ── DRAW-4: search filtering off the main thread ────────────────────────

    @Test
    fun `drawer filtering runs on the injected computation dispatcher`() =
        runTest(dispatcher) {
            // Main runs eagerly; if filtering honoured the injected dispatcher the
            // result cannot be ready before the computation dispatcher advances.
            val computation = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
            val vm = viewModel(appRepo = repo, computationDispatcher = computation)
            advanceUntilIdle()

            vm.setQuery("chr")

            assertEquals(
                "filtering must not run inline on the main thread",
                listOf("Chrome", "Maps"),
                vm.uiState.value.drawerApps.map { it.label },
            )

            testScheduler.advanceUntilIdle()
            assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
        }

    @Test
    fun `search matching still folds accents and case after precomputing keys`() =
        runTest(dispatcher) {
            val repo = FakeAppRepository(listOf(app("Αθήνα"), app("Café"), app("Chrome")))
            val vm = viewModel(appRepo = repo)
            advanceUntilIdle()

            vm.setQuery("αθηνα")
            advanceUntilIdle()
            assertEquals(listOf("Αθήνα"), vm.uiState.value.drawerApps.map { it.label })

            vm.setQuery("cafe")
            advanceUntilIdle()
            assertEquals(listOf("Café"), vm.uiState.value.drawerApps.map { it.label })

            vm.setQuery("CHRO")
            advanceUntilIdle()
            assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
        }

    @Test
    fun `an app entry precomputes a normalised search key`() {
        val entry = app("Αθήνα")

        assertEquals("αθηνα", entry.searchKey)
    }

    // ── STAB-7: no false empty state while loading ──────────────────────────

    @Test
    fun `the app list reports loading before the first load completes`() = runTest(dispatcher) {
        val vm = viewModel(appRepo = FakeAppRepository(listOf(app("Chrome"))))

        assertTrue(vm.uiState.value.isLoadingApps)
    }

    @Test
    fun `loading ends after a successful first load`() = runTest(dispatcher) {
        val vm = viewModel(appRepo = FakeAppRepository(listOf(app("Chrome"))))

        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isLoadingApps)
    }

    @Test
    fun `loading ends after a failed first load`() = runTest(dispatcher) {
        val repo = FakeAppRepository().apply { failure = RuntimeException("boom") }
        val vm = viewModel(appRepo = repo)

        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isLoadingApps)
        assertNotNull(vm.uiState.value.appListError)
    }

    @Test
    fun `a later refresh does not flip the screen back into loading`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertEquals(false, vm.uiState.value.isLoadingApps)

        vm.refresh()
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()

        assertEquals("refresh must not flash an empty state", false, vm.uiState.value.isLoadingApps)
        assertEquals(
            "the previously loaded list stays visible across a refresh",
            listOf("Chrome"),
            vm.uiState.value.apps.map { it.label },
        )
    }

    @Test
    fun `a superseded first load keeps the loading state for its replacement`() =
        runTest(dispatcher) {
            val repo = FakeAppRepository(listOf(app("Chrome")))
            val vm = viewModel(appRepo = repo)

            val gate1 = repo.gateNextLoad()
            val gate2 = repo.gateNextLoad()
            testScheduler.runCurrent() // initial load starts, blocked on gate1
            assertTrue(vm.uiState.value.isLoadingApps)

            vm.refresh() // supersedes the first load
            testScheduler.advanceTimeBy(300)
            testScheduler.runCurrent() // replacement starts, blocked on gate2

            assertTrue(
                "cancelling a load must not report the list as loaded",
                vm.uiState.value.isLoadingApps,
            )

            gate1.complete(Unit)
            gate2.complete(Unit)
            advanceUntilIdle()

            assertEquals(false, vm.uiState.value.isLoadingApps)
            assertEquals(listOf("Chrome"), vm.uiState.value.apps.map { it.label })
        }

    // ── ARCH-2: one state, with loading distinguished from empty ────────────

    @Test
    fun `the initial state is Loading, not an empty list`() = runTest(dispatcher) {
        val vm = viewModel(appRepo = FakeAppRepository(listOf(app("Chrome"))))

        assertEquals(AppListState.Loading, vm.uiState.value.appList)
        assertTrue(vm.uiState.value.isLoadingApps)
    }

    @Test
    fun `a successful load moves the app list to Ready`() = runTest(dispatcher) {
        val vm = viewModel(appRepo = FakeAppRepository(listOf(app("Chrome"))))

        advanceUntilIdle()

        assertEquals(AppListState.Ready(listOf(app("Chrome"))), vm.uiState.value.appList)
        assertEquals(false, vm.uiState.value.isLoadingApps)
        assertNull(vm.uiState.value.appListError)
    }

    @Test
    fun `a failed first load reports Error with its cause`() = runTest(dispatcher) {
        val boom = RuntimeException("boom")
        val vm = viewModel(appRepo = FakeAppRepository().apply { failure = boom })

        advanceUntilIdle()

        val state = vm.uiState.value.appList
        assertTrue("expected Error, was $state", state is AppListState.Error)
        assertEquals("boom", (state as AppListState.Error).cause.message)
        // The same failure is reachable through the convenience accessor the UI uses.
        assertEquals("boom", vm.uiState.value.appListError?.message)
        assertEquals(false, vm.uiState.value.isLoadingApps)
    }

    @Test
    fun `a failed refresh keeps the last known good list instead of an error`() =
        runTest(dispatcher) {
            val repo = FakeAppRepository(listOf(app("Chrome")))
            val vm = viewModel(appRepo = repo)
            advanceUntilIdle()

            repo.failure = RuntimeException("boom")
            vm.refresh()
            advanceUntilIdle()

            // Ready is retained so the user keeps their apps; the failure is
            // reported alongside rather than replacing the list.
            assertEquals(AppListState.Ready(listOf(app("Chrome"))), vm.uiState.value.appList)
            assertNotNull(vm.uiState.value.appListError)
            assertNotNull(vm.uiState.value.loadFailure)
        }

    @Test
    fun `one snapshot carries every screen's data`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply {
            setFavorites("maps")
            setHidden("chrome")
            setThemeKey("nord")
            primeUse24h(false)
        }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        // Everything a screen needs comes from a single emission, so no screen can
        // render data derived from two different app lists.
        val state = vm.uiState.value
        assertEquals(listOf("Maps"), state.favorites.map { it.label })
        // chrome is hidden, so the drawer shows only Maps.
        assertEquals(listOf("Maps"), state.drawerApps.map { it.label })
        assertEquals(setOf("maps"), state.favoritePackages)
        assertEquals(setOf("chrome"), state.hiddenPackages)
        assertEquals("nord", state.themeKey)
        assertEquals(false, state.use24h)
    }

    @Test
    fun `every screen's data agrees on the same favorite and hidden sets`() =
        runTest(dispatcher) {
            val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
            val settings = FakeSettingsRepository().apply {
                setFavorites("chrome", "maps")
                setHidden("maps")
            }
            val vm = viewModel(appRepo = repo, settingsRepo = settings)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(listOf("Chrome"), state.favorites.map { it.label })
            assertEquals(listOf("Chrome"), state.drawerApps.map { it.label })
            // The flag is retained while hidden so settings can still show it as favorited.
            assertEquals(setOf("chrome", "maps"), state.favoritePackages)
        }

    // ── ARCH-6: no failure escapes unobserved ───────────────────────────────

    @Test
    fun `an Error escaping a settings write is caught by the scope handler`() =
        runTest(dispatcher) {
            // settingsWrite catches Exception, not Error. Without the
            // CoroutineExceptionHandler the Error would reach the thread's uncaught
            // handler and kill the process — which for a HOME app means no home screen.
            val settings = FakeSettingsRepository().apply {
                writeFailure = StackOverflowError("simulated")
            }
            val logger = RecordingAppLogger()
            val vm = viewModel(settingsRepo = settings, logger = logger)
            advanceUntilIdle()

            vm.toggleFavorite("com.example.chrome")
            advanceUntilIdle()

            assertEquals(1, logger.recordsFor("uncaught").size)
            assertTrue(
                logger.recordsFor("uncaught").single().throwable is StackOverflowError,
            )
        }

    @Test
    fun `the launcher keeps working after an uncaught failure`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply {
            writeFailure = StackOverflowError("simulated")
        }
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo, settingsRepo = settings)
        advanceUntilIdle()

        vm.toggleFavorite("com.example.chrome")
        advanceUntilIdle()

        // The scope is not cancelled by the handler, so state still updates.
        repo.apps = listOf(app("Chrome"), app("Maps"))
        vm.refresh()
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.apps.size)
    }

    // ── PLAT-3: survive process death ───────────────────────────────────────

    @Test
    fun `the search query survives view model recreation`() = runTest(dispatcher) {
        val handle = androidx.lifecycle.SavedStateHandle()
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        viewModel(appRepo = repo, savedState = handle).apply {
            advanceUntilIdle()
            setQuery("chr")
        }

        // A new ViewModel over the same saved state, as after process death.
        val restored = viewModel(appRepo = repo, savedState = handle)
        advanceUntilIdle()

        assertEquals("chr", restored.uiState.value.query)
        assertEquals(listOf("Chrome"), restored.uiState.value.drawerApps.map { it.label })
    }

    @Test
    fun `a query restored from saved state never auto-launches`() = runTest(dispatcher) {
        val handle = androidx.lifecycle.SavedStateHandle()
        handle["query"] = "chr"
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = FakeLauncherGateway()

        val restored = viewModel(appRepo = repo, gateway = gateway, savedState = handle)
        testScheduler.advanceTimeBy(1_000)
        advanceUntilIdle()

        assertEquals("chr", restored.uiState.value.query)
        assertTrue("restoring a search must never open an app", gateway.calls.isEmpty())
    }

    @Test
    fun `an empty saved state restores an empty query without crashing`() = runTest(dispatcher) {
        // stateNotNeeded="true" means the handle may legitimately be empty.
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo, savedState = androidx.lifecycle.SavedStateHandle())

        advanceUntilIdle()

        assertEquals("", vm.uiState.value.query)
        assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
    }

    @Test
    fun `returning home clears the persisted query`() = runTest(dispatcher) {
        val handle = androidx.lifecycle.SavedStateHandle()
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo, savedState = handle)
        advanceUntilIdle()
        vm.setQuery("chr")

        vm.onReturnToHome()
        advanceUntilIdle()

        assertEquals("", handle.get<String>("query"))
        assertEquals("", viewModel(appRepo = repo, savedState = handle).uiState.value.query)
    }

    @Test
    fun `a restored query is not treated as a user edit`() = runTest(dispatcher) {
        // The generation guard is what makes restore safe; this pins that seeding
        // from saved state does not count as an edit.
        val handle = androidx.lifecycle.SavedStateHandle()
        handle["query"] = "chr"
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = FakeLauncherGateway()
        val vm = viewModel(appRepo = repo, gateway = gateway, savedState = handle)
        advanceUntilIdle()

        // A real edit for a different app must still auto-launch.
        vm.setQuery("map")
        testScheduler.advanceTimeBy(400)
        testScheduler.runCurrent()

        assertEquals(listOf("maps"), gateway.launchedPackages)
    }

    // ── DRAW-2: search-focus failures are reported, not swallowed ───────────

    @Test
    fun `a focus failure is recorded through the logger seam`() = runTest(dispatcher) {
        val logger = RecordingAppLogger()
        val vm = viewModel(logger = logger)

        vm.onSearchFocusFailed(IllegalStateException("no focus target"))

        assertEquals(1, logger.recordsFor("ui").size)
        assertTrue(
            logger.recordsFor("ui").single().throwable is IllegalStateException,
        )
    }

    @Test
    fun `reporting a focus failure does not crash or disturb state`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        vm.onSearchFocusFailed(IllegalStateException("no focus target"))
        advanceUntilIdle()

        assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
        assertNull(vm.uiState.value.appListError)
    }

    // ── DRAW-3: consistent return-to-home ───────────────────────────────────

    @Test
    fun `returning to home restores the full drawer after a search`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps"), app("Photos")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        vm.setQuery("chr")
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.drawerApps.size)

        vm.onReturnToHome()
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.query)
        assertEquals(3, vm.uiState.value.drawerApps.size)
    }

    @Test
    fun `returning to home is safe to call repeatedly`() = runTest(dispatcher) {
        // onResume and onNewIntent can both fire for a single Home press.
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        vm.setQuery("chr")
        advanceUntilIdle()

        vm.onReturnToHome()
        vm.onReturnToHome()
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.query)
        assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
    }

    @Test
    fun `returning to home clears the search without disturbing favourites`() =
        runTest(dispatcher) {
            val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
            val settings = FakeSettingsRepository().apply { setFavorites("maps") }
            val vm = viewModel(appRepo = repo, settingsRepo = settings)
            advanceUntilIdle()
            vm.setQuery("chr")
            advanceUntilIdle()

            vm.onReturnToHome()
            advanceUntilIdle()

            assertEquals(listOf("Maps"), vm.uiState.value.favorites.map { it.label })
        }

    // ── DRAW-1: auto-launch only on deliberate input, exactly once ──────────

    /** Records launches through a fake gateway, so tests assert the real action. */
    private fun launchRecorder(): FakeLauncherGateway = FakeLauncherGateway()

    private suspend fun TestScope.typeAndWait(vm: LauncherViewModel, text: String) {
        vm.setQuery(text)
        testScheduler.advanceTimeBy(400) // past the search debounce
        testScheduler.runCurrent()
    }

    @Test
    fun `a query narrowing to one match auto-launches that app once`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = launchRecorder()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        typeAndWait(vm, "chr")

        assertEquals(listOf("chrome"), gateway.launchedPackages)
    }

    @Test
    fun `typing more characters for the same single match does not relaunch`() =
        runTest(dispatcher) {
            val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
            val gateway = launchRecorder()
            val vm = viewModel(appRepo = repo, gateway = gateway)
            advanceUntilIdle()

            typeAndWait(vm, "chr")
            assertEquals(1, gateway.calls.size)

            typeAndWait(vm, "chro")
            typeAndWait(vm, "chrom")

            assertEquals(
                "narrowing further must not reopen the same app",
                listOf("chrome"),
                gateway.launchedPackages,
            )
        }

    @Test
    fun `auto-launch does not fire before the debounce elapses`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = launchRecorder()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        vm.setQuery("chr")
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        assertTrue("must not launch mid-typing", gateway.calls.isEmpty())

        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()
        assertEquals(1, gateway.calls.size)
    }

    @Test
    fun `a restored single-match query never auto-launches`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = launchRecorder()
        // Simulates the query surviving a recreation without a user edit in this
        // session: the handle carries it, the ViewModel is brand new.
        val handle = androidx.lifecycle.SavedStateHandle()
        handle["query"] = "chr"
        val vm = viewModel(appRepo = repo, gateway = gateway, savedState = handle)
        testScheduler.advanceTimeBy(1_000)
        advanceUntilIdle()

        assertEquals("chr", vm.uiState.value.query)
        assertTrue("restore must never open an app", gateway.calls.isEmpty())
    }

    @Test
    fun `clearing the query re-arms auto-launch`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = launchRecorder()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        typeAndWait(vm, "chr")
        typeAndWait(vm, "")
        typeAndWait(vm, "chr")

        assertEquals(2, gateway.calls.size)
    }

    @Test
    fun `pressing Home re-arms auto-launch`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = launchRecorder()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        typeAndWait(vm, "chr")
        vm.onReturnToHome()
        advanceUntilIdle()
        typeAndWait(vm, "chr")

        assertEquals(2, gateway.calls.size)
    }

    @Test
    fun `a query matching several apps does not auto-launch`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Chromecast")))
        val gateway = launchRecorder()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        typeAndWait(vm, "chrom")

        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `a query matching nothing does not auto-launch`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val gateway = launchRecorder()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        typeAndWait(vm, "zzz")

        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `a hidden app is never auto-launched`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply { setHidden("chrome") }
        val gateway = launchRecorder()
        val vm = viewModel(appRepo = repo, settingsRepo = settings, gateway = gateway)
        advanceUntilIdle()

        typeAndWait(vm, "chrome")

        assertTrue("hidden apps must stay hidden from search", gateway.calls.isEmpty())
    }

    @Test
    fun `submitting the search launches the top result once`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = launchRecorder()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        vm.setQuery("map")
        advanceUntilIdle()
        vm.submitSearch()
        typeAndWait(vm, "map")

        assertEquals(
            "submitting must not be followed by an auto-launch of the same app",
            listOf("maps"),
            gateway.launchedPackages,
        )
    }

    @Test
    fun `submitting an empty search does nothing`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val gateway = launchRecorder()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        vm.submitSearch()
        advanceUntilIdle()

        assertTrue(gateway.calls.isEmpty())
    }

    // ── DATA-2: hiding an app removes it from the home screen ───────────────

    @Test
    fun `a hidden favorite is removed from the home screen`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply {
            setFavorites("chrome", "maps")
            setHidden("maps")
        }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(
            "hide must mean hidden everywhere, not just in the drawer",
            listOf("Chrome"),
            vm.uiState.value.favorites.map { it.label },
        )
    }

    @Test
    fun `the favorite flag is retained while an app is hidden`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply {
            setFavorites("chrome", "maps")
            setHidden("maps")
        }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)
        advanceUntilIdle()

        // The settings screen still renders Maps as favorited so unhiding can
        // restore the previous ordering.
        assertEquals(setOf("chrome", "maps"), vm.uiState.value.favoritePackages)
    }

    @Test
    fun `unhiding an app restores it to the home screen in its saved position`() =
        runTest(dispatcher) {
            val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps"), app("Photos")))
            val settings = FakeSettingsRepository().apply {
                setFavorites("photos", "maps", "chrome")
                setHidden("maps")
            }
            val vm = viewModel(appRepo = repo, settingsRepo = settings)
            advanceUntilIdle()
            assertEquals(listOf("Photos", "Chrome"), vm.uiState.value.favorites.map { it.label })

            vm.toggleHidden("maps")
            advanceUntilIdle()

            assertEquals(
                listOf("Photos", "Maps", "Chrome"),
                vm.uiState.value.favorites.map { it.label },
            )
        }

    @Test
    fun `hiding a non-favorite does not affect the home screen`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val settings = FakeSettingsRepository().apply { setFavorites("chrome") }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)
        advanceUntilIdle()

        vm.toggleHidden("maps")
        advanceUntilIdle()

        assertEquals(listOf("Chrome"), vm.uiState.value.favorites.map { it.label })
        assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
    }

    // ── STAB-3: single-flight, conflated reloads ────────────────────────────

    @Test
    fun `the initial load is not delayed by the package-event debounce`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)

        testScheduler.runCurrent()

        assertEquals("first load must start immediately", 1, repo.loadCount)
        assertEquals(listOf("Chrome"), vm.uiState.value.apps.map { it.label })
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
        assertEquals(listOf("Stale"), vm.uiState.value.apps.map { it.label })

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
            vm.uiState.value.apps.map { it.label },
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

        assertNull("cancellation is not a failure", vm.uiState.value.appListError)
        assertTrue(logger.records.isEmpty())
        assertEquals(listOf("Fresh"), vm.uiState.value.apps.map { it.label })
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

        assertNotNull("load failure must be surfaced to the UI", vm.uiState.value.appListError)
    }

    @Test
    fun `a failed refresh keeps the last known good app list`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.apps.size)

        repo.failure = RuntimeException("PackageManager failed")
        vm.refresh()
        advanceUntilIdle()

        assertEquals(
            "the previously loaded list must survive a failed refresh",
            listOf("Chrome"),
            vm.uiState.value.apps.map { it.label },
        )
    }

    @Test
    fun `retry after a failure clears the error and loads the new list`() = runTest(dispatcher) {
        val repo = FakeAppRepository().apply { failure = RuntimeException("PackageManager failed") }
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.appListError)

        repo.failure = null
        repo.apps = listOf(app("Chrome"), app("Maps"))
        vm.retryLoad()
        advanceUntilIdle()

        assertNull(vm.uiState.value.appListError)
        assertEquals(listOf("Chrome", "Maps"), vm.uiState.value.apps.map { it.label })
    }

    @Test
    fun `a successful load clears a previously surfaced error`() = runTest(dispatcher) {
        val repo = FakeAppRepository().apply { failure = RuntimeException("boom") }
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.appListError)

        repo.failure = null
        repo.apps = listOf(app("Chrome"))
        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.uiState.value.appListError)
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

        assertNotNull(vm.uiState.value.appListError)
        assertEquals(listOf("Chrome"), vm.uiState.value.apps.map { it.label })
    }

    // ── favorites ───────────────────────────────────────────────────────────

    @Test
    fun `favorites preserve the saved order, not the app-list order`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps"), app("Photos")))
        val settings = FakeSettingsRepository().apply { setFavorites("photos", "chrome") }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(listOf("Photos", "Chrome"), vm.uiState.value.favorites.map { it.label })
    }

    @Test
    fun `favorites skip packages that are not installed`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val settings = FakeSettingsRepository().apply { setFavorites("chrome", "GhostApp") }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(listOf("chrome"), vm.uiState.value.favorites.map { it.packageName })
    }

    @Test
    fun `favoriteSet is derived from the favorites list`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { setFavorites("a", "b") }
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()

        assertEquals(setOf("a", "b"), vm.uiState.value.favoritePackages)
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

        assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
    }

    @Test
    fun `hiddenSet reflects the repository`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { setHidden("maps") }
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()

        assertEquals(setOf("maps"), vm.uiState.value.hiddenPackages)
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

        assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
    }

    @Test
    fun `query filters the drawer case-insensitively`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        vm.setQuery("CHRO")
        advanceUntilIdle()

        assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
    }

    @Test
    fun `query filters the drawer accent-insensitively for Greek labels`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Αθήνα"), app("Maps")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        vm.setQuery("αθηνα")
        advanceUntilIdle()

        assertEquals(listOf("Αθήνα"), vm.uiState.value.drawerApps.map { it.label })
    }

    @Test
    fun `query that matches nothing yields an empty drawer`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()

        vm.setQuery("zzz")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.drawerApps.isEmpty())
    }

    @Test
    fun `onReturnToHome clears the query`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        vm.setQuery("chr")
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.drawerApps.size)

        vm.onReturnToHome()
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.query)
        assertEquals(2, vm.uiState.value.drawerApps.size)
    }

    @Test
    fun `onReturnToHome emits a single home signal`() = runTest(dispatcher) {
        val vm = viewModel()
        val received = mutableListOf<Unit>()
        val job = launch { vm.goHome.collect { received += it } }
        advanceUntilIdle()

        vm.onReturnToHome()
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

        assertNotNull("write failure must be surfaced", vm.uiState.value.settingsError)
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
        assertEquals(setOf("existing"), vm.uiState.value.favoritePackages)
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

        assertNotNull(vm.uiState.value.settingsError)
    }

    @Test
    fun `dismissing the settings error clears the warning`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { failReads(java.io.IOException("corrupt")) }
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.settingsError)

        vm.dismissSettingsError()
        advanceUntilIdle()

        assertNull(vm.uiState.value.settingsError)
    }

    @Test
    fun `a settings read failure still allows the app list to load`() = runTest(dispatcher) {
        // The launcher's core action (enumerate + open an app) must not depend on
        // settings storage being healthy.
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val settings = FakeSettingsRepository().apply { failReads(java.io.IOException("corrupt")) }
        val vm = viewModel(appRepo = repo, settingsRepo = settings)

        advanceUntilIdle()

        assertEquals(listOf("Chrome"), vm.uiState.value.drawerApps.map { it.label })
    }

    // ── settings pass-through ───────────────────────────────────────────────

    @Test
    fun `use24h defaults to true and follows the repository`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.use24h)

        vm.setUse24h(false)
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.use24h)
        assertEquals(false, settings.currentUse24h)
    }

    @Test
    fun `themeKey defaults to the default theme and follows the repository`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settingsRepo = settings)
        advanceUntilIdle()
        assertEquals(com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY, vm.uiState.value.themeKey)

        vm.setTheme("dracula")
        advanceUntilIdle()

        assertEquals("dracula", vm.uiState.value.themeKey)
        assertEquals("dracula", settings.currentTheme)
    }
}
