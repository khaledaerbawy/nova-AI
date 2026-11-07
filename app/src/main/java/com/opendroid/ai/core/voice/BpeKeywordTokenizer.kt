package com.opendroid.ai.core.voice
import java.util.Locale

/** Converts a phrase into bounded valid BPE token paths accepted by sherpa-onnx KWS. */
object BpeKeywordTokenizer {
    fun alternatives(phrase: String, vocabulary: Set<String>, maxPaths: Int = 48): List<String> {
        val normalized = phrase.trim().uppercase(Locale.ROOT).split(Regex("\\s+")).filter(String::isNotBlank)
        if (normalized.isEmpty() || vocabulary.isEmpty()) return emptyList()
        val encoded = normalized.joinToString("") { "▁$it" }
        val memo = mutableMapOf<Int, List<List<String>>>()
        fun segment(offset: Int): List<List<String>> {
            if (offset == encoded.length) return listOf(emptyList())
            memo[offset]?.let { return it }
            val matches = vocabulary.asSequence().filter { it.isNotEmpty() && encoded.startsWith(it, offset) }
                .sortedByDescending(String::length).take(maxPaths).toList()
            val paths = matches.flatMap { token -> segment(offset + token.length).map { tail -> listOf(token) + tail } }.take(maxPaths)
            memo[offset] = paths
            return paths
        }
        return segment(0).map { it.joinToString(" ") }.distinct()
    }
}
