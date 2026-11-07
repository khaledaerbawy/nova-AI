package com.opendroid.ai.core.agent

/** Offline-first recognition of common Egyptian Arabic device/action requests. */
object ArabicCommandHeuristics {
    private val actionPhrases = listOf(
        "افتح", "افتحلي", "شغل", "شغّل", "اقفل", "اطفي", "طفي", "فعّل", "الغاء",
        "ابعت", "ابعث", "ارسل", "رسالة", "واتساب", "تيليجرام", "اتصل", "كلم", "رن",
        "اتصال", "منبه", "مؤقت", "تايمر", "تذكير", "فكرني", "احفظ", "افتكر",
        "احذف", "امسح", "دور", "ابحث", "صور", "لقطة شاشة", "سكرين شوت", "سطوع",
        "الصوت", "علي الصوت", "وطي الصوت", "واي فاي", "وايفاي", "بلوتوث", "فلاش",
        "ماكرو", "روتين", "نفذ الروتين", "شغل الروتين", "انشر", "بوست", "منشور",
        "السوشيال", "الرسائل", "الانبوكس", "جدول المنشور", "اعمل منشور"
    )

    fun requiresAction(query: String): Boolean {
        val normalized = query.lowercase().replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
        return actionPhrases.any { normalized.contains(it) }
    }
}
