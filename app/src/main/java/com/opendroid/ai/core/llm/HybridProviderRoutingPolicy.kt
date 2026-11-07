package com.opendroid.ai.core.llm

import com.opendroid.ai.core.agent.ActionRisk
import com.opendroid.ai.core.agent.QueryComplexity

/**
 * Routing policy for NOVA's hybrid brain.
 *
 * Simple conversation is local-first. Multi-step reasoning stays with the
 * explicitly selected provider. High-impact language disables silent provider
 * fallback; it never changes the user's approval policy.
 */
object HybridProviderRoutingPolicy {
    private val highImpactIntent = Regex(
        """\b(send|message|text|whatsapp|telegram|sms|call|dial|pay|purchase|buy|delete|remove|transfer|publish|post|run|execute|schedule|create|edit|update|enable|disable|install|uninstall|book|order)\b|ابعت|ابعث|ارسل|أرسل|كلم|اتصل|ادفع|اشتري|احذف|امسح|حوّل|انشر|شغل|نفذ|جدول|احجز|ثبت|فعل|عطل|انشئ|اعمل""",
        RegexOption.IGNORE_CASE
    )

    fun riskFor(query: String): ActionRisk =
        if (highImpactIntent.containsMatchIn(query)) ActionRisk.SENSITIVE else ActionRisk.READ_ONLY

    fun shouldPreferLocal(query: String, complexity: QueryComplexity): Boolean =
        complexity == QueryComplexity.SIMPLE && riskFor(query) == ActionRisk.READ_ONLY

    fun allowsFallback(query: String): Boolean =
        riskFor(query) == ActionRisk.READ_ONLY
}
