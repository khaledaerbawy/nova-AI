package com.opendroid.ai.core.voice

enum class VoiceApprovalIntent { APPROVE, REJECT, NONE }

/** Conservative spoken approval parser with explicit Egyptian Arabic support. */
object VoiceApprovalParser {
    private val affirmativeUtterances = setOf(
        "approve", "yes", "haan", "si", "sim", "run", "execute", "go ahead", "do it",
        "ايوه", "اه", "تمام", "ماشي", "موافق", "وافق", "نفذ", "نفذها", "شغلها",
        "ايوه نفذ", "اه نفذ", "تمام نفذ", "ماشي نفذ", "موافق نفذ"
    )
    // JVM word boundaries do not reliably delimit Arabic words. Match English
    // phrases with regex and normalized Arabic with whole-token checks.
    private val englishRejectPattern = Regex(
        """\\b(cancel|stop|reject|no)\\b|\\b(dont|do not)\\s+(run|do|execute)\\b""",
        RegexOption.IGNORE_CASE
    )
    private val arabicRejectWords = setOf("لا", "الغيه", "الغي", "متنفذش", "بلاش")

    fun parse(utterance: String): VoiceApprovalIntent {
        val normalized = VoiceLanguagePolicy.normalizeTranscript(utterance)
        val tokens = normalized.split(' ').filter(String::isNotBlank)
        val hasArabicRejection = tokens.any { it in arabicRejectWords } ||
            tokens.windowed(2).any { it == listOf("مش", "موافق") || it == listOf("مش", "موافقة") }

        return when {
            normalized.isBlank() -> VoiceApprovalIntent.NONE
            englishRejectPattern.containsMatchIn(normalized) || hasArabicRejection ->
                VoiceApprovalIntent.REJECT
            normalized in affirmativeUtterances -> VoiceApprovalIntent.APPROVE
            else -> VoiceApprovalIntent.NONE
        }
    }
}
