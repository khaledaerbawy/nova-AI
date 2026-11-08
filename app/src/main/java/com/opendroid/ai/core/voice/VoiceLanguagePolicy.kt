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
        .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
        .replace(diacritics, "")
        .replace(punctuation, " ")
        .replace(whitespace, " ")
        .trim()

    fun containsWakePhrase(transcript: String, configuredPhrase: String = "NOVA"): Boolean {
        val tokens = normalizeTranscript(transcript).split(' ').filter(String::isNotBlank)
        val configured = normalizeTranscript(configuredPhrase).split(' ').filter(String::isNotBlank)
        if (configured.isNotEmpty() && tokens.take(configured.size) == configured) return true
        return tokens.firstOrNull() in setOf("نوفا", "نوفه", "nova") ||
            tokens.take(2) == listOf("يا", "نوفا") ||
            tokens.take(2) == listOf("يا", "نوفه") ||
            tokens.take(2) == listOf("hey", "nova")
    }
}
