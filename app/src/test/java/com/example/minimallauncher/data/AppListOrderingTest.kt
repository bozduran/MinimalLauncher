package com.example.minimallauncher.data

import java.text.Collator
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [AppListOrdering] — the app-identity rules behind the duplicate
 * entry and mis-keyed favorite bugs (see CODE_AUDIT.md F5).
 */
class AppListOrderingTest {

    private val english = Collator.getInstance(Locale.ENGLISH)
    private val greek = Collator.getInstance(Locale.forLanguageTag("el"))

    private fun app(label: String, pkg: String, activity: String = "MainActivity") =
        AppInfo(label = label, packageName = pkg, activityName = "$pkg.$activity")

    @Test
    fun `a package with several launcher activities collapses to one entry`() {
        val apps = listOf(
            app("Camera", "com.example.camera", "CameraActivity"),
            app("Camera", "com.example.camera", "AliasActivity"),
            app("Camera", "com.example.camera", "MainActivity"),
        )

        val result = AppListOrdering.collapseAndSort(apps, english)

        assertEquals(1, result.size)
        assertEquals("com.example.camera", result.single().packageName)
    }

    @Test
    fun `the surviving activity is chosen deterministically`() {
        val apps = listOf(
            app("Camera", "com.example.camera", "ZetaActivity"),
            app("Camera", "com.example.camera", "AlphaActivity"),
            app("Camera", "com.example.camera", "MidActivity"),
        )

        val first = AppListOrdering.collapseAndSort(apps, english).single()
        val second = AppListOrdering.collapseAndSort(apps.reversed(), english).single()

        assertEquals("com.example.camera.AlphaActivity", first.activityName)
        assertEquals("input order must not change the chosen activity", first, second)
    }

    @Test
    fun `distinct apps sharing a label are both kept`() {
        val apps = listOf(
            app("Photos", "com.example.photos"),
            app("Photos", "org.example.gallery"),
        )

        val result = AppListOrdering.collapseAndSort(apps, english)

        assertEquals(2, result.size)
        assertEquals(
            listOf("com.example.photos", "org.example.gallery"),
            result.map { it.packageName }.sorted(),
        )
    }

    @Test
    fun `a single-activity package is unchanged`() {
        val apps = listOf(app("Chrome", "com.android.chrome"))

        assertEquals(apps, AppListOrdering.collapseAndSort(apps, english))
    }

    @Test
    fun `entries are sorted by label`() {
        val apps = listOf(
            app("Zebra", "a.zebra"),
            app("Apple", "b.apple"),
            app("Mango", "c.mango"),
        )

        assertEquals(
            listOf("Apple", "Mango", "Zebra"),
            AppListOrdering.collapseAndSort(apps, english).map { it.label },
        )
    }

    @Test
    fun `sorting uses the supplied collator so Greek labels order correctly`() {
        // Greek collation distinguishes accented vowels from their base letter;
        // asserting against the collator itself keeps this locale-independent.
        val apps = listOf(
            app("Ωμέγα", "a.omega"),
            app("Άλφα", "b.alpha"),
            app("Βήτα", "c.beta"),
        )

        val expected = apps.sortedWith(compareBy(greek) { it.label }).map { it.label }

        assertEquals(expected, AppListOrdering.collapseAndSort(apps, greek).map { it.label })
    }

    @Test
    fun `an empty list produces an empty list`() {
        assertEquals(emptyList<AppInfo>(), AppListOrdering.collapseAndSort(emptyList(), english))
    }
}
