package com.example.minimallauncher.testutil

import com.example.minimallauncher.data.AppChangeSource
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppLogger
import com.example.minimallauncher.data.AppRepository
import com.example.minimallauncher.data.SettingsRepository
import com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/** Builds an [AppInfo] without repeating the activity-name noise in every test. */
fun app(label: String, packageName: String = label.lowercase(), activity: String = "MainActivity") =
    AppInfo(label = label, packageName = packageName, activityName = "$packageName.$activity")

/**
 * Hand-written [AppRepository] fake.
 *
 * Hand-written rather than mock-framework based: it is small, explicit, and the
 * gate/queue helpers are needed to reproduce real ordering hazards.
 */
class FakeAppRepository(apps: List<AppInfo> = emptyList()) : AppRepository {

    /** Result returned by the next load. Set before triggering a refresh. */
    var apps: List<AppInfo> = apps

    /** When non-null, the next load throws it (and [loadCount] is still incremented). */
    var failure: Throwable? = null

    var loadCount: Int = 0
        private set

    /** Optional hook run at the start of each load, for assertions on ordering. */
    var onLoad: (suspend (callIndex: Int) -> Unit)? = null

    private val gates = ArrayDeque<CompletableDeferred<Unit>>()

    /**
     * Makes the *next* call to [loadApps] suspend until the returned deferred is
     * completed. Calls are gated in FIFO order, so tests can release a later load
     * before an earlier one and force out-of-order completion.
     */
    fun gateNextLoad(): CompletableDeferred<Unit> =
        CompletableDeferred<Unit>().also { gates.addLast(it) }

    override suspend fun loadApps(): List<AppInfo> {
        loadCount++
        // Snapshot the result at call time so concurrent loads can be observed
        // returning different data regardless of completion order.
        val result = apps
        val error = failure
        val gate = gates.removeFirstOrNull()
        onLoad?.invoke(loadCount)
        gate?.await()
        error?.let { throw it }
        return result
    }
}

/** In-memory [SettingsRepository] with switchable read/write failures. */
class FakeSettingsRepository : SettingsRepository {

    private val favoritesFlow = MutableStateFlow<List<String>>(emptyList())
    private val hiddenFlow = MutableStateFlow<Set<String>>(emptySet())
    private val use24hFlow = MutableStateFlow(true)
    private val themeFlow = MutableStateFlow(DEFAULT_THEME_KEY)

    /** When non-null, every settings flow fails with it. */
    var failure: Throwable? = null

    /** When non-null, every write fails with it. */
    var writeFailure: Throwable? = null

    private fun <T> read(source: Flow<T>): Flow<T> =
        failure?.let { error -> flow<T> { throw error } } ?: source

    override val favorites: Flow<List<String>> get() = read(favoritesFlow)
    override val hidden: Flow<Set<String>> get() = read(hiddenFlow)
    override val use24h: Flow<Boolean> get() = read(use24hFlow)
    override val themeKey: Flow<String> get() = read(themeFlow)

    val currentFavorites: List<String> get() = favoritesFlow.value
    val currentHidden: Set<String> get() = hiddenFlow.value
    val currentTheme: String get() = themeFlow.value
    val currentUse24h: Boolean get() = use24hFlow.value

    fun setFavorites(vararg packages: String) {
        favoritesFlow.value = packages.toList()
    }

    fun setHidden(vararg packages: String) {
        hiddenFlow.value = packages.toSet()
    }

    fun setThemeKey(key: String) {
        themeFlow.value = key
    }

    fun primeUse24h(value: Boolean) {
        use24hFlow.value = value
    }

    override suspend fun toggleFavorite(pkg: String) {
        writeFailure?.let { throw it }
        favoritesFlow.update { current -> if (pkg in current) current - pkg else current + pkg }
    }

    override suspend fun toggleHidden(pkg: String) {
        writeFailure?.let { throw it }
        hiddenFlow.update { current -> if (pkg in current) current - pkg else current + pkg }
    }

    override suspend fun setUse24h(value: Boolean) {
        writeFailure?.let { throw it }
        use24hFlow.value = value
    }

    override suspend fun setTheme(key: String) {
        writeFailure?.let { throw it }
        themeFlow.value = key
    }
}

/** [AppChangeSource] whose events the test emits explicitly. */
class FakeAppChangeSource : AppChangeSource {

    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    override val changes: Flow<Unit> = events

    /** Simulates a package install/uninstall/update broadcast. */
    fun emitChange() {
        check(events.tryEmit(Unit)) { "change event dropped" }
    }
}

/** [AppLogger] that keeps every record so tests can assert on failure reporting. */
class RecordingAppLogger : AppLogger {

    data class Record(val tag: String, val throwable: Throwable?, val message: String?)

    val records = mutableListOf<Record>()

    override fun record(tag: String, throwable: Throwable?, message: String?) {
        records += Record(tag, throwable, message)
    }

    fun recordsFor(tag: String): List<Record> = records.filter { it.tag == tag }
}
