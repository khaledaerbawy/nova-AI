package com.opendroid.ai.core.agent

import com.opendroid.ai.actions.ActionAutoMapper
import com.opendroid.ai.data.models.Plan
import com.opendroid.ai.data.models.PlanStep

/**
 * What the approval card and the spoken approval prompt tell the user before a plan
 * runs. Built from the steps themselves: a plan's goal, step descriptions and
 * estimatedSteps are text the model wrote about its own plan, and a plan that says
 * "check the weather, 1 step" can still send an SMS.
 */
object PlanApprovalSummary {

    private val ACRONYMS = setOf("SMS", "UPI", "URL", "PDF", "QR", "GPS", "NFC", "VPN")
    // Every key a send or call handler reads a recipient from (CommunicationActions,
    // ActionAutoMapper.RECIPIENT_PARAMS). Handlers differ in which key wins, so all of
    // a step's recipients are shown, not just the first.
    private val RECIPIENT_KEYS = listOf(
        "contact", "to", "recipient", "number", "phone", "phoneNumber", "username", "channel", "destination"
    )
    private val PATH_KEYS = listOf("filePath", "path")
    private const val MAX_VALUE_LENGTH = 40
    // The same resolver ActionDispatcher runs, so an alias is labelled as what it will do.
    private val autoMapper = ActionAutoMapper()

    /** One line per step that will run, in order: the action and who or what it acts on. */
    fun stepLines(plan: Plan): List<String> = plan.steps.map { describe(it, plan.steps) }

    fun stepCount(plan: Plan): String = if (plan.steps.size == 1) "1 step" else "${plan.steps.size} steps"

    fun spokenPrompt(plan: Plan): String {
        val count = when (plan.steps.size) {
            1 -> "خطوة واحدة"
            2 -> "خطوتين"
            in 3..10 -> "${plan.steps.size} خطوات"
            else -> "${plan.steps.size} خطوة"
        }
        val spokenSteps = plan.steps.joinToString("، ") { describeForSpeech(it, plan.steps) }
        return "جهزت خطة من $count: $spokenSteps. تحب أنفذها؟ قول موافق أو لا."
    }

    private fun describeForSpeech(step: PlanStep, steps: List<PlanStep>): String {
        val mapping = autoMapper.mapAction(step.action, step.params, emptySet())
        val action = mapping.mappedAction ?: step.action
        val params = if (mapping.mappedAction != null) mapping.mappedParams else step.params
        val recipients = RECIPIENT_KEYS.mapNotNull { params[it]?.takeIf(String::isNotBlank) }.distinct()
        val path = PATH_KEYS.firstNotNullOfOrNull { params[it]?.takeIf(String::isNotBlank) }
        val amount = params["amount"]?.takeIf(String::isNotBlank)
        return buildString {
            append(spokenActionLabel(action))
            if (recipients.isNotEmpty()) append(" لـ ").append(recipients.joinToString(" أو ") { value(it, steps) })
            if (path != null) append(" ").append(value(path, steps))
            if (amount != null) append(" بقيمة ").append(value(amount, steps))
        }
    }

