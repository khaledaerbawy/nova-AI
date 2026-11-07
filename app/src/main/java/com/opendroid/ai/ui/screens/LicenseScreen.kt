package com.opendroid.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
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
fun LicenseScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "التراخيص",
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
                        .border(1.dp, AccentPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "الترخيص",
                            tint = AccentPurple,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ترخيص المصدر المفتوح",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Apache License 2.0",
                                fontSize = 12.sp,
                                color = AccentPurple
                            )
                        }
                    }
                }
            }

            item {
                PolicySection(
                    title = "ترخيص Apache 2.0",
                    content = "Copyright (c) 2026 OpenDroid Contributors\n" +
                            "Last Updated: August 18, 2026\n\n" +
                            "Licensed under the Apache License, Version 2.0 (the \"License\"); " +
                            "you may not use this file except in compliance with the License.\n" +
                            "You may obtain a copy of the License at:\n\n" +
                            "    http://www.apache.org/licenses/LICENSE-2.0\n\n" +
                            "Unless required by applicable law or agreed to in writing, software " +
                            "distributed under the License is distributed on an \"AS IS\" BASIS, " +
                            "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. " +
                            "See the License for the specific language governing permissions and " +
                            "limitations under the License."
                )
            }

            item {
                PolicySection(
                    title = "تراخيص المكتبات الخارجية",
                    content = "تستخدم NOVA المكتبات مفتوحة المصدر التالية:\n\n" +
                            "• Jetpack Compose — Apache License 2.0\n" +
                            "• Dagger/Hilt — Apache License 2.0\n" +
                            "• Room Database — Apache License 2.0\n" +
                            "• OkHttp & Retrofit — Apache License 2.0\n" +
                            "• Kotlin Serialization — Apache License 2.0\n" +
                            "• Coil Image Loading — Apache License 2.0\n" +
                            "• Lottie Animations — Apache License 2.0\n" +
                            "• DataStore Preferences — Apache License 2.0"
                )
            }

            item {
                PolicySection(
                    title = "المساهمة",
                    content = "NOVA مشروع يرحب بمساهمات المجتمع. عند تقديم كود أو توثيق أو مواد أخرى، " +
                            "تخضع مساهمتك لشروط الترخيص وإرشادات المساهمة المعتمدة في المستودع.\n\n" +
                            "نرحب بجميع أنواع المساهمات:\n\n" +
                            "• الإبلاغ عن الأخطاء واقتراح المميزات\n" +
                            "• المساهمة بالكود عبر طلبات الدمج\n" +
                            "• تحسين التوثيق والشروحات\n" +
                            "• الترجمة والتعريب\n\n" +
                            "راجع ملف CONTRIBUTING.md في المستودع لمعرفة إرشادات المساهمة."
                )
            }

            item {
                PolicySection(
                    title = "شكر وتقدير",
                    content = "تم تطوير NOVA بالاستفادة من مجتمع المصدر المفتوح.\n\n" +
                            "شكراً لكل المساهمين الذين ساعدوا في تطوير هذا المشروع. " +
                            "يمكن الاطلاع على قائمة المساهمين في مستودع GitHub."
                )
            }
        }
    }
}
