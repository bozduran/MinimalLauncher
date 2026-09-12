package com.example.minimallauncher.data

import java.text.Normalizer

/**
 * Accent- and case-insensitive matching that works for Greek as well as Latin.
 *
 * Steps:
 *  1. lowercase (Σ -> σ, Α -> α …)
 *  2. NFD-decompose and strip combining marks, so tonos/dialytika and Latin
 *     accents are removed:  "Αθήνα" -> "αθηνα",  "Café" -> "cafe"
 *  3. fold the Greek final sigma so "ς" and "σ" are treated the same.
 */
object TextNormalizer {

    private val combiningMarks = Regex("\\p{Mn}+")

    fun normalize(input: String): String {
        val lowered = input.lowercase()
        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        val stripped = combiningMarks.replace(decomposed, "")
        return stripped.replace('ς', 'σ')
    }

    fun matches(label: String, query: String): Boolean {
        if (query.isBlank()) return true
        return normalize(label).contains(normalize(query))
    }

    /**
     * Matches an already-[normalize]d search key against a raw query.
     *
     * Used by the drawer so an app's label is normalised **once per app-list
     * load** rather than once per installed app on every keystroke.
     */
    fun matchesKey(searchKey: String, query: String): Boolean {
        if (query.isBlank()) return true
        return searchKey.contains(normalize(query))
    }
}
