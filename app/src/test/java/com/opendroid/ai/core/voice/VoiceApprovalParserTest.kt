package com.opendroid.ai.core.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceApprovalParserTest {
    @Test fun egyptianAffirmativesApproveOnlyWhenStandalone() {
        assertEquals(VoiceApprovalIntent.APPROVE, VoiceApprovalParser.parse("أيوه"))
        assertEquals(VoiceApprovalIntent.APPROVE, VoiceApprovalParser.parse("تمام نفذ"))
        assertEquals(VoiceApprovalIntent.APPROVE, VoiceApprovalParser.parse("ماشي"))
        assertEquals(VoiceApprovalIntent.NONE, VoiceApprovalParser.parse("أيوه بس استنى"))
    }
    @Test fun egyptianNegativesAlwaysWin() {
        assertEquals(VoiceApprovalIntent.REJECT, VoiceApprovalParser.parse("لا"))
        assertEquals(VoiceApprovalIntent.REJECT, VoiceApprovalParser.parse("مش موافق"))
        assertEquals(VoiceApprovalIntent.REJECT, VoiceApprovalParser.parse("بلاش تنفذ"))
    }
    @Test fun englishApprovalRemainsSupported() {
        assertEquals(VoiceApprovalIntent.APPROVE, VoiceApprovalParser.parse("go ahead"))
        assertEquals(VoiceApprovalIntent.REJECT, VoiceApprovalParser.parse("do not execute"))
    }
}
