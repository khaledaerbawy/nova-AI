package com.opendroid.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Arrowرجوع
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendroid.ai.core.llm.ConnectionاختبارPlanner
import com.opendroid.ai.core.llm.ConnectionاختبارState
import com.opendroid.ai.core.llm.error.LLMError
import com.opendroid.ai.ui.theme.*
import com.opendroid.ai.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    viewModel: SettingsViewModel,
    onNavigateرجوع: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.llmConfig.collectAsState()
    val connectionResults by viewModel.connectionResults.collectAsState()
    val batchProgress by viewModel.connectionBatchProgress.collectAsState()
    var showConfirm by remember { mutableStateOf(false) }

    val providers = ConnectionاختبارPlanner.cloudProviders()
        .filter { it != "متوافق مع OpenAI" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اختبار أداء الذكاء الاصطناعي",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateرجوع) {
                        Icon(Icons.Default.Arrowرجوع, contentDescription = "رجوع", tint = TextPrimary)
                    }
                },
                actions = {
                    if (batchProgress != null) {
                        TextButton(onClick = { viewModel.cancelConnectionاختبارs() }) {
                            Text("إلغاء", fontSize = 11.sp, color = AccentRed)
                        }
                    } else {
                        Button(
                            onClick = { showConfirm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextPrimary,
                                contentColor = Darkرجوعground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "بدء الاختبار",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اختبار كل الخدمات المعدّة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Darkرجوعground)
            )
        },
        containerColor = Darkرجوعground,
        modifier = modifier
    ) { padding ->
        if (showConfirm) {
            val configuredCount = ConnectionاختبارPlanner.configuredProviders(config).size
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                title = { Text("اختبار كل الخدمات المعدّة?") },
                text = {
                    Text(
                        "سيتم إرسال $configuredCount طلبات متتالية إلى مزودي الخدمة. " +
                            "قد تُحتسب رسوم من مزود الخدمة."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirm = false
                            viewModel.testAllConfigured()
                        }
                    ) { Text("متابعة") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) { Text("إلغاء") }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Cardرجوعground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ملخص تقرير التشخيص",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = batchProgress?.let {
                                "اختبار ${it.index} من ${it.total}: ${it.provider}"
                            } ?: "تستخدم اختبارات الاتصال النموذج المحدد لكل مزود. " +
                                "تظهر المفاتيح الناقصة كأخطاء إعداد واضحة بدلاً من تخطيها.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            items(providers) { provider ->
                val result = connectionResults[provider]
                val legacyLatency = config.latencyBenchmarks[provider]
                ProviderConnectionRow(
                    providerName = provider,
                    state = result,
                    legacyLatencyMs = legacyLatency,
                    onاختبار = { viewModel.testConnection(provider) }
                )
            }
        }
    }
}

@Composable
fun ProviderConnectionRow(
    providerName: String,
    state: ConnectionاختبارState?,
    legacyLatencyMs: Long?,
    onاختبار: () -> Unit
) {
    val statusText = when (state) {
        is ConnectionاختبارState.اختبارing -> "جاري الاختبار…"
        is ConnectionاختبارState.Connected -> "متصل · ${state.latencyMs} مللي ثانية · ${state.model}"
        is ConnectionاختبارState.فشل -> connectionFailureLabel(state.error)
        is ConnectionاختبارState.ConfigMissing -> when (state.reason) {
            LLMError.AuthMissing -> "مطلوب مفتاح API"
            else -> "مطلوب إكمال الإعدادات"
        }
        else -> legacyLatencyMs?.takeIf { it > 0 && it != 9999L }?.let { "آخر زمن استجابة $it مللي ثانية" }
            ?: "لم يتم الاختبار"
    }
    val barColor = when (state) {
        is ConnectionاختبارState.Connected -> AccentCyan
        is ConnectionاختبارState.فشل, is ConnectionاختبارState.ConfigMissing -> AccentRed
        is ConnectionاختبارState.اختبارing -> AccentCyan
        else -> BorderColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Cardرجوعground)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = providerName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = onاختبار) {
                    Text("اختبار", fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BorderColor)
            ) {
                val fraction = when (state) {
                    is ConnectionاختبارState.Connected ->
                        (state.latencyMs / 3000f).coerceIn(0.1f, 1f)
                    is ConnectionاختبارState.فشل, is ConnectionاختبارState.ConfigMissing -> 1f
                    is ConnectionاختبارState.اختبارing -> 0.35f
                    else -> 0.05f
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }
        }
    }
}

private fun connectionFailureLabel(error: LLMError): String = when (error) {
    LLMError.AuthMissing -> "مطلوب مفتاح API"
    LLMError.AuthInvalid -> "مفتاح غير صالح"
    LLMError.QuotaExhausted -> "تم استهلاك الحصة"
    LLMError.RateLimited -> "طلبات كثيرة في وقت قصير"
    LLMError.ModelUnavailable -> "النموذج غير متاح"
    LLMError.RequestInvalid -> "طلب غير صالح"
    LLMError.Network -> "خطأ في الشبكة"
    LLMError.ServerError -> "خطأ في الخادم"
    LLMError.MalformedResponse -> "استجابة غير مفهومة"
    LLMError.SafeFallbackUnavailable -> "لا يوجد بديل آمن"
    LLMError.ProviderUnrecognized -> "مزود غير معروف"
    LLMError.Unknown -> "فشل"
}
