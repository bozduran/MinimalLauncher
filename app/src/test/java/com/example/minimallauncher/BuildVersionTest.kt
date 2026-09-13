package com.example.minimallauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps the declared version and the documented scheme in step (USER_STORIES.md
 * PERF-6).
 *
 * The scheme is `versionCode = major * 10_000 + minor * 100 + patch`. Without this
 * test, bumping `versionName` and forgetting `versionCode` produces a release that
 * Play rejects (or worse, one the device refuses to install as an update) — a
 * failure that only shows up at upload time.
 */
class BuildVersionTest {

    private val semantic = Regex("""^\d+\.\d+\.\d+$""")

    /** The documented scheme, expressed once so the test *is* the specification. */
    private fun versionCodeFor(versionName: String): Int {
        val parts = versionName.split(".")
        require(parts.size == 3) { "not a semantic version: $versionName" }
        val (major, minor, patch) = parts.map { it.toInt() }
        require(minor < 100 && patch < 100) { "minor and patch must be < 100: $versionName" }
        return major * 10_000 + minor * 100 + patch
    }

    @Test
    fun `the declared version name is semantic`() {
        assertTrue(
            "versionName '${BuildConfig.VERSION_NAME}' is not major.minor.patch",
            BuildConfig.VERSION_NAME.matches(semantic),
        )
    }

    @Test
    fun `the declared version code matches the documented scheme`() {
        assertEquals(
            "versionCode and versionName disagree — bump both together",
            versionCodeFor(BuildConfig.VERSION_NAME),
            BuildConfig.VERSION_CODE,
        )
    }

    @Test
    fun `the version is not left at the project template default`() {
        assertNotEquals(1, BuildConfig.VERSION_CODE)
        assertNotEquals("1.0", BuildConfig.VERSION_NAME)
    }

    @Test
    fun `the scheme maps versions to increasing codes`() {
        assertEquals(10_000, versionCodeFor("1.0.0"))
        assertEquals(10_001, versionCodeFor("1.0.1"))
        assertEquals(10_100, versionCodeFor("1.1.0"))
        assertEquals(20_000, versionCodeFor("2.0.0"))
        assertTrue(versionCodeFor("1.0.1") > versionCodeFor("1.0.0"))
        assertTrue(versionCodeFor("1.1.0") < versionCodeFor("2.0.0"))
    }

    @Test
    fun `the scheme rejects a version it cannot encode unambiguously`() {
        val tooBig = runCatching { versionCodeFor("1.100.0") }.exceptionOrNull()
        val notSemantic = runCatching { versionCodeFor("1.0") }.exceptionOrNull()

        assertTrue(tooBig is IllegalArgumentException)
        assertTrue(notSemantic is IllegalArgumentException)
    }

    @Test
    fun `the application id is the one the manifest declares`() {
        // Guards against the id being changed in one place only.
        assertEquals("com.example.minimallauncher", BuildConfig.APPLICATION_ID)
    }
}
