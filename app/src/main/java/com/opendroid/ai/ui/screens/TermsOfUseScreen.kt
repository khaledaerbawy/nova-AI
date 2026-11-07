package com.opendroid.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
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
fun TermsOfUseScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "شروط الاستخدام",
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
                            imageVector = Icons.Default.Description,
                            contentDescription = "الشروط",
                            tint = AccentCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "شروط الاستخدام",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Effective: May 2026",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            item {
                PolicySection(
                    title = "1. ACCEPTANCE OF TERMS",
                    content = "باستخدامك لتطبيق NOVA أو تثبيته، فإنك توافق على شروط الاستخدام التالية. " +
                            "إذا لم توافق على هذه الشروط، يُرجى عدم استخدام التطبيق.\n\n" +
                            "NOVA مساعد ذكي مستقل ومفتوح المصدر لنظام أندرويد. تنظم هذه الشروط استخدامك للتطبيق والخدمات المرتبطة به."
                )
            }

            item {
                PolicySection(
                    title = "2. PERMITTED USE",
                    content = "يمكنك استخدام NOVA للأغراض الشخصية وغير التجارية، ومنها:\n\n" +
                            "• أتمتة مهام الجهاز مثل الرسائل والمكالمات والمنبهات\n" +
                            "• إدارة أجهزة المنزل الذكي\n" +
                            "• البحث على الإنترنت واسترجاع المعلومات\n" +
                            "• إدارة الملفات والتحكم في الجهاز\n" +
                            "• تنفيذ الأوامر الصوتية\n\n" +
                            "وتتعهد بعدم استخدام NOVA في:\n\n" +
                            "• مخالفة القوانين أو اللوائح\n" +
                            "• مضايقة الآخرين أو إزعاجهم أو الإضرار بهم\n" +
                            "• تجاوز حماية الأجهزة أو الوصول إلى أنظمة دون تصريح\n" +
                            "• التدخل في التطبيقات الأخرى بطريقة ضارة"
                )
            }

            item {
                PolicySection(
                    title = "3. API KEYS & THIRD-PARTY SERVICES",
                    content = "تتصل NOVA بمزودي الذكاء الاصطناعي الخارجيين مثل Google Gemini وOpenAI وAnthropic باستخدام مفاتيح API التي تقدمها.\n\n" +
                            "• أنت مسؤول عن الحصول على مفاتيح API وإدارتها.\n" +
                            "• يخضع استخدام مفاتيح API لشروط مزود الخدمة المعني.\n" +
                            "• لا تتحمل NOVA مسؤولية الرسوم التي يفرضها مزودو الخدمات الخارجية.\n" +
                            "• تُخزّن مفاتيح API محلياً على جهازك بتشفير AES-256 ولا تُرسل إلى خوادم NOVA."
                )
            }

            item {
                PolicySection(
                    title = "4. ACCESSIBILITY SERVICE",
                    content = "تستخدم NOVA خدمة إمكانية الوصول في أندرويد لتنفيذ إجراءات على الشاشة نيابةً عنك. " +
                            "بتفعيل هذه الخدمة، فإنك تقر بأن:\n\n" +
                            "• الخدمة يمكنها التفاعل مع التطبيقات الأخرى على جهازك\n" +
                            "• لا تنفذ أي إجراء إلا عندما تطلب منها ذلك صراحةً\n" +
                            "• يمكنك إيقافها في أي وقت من إعدادات أندرويد\n" +
                            "• لا تستخدم NOVA هذه الخدمة لجمع البيانات أو إرسالها"
                )
            }

            item {
                PolicySection(
                    title = "5. DISCLAIMER OF WARRANTIES",
                    content = "OpenDroid is provided \"AS IS\" without warranties of any kind, either express or implied.\n\n" +
                            "• لا نضمن التشغيل دون انقطاع أو أخطاء.\n" +
                            "• قد تكون ردود الذكاء الاصطناعي غير دقيقة أو غير مكتملة.\n" +
                            "• قد لا تنفذ الإجراءات التلقائية كما هو متوقع في جميع الحالات.\n" +
                            "• تتحمل مسؤولية المخاطر المرتبطة باستخدام التطبيق."
                )
            }

            item {
                PolicySection(
                    title = "6. LIMITATION OF LIABILITY",
                    content = "إلى أقصى حد يسمح به القانون، لا يتحمل مطورو NOVA المسؤولية عن أي " +
                            "أضرار مباشرة أو غير مباشرة أو عرضية أو خاصة أو تبعية ناتجة عن:\n\n" +
                            "• استخدام التطبيق أو عدم القدرة على استخدامه\n" +
                            "• الوصول غير المصرح به إلى بياناتك\n" +
                            "• الإجراءات التي ينفذها المساعد الذكي\n" +
                            "• أعطال الخدمات الخارجية أو الرسوم التي تفرضها"
                )
            }

            item {
                PolicySection(
                    title = "7. OPEN SOURCE",
                    content = "NOVA برنامج مفتوح المصدر. يمكنك الاطلاع على الكود المصدري وتعديله وتوزيعه " +
                            "وفقاً لشروط الترخيص. ونرحب بالمساهمات التي تخضع لإرشادات المساهمة الخاصة بالمشروع."
                )
            }

            item {
                PolicySection(
                    title = "8. CHANGES TO TERMS",
                    content = "قد نحدّث هذه الشروط من وقت لآخر، وستظهر التعديلات داخل التطبيق مع تاريخ سريان محدث. " +
                            "استمرارك في استخدام NOVA بعد التعديلات يعني موافقتك على الشروط المحدّثة."
                )
            }

            item {
                PolicySection(
                    title = "9. CONTACT",
                    content = "للاستفسارات حول شروط الاستخدام، يمكنك التواصل مع فريق التطوير عبر مستودع المشروع على GitHub أو مجتمع Discord."
                )
            }
        }
    }
}
