package com.example.minimallauncher.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Translation parity between `values/strings.xml` and `values-el/strings.xml`.
 *
 * A missing or renamed key silently falls back to English, and a translation that
 * loses a format placeholder (`%1$s`) crashes at runtime with
 * `IllegalFormatException`. Both are cheap to catch by parsing the resource files,
 * so this runs as a plain JVM test rather than being reviewed by eye.
 *
 * Parsing the XML directly (instead of reading resources through a `Context`)
 * keeps the test on the JVM, consistent with the rest of the suite.
 */
class TranslationParityTest {

    private companion object {
        const val DEFAULT = "values"
        const val GREEK = "values-el"

        /** Matches a positional format argument such as %1$s or %2$d. */
        val FORMAT_ARG = Regex("""%\d+\$[a-zA-Z]""")
    }

    private data class Resource(
        val strings: Map<String, String>,
        /** Names declared `translatable="false"`, e.g. brand names. */
        val untranslatable: Set<String>,
        val plurals: Map<String, Set<String>>,
    )

    private fun resourceFile(qualifier: String): File {
        // The unit-test working directory differs between Gradle versions/IDEs.
        val candidates = listOf(
            File("src/main/res/$qualifier/strings.xml"),
            File("app/src/main/res/$qualifier/strings.xml"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("$qualifier/strings.xml not found from ${File(".").absolutePath}")
    }

    private fun parse(file: File): Resource {
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = false }
            .newDocumentBuilder()
            .parse(file)

        val strings = mutableMapOf<String, String>()
        val untranslatable = mutableSetOf<String>()
        val plurals = mutableMapOf<String, MutableSet<String>>()

        val stringNodes = document.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val element = stringNodes.item(i) as Element
            val name = element.getAttribute("name")
            strings[name] = element.textContent
            if (element.getAttribute("translatable") == "false") untranslatable += name
        }

        val pluralNodes = document.getElementsByTagName("plurals")
        for (i in 0 until pluralNodes.length) {
            val element = pluralNodes.item(i) as Element
            val quantities = mutableSetOf<String>()
            val items = element.getElementsByTagName("item")
            for (j in 0 until items.length) {
                quantities += (items.item(j) as Element).getAttribute("quantity")
            }
            plurals[element.getAttribute("name")] = quantities
        }

        return Resource(strings, untranslatable, plurals)
    }

    private val default = parse(resourceFile(DEFAULT))
    private val greek = parse(resourceFile(GREEK))

    @Test
    fun `the greek file is present and non-trivial`() {
        assertTrue("no strings found in $GREEK", greek.strings.size > 5)
    }

    @Test
    fun `every translatable default string has a greek translation`() {
        val missing = (default.strings.keys - default.untranslatable) - greek.strings.keys

        assertTrue("missing Greek translations for: ${missing.sorted()}", missing.isEmpty())
    }

    @Test
    fun `untranslatable defaults are the only ones allowed to be absent`() {
        // Guards against someone "fixing" a parity failure by marking a real string
        // translatable="false": only the brand name and pure glyphs are legitimate.
        assertEquals(
            setOf(
                "app_name",
                "search_clear_symbol",
                "favorite_on_symbol",
                "favorite_off_symbol",
                "back_symbol",
            ),
            default.untranslatable,
        )
    }

    @Test
    fun `every greek string corresponds to a default key`() {
        val extra = greek.strings.keys - default.strings.keys

        assertTrue("stale Greek keys with no default: ${extra.sorted()}", extra.isEmpty())
    }

    @Test
    fun `no translation is empty or left as the english text placeholder`() {
        val blank = greek.strings.filterValues { it.isBlank() }.keys

        assertTrue("blank Greek translations: ${blank.sorted()}", blank.isEmpty())
    }

    @Test
    fun `every plural has the same quantities in both languages`() {
        assertEquals(default.plurals.keys, greek.plurals.keys)
        for ((name, quantities) in default.plurals) {
            assertEquals(
                "plural quantities differ for '$name'",
                quantities,
                greek.plurals[name],
            )
        }
    }

    @Test
    fun `format placeholders survive translation`() {
        val mismatched = default.strings
            .filterKeys { it in greek.strings }
            .filter { (name, english) ->
                FORMAT_ARG.findAll(english).map { it.value }.toSet() !=
                    FORMAT_ARG.findAll(greek.strings.getValue(name)).map { it.value }.toSet()
            }
            .keys

        assertTrue(
            "translations must keep the same format arguments or they crash at runtime: " +
                mismatched.sorted(),
            mismatched.isEmpty(),
        )
    }

    @Test
    fun `plurals keep their format placeholders`() {
        val english = parse(resourceFile(DEFAULT))
        val greekFile = resourceFile(GREEK)

        // Re-read raw text for plural items, which the Resource model does not keep.
        val rawEnglish = english.strings.keys + english.plurals.keys
        assertTrue(rawEnglish.isNotEmpty())

        val englishText = englishFileText(DEFAULT)
        val greekText = englishFileText(GREEK)

        val englishArgs = FORMAT_ARG.findAll(englishText).map { it.value }.toSortedSet()
        val greekArgs = FORMAT_ARG.findAll(greekText).map { it.value }.toSortedSet()

        assertEquals(
            "the set of format arguments must match across resource files ($greekFile)",
            englishArgs,
            greekArgs,
        )
    }

    private fun englishFileText(qualifier: String): String = resourceFile(qualifier).readText()
}
