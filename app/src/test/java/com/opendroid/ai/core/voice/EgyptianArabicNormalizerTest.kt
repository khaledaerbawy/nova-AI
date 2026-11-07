package com.opendroid.ai.core.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class EgyptianArabicNormalizerTest {
    @Test
    fun removesArabicDiacriticsAndNormalizesAlefVariants() {
        assertEquals("اقفل الواي فاي", EgyptianArabicNormalizer.normalizeTranscript("إِقْفِل الوَاي فَاي"))
    }

    @Test
    fun normalizesArabicIndicDigitsWithoutChangingEnglishNames() {
        assertEquals("ابعت WhatsApp لمحمد 123", EgyptianArabicNormalizer.normalizeTranscript("ابعت WhatsApp لمحمد ١٢٣"))
    }

    @Test
    fun matchingKeyIgnoresPunctuationAndCase() {
        assertEquals("open whatsapp", EgyptianArabicNormalizer.matchingKey("Open WhatsApp!"))
    }
}
