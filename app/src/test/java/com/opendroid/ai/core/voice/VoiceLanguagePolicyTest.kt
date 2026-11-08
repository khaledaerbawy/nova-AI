package com.opendroid.ai.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceLanguagePolicyTest {
    @Test fun egyptianLocaleIsExplicit() { assertEquals("ar-EG", VoiceLanguagePolicy.LOCALE_TAG) }
    @Test fun recognizesArabicWakeWord() {
        assertTrue(VoiceLanguagePolicy.containsWakePhrase("يا نوفا"))
        assertTrue(VoiceLanguagePolicy.containsWakePhrase("نوفا"))
        assertTrue(VoiceLanguagePolicy.containsWakePhrase("يَا نُوفَا"))
    }
    @Test fun recognizesEnglishWakeWord() {
        assertTrue(VoiceLanguagePolicy.containsWakePhrase("Nova"))
        assertTrue(VoiceLanguagePolicy.containsWakePhrase("Hey, Nova!"))
    }
    @Test fun recognizesConfiguredCustomWakePhrases() {
        assertTrue(VoiceLanguagePolicy.containsWakePhrase("يا ليلى افتحي الكاميرا", "يا ليلى"))
        assertTrue(VoiceLanguagePolicy.containsWakePhrase("Luna, open Spotify", "Luna"))
        assertFalse(VoiceLanguagePolicy.containsWakePhrase("hello there", "Luna"))
    }
    @Test fun rejectsSubstringsAndUnrelatedPhrases() {
        assertFalse(VoiceLanguagePolicy.containsWakePhrase("innovation"))
        assertFalse(VoiceLanguagePolicy.containsWakePhrase("NovaStore"))
        assertFalse(VoiceLanguagePolicy.containsWakePhrase("يا نور"))
        assertFalse(VoiceLanguagePolicy.containsWakePhrase(""))
    }
}
