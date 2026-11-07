package com.opendroid.ai.core.voice
import org.junit.Assert.assertTrue
import org.junit.Test

class BpeKeywordTokenizerTest {
    @Test fun returnsValidTokenPathsForSingleAndMultiwordKeywords() {
        val vocabulary = setOf("▁NOVA", "▁NO", "VA", "▁N", "O", "V", "A", "▁YA", "▁HEY")
        val nova = BpeKeywordTokenizer.alternatives("NOVA", vocabulary)
        assertTrue(nova.contains("▁NOVA"))
        assertTrue(nova.contains("▁NO VA"))
        val phrase = BpeKeywordTokenizer.alternatives("YA NOVA", vocabulary)
        assertTrue(phrase.any { it.startsWith("▁YA ") && it.endsWith("▁NOVA") })
    }
    @Test fun returnsNoPathWhenVocabularyCannotEncodeKeyword() {
        assertTrue(BpeKeywordTokenizer.alternatives("NOVA", setOf("▁HELLO")).isEmpty())
    }
}
