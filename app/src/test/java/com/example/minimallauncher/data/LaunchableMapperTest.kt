package com.example.minimallauncher.data

import com.example.minimallauncher.testutil.RecordingAppLogger
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for profile-aware app enumeration (USER_STORIES.md PLAT-1).
 *
 * These run without a device and without a work profile, which is the whole reason
 * enumeration was put behind [LaunchableSource]: the previous implementation called
 * `PackageManager` directly, so nothing about it could be verified off-device.
 */
class LaunchableMapperTest {

    private val english = Collator.getInstance(Locale.ENGLISH)

    private fun entry(
        label: String,
        pkg: String,
        activity: String = "MainActivity",
        profile: ProfileKind = ProfileKind.Current,
        enabled: Boolean = true,
    ) = LaunchableEntry(
        label = label,
        packageName = pkg,
        activityName = "$pkg.$activity",
        profileKind = profile,
        enabled = enabled,
    )

    private fun map(
        entries: List<LaunchableEntry>,
        self: String = SELF,
    ) = LaunchableMapper.map(entries, english, self)

    private companion object {
        const val SELF = "com.example.minimallauncher"
    }

    // ── filtering ───────────────────────────────────────────────────────────

    @Test
    fun `disabled activities are not offered`() {
        val result = map(
            listOf(
                entry("Chrome", "com.android.chrome"),
                entry("Disabled", "com.example.disabled", enabled = false),
            ),
        )

        assertEquals(listOf("Chrome"), result.map { it.label })
    }

    @Test
    fun `the launcher excludes itself`() {
        val result = map(listOf(entry("Minimal", SELF), entry("Chrome", "com.android.chrome")))

        assertEquals(listOf("Chrome"), result.map { it.label })
    }

    // ── identity (DATA-1, extended per profile) ─────────────────────────────

    @Test
    fun `a package with several launcher activities shows once`() {
        val result = map(
            listOf(
                entry("Camera", "com.example.camera", "Zeta"),
                entry("Camera", "com.example.camera", "Alpha"),
                entry("Camera", "com.example.camera", "Main"),
            ),
        )

        assertEquals(1, result.size)
        assertEquals("com.example.camera.Alpha", result.single().activityName)
    }

    @Test
    fun `the surviving activity is independent of input order`() {
        val forwards = map(
            listOf(
                entry("Camera", "com.example.camera", "Alpha"),
                entry("Camera", "com.example.camera", "Zeta"),
            ),
        )
        val backwards = map(
            listOf(
                entry("Camera", "com.example.camera", "Zeta"),
                entry("Camera", "com.example.camera", "Alpha"),
            ),
        )

        assertEquals(forwards, backwards)
    }

    @Test
    fun `the same package in two profiles stays two entries`() {
        // The reason identity is (package, profile) rather than package alone: a work
        // copy and a personal copy are genuinely different apps.
        val result = map(
            listOf(
                entry("Chat", "com.example.chat", profile = ProfileKind.Current),
                entry("Chat", "com.example.chat", profile = ProfileKind.Other),
            ),
        )

        assertEquals(2, result.size)
        assertEquals(
            setOf(ProfileKind.Current, ProfileKind.Other),
            result.map { it.profileKind }.toSet(),
        )
    }

    @Test
    fun `the profile is carried through to the app entry`() {
        val result = map(listOf(entry("Chat", "com.example.chat", profile = ProfileKind.Other)))

        assertEquals(ProfileKind.Other, result.single().profileKind)
    }

    @Test
    fun `an entry from the current profile is marked as such by default`() {
        val result = map(listOf(entry("Chrome", "com.android.chrome")))

        assertEquals(ProfileKind.Current, result.single().profileKind)
    }

    // ── ordering and shape ──────────────────────────────────────────────────

    @Test
    fun `entries are ordered by label`() {
        val result = map(
            listOf(
                entry("Zebra", "a.zebra"),
                entry("Apple", "b.apple"),
                entry("Mango", "c.mango"),
            ),
        )

        assertEquals(listOf("Apple", "Mango", "Zebra"), result.map { it.label })
    }

