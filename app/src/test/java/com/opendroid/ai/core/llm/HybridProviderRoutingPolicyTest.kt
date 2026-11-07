package com.opendroid.ai.core.llm

import com.opendroid.ai.core.agent.ActionRisk
import com.opendroid.ai.core.agent.QueryComplexity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridProviderRoutingPolicyTest {
    @Test
    fun simpleConversationPrefersLocal() {
        assertTrue(HybridProviderRoutingPolicy.shouldPreferLocal("اشرحلي الفرق بين الواي فاي والبلوتوث", QueryComplexity.SIMPLE))
    }

    @Test
    fun multiStepReasoningDoesNotPreferLocal() {
        assertFalse(HybridProviderRoutingPolicy.shouldPreferLocal("plan a multi-step workflow", QueryComplexity.COMPLEX))
    }

    @Test
    fun sensitiveEgyptianAndEnglishActionsDisableAutomaticFallback() {
        for (query in listOf("ابعت رسالة لمحمد", "اتصل بأحمد", "send a WhatsApp message", "pay this invoice")) {
            assertEquals(query, ActionRisk.SENSITIVE, HybridProviderRoutingPolicy.riskFor(query))
            assertFalse(query, HybridProviderRoutingPolicy.allowsFallback(query))
        }
    }

    @Test
    fun readOnlyRequestsCanUseConfiguredFallbacks() {
        assertTrue(HybridProviderRoutingPolicy.allowsFallback("لخصلي المقال ده"))
    }
}