    private fun spokenActionLabel(action: String): String = when (action.uppercase()) {
        "GET_WEATHER" -> "معرفة حالة الطقس"
        "GET_NEWS" -> "عرض الأخبار"
        "GET_SYSTEM_INFO" -> "عرض معلومات الجهاز"
        "OPEN_APP" -> "فتح تطبيق"
        "SEND_WHATSAPP" -> "إرسال رسالة واتساب"
        "SEND_TELEGRAM" -> "إرسال رسالة تيليجرام"
        "SEND_SMS" -> "إرسال رسالة نصية"
        "SEND_EMAIL" -> "إرسال بريد إلكتروني"
        "MAKE_CALL" -> "إجراء مكالمة"
        "PAY_UPI" -> "تنفيذ عملية دفع"
        "DELETE_FILE" -> "حذف ملف"
        "MOVE_FILE" -> "نقل ملف"
        "COPY_FILE" -> "نسخ ملف"
        "ZIP_FILES" -> "ضغط ملفات"
        "UNZIP_FILE" -> "فك ضغط ملفات"
        "TAKE_SCREENSHOT" -> "التقاط صورة للشاشة"
        "TOGGLE_FLASHLIGHT" -> "التحكم في الكشاف"
        "TOGGLE_WIFI" -> "التحكم في الواي فاي"
        "TOGGLE_BLUETOOTH" -> "التحكم في البلوتوث"
        "SET_REMINDER" -> "إضافة تذكير"
        "CREATE_CALENDAR_EVENT" -> "إضافة موعد للتقويم"
        "RUN_MACRO" -> "تشغيل ماكرو"
        "CREATE_MACRO" -> "إنشاء ماكرو"
        "SCHEDULE_MACRO" -> "جدولة ماكرو"
        "RUN_ROUTINE" -> "تشغيل روتين"
        "SOCIAL_CREATE_POST" -> "تجهيز مسودة منشور"
        "SOCIAL_SCHEDULE_POST" -> "جدولة منشور"
        "SOCIAL_CREATE_CAMPAIGN" -> "إنشاء حملة تسويقية"
        "SOCIAL_CHECK_INBOX" -> "مراجعة رسائل السوشيال"
        "SOCIAL_CREATE_REPORT" -> "إنشاء تقرير السوشيال"
        "ASK_USER" -> "سؤال توضيحي"
        else -> actionLabel(action)
    }

    private fun describe(step: PlanStep, steps: List<PlanStep>): String {
        val mapping = autoMapper.mapAction(step.action, step.params, emptySet())
        // A SKIP resolves to no action: the step is described as written.
        val action = mapping.mappedAction ?: step.action
        val params = if (mapping.mappedAction != null) mapping.mappedParams else step.params
        val recipients = RECIPIENT_KEYS.mapNotNull { params[it]?.takeIf(String::isNotBlank) }.distinct()
        val path = PATH_KEYS.firstNotNullOfOrNull { params[it]?.takeIf(String::isNotBlank) }
        // MOVE_FILE, COPY_FILE and ZIP_FILES read sourcePath and destPath/zipFilePath; UNZIP_FILE
        // reads zipFilePath and destDirPath. "from" and "to" keep the two ends apart.
        val zip = params["zipFilePath"]?.takeIf(String::isNotBlank)
        val source = params["sourcePath"]?.takeIf(String::isNotBlank)
            ?: zip?.takeIf { !params["destDirPath"].isNullOrBlank() }
        val destination = params["destPath"]?.takeIf(String::isNotBlank)
            ?: params["destDirPath"]?.takeIf(String::isNotBlank)
            ?: zip?.takeIf { !params["sourcePath"].isNullOrBlank() }
        val amount = params["amount"]?.takeIf(String::isNotBlank)
        return buildString {
            append(actionLabel(action))
            if (recipients.isNotEmpty()) append(" to ").append(recipients.joinToString(" or ") { value(it, steps) })
            if (path != null) append(" ").append(value(path, steps))
            if (source != null) append(" from ").append(value(source, steps))
            if (destination != null) append(" to ").append(value(destination, steps))
            if (amount != null) append(" (amount ").append(value(amount, steps)).append(")")
        }
    }

    private fun actionLabel(action: String): String {
        val words = action.split('_').filter(String::isNotBlank)
        if (words.isEmpty()) return "Unknown action"
        return words.mapIndexed { i, word ->
            val upper = word.uppercase()
            when {
                upper in ACRONYMS -> upper
                i == 0 -> upper.lowercase().replaceFirstChar(Char::uppercaseChar)
                else -> upper.lowercase()
            }
        }.joinToString(" ")
    }

    // "$stepId" is filled in from that step's result when the plan runs.
    private fun value(raw: String, steps: List<PlanStep>): String {
        if (raw.startsWith("$")) {
            steps.firstOrNull { it.stepId == raw.removePrefix("$") }?.let { return "the answer to step ${it.order}" }
        }
        return if (raw.length > MAX_VALUE_LENGTH) raw.take(MAX_VALUE_LENGTH - 1) + "…" else raw
    }
}
