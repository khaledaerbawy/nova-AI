package com.opendroid.ai.core.voice

import java.util.Locale

/** Central Egyptian-Arabic voice locale and wake phrase policy. */
object VoiceLanguagePolicy {
    const val LOCALE_TAG = "ar-EG"
    val locale: Locale = Locale.forLanguageTag(LOCALE_TAG)
    private val diacritics = Regex("[\\u064B-\\u065F\\u0670]")
    private val punctuation = Regex("[^\\p{L}\\p{N}\\s]")
    private val whitespace = Regex("\\s+")

    fun normalizeTranscript(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(diacritics, "")
        .replace(punctuation, " ")
        .replace(whitespace, " ")
        .trim()

    fun containsWakePhrase(transcript: String): Boolean {
        val tokens = normalizeTranscript(transcript).split(' ').filter(String::isNotBlank)
        return "نوفا" in tokens || "نوفه" in tokens ||
            tokens == listOf("nova") || tokens == listOf("hey", "nova")
    }
}