    @Test
    fun `the search key is precomputed for every entry`() {
        val result = map(listOf(entry("Αθήνα", "gr.athens")))

        assertEquals("αθηνα", result.single().searchKey)
    }

    @Test
    fun `an empty source produces an empty list`() {
        assertEquals(emptyList<AppInfo>(), map(emptyList()))
    }

    @Test
    fun `a source containing only the launcher produces an empty list`() {
        assertEquals(emptyList<AppInfo>(), map(listOf(entry("Minimal", SELF))))
    }
}

/** [LaunchableSource] returning a fixed list, or throwing. */
private class FakeLaunchableSource(
    private var entries: List<LaunchableEntry> = emptyList(),
    private var failure: Throwable? = null,
) : LaunchableSource {

    var calls: Int = 0
        private set

    fun setEntries(value: List<LaunchableEntry>) {
        entries = value
    }

    fun failWith(error: Throwable) {
        failure = error
    }

    override fun entries(): List<LaunchableEntry> {
        calls++
        failure?.let { throw it }
        return entries
    }
}

class FallbackLaunchableSourceTest {

    private val primary = FakeLaunchableSource()
    private val secondary = FakeLaunchableSource()
    private val logger = RecordingAppLogger()
    private val source = FallbackLaunchableSource(primary, secondary, logger)

    private val entry = LaunchableEntry("Chrome", "com.android.chrome", "a")

    @Test
    fun `the primary source is used when it has entries`() {
        primary.setEntries(listOf(entry))
        secondary.setEntries(listOf(LaunchableEntry("FromFallback", "x", "a")))

        val result = source.entries()

        assertEquals(listOf("Chrome"), result.map { it.label })
        assertEquals("the fallback should not be called at all", 0, secondary.calls)
        assertTrue(logger.records.isEmpty())
    }

    @Test
    fun `an empty primary falls back and records why`() {
        primary.setEntries(emptyList())
        secondary.setEntries(listOf(entry))

        val result = source.entries()

        assertEquals(listOf("Chrome"), result.map { it.label })
        assertEquals(1, logger.recordsFor("app-enumeration").size)
    }

    @Test
    fun `a failing primary falls back and records the cause`() {
        primary.failWith(IllegalStateException("LauncherApps unavailable"))
        secondary.setEntries(listOf(entry))

        val result = source.entries()

        assertEquals(listOf("Chrome"), result.map { it.label })
        val record = logger.recordsFor("app-enumeration").single()
        assertTrue(record.throwable is IllegalStateException)
    }

    @Test
    fun `a failure in both sources propagates`() {
        primary.failWith(IllegalStateException("primary"))
        secondary.failWith(IllegalStateException("secondary"))

        val thrown = runCatching { source.entries() }.exceptionOrNull()

        assertEquals("secondary", thrown?.message)
    }

    @Test
    fun `an empty primary and empty fallback yields an empty list`() {
        assertTrue(source.entries().isEmpty())
    }
}

class SourceBackedAppRepositoryTest {

    @Test
    fun `the repository maps source entries into the launcher's app list`() = runTest {
        val source = FakeLaunchableSource().apply {
            setEntries(
                listOf(
                    LaunchableEntry("Maps", "com.example.maps", "com.example.maps.Main"),
                    LaunchableEntry("Chrome", "com.android.chrome", "com.android.chrome.Main"),
                    LaunchableEntry("Minimal", "com.example.minimallauncher", "self"),
                ),
            )
        }
        val repository = SourceBackedAppRepository(
            source = source,
            selfPackage = "com.example.minimallauncher",
            collator = { Collator.getInstance(Locale.ENGLISH) },
        )

        val apps = repository.loadApps()

        assertEquals(listOf("Chrome", "Maps"), apps.map { it.label })
    }

    @Test
    fun `a source failure surfaces to the caller`() = runTest {
        val source = FakeLaunchableSource().apply { failWith(IllegalStateException("boom")) }
        val repository = SourceBackedAppRepository(
            source = source,
            selfPackage = "com.example.minimallauncher",
        )

        val thrown = runCatching { repository.loadApps() }.exceptionOrNull()

        assertEquals("boom", thrown?.message)
    }
}
