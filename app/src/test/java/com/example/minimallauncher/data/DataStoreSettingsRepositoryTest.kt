package com.example.minimallauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * Integration tests for [DataStoreSettingsRepository] against a **real** DataStore
 * backed by a temporary file — no Robolectric, no device, no mocking of the
 * storage engine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun file(name: String = "launcher_settings.preferences_pb"): File =
        File(tempFolder.newFolder(), name)

    // ── defaults ────────────────────────────────────────────────────────────

    @Test
    fun `defaults are used when nothing has been stored`() = runTest {
        val repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file() },
        )

        assertEquals(emptyList<String>(), repo.favorites.first())
        assertEquals(emptySet<String>(), repo.hidden.first())
        assertTrue(repo.use24h.first())
        assertEquals(DEFAULT_THEME_KEY, repo.themeKey.first())
        assertNull(repo.readError.first())
    }

    // ── favorites ───────────────────────────────────────────────────────────

    @Test
    fun `favorite toggles on and back off`() = runTest {
        val repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file() },
        )

        repo.toggleFavorite("com.example.chrome")
        assertEquals(listOf("com.example.chrome"), repo.favorites.first())

        repo.toggleFavorite("com.example.chrome")
        assertEquals(emptyList<String>(), repo.favorites.first())
    }

    @Test
    fun `favorites keep insertion order`() = runTest {
        val repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file() },
        )

        repo.toggleFavorite("a")
        repo.toggleFavorite("b")
        repo.toggleFavorite("c")
        repo.toggleFavorite("b")

        assertEquals(listOf("a", "c"), repo.favorites.first())
    }

    @Test
    fun `blank lines in the stored favorites string are ignored`() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file() }
        store.edit { it[stringPreferencesKey("favorites")] = "a\n\n  \nb\n" }
        val repo = DataStoreSettingsRepository(store)

        assertEquals(listOf("a", "b"), repo.favorites.first())
    }

    // ── hidden ──────────────────────────────────────────────────────────────

    @Test
    fun `hidden toggles on and back off`() = runTest {
        val repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file() },
        )

        repo.toggleHidden("com.example.maps")
        assertEquals(setOf("com.example.maps"), repo.hidden.first())

        repo.toggleHidden("com.example.maps")
        assertEquals(emptySet<String>(), repo.hidden.first())
    }

    // ── simple values ───────────────────────────────────────────────────────

    @Test
    fun `use24h and themeKey persist`() = runTest {
        val repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file() },
        )

        repo.setUse24h(false)
        repo.setTheme("dracula")

        assertEquals(false, repo.use24h.first())
        assertEquals("dracula", repo.themeKey.first())
    }

    // ── durability ──────────────────────────────────────────────────────────

    @Test
    fun `settings survive a new repository instance over the same file`() = runTest {
        val target = file()

        // DataStore allows only one active instance per file, so the first store's
        // scope is cancelled to model a process shutdown before reopening the file.
        val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        val first = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = firstScope) { target },
        )
        first.toggleFavorite("com.example.chrome")
        first.setTheme("nord")
        firstScope.cancel()
        firstScope.coroutineContext.job.join()

        val second = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { target },
        )

        assertEquals(listOf("com.example.chrome"), second.favorites.first())
        assertEquals("nord", second.themeKey.first())
    }

    // ── resilience: STAB-2 ──────────────────────────────────────────────────

    @Test
    fun `an unreadable store falls back to defaults instead of failing the flow`() = runTest {
        val repo = DataStoreSettingsRepository(FailingDataStore(IOException("disk gone")))

        // Without the catch operator these would throw and the ViewModel's
        // stateIn upstream would die, freezing the UI on its initial values.
        assertEquals(emptyList<String>(), repo.favorites.first())
        assertEquals(emptySet<String>(), repo.hidden.first())
        assertTrue(repo.use24h.first())
        assertEquals(DEFAULT_THEME_KEY, repo.themeKey.first())
    }

    @Test
    fun `an unreadable store raises a sticky read error`() = runTest {
        val repo = DataStoreSettingsRepository(FailingDataStore(IOException("disk gone")))

        repo.favorites.first() // triggers the read that fails

        val error = repo.readError.first()
        assertNotNull(error)
        assertTrue(error is IOException)
    }

    @Test
    fun `a non-io failure is not swallowed`() = runTest {
        val repo = DataStoreSettingsRepository(FailingDataStore(IllegalStateException("bug")))

        val thrown = runCatching { repo.favorites.first() }.exceptionOrNull()

        assertTrue("programming errors must surface", thrown is IllegalStateException)
    }

    @Test
    fun `read error clears once the store becomes readable again`() = runTest {
        val switchable = SwitchableDataStore()
        switchable.failWith(IOException("transient"))
        val repo = DataStoreSettingsRepository(switchable)

        repo.favorites.first() // fails, setting the sticky error
        assertNotNull(repo.readError.first())

        switchable.recover()
        repo.favorites.first() // succeeds, clearing it

        assertNull(repo.readError.first())
    }

    // ── pruning (DATA-3) ────────────────────────────────────────────────────

    @Test
    fun `pruning removes entries for packages that are no longer installed`() = runTest {
        val repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file() },
        )
        repo.toggleFavorite("kept")
        repo.toggleFavorite("gone")
        repo.toggleHidden("hiddenGone")

        repo.pruneMissing(setOf("kept"))

        assertEquals(listOf("kept"), repo.favorites.first())
        assertEquals(emptySet<String>(), repo.hidden.first())
    }

    @Test
    fun `pruning preserves the favorite order of surviving entries`() = runTest {
        val repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file() },
        )
        listOf("a", "gone", "b", "c").forEach { repo.toggleFavorite(it) }

        repo.pruneMissing(setOf("a", "b", "c"))

        assertEquals(listOf("a", "b", "c"), repo.favorites.first())
    }

    @Test
    fun `pruning with everything installed changes nothing`() = runTest {
        val repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file() },
        )
        repo.toggleFavorite("a")
        repo.toggleHidden("b")

        repo.pruneMissing(setOf("a", "b", "c"))

        assertEquals(listOf("a"), repo.favorites.first())
        assertEquals(setOf("b"), repo.hidden.first())
    }

    @Test
    fun `pruning against an empty installed set is refused`() = runTest {
        val repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file() },
        )
        repo.toggleFavorite("a")

        val thrown = runCatching { repo.pruneMissing(emptySet()) }.exceptionOrNull()

        assertTrue("an empty set would wipe the user's config", thrown is IllegalArgumentException)
        assertEquals(listOf("a"), repo.favorites.first())
    }

    // ── test doubles ────────────────────────────────────────────────────────

    /** A [DataStore] whose reads and writes always fail. */
    private class FailingDataStore(private val error: Throwable) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw error }
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = throw error
    }

    /** A [DataStore] that can be switched between failing and working. */
    private class SwitchableDataStore : DataStore<Preferences> {
        @Volatile
        private var error: Throwable? = IOException("transient")

        fun failWith(error: Throwable) {
            this.error = error
        }

        fun recover() {
            error = null
        }

        override val data: Flow<Preferences> = flow {
            error?.let { throw it }
            emit(emptyPreferences())
        }

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = transform(mutablePreferencesOf())
    }
}
