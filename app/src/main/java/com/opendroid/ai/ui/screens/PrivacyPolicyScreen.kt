package com.opendroid.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "سياسة الخصوصية",
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
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "الخصوصية",
                            tint = AccentCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "خصوصيتك مهمة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Last updated: August 2026",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Section: Overview
            item {
                PolicySection(
                    title = "1. OVERVIEW",
                    content = "NOVA مساعد ذكاء اصطناعي مستقل يعمل على جهاز أندرويد. " +
                            "نلتزم بحماية خصوصيتك وتوضيح كيفية التعامل مع بياناتك. " +
                            "توضح هذه السياسة البيانات التي يتعامل معها التطبيق وكيفية استخدامها وحقوقك المتعلقة بها."
                )
            }

            // Section: Data Collection
            item {
                PolicySection(
                    title = "2. DATA COLLECTION",
                    content = "تعالج NOVA البيانات التالية محلياً على جهازك:\n\n" +
                            "• الأوامر الصوتية والاستفسارات النصية التي تقدمها\n" +
                            "• حالة الجهاز مثل البطارية والواي فاي والاتصال\n" +
                            "• أسماء جهات الاتصال لتنفيذ إجراءات التواصل\n" +
                            "• سجل المحادثات لتقديم ردود تراعي السياق\n" +
                            "• تفضيلاتك والمعلومات التي تختار حفظها في الذاكرة\n\n" +
                            "تُخزّن البيانات في قاعدة بيانات Room محلية على جهازك. لا يجمع مطورو NOVA هذه البيانات."
                )
            }

            // Section: LLM Providers
            item {
                PolicySection(
                    title = "3. THIRD-PARTY LLM PROVIDERS",
                    content = "ترسل NOVA استفساراتك إلى مزود الذكاء الاصطناعي الذي تختاره مثل Google Gemini أو OpenAI أو Anthropic أو Groq " +
                            "لإنشاء الردود وخطط التنفيذ. ولكل مزود سياسة خصوصية خاصة به تحدد طريقة تعامله مع بياناتك.\n\n" +
                            "• تُخزّن مفاتيح API محلياً على جهازك ولا تُرسل إلى خوادم NOVA.\n" +
                            "• تخضع الاستفسارات المرسلة إلى مزودي الذكاء الاصطناعي لسياسات الخصوصية الخاصة بهم.\n" +
                            "• يمكنك تغيير المزود أو استخدام نماذج محلية مثل Ollama لإبقاء المعالجة على جهازك."
                )
            }

            // Section: Permissions
            item {
                PolicySection(
                    title = "4. DEVICE PERMISSIONS",
                    content = "تطلب NOVA الصلاحيات التالية لتشغيل ميزاتها:\n\n" +
                            "• الميكروفون — لاستقبال الأوامر الصوتية\n" +
                            "• إمكانية الوصول — للتفاعل مع التطبيقات وأتمتة بعض الإجراءات\n" +
                            "• جهات الاتصال — للوصول إلى أرقام الأشخاص الذين تحددهم\n" +
                            "• الهاتف — لإجراء المكالمات\n" +
                            "• الرسائل — لإرسال الرسائل النصية\n" +
                            "• الكاميرا — لالتقاط الصور والتحكم في الكشاف\n" +
                            "• التخزين — لإدارة الملفات\n" +
                            "• الموقع — لخدمات الطقس والاتجاهات\n\n" +
                            "كل الصلاحيات اختيارية. الميزات التي تحتاج إلى صلاحية غير ممنوحة ستطلب تفعيلها أو لن تعمل."
                )
            }

            // Section: Accessibility Service Declaration
            item {
                PolicySection(
                    title = "4b. ACCESSIBILITY SERVICE",
                    content = "تستخدم NOVA خدمة إمكانية الوصول في أندرويد لأتمتة التفاعل مع التطبيقات " +
                            "(مثل إرسال رسائل واتساب) عندما تطلب ذلك صراحةً.\n\n" +
                            "تعمل خدمة إمكانية الوصول وفق الضوابط التالية:\n\n" +
                            "• لا تعمل إلا بعد تفعيلها من إعدادات أندرويد\n" +
                            "• تنفذ الإجراءات عندما تطلب من NOVA ذلك\n" +
                            "• لا تنفذ إجراءات في الخلفية دون أمرك\n" +
                            "• لا تقرأ كلمات المرور أو المعلومات البنكية\n" +
                            "• لا تسجل محتوى الشاشة بشكل سلبي\n" +
                            "• لا تجمع أو تحفظ أو ترسل إلى خوادم خارجية بيانات تمت ملاحظتها عبر الخدمة\n\n" +
                            "تُستخدم الخدمة فقط لتنفيذ الإجراءات التي تطلبها على الشاشة، مثل الضغط على الأزرار أو الكتابة داخل التطبيقات. " +
                            "يمكنك إلغاء صلاحية إمكانية الوصول في أي وقت من إعدادات أندرويد ← إمكانية الوصول ← NOVA."
                )
            }

            // Section: Data Storage
            item {
                PolicySection(
                    title = "5. DATA STORAGE & RETENTION",
                    content = "• يُحفظ سجل المحادثات والذاكرة وسجل المهام في قاعدة بيانات SQLite محلية على جهازك.\n" +
                            "• تدعم عناصر الذاكرة مدة صلاحية (TTL) وتُمسح تلقائياً عند انتهائها.\n" +
                            "• يظل سجل التنفيذ محفوظاً حتى تمسحه من سجل النظام أو تحذف التطبيق.\n" +
                            "• حفظ المهمة كأمر مخصص يتم بطلب منك؛ وتُستبعد مفاتيح API والرموز وكلمات المرور من التسجيل.\n" +
                            "• يمكنك مسح أي نوع من الذاكرة من شاشة الذاكرة.\n" +
                            "• يؤدي حذف التطبيق إلى إزالة البيانات المخزنة محلياً."
                )
            }

            // Section: Data Sharing
            item {
                PolicySection(
                    title = "6. DATA SHARING",
                    content = "لا تقوم NOVA بما يلي:\n\n" +
                            "• بيع بياناتك الشخصية أو تأجيرها أو مشاركتها مع أطراف خارجية\n" +
                            "• جمع بيانات التحليلات أو إحصاءات الاستخدام\n" +
                            "• إرسال البيانات إلى خوادم يملكها فريق NOVA\n" +
                            "• عرض إعلانات أو استخدام تقنيات تتبع إعلاني\n\n" +
                            "يحدث الإرسال الخارجي فقط عند إرسال استفساراتك إلى مزود الذكاء الاصطناعي الذي قمت بإعداده."
                )
            }

            // Section: Security
            item {
                PolicySection(
                    title = "7. SECURITY",
                    content = "• تُخزّن مفاتيح API بتشفير AES-256-GCM باستخدام Android Keystore على جهازك.\n" +
                            "• يُشفّر اسم ملفك الشخصي وتاريخ ميلادك بالطريقة نفسها باستخدام مفتاح منفصل.\n" +
                            "• لا تغادر مفاتيح التشفير مخزن المفاتيح المحمي بالجهاز، لذلك لا يمكن قراءة البيانات المشفرة على جهاز آخر أو استعادتها من نسخة احتياطية.\n" +
                            "• إذا أصبح المفتاح غير صالح، لا يمكن استعادة القيم المتأثرة ولا تُحفظ دون تشفير؛ سيطلب منك التطبيق إدخالها مجدداً.\n" +
                            "• تُحفظ الإعدادات العامة غير الشخصية، مثل اكتمال الإعداد الأولي، في مساحة التطبيق الخاصة دون تشفير.\n" +
                            "• تستخدم اتصالات API تشفير HTTPS.\n" +
                            "• لا تعمل خدمة إمكانية الوصول إلا بعد تفعيلها منك.\n" +
                            "• تتطلب الإجراءات الخطيرة مثل إعادة تشغيل الجهاز أو حذف الملفات موافقتك."
                )
            }

            // Section: Children
            item {
                PolicySection(
                    title = "8. CHILDREN'S PRIVACY",
                    content = "لا يُنصح باستخدام NOVA للأطفال دون 13 عاماً. " +
                            "ولا نجمع عن قصد معلومات من الأطفال."
                )
            }

            // Section: Changes
            item {
                PolicySection(
                    title = "9. CHANGES TO THIS POLICY",
                    content = "We may update this privacy policy from time to time. Any changes will be reflected in the app with an updated \"Last updated\" date. " +
                            "استمرارك في استخدام NOVA بعد التعديلات يعني موافقتك على السياسة المحدّثة."
                )
            }

            // Section: Contact
            item {
                PolicySection(
                    title = "10. CONTACT",
                    content = "إذا كانت لديك أسئلة حول سياسة الخصوصية أو طريقة تعامل NOVA مع البيانات، " +
                            "يمكنك التواصل مع فريق التطوير عبر مستودع المشروع على GitHub أو مجتمع Discord."
                )
            }
        }
    }
}

@Composable
internal fun PolicySection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AccentCyan
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}
