package com.opendroid.ai.core.voice

import java.util.Locale

/**
 * Conservative normalization for Egyptian Arabic speech transcripts.
 *
 * It removes orthographic noise commonly introduced by ASR while preserving
 * Latin app/contact names and the original word boundaries. It is not a
 * transliterator and must never be used to rewrite message bodies.
 */
object EgyptianArabicNormalizer {
    private val diacritics = Regex("[\\u064B-\\u065F\\u0670]")
    private val repeatedWhitespace = Regex("\\s+")
    private val punctuation = Regex("[^\\p{L}\\p{N}\\s+@._-]")

    fun normalizeTranscript(input: String): String = input
        .replace('ـ', ' ')
        .replace(diacritics, "")
        .replace(Regex("[أإآٱ]"), "ا")
        .replace('ى', 'ي')
        .replace('ؤ', 'و')
        .replace('ئ', 'ي')
        .map { char ->
            when (char) {
                in '٠'..'٩' -> ('0'.code + (char.code - '٠'.code)).toChar()
                in '۰'..'۹' -> ('0'.code + (char.code - '۰'.code)).toChar()
                else -> char
            }
        }
        .replace(repeatedWhitespace, " ")
        .trim()

    /** A comparison key only; callers should retain the original transcript for execution. */
    fun matchingKey(input: String): String = normalizeTranscript(input)
        .lowercase(Locale.ROOT)
        .replace(punctuation, " ")
        .replace(repeatedWhitespace, " ")
        .trim()
}
