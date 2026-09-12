package com.example.minimallauncher.ui

import com.example.minimallauncher.testutil.FakeAppChangeSource
import com.example.minimallauncher.testutil.FakeAppRepository
import com.example.minimallauncher.testutil.FakeSettingsRepository
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
    ) = LauncherViewModel(appRepo, settingsRepo, changeSource, ioDispatcher)

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
