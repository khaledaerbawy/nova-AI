package com.opendroid.ai.core.voice

enum class VoiceApprovalIntent { APPROVE, REJECT, NONE }

/** Conservative spoken approval parser with explicit Egyptian Arabic support. */
object VoiceApprovalParser {
    private val affirmativeUtterances = setOf(
        "approve", "yes", "haan", "si", "sim", "run", "execute", "go ahead", "do it",
        "ايوه", "اه", "تمام", "ماشي", "موافق", "وافق", "نفذ", "نفذها", "شغلها",
        "ايوه نفذ", "اه نفذ", "تمام نفذ", "ماشي نفذ", "موافق نفذ"
    )
    private val englishRejectPattern = Regex(
        """\b(cancel|stop|reject|no)\b|\b(dont|do not)\s+(run|do|execute)\b""",
        RegexOption.IGNORE_CASE
    )
    // Java/ASCII \b does not recognize Arabic letters as word characters.
    // Match normalized Arabic tokens instead, so "مش موافق" and "بلاش تنفذ" win.
    private val arabicRejectPhrases = listOf(
        "لا", "لأ", "الغيه", "الغي", "متنفذش", "مش موافق", "مش موافقة", "بلاش"
    ).map { it.split(' ') }

    fun parse(utterance: String): VoiceApprovalIntent {
        val normalized = VoiceLanguagePolicy.normalizeTranscript(utterance)
        if (normalized.isBlank()) return VoiceApprovalIntent.NONE

        val tokens = normalized.split(' ').filter(String::isNotBlank)
        val hasArabicRejection = arabicRejectPhrases.any { phrase ->
            phrase.size <= tokens.size && tokens.windowed(phrase.size).any { it == phrase }
        }
        return when {
            englishRejectPattern.containsMatchIn(normalized) || hasArabicRejection ->
                VoiceApprovalIntent.REJECT
            normalized in affirmativeUtterances -> VoiceApprovalIntent.APPROVE
            else -> VoiceApprovalIntent.NONE
        }
    }
}
