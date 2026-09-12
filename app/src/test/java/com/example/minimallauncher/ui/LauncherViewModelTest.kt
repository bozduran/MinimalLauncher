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
    ) = LauncherViewModel(
        appRepo,
        settingsRepo,
        changeSource,
        gateway,
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
            vm.favorites.value.map { it.label },
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

        assertNotNull(vm.settingsError.value)
    }

    // ── STAB-5: failed actions are visible, not silent ──────────────────────

    @Test
    fun `a successful action reports no failure`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway()
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()

        vm.launchApp(app("Chrome"))
        vm.openClock()

        assertNull(vm.actionFailure.value)
    }

    @Test
    fun `a failed launch surfaces the app it could not open`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway().apply { failEverything = true }
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()

        vm.launchApp(app("Chrome", packageName = "com.example.chrome"))

        val failure = vm.actionFailure.value
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
            assertEquals(expected, vm.actionFailure.value?.action)
        }
    }

    @Test
    fun `dismissing a failure clears it`() = runTest(dispatcher) {
        val gateway = FakeLauncherGateway().apply { failEverything = true }
        val vm = viewModel(gateway = gateway)
        advanceUntilIdle()
        vm.openClock()
        assertNotNull(vm.actionFailure.value)

        vm.dismissActionFailure()

        assertNull(vm.actionFailure.value)
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

        assertEquals(LauncherAction.OpenApp, vm.actionFailure.value?.action)
    }

    // ── ARCH-3: all outgoing actions go through the gateway ─────────────────

    @Test
    fun `tapping an app routes through the gateway`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val gateway = FakeLauncherGateway()
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        vm.launchApp(vm.drawerApps.value.first { it.packageName == "maps" })

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
                vm.drawerApps.value.map { it.label },
            )

            testScheduler.advanceUntilIdle()
            assertEquals(listOf("Chrome"), vm.drawerApps.value.map { it.label })
        }

    @Test
    fun `search matching still folds accents and case after precomputing keys`() =
        runTest(dispatcher) {
            val repo = FakeAppRepository(listOf(app("Αθήνα"), app("Café"), app("Chrome")))
            val vm = viewModel(appRepo = repo)
            advanceUntilIdle()

            vm.setQuery("αθηνα")
            advanceUntilIdle()
            assertEquals(listOf("Αθήνα"), vm.drawerApps.value.map { it.label })

            vm.setQuery("cafe")
            advanceUntilIdle()
            assertEquals(listOf("Café"), vm.drawerApps.value.map { it.label })

            vm.setQuery("CHRO")
            advanceUntilIdle()
            assertEquals(listOf("Chrome"), vm.drawerApps.value.map { it.label })
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

        assertTrue(vm.isLoadingApps.value)
    }

    @Test
    fun `loading ends after a successful first load`() = runTest(dispatcher) {
        val vm = viewModel(appRepo = FakeAppRepository(listOf(app("Chrome"))))

        advanceUntilIdle()

        assertEquals(false, vm.isLoadingApps.value)
    }

    @Test
    fun `loading ends after a failed first load`() = runTest(dispatcher) {
        val repo = FakeAppRepository().apply { failure = RuntimeException("boom") }
        val vm = viewModel(appRepo = repo)

        advanceUntilIdle()

        assertEquals(false, vm.isLoadingApps.value)
        assertNotNull(vm.appListError.value)
    }

    @Test
    fun `a later refresh does not flip the screen back into loading`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        assertEquals(false, vm.isLoadingApps.value)

        vm.refresh()
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()

        assertEquals("refresh must not flash an empty state", false, vm.isLoadingApps.value)
        assertEquals(
            "the previously loaded list stays visible across a refresh",
            listOf("Chrome"),
            vm.allApps.value.map { it.label },
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
            assertTrue(vm.isLoadingApps.value)

            vm.refresh() // supersedes the first load
            testScheduler.advanceTimeBy(300)
            testScheduler.runCurrent() // replacement starts, blocked on gate2

            assertTrue(
                "cancelling a load must not report the list as loaded",
                vm.isLoadingApps.value,
            )

            gate1.complete(Unit)
            gate2.complete(Unit)
            advanceUntilIdle()

            assertEquals(false, vm.isLoadingApps.value)
            assertEquals(listOf("Chrome"), vm.allApps.value.map { it.label })
        }

    // ── DRAW-3: consistent return-to-home ───────────────────────────────────

    @Test
    fun `returning to home restores the full drawer after a search`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps"), app("Photos")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        vm.setQuery("chr")
        advanceUntilIdle()
        assertEquals(1, vm.drawerApps.value.size)

        vm.onReturnToHome()
        advanceUntilIdle()

        assertEquals("", vm.query.value)
        assertEquals(3, vm.drawerApps.value.size)
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

        assertEquals("", vm.query.value)
        assertEquals(listOf("Chrome"), vm.drawerApps.value.map { it.label })
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

            assertEquals(listOf("Maps"), vm.favorites.value.map { it.label })
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
        val vm = viewModel(appRepo = repo, gateway = gateway)
        advanceUntilIdle()

        // Simulates the query surviving a recreation (a restored ViewModel, or a
        // future SavedStateHandle restore) without a user edit in this session.
        vm.query.value = "chr"
        testScheduler.advanceTimeBy(1_000)
        advanceUntilIdle()

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
            vm.favorites.value.map { it.label },
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
        assertEquals(setOf("chrome", "maps"), vm.favoriteSet.value)
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
            assertEquals(listOf("Photos", "Chrome"), vm.favorites.value.map { it.label })

            vm.toggleHidden("maps")
            advanceUntilIdle()

            assertEquals(
                listOf("Photos", "Maps", "Chrome"),
                vm.favorites.value.map { it.label },
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

        assertEquals(listOf("Chrome"), vm.favorites.value.map { it.label })
        assertEquals(listOf("Chrome"), vm.drawerApps.value.map { it.label })
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
    fun `onReturnToHome clears the query`() = runTest(dispatcher) {
        val repo = FakeAppRepository(listOf(app("Chrome"), app("Maps")))
        val vm = viewModel(appRepo = repo)
        advanceUntilIdle()
        vm.setQuery("chr")
        advanceUntilIdle()
        assertEquals(1, vm.drawerApps.value.size)

        vm.onReturnToHome()
        advanceUntilIdle()

        assertEquals("", vm.query.value)
        assertEquals(2, vm.drawerApps.value.size)
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
