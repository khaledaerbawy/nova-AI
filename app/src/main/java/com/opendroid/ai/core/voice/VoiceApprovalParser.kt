package com.opendroid.ai.core.voice

enum class VoiceApprovalIntent { APPROVE, REJECT, NONE }

/** Conservative spoken approval parser with explicit Egyptian Arabic support. */
object VoiceApprovalParser {
    private val affirmativeUtterances = setOf(
        "approve", "yes", "haan", "si", "sim", "run", "execute", "go ahead", "do it",
        "ايوه", "اه", "تمام", "ماشي", "موافق", "وافق", "نفذ", "نفذها", "شغلها",
        "ايوه نفذ", "اه نفذ", "تمام نفذ", "ماشي نفذ", "موافق نفذ"
    )
    private val rejectPattern = Regex(
        """\b(cancel|stop|reject|no)\b|\b(dont|do not)\s+(run|do|execute)\b|\b(لا|لأ|الغيه|الغي|متنفذش|مش موافق|مش موافقة|بلاش)\b""",
        RegexOption.IGNORE_CASE
    )

    fun parse(utterance: String): VoiceApprovalIntent {
        val normalized = VoiceLanguagePolicy.normalizeTranscript(utterance)
        return when {
            normalized.isBlank() -> VoiceApprovalIntent.NONE
            rejectPattern.containsMatchIn(normalized) -> VoiceApprovalIntent.REJECT
            normalized in affirmativeUtterances -> VoiceApprovalIntent.APPROVE
            else -> VoiceApprovalIntent.NONE
        }
    }
}
