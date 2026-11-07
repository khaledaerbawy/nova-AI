package com.opendroid.ai.core.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordMatcherTest {
    @Test
    fun detectsConfiguredEnglishAndEgyptianPhoneticWakeWords() {
        assertTrue(WakeWordMatcher.isMatch("NOFA"))
        assertTrue(WakeWordMatcher.isMatch("NOVA"))
        assertTrue(WakeWordMatcher.isMatch("YA_NOFA"))
        assertTrue(WakeWordMatcher.isMatch("YA NOFA"))
    }

    @Test
    fun rejectsUnrelatedKeywords() {
        assertFalse(WakeWordMatcher.isMatch(""))
        assertFalse(WakeWordMatcher.isMatch("HELLO"))
        assertFalse(WakeWordMatcher.isMatch("NOVEL"))
    }
}
