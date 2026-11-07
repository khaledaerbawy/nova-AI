package com.opendroid.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendroid.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مركز المساعدة",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "مساعدة",
                            tint = AccentCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "إزاي نقدر نساعدك؟",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "إجابات سريعة لأهم الأسئلة",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            item {
                PolicySection(
                    title = "البدء مع نوفا",
                    content = "1. افتح NOVA وأكمل خطوات الإعداد الأولي\n" +
                            "2. فعّل الصلاحيات التي تحتاجها مثل الميكروفون وإمكانية الوصول\n" +
                            "3. افتح الإعدادات وأدخل مفتاح API لمزود الذكاء الاصطناعي\n" +
                            "4. ابدأ التحدث أو كتابة أوامرك!\n\n" +
                            "ملحوظة: يمكنك استخدام Google Gemini والحصول على مفتاح مجاني من ai.google.dev"
                )
            }

            item {
                PolicySection(
                    title = "الأوامر الصوتية",
                    content = "OpenDroid listens for the wake word \"Hey OpenDroid\" when the app is running.\n\n" +
                            "أمثلة لأوامر يمكنك قولها:\n\n" +
                            "• \"Send a WhatsApp message to Mom saying I'll be late\"\n" +
                            "• \"Set an alarm for 7 AM tomorrow\"\n" +
                            "• \"Turn on the flashlight\"\n" +
                            "• \"What's the weather like today?\"\n" +
                            "• \"Play some music on Spotify\"\n" +
                            "• \"Call John\"\n" +
                            "• \"Take a photo\""
                )
            }

            item {
                PolicySection(
                    title = "إعداد مفاتيح API",
                    content = "تحتاج NOVA إلى مفتاح API لمزود ذكاء اصطناعي حتى تتمكن من إنشاء الردود:\n\n" +
                            "1. افتح الإعدادات ← مفاتيح خدمات الذكاء الاصطناعي\n" +
                            "2. أدخل مفتاح API الخاص بالخدمة التي تريد استخدامها\n" +
                            "3. Select that provider from the \"Active Brain Provider\" dropdown\n\n" +
                            "الخدمات المدعومة:\n" +
                            "• Google Gemini (مناسب للمبتدئين)\n" +
                            "• OpenAI\n" +
                            "• Anthropic Claude\n" +
                            "• Groq (استجابة سريعة)\n" +
                            "• Mistral AI وOpenRouter وTogether AI وCohere وDeepSeek وOpencode-Go\n" +
                            "• Ollama (يعمل محلياً ولا يحتاج مفتاح API)"
                )
            }

            item {
                PolicySection(
                    title = "خدمة إمكانية الوصول",
                    content = "تسمح خدمة إمكانية الوصول لـ NOVA بالنقر على الأزرار والكتابة داخل التطبيقات الأخرى، مثل إرسال رسائل واتساب بناءً على طلبك.\n\n" +
                            "لتفعيلها:\n" +
                            "1. افتح إعدادات أندرويد ← إمكانية الوصول\n" +
                            "2. Find \"OpenDroid\" in Installed Services\n" +
                            "3. فعّل الخدمة\n\n" +
                            "Note: This is optional. Without it, OpenDroid will still open apps but may need you to tap the final \"Send\" button."
                )
            }

            item {
                PolicySection(
                    title = "الأوامر والأتمتة",
                    content = "يمكنك إنشاء أوامر مخصصة لتنفيذ عدة إجراءات بالترتيب:\n\n" +
                            "• افتح تبويب الأوامر المخصصة\n" +
                            "• أنشئ أمراً جديداً وحدد اسمه وخطواته\n" +
                            "• جدْول الأوامر باستخدام تعبيرات cron لتشغيلها في أوقات محددة\n\n" +
                            "Example: Create a \"Good Morning\" macro that turns on lights, reads the weather, and plays your favorite playlist."
                )
            }

            item {
                PolicySection(
                    title = "نظام الذاكرة",
                    content = "تحتوي NOVA على أربعة أنواع من الذاكرة:\n\n" +
                            "• الذاكرة المؤقتة — سياق الجلسة الحالية (تُمسح تلقائياً)\n" +
                            "• ذاكرة المحادثات — سجل المحادثات السابقة\n" +
                            "• الذاكرة الدلالية — معلوماتك وتفضيلاتك\n" +
                            "• الذاكرة الإجرائية — أنماط المهام التي تعلمتها NOVA\n\n" +
                            "يمكنك مراجعة أي نوع من الذاكرة أو مسحه من تبويب الذاكرة."
                )
            }

            item {
                PolicySection(
                    title = "حل المشكلات",
                    content = "\"OpenDroid isn't responding\"\n" +
                            "← تأكد من صحة مفتاح API ومن اتصالك بمزود الخدمة.\n\n" +
                            "\"Voice commands don't work\"\n" +
                            "← تأكد من منح إذن الميكروفون وأن خدمة الاستماع تعمل.\n\n" +
                            "\"WhatsApp messages aren't sending automatically\"\n" +
                            "← فعّل خدمة إمكانية الوصول من إعدادات أندرويد.\n\n" +
                            "\"App crashes on startup\"\n" +
                            "← تحقق من مساحة التخزين ثم أعد إدخال الإعدادات. قد تحتاج إلى إدخال مفاتيح API مرة أخرى إذا تعذر فك تشفيرها."
                )
            }

            item {
                PolicySection(
                    title = "التواصل والدعم",
                    content = "• GitHub: أبلغ عن الأخطاء واقترح المميزات من خلال مستودع المشروع\n" +
                            "• Discord: انضم إلى مجتمع المستخدمين للحصول على المساعدة والنقاش\n" +
                            "• الأمان: أبلغ عن الثغرات بشكل خاص عبر صفحة التنبيهات الأمنية في المستودع، وليس في البلاغات العامة أو Discord\n\n" +
                            "نوفا مشروع مفتوح المصدر ونرحب بالمساهمات والتطوير."
                )
            }
        }
    }
}
