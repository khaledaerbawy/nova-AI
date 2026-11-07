package com.opendroid.ai.core.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicCommandHeuristicsTest {
    @Test fun recognizesEgyptianDeviceCommandsOffline() {
        assertTrue(ArabicCommandHeuristics.requiresAction("افتح واتساب"))
        assertTrue(ArabicCommandHeuristics.requiresAction("شغل الفلاش"))
        assertTrue(ArabicCommandHeuristics.requiresAction("ابعت لمحمد رسالة"))
        assertTrue(ArabicCommandHeuristics.requiresAction("شغل الروتين الصباحي"))
        assertTrue(ArabicCommandHeuristics.requiresAction("اعمل منشور للسوشيال"))
    }
    @Test fun leavesNormalConversationAlone() {
        assertFalse(ArabicCommandHeuristics.requiresAction("عامل ايه النهارده"))
        assertFalse(ArabicCommandHeuristics.requiresAction("احكيلي نكتة"))
    }
}
