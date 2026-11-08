package com.opendroid.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.opendroid.ai.data.models.AutoMode
import com.opendroid.ai.data.models.LLMConfig
import com.opendroid.ai.data.models.OllamaTimeout
import com.opendroid.ai.data.models.effectiveGrantedActions
import com.opendroid.ai.data.models.resolvedAutoMode
import com.opendroid.ai.data.models.resolvedOllamaTimeoutSeconds
import com.opendroid.ai.core.llm.OnDeviceModelRegistry
import com.opendroid.ai.core.llm.OnDeviceBackend
import com.opendroid.ai.core.llm.ConnectionTestState
import com.opendroid.ai.core.llm.error.LLMError
import com.opendroid.ai.core.security.ProviderCredentialRecoveryState
import com.opendroid.ai.core.service.McpConfigRecoveryState
import com.opendroid.ai.data.repository.ProviderCredentialPersistenceState
import com.google.mlkit.genai.prompt.*
import com.google.mlkit.genai.common.FeatureStatus
import com.opendroid.ai.ui.theme.*
import com.opendroid.ai.ui.viewmodel.SettingsViewModel
import com.opendroid.ai.data.db.entities.ModelEntity
import com.opendroid.ai.data.db.entities.ModelStatus
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToBenchmark: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTermsOfUse: () -> Unit = {},
    onNavigateToHelpCenter: () -> Unit = {},
    onNavigateToLicense: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAutoReply: () -> Unit = {},
    onNavigateToNotificationHistory: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToCrashLog: () -> Unit = {},
    onNavigateToRoutines: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.llmConfig.collectAsState()
    val connectionResults by viewModel.connectionResults.collectAsState()
    val dbModels by viewModel.allModels.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    val hfToken by viewModel.huggingFaceToken.collectAsState()
    val providerCredentialRecoveryState by viewModel.providerCredentialRecoveryState.collectAsState()
    val providerCredentialPersistenceState by viewModel.providerCredentialPersistenceState.collectAsState()
    val mcpConfigRecoveryState by viewModel.mcpConfigRecoveryState.collectAsState()
    val mcpResetFailed by viewModel.mcpResetFailed.collectAsState()

    val providers = listOf(
        "Google Gemini",
        "OpenAI",
        "Anthropic Claude",
        "Groq",
        "Mistral AI",
        "OpenRouter",
        "Together AI",
        "Cohere",
        "DeepSeek",
        "Copilot API",
        "Opencode-Go",
        "Custom OpenAI Compatible",
        "Ollama",
        "On-Device AI"
    )

    var providerDropdownExpanded by remember { mutableStateOf(false) }
    var keysSectionExpanded by remember { mutableStateOf(false) }
    var voiceSectionExpanded by remember { mutableStateOf(false) }
    var planningSectionExpanded by remember { mutableStateOf(false) }

    var showAuthRequiredDialog by remember { mutableStateOf<String?>(null) }
    var licenseUrlForDialog by remember { mutableStateOf("") }
    var showCellularWarningDialog by remember { mutableStateOf<String?>(null) }
    var pendingCellularResumeModelId by remember { mutableStateOf<String?>(null) }
    var activeImportModelId by remember { mutableStateOf<String?>(null) }
    var importAsCustomModel by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var assistantNameDraft by remember(config.assistantDisplayName) { mutableStateOf(config.assistantDisplayName) }
    var wakeWordDraft by remember(config.wakeWordPhrase) { mutableStateOf(config.wakeWordPhrase) }
    var wakeWordTestResult by remember { mutableStateOf<String?>(null) }
    val wakeWordTestLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val transcript = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        wakeWordTestResult = if (result.resultCode == android.app.Activity.RESULT_OK &&
            com.opendroid.ai.core.voice.VoiceLanguagePolicy.containsWakePhrase(transcript, wakeWordDraft)
        ) {
            "تم التعرف على كلمة التنبيه: $transcript"
        } else if (transcript.isNotBlank()) {
            "لم تتطابق العبارة المسموعة ($transcript) مع كلمة التنبيه الحالية."
        } else {
            "لم يتم التقاط صوت. جرّب مرة أخرى وتأكد من إذن الميكروفون."
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            when {
                importAsCustomModel -> viewModel.importCustomLocalModel(uri)
                activeImportModelId != null -> viewModel.importLocalModel(activeImportModelId!!, uri)
            }
        }
        activeImportModelId = null
        importAsCustomModel = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تفضيلات المساعد",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (providerCredentialRecoveryState == ProviderCredentialRecoveryState.CredentialsMustBeReentered) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentOrange, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "لازم تدخل بيانات الدخول تاني",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "تعذر قراءة بيانات الدخول المحفوظة على هذا الجهاز، لذلك " +
                                    "لن يتم حفظ مفاتيح API الجديدة. باقي الإعدادات ستُحفظ. " +
                                    "امسح السجلات غير المتاحة، ثم أدخل مفاتيح API مرة أخرى.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = viewModel::resetProviderCredentialsForReentry,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                            ) {
                                Text("مسح بيانات الدخول غير المتاحة", color = AppTheme.colors.onAccent)
                            }
                        }
                    }
                }
            }

            if (mcpConfigRecoveryState == McpConfigRecoveryState.SecretsMustBeReentered) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentOrange, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "إعداد الاتصال الخارجي محتاج يتعمل من جديد",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "تعذر قراءة رمز MCP والخوادم المحفوظة على " +
                                    "هذا الجهاز، لذلك تم إيقاف طلبات MCP. إعادة الضبط ستحذفها؛ " +
                                    "أعد ربط عميل MCP وأضف الخوادم مرة أخرى.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            if (mcpResetFailed) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "فشلت إعادة الضبط. حاول مرة أخرى.",
                                    fontSize = 12.sp,
                                    color = AccentOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = viewModel::resetMcpConfigForReentry,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                            ) {
                                Text("إعادة إعداد الاتصال الخارجي", color = AppTheme.colors.onAccent)
                            }
                        }
                    }
                }
            }

            if (providerCredentialPersistenceState ==
                ProviderCredentialPersistenceState.StorageUnavailable
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentOrange, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "الإعدادات ما اتحفظتش",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "مساحة تخزين التطبيق غير متاحة. تم الحفاظ على الإعدادات وبيانات الدخول " +
                                    "الحالية؛ تحقق من مساحة الجهاز وحاول مرة أخرى.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Active LLM Provider Selection Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "مزود الذكاء الاصطناعي الحالي",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Dropdown menu trigger
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable { providerDropdownExpanded = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (config.activeProvider) {
                                        "On-Device AI", "Gemma 4 (On-device)" -> "ذكاء اصطناعي على الجهاز"
                                        "Custom OpenAI Compatible" -> "متوافق مع OpenAI"
                                        else -> config.activeProvider
                                    },
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "قائمة الاختيارات",
                                    tint = TextPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = providerDropdownExpanded,
                                onDismissRequest = { providerDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(CardBackground)
                                    .border(1.dp, BorderColor)
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "ذكاء اصطناعي على الجهاز", 
                                            color = AccentCyan, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 11.sp, 
                                            fontFamily = FontFamily.Monospace
                                        ) 
                                    },
                                    enabled = false,
                                    onClick = {}
                                )
                                DropdownMenuItem(
                                    text = { Text("ذكاء اصطناعي على الجهاز", color = TextPrimary, modifier = Modifier.padding(start = 8.dp)) },
                                    onClick = {
                                        viewModel.updateActiveProvider("On-Device AI")
                                        providerDropdownExpanded = false
                                    }
                                )
                                
                                Divider(color = BorderColor, thickness = 1.dp)

                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "ذكاء اصطناعي سحابي", 
                                            color = AccentCyan, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 11.sp, 
                                            fontFamily = FontFamily.Monospace
                                        ) 
                                    },
                                    enabled = false,
                                    onClick = {}
                                )
                                val cloudProvidersList = providers.filter { it != "On-Device AI" }
                                cloudProvidersList.forEach { name ->
                                    val displayName = when (name) {
                                        "Custom OpenAI Compatible" -> "متوافق مع OpenAI"
                                        "Google Gemini" -> "Gemini"
                                        "Anthropic Claude" -> "Claude"
                                        else -> name
                                    }
                                    DropdownMenuItem(
                                        text = { Text(displayName, color = TextPrimary, modifier = Modifier.padding(start = 8.dp)) },
                                        onClick = {
                                            viewModel.updateActiveProvider(name)
                                            providerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        val modelsLoading by viewModel.modelsLoading.collectAsState()
                        val modelFetchNotice by viewModel.modelFetchNotice.collectAsState()
                        val fetchedModels = config.modelCache[config.activeProvider] ?: emptyList()
                        var modelDropdownExpanded by remember { mutableStateOf(false) }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "النموذج الحالي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            if (modelsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = TextPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = { viewModel.refreshModels(force = true) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تحديث النماذج",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = config.activeModel,
                                onValueChange = { viewModel.updateActiveModel(it) },
                                label = { Text("نموذج الذكاء الاصطناعي النشط", fontSize = 12.sp) },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { modelDropdownExpanded = !modelDropdownExpanded }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "عرض قائمة النماذج",
                                            tint = TextPrimary
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                        if (fetchedModels.isNotEmpty()) {
                                DropdownMenu(
                                    expanded = modelDropdownExpanded,
                                    onDismissRequest = { modelDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(CardBackground)
                                        .border(1.dp, BorderColor)
                                ) {
                                    fetchedModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = model.displayName,
                                                        color = TextPrimary,
                                                        fontSize = 14.sp
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (model.isRecommended) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(
                                                                        AccentCyan.copy(alpha = 0.15f),
                                                                        RoundedCornerShape(4.dp)
                                                                    )
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "موصى به",
                                                                    color = AccentCyan,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                        if (model.isFree) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(
                                                                        AccentCyan.copy(alpha = 0.15f),
                                                                        RoundedCornerShape(4.dp)
                                                                    )
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "مجاني",
                                                                    color = AccentCyan,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                        if (model.isPremium) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(
                                                                        AccentOrange.copy(alpha = 0.15f),
                                                                        RoundedCornerShape(4.dp)
                                                                    )
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "احترافي",
                                                                    color = AccentOrange,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateActiveModel(model.id)
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "بدائل التخطيط والتنفيذ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AccentCyan
                        )
                        Text(
                            text = "قد تتم إعادة المحاولة عبر مزود آخر في المهام البسيطة فقط. المهام الحساسة لا تنتقل تلقائياً.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                        providers
                            .filter { it != config.activeProvider && it != "On-Device AI" }
                            .forEach { fallbackProvider ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = config.fallbackProviders.contains(fallbackProvider),
                                        onCheckedChange = { enabled ->
                                            viewModel.updateFallbackProvider(fallbackProvider, enabled)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = AccentCyan,
                                            uncheckedColor = BorderColor,
                                            checkmarkColor = DarkBackground
                                        )
                                    )
                                    Text(
                                        text = fallbackProvider,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                        // Model names are never bundled with the app, so when the
                        // live list is unavailable the reason is shown rather than
                        // a list that quietly went out of date.
                        modelFetchNotice?.let { notice ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = notice,
                                fontSize = 11.sp,
                                color = AccentRed,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Benchmark latency report card link
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToBenchmark() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "اختبار الأداء",
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تقرير سرعة الاستجابة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "شوف مقارنة مباشرة لسرعة الاستجابة وزمن الاتصال.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Ollama Endpoint Config Card (Visible only when Ollama is selected)
            if (config.activeProvider == "Ollama") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "اتصال Ollama المحلي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = config.ollamaUrl,
                                onValueChange = { viewModel.updateOllamaUrl(it) },
                                label = { Text("رابط خادم Ollama", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "استخدم عنوان IP المحلي للشبكة عند الاختبار على هاتف فعلي.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Local text so the field can be empty or half-typed; the
                            // view model clamps what it saves.
                            val savedOllamaTimeout = config.resolvedOllamaTimeoutSeconds()
                            var ollamaTimeoutText by remember {
                                mutableStateOf(savedOllamaTimeout.toString())
                            }
                            // Re-sync only when the saved value is not what the text means,
                            // e.g. the stored setting loaded after first composition.
                            LaunchedEffect(savedOllamaTimeout) {
                                val typed = ollamaTimeoutText.toIntOrNull()?.let(OllamaTimeout::clamp)
                                if (typed != savedOllamaTimeout) {
                                    ollamaTimeoutText = savedOllamaTimeout.toString()
                                }
                            }
                            OutlinedTextField(
                                value = ollamaTimeoutText,
                                onValueChange = { input ->
                                    val digits = input.filter(Char::isDigit).take(3)
                                    ollamaTimeoutText = digits
                                    digits.toIntOrNull()?.let(viewModel::updateOllamaTimeout)
                                },
                                label = { Text("مهلة انتظار الرد (بالثواني)", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                // Once editing ends, show what was saved: an entry of 999
                                // or 5 is stored clamped, and an empty field keeps the old value.
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focus ->
                                        if (!focus.isFocused) {
                                            ollamaTimeoutText = savedOllamaTimeout.toString()
                                        }
                                    }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "How long to wait for a reply (${OllamaTimeout.MIN_SECONDS}–${OllamaTimeout.MAX_SECONDS}s). Raise it if the first message after loading a model times out.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // On-Device AI Status Card (Visible when On-Device AI or legacy Gemma provider is selected)
            if (config.activeProvider == "On-Device AI" || config.activeProvider == "Gemma 4 (On-device)") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "حالة الذكاء الاصطناعي على الجهاز",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Show which model is active
                            val activeSpec = OnDeviceModelRegistry.findById(config.activeModel)
                            Text(
                                text = "النموذج النشط: ${activeSpec?.displayName ?: config.activeModel}",
                                fontSize = 12.sp,
                                color = AccentCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (activeSpec != null) {
                                Text(
                                    text = "المحرك: ${if (activeSpec.backend == OnDeviceBackend.AI_CORE) "Android AI Core" else "LiteRT-LM"}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // ─── AI Core Backend Section ───
                            Text(
                                text = "محرك الذكاء الاصطناعي من أندرويد",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var gemma4Status by remember { mutableStateOf("جاري التحقق...") }
                            var showGemma4Download by remember { mutableStateOf(false) }
                            var gemma3nStatus by remember { mutableStateOf("جاري التحقق...") }
                            var showGemma3nDownload by remember { mutableStateOf(false) }
                            
                            LaunchedEffect(Unit) {
                                // Check Gemma 4 (default/stable)
                                try {
                                    val client = Generation.getClient()
                                    val status = client.checkStatus()
                                    gemma4Status = when (status) {
                                        FeatureStatus.AVAILABLE -> "متاح وجاهز"
                                        FeatureStatus.DOWNLOADABLE -> {
                                            showGemma4Download = true
                                            "يحتاج إلى تنزيل"
                                        }
                                        FeatureStatus.DOWNLOADING -> "جاري التنزيل..."
                                        FeatureStatus.UNAVAILABLE -> "غير مدعوم على هذا الجهاز"
                                        else -> "غير معروف"
                                    }
                                } catch (e: Exception) {
                                    gemma4Status = "غير مدعوم على هذا الجهاز"
                                }
                                
                                // Check Gemma 3n (preview/fast)
                                try {
                                    val previewConfig = generationConfig {
                                        modelConfig = modelConfig {
                                            releaseStage = ModelReleaseStage.PREVIEW
                                            preference = ModelPreference.FAST
                                        }
                                    }
                                    val client3n = Generation.getClient(previewConfig)
                                    val status3n = client3n.checkStatus()
                                    gemma3nStatus = when (status3n) {
                                        FeatureStatus.AVAILABLE -> "متاح وجاهز"
                                        FeatureStatus.DOWNLOADABLE -> {
                                            showGemma3nDownload = true
                                            "يحتاج إلى تنزيل"
                                        }
                                        FeatureStatus.DOWNLOADING -> "جاري التنزيل..."
                                        FeatureStatus.UNAVAILABLE -> "غير مدعوم على هذا الجهاز"
                                        else -> "غير معروف"
                                    }
                                } catch (e: Exception) {
                                    gemma3nStatus = "غير مدعوم على هذا الجهاز"
                                }
                            }
                            
                            // Gemma 4 AI Core row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Gemma 4", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = gemma4Status,
                                    fontSize = 11.sp,
                                    color = if (gemma4Status.contains("جاهز")) AccentCyan else TextSecondary
                                )
                            }
                            if (showGemma4Download) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                val client = Generation.getClient()
                                                gemma4Status = "جاري التنزيل..."
                                                showGemma4Download = false
                                                client.download().collect { }
                                                gemma4Status = "اكتمل التنزيل"
                                            } catch (e: Exception) {
                                                gemma4Status = "فشل التنزيل: ${e.localizedMessage}"
                                                showGemma4Download = true
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreenButton),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تنزيل Gemma 4 (AI Core)", color = AppTheme.colors.onAccent)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Gemma 3n AI Core row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Gemma 3n متعدد الوسائط", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = gemma3nStatus,
                                    fontSize = 11.sp,
                                    color = if (gemma3nStatus.contains("جاهز")) AccentCyan else TextSecondary
                                )
                            }
                            if (showGemma3nDownload) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                val previewConfig = generationConfig {
                                                    modelConfig = modelConfig {
                                                        releaseStage = ModelReleaseStage.PREVIEW
                                                        preference = ModelPreference.FAST
                                                    }
                                                }
                                                val client3n = Generation.getClient(previewConfig)
                                                gemma3nStatus = "جاري التنزيل..."
                                                showGemma3nDownload = false
                                                client3n.download().collect { }
                                                gemma3nStatus = "اكتمل التنزيل"
                                            } catch (e: Exception) {
                                                gemma3nStatus = "فشل التنزيل: ${e.localizedMessage}"
                                                showGemma3nDownload = true
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تنزيل Gemma 3n (AI Core)", color = AppTheme.colors.onAccent)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // ─── Hugging Face Section ───
                            Text(
                                text = "رمز Hugging Face للنماذج المقيدة فقط",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Needed only for gated Hugging Face downloads (the Google-hosted Gemma 3n LiteRT builds). Public models such as Qwen 2.5 and the Gemma 4 community mirrors download without a token. Not used for cloud API providers.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val validationStatus by viewModel.huggingFaceValidationStatus.collectAsState()
                                    val lastVerified by viewModel.huggingFaceLastVerified.collectAsState()
                                    var showToken by remember { mutableStateOf(false) }

                                    OutlinedTextField(
                                        value = hfToken,
                                        onValueChange = { viewModel.updateHuggingFaceToken(it) },
                                        label = { Text("رمز الدخول إلى Hugging Face", fontSize = 12.sp) },
                                        singleLine = true,
                                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                                        placeholder = { Text("hf_...", fontSize = 12.sp, color = TextSecondary) },
                                        trailingIcon = {
                                            IconButton(onClick = { showToken = !showToken }) {
                                                Icon(
                                                    imageVector = if (showToken) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "إظهار أو إخفاء الرمز",
                                                    tint = TextSecondary
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentOrange,
                                            unfocusedBorderColor = BorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val context = LocalContext.current
                                        val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(
                                                onClick = {
                                                    val clip = clipboardManager.primaryClip
                                                    if (clip != null && clip.itemCount > 0) {
                                                        val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                                        viewModel.updateHuggingFaceToken(pasted)
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("📋 لصق", fontSize = 11.sp, color = AccentOrange)
                                            }

                                            TextButton(
                                                onClick = { viewModel.updateHuggingFaceToken("") },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("❌ مسح", fontSize = 11.sp, color = AccentRed)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Status display
                                    val statusDisplay = when (validationStatus) {
                                        "Valid" -> "✓ الرمز صالح"
                                        "Invalid" -> "✗ الرمز غير صالح"
                                        "Verifying..." -> "جاري فحص الرمز..."
                                        "Unable to verify" -> "تعذر التحقق من الرمز."
                                        else -> "⚠ الرمز مطلوب"
                                    }

                                    val statusColor = when (validationStatus) {
                                        "Valid" -> AccentCyan
                                        "Invalid" -> AccentRed
                                        "Verifying..." -> AccentCyan
                                        "Unable to verify" -> AccentOrange
                                        else -> TextSecondary
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("الحالة: $statusDisplay", fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Bold)
                                            Text("آخر تحقق: ${if (lastVerified == "Never") "لم يتم التحقق بعد" else lastVerified}", fontSize = 9.sp, color = TextSecondary)
                                            Text("التخزين: مشفر", fontSize = 9.sp, color = TextSecondary)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { viewModel.validateHuggingFaceToken() },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("التحقق من الرمز", fontSize = 10.sp, color = AppTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                                            }

                                            if (hfToken.isNotBlank()) {
                                                Button(
                                                    onClick = { viewModel.removeHuggingFaceToken() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.2f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("حذف الرمز", fontSize = 10.sp, color = AccentRed, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // ─── LiteRT-LM Backend Section ───
                            Text(
                                text = "محرك LiteRT-LM الاحتياطي",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Runs without Google AI Core. Models tagged PUBLIC (Qwen, the Gemma 4 community mirrors) need no HF token; models tagged GATED (the Google-hosted Gemma 3n builds) do. Or import your own .task / .litertlm file.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Dynamically list all LiteRT-LM models from database
                            val liteRTModels = OnDeviceModelRegistry.liteRTOnly
                            liteRTModels.forEach { spec ->
                                val modelEntity = dbModels.find { it.id == spec.id }
                                val status = modelEntity?.status ?: ModelStatus.NOT_DOWNLOADED
                                val progress = modelEntity?.downloadProgress ?: 0
                                val downloadedSize = modelEntity?.downloadedSize ?: 0L
                                val totalSize = modelEntity?.size ?: spec.expectedSize
                                val speed = modelEntity?.downloadSpeed ?: ""
                                val eta = modelEntity?.etaString ?: ""
                                
                                var expanded by remember { mutableStateOf(false) }
                                val isApiCompatible = android.os.Build.VERSION.SDK_INT >= spec.minSdk
                                val managedDownloadAvailable = spec.isManagedDownloadAvailable
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .border(
                                            1.dp,
                                            if (config.activeModel == spec.id) AccentCyan.copy(alpha = 0.5f) else BorderColor,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { if (isApiCompatible) expanded = !expanded },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (config.activeModel == spec.id) CardBackground.copy(alpha = 0.8f) else CardBackground.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = spec.displayName,
                                                        fontSize = 13.sp,
                                                        color = if (isApiCompatible) TextPrimary else TextSecondary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    if (spec.isRecommended) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "موصى به",
                                                                color = AccentOrange,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (spec.authRequired) AccentOrange.copy(alpha = 0.12f)
                                                                else AccentCyan.copy(alpha = 0.12f),
                                                                RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (spec.authRequired) "يتطلب رمز Hugging Face" else "عام · لا يحتاج رمزاً",
                                                            color = if (spec.authRequired) AccentOrange else AccentCyan,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (managedDownloadAvailable) {
                                                        if (spec.authRequired) {
                                                            "المحرك: LiteRT-LM · تنزيل يتطلب رمز Hugging Face"
                                                        } else {
                                                            "المحرك: LiteRT-LM · تنزيل عام (بدون رمز)"
                                                        }
                                                    } else {
                                                        "المحرك: LiteRT-LM · التنزيل من التطبيق غير متاح؛ الاستيراد المحلي فقط"
                                                    },
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            
                                            val badgeColor = when (status) {
                                                ModelStatus.READY -> AccentCyan
                                                ModelStatus.DOWNLOADING -> AccentOrange
                                                ModelStatus.PAUSED -> AccentOrange
                                                ModelStatus.LOADING -> AccentCyan
                                                ModelStatus.FAILED -> AccentRed
                                                else -> TextSecondary
                                            }
                                            
                                            val statusText = when {
                                                !isApiCompatible -> "يتطلب API ${spec.minSdk} أو أحدث"
                                                status == ModelStatus.READY -> "تم التنزيل"
                                                !managedDownloadAvailable -> "غير متاح من داخل التطبيق"
                                                status == ModelStatus.DOWNLOADING -> "${progress}%"
                                                status == ModelStatus.PAUSED -> "متوقف مؤقتاً"
                                                status == ModelStatus.LOADING -> "جاري التحميل..."
                                                status == ModelStatus.FAILED -> "فشل"
                                                else -> "لم يتم التنزيل"
                                            }
                                            
                                            Text(
                                                text = statusText,
                                                fontSize = 10.sp,
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                        
                                        if (isApiCompatible && (status == ModelStatus.DOWNLOADING || status == ModelStatus.PAUSED)) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = { progress.toFloat() / 100f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = AccentOrange,
                                                trackColor = BorderColor
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${formatBytes(downloadedSize)} / ${formatBytes(totalSize)}" +
                                                           (if (status == ModelStatus.DOWNLOADING && speed.isNotEmpty()) " @ $speed" else ""),
                                                    fontSize = 9.sp,
                                                    color = TextSecondary
                                                )
                                                if (status == ModelStatus.DOWNLOADING && eta.isNotEmpty()) {
                                                    Text(
                                                        text = "الوقت المتبقي: $eta",
                                                        fontSize = 9.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                if (status == ModelStatus.DOWNLOADING) {
                                                    Button(
                                                        onClick = { viewModel.pauseDownload(spec.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                                        modifier = Modifier.height(28.dp).padding(horizontal = 4.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Pause, contentDescription = "إيقاف مؤقت", modifier = Modifier.size(12.dp), tint = TextPrimary)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("إيقاف مؤقت", fontSize = 10.sp, color = TextPrimary)
                                                    }
                                                } else if (status == ModelStatus.PAUSED) {
                                                    Button(
                                                        onClick = {
                                                            if (viewModel.isCellularNetwork()) {
                                                                pendingCellularResumeModelId = spec.id
                                                            } else {
                                                                viewModel.resumeDownload(spec.id)
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                                        modifier = Modifier.height(28.dp).padding(horizontal = 4.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "استئناف", modifier = Modifier.size(12.dp), tint = DarkBackground)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("استئناف", fontSize = 10.sp, color = AppTheme.colors.onAccent)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = { viewModel.cancelDownload(spec.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.6f)),
                                                    modifier = Modifier.height(28.dp).padding(horizontal = 4.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("إلغاء", fontSize = 10.sp, color = Color.White)
                                                }
                                            }
                                        }

                                        if (isApiCompatible && status == ModelStatus.FAILED) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val errorText = modelEntity?.etaString ?: "فشل التنزيل"
                                            Text(
                                                text = errorText,
                                                fontSize = 10.sp,
                                                color = AccentRed,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (spec.licenseUrl.isNotEmpty() && (errorText.contains("الصلاحيات", ignoreCase = true) || errorText.contains("الترخيص", ignoreCase = true))) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                                                Button(
                                                    onClick = { uriHandler.openUri(spec.licenseUrl) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange.copy(alpha = 0.2f)),
                                                    modifier = Modifier.fillMaxWidth().height(28.dp),
                                                    contentPadding = PaddingValues(vertical = 2.dp)
                                                ) {
                                                    Text("فتح صفحة النموذج", color = AccentOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        
                                        AnimatedVisibility(visible = expanded) {
                                            Column {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Divider(color = BorderColor, thickness = 0.5.dp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (status == ModelStatus.NOT_DOWNLOADED || status == ModelStatus.FAILED) {
                                                        if (managedDownloadAvailable) {
                                                            Button(
                                                                onClick = {
                                                                    val hfTokenVal = hfToken
                                                                    if (spec.authRequired && hfTokenVal.isBlank()) {
                                                                        showAuthRequiredDialog = spec.displayName
                                                                        licenseUrlForDialog = spec.licenseUrl
                                                                    } else if (viewModel.isCellularNetwork()) {
                                                                        showCellularWarningDialog = spec.id
                                                                    } else {
                                                                        viewModel.downloadModel(spec.id)
                                                                    }
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                                                modifier = Modifier.weight(1f).height(32.dp),
                                                                contentPadding = PaddingValues(horizontal = 4.dp)
                                                            ) {
                                                                Text("تنزيل", fontSize = 11.sp, color = AppTheme.colors.onAccent)
                                                            }
                                                        }

                                                        Button(
                                                            onClick = {
                                                                importAsCustomModel = false
                                                                activeImportModelId = spec.id
                                                                importLauncher.launch("*/*")
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                                            modifier = Modifier.weight(1f).height(32.dp),
                                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                                        ) {
                                                            Text("استيراد", fontSize = 11.sp, color = TextPrimary)
                                                        }
                                                    }
                                                    
                                                    if (status == ModelStatus.READY) {
                                                         Button(
                                                             onClick = { viewModel.loadModel(spec.id) },
                                                             colors = ButtonDefaults.buttonColors(
                                                                 containerColor = if (config.activeModel == spec.id) TextPrimary else AccentCyan
                                                             ),
                                                             modifier = Modifier.weight(1f).height(32.dp),
                                                             contentPadding = PaddingValues(horizontal = 4.dp)
                                                         ) {
                                                             Icon(
                                                                 if (config.activeModel == spec.id) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                                                 contentDescription = null,
                                                                 modifier = Modifier.size(12.dp),
                                                                 tint = DarkBackground
                                                             )
                                                             Spacer(modifier = Modifier.width(4.dp))
                                                             Text(if (config.activeModel == spec.id) "نشط" else "تحميل النموذج", fontSize = 11.sp, color = AppTheme.colors.onAccent)
                                                         }
                                                         
                                                         Button(
                                                             onClick = { viewModel.deleteModel(spec.id) },
                                                             colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.2f)),
                                                             modifier = Modifier.height(32.dp),
                                                             contentPadding = PaddingValues(horizontal = 8.dp)
                                                         ) {
                                                             Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(14.dp), tint = AccentRed)
                                                         }
                                                    }
                                                    
                                                    Button(
                                                        onClick = {
                                                            // Info clicked (No-op or log details)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                                        modifier = Modifier.height(32.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Info, contentDescription = "معلومات", modifier = Modifier.size(14.dp), tint = TextSecondary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ─── Custom LiteRT imports ───
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "نماذج LiteRT مخصصة",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "استورد ملف .task أو .litertlm كنموذج مستقل. ملفات GGUF غير مدعومة حالياً.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    importAsCustomModel = true
                                    activeImportModelId = null
                                    importLauncher.launch("*/*")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Text("استيراد نموذج LiteRT مخصص", fontSize = 12.sp, color = AppTheme.colors.onAccent)
                            }

                            val customModels = dbModels.filter {
                                OnDeviceModelRegistry.isCustomId(it.id) &&
                                    it.status == ModelStatus.READY
                            }
                            customModels.forEach { entity ->
                                var expanded by remember(entity.id) { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .border(
                                            1.dp,
                                            if (config.activeModel == entity.id) AccentCyan.copy(alpha = 0.5f) else BorderColor,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { expanded = !expanded },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (config.activeModel == entity.id) {
                                            CardBackground.copy(alpha = 0.8f)
                                        } else {
                                            CardBackground.copy(alpha = 0.3f)
                                        }
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entity.name,
                                                    fontSize = 13.sp,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "LiteRT مخصص · ${formatBytes(entity.size)} · بدون رمز",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            Text(
                                                text = if (config.activeModel == entity.id) "نشط" else "جاهز",
                                                fontSize = 10.sp,
                                                color = AccentCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        AnimatedVisibility(visible = expanded) {
                                            Column {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Divider(color = BorderColor, thickness = 0.5.dp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { viewModel.loadModel(entity.id) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (config.activeModel == entity.id) {
                                                                TextPrimary
                                                            } else {
                                                                AccentCyan
                                                            }
                                                        ),
                                                        modifier = Modifier.weight(1f).height(32.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                                    ) {
                                                        Text(
                                                            if (config.activeModel == entity.id) "نشط" else "تحميل النموذج",
                                                            fontSize = 11.sp,
                                                            color = AppTheme.colors.onAccent
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { viewModel.deleteModel(entity.id) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = AccentRed.copy(alpha = 0.2f)
                                                        ),
                                                        modifier = Modifier.height(32.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "حذف",
                                                            modifier = Modifier.size(14.dp),
                                                            tint = AccentRed
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // ─── Storage Cleanup Section ───
                            Text(
                                text = "إدارة مساحة التخزين",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val totalSpace = storageInfo.totalBytes
                            val freeSpace = storageInfo.freeBytes
                            val usedByApp = storageInfo.usedByAppBytes
                            val usedPercentage = if (totalSpace > 0) ((totalSpace - freeSpace).toFloat() / totalSpace.toFloat()) else 0f
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "المستخدم: ${formatBytes(totalSpace - freeSpace)} / ${formatBytes(totalSpace)}",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${((totalSpace - freeSpace) * 100 / (totalSpace.coerceAtLeast(1L)))}% مستخدم",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                             }
                             Spacer(modifier = Modifier.height(4.dp))
                             LinearProgressIndicator(
                                 progress = { usedPercentage },
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .height(6.dp)
                                     .clip(RoundedCornerShape(3.dp)),
                                 color = AccentOrange,
                                 trackColor = BorderColor
                             )
                             Spacer(modifier = Modifier.height(6.dp))
                             Text(
                                 text = "نماذج NOVA تشغل ${formatBytes(usedByApp)} من مساحة الجهاز.",
                                 fontSize = 10.sp,
                                 color = TextSecondary
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             Button(
                                 onClick = { viewModel.deleteUnusedModels() },
                                 colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.8f)),
                                 modifier = Modifier.fillMaxWidth(),
                                 shape = RoundedCornerShape(8.dp)
                             ) {
                                 Text("حذف النماذج غير المستخدمة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                             }
                        }
                    }
                }
            }

            // Copilot Endpoint Config Card (Visible only when Copilot API is selected)
            if (config.activeProvider == "Copilot API") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "اتصال Copilot المحلي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = config.copilotUrl,
                                onValueChange = { viewModel.updateCopilotUrl(it) },
                                label = { Text("رابط خادم Copilot", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "استخدم عنوان IP المحلي للشبكة عند الاختبار على هاتف فعلي.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Custom OpenAI Compatible Endpoint Config Card (Visible only when Custom OpenAI Compatible is selected)
            if (config.activeProvider == "Custom OpenAI Compatible") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "عنوان API مخصص متوافق مع OpenAI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = config.customEndpoints["Custom OpenAI Compatible"] ?: "",
                                onValueChange = { viewModel.updateCustomEndpoint("Custom OpenAI Compatible", it) },
                                label = { Text("الرابط الأساسي للخدمة", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "أدخل الرابط الأساسي لخدمة API المتوافقة مع OpenAI.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Provider API Keys Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { keysSectionExpanded = !keysSectionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مفاتيح خدمات الذكاء الاصطناعي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Icon(
                                imageVector = if (keysSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "فتح أو إغلاق قسم المفاتيح",
                                tint = AccentCyan
                            )
                        }

                        AnimatedVisibility(visible = keysSectionExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val inputProviders = providers.filter { it != "Ollama" && it != "On-Device AI" }
                                inputProviders.forEach { providerName ->
                                    val keyVal = config.apiKeys[providerName] ?: ""
                                    val connectionState = connectionResults[providerName]
                                    SecureApiKeyField(
                                        value = keyVal,
                                        onValueChange = { viewModel.updateApiKey(providerName, it) },
                                        label = "مفتاح API لخدمة $providerName"
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = connectionStatusLabel(connectionState),
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { viewModel.testConnection(providerName) }
                                        ) {
                                            Text("اختبار الاتصال", fontSize = 11.sp)
                                        }
                                    }
                                    Text(
                                        text = "يرسل طلب اختبار صغيراً إلى $providerName وقد تُحتسب رسوم من المزود.",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                    if (providerName == "Opencode-Go") {
                                        Text(
                                            text = "تضيف NOVA ترويسات الاتصال المطلوبة تلقائياً. تحتاج فقط إلى مفتاح API.",
                                            fontSize = 10.sp,
                                            color = AccentCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ElevenLabs Voice Synthesis Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { voiceSectionExpanded = !voiceSectionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تحويل النص إلى صوت - ElevenLabs",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Icon(
                                imageVector = if (voiceSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "فتح أو إغلاق قسم الصوت",
                                tint = AccentCyan
                            )
                        }

                        AnimatedVisibility(visible = voiceSectionExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("هوية NOVA والاستدعاء الصوتي", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = assistantNameDraft,
                                    onValueChange = { assistantNameDraft = it },
                                    label = { Text("اسم المساعدة") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = wakeWordDraft,
                                    onValueChange = { wakeWordDraft = it; wakeWordTestResult = null },
                                    label = { Text("كلمة أو عبارة الاستدعاء") },
                                    supportingText = { Text("مثال: NOVA أو يا ليلى. العبارة العربية تستخدم التعرف الصوتي الاحتياطي على الجهاز.") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = {
                                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
                                                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "قول كلمة الاستدعاء")
                                                putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                                            }
                                            wakeWordTestLauncher.launch(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                                    ) { Text("اختبار صوتي") }
                                    Button(
                                        onClick = {
                                            viewModel.updateAssistantIdentity(assistantNameDraft, wakeWordDraft)
                                            wakeWordTestResult = "تم حفظ الاسم وكلمة الاستدعاء، وسيعاد تشغيل مستمع الاستدعاء تلقائيًا."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                                    ) { Text("حفظ الهوية") }
                                }
                                wakeWordTestResult?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
                                Text("اختيار شخصية NOVA", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(selected = config.personaStyle == "warm", onClick = { viewModel.updatePersonaStyle("warm") }, label = { Text("حنونة وودودة") })
                                    FilterChip(selected = config.personaStyle == "professional", onClick = { viewModel.updatePersonaStyle("professional") }, label = { Text("احترافية") })
                                    FilterChip(selected = config.personaStyle == "playful", onClick = { viewModel.updatePersonaStyle("playful") }, label = { Text("مرحة") })
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("تفضيل صوت بنت", color = TextPrimary, fontSize = 12.sp)
                                        Text("يختار صوتًا عربيًا أنثويًا إذا كان محرك أندرويد المثبّت يوفّره.", color = TextSecondary, fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = config.preferFemaleVoice,
                                        onCheckedChange = { viewModel.updatePreferFemaleVoice(it) }
                                    )
                                }
                                Divider(color = BorderColor)
                                SecureApiKeyField(
                                    value = config.elevenLabsApiKey,
                                    onValueChange = { viewModel.updateElevenLabsApiKey(it) },
                                    label = "مفتاح ElevenLabs"
                                )
                                OutlinedTextField(
                                    value = config.elevenLabsVoiceId,
                                    onValueChange = { viewModel.updateElevenLabsVoiceId(it) },
                                    label = { Text("معرّف الصوت", fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "إذا لم تضف مفتاح ElevenLabs، سيستخدم التطبيق محرك تحويل النص إلى كلام المدمج في أندرويد.",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Planning & Automation Preferences Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { planningSectionExpanded = !planningSectionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "التخطيط والأتمتة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Icon(
                                imageVector = if (planningSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "فتح أو إغلاق قسم التخطيط",
                                tint = AccentCyan
                            )
                        }

                        AnimatedVisibility(visible = planningSectionExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {

                        var showYoloWarning by remember { mutableStateOf(false) }
                        val autoMode = config.resolvedAutoMode()

                        Text(
                            text = "وضع التنفيذ التلقائي",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "التلقائي ينفذ الخطوات التي سمحت بها. الوضع الكامل ينفذ دون طلب موافقة إضافية.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoMode.entries.forEach { mode ->
                                val selected = autoMode == mode
                                val accent = if (mode == AutoMode.YOLO) AccentRed else AccentCyan
                                OutlinedButton(
                                    onClick = {
                                        if (mode == AutoMode.YOLO && !selected) showYoloWarning = true
                                        else viewModel.setAutoMode(mode)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (selected) accent else TextSecondary
                                    ),
                                    border = BorderStroke(1.dp, if (selected) accent else BorderColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = when (mode) {
                                            AutoMode.OFF -> "إيقاف"
                                            AutoMode.AUTO -> "تلقائي"
                                            AutoMode.YOLO -> "كامل"
                                        },
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        if (showYoloWarning) {
                            AlertDialog(
                                onDismissRequest = { showYoloWarning = false },
                                containerColor = DarkSurface,
                                title = { Text("تفعيل وضع التنفيذ الكامل؟", color = AccentRed, fontWeight = FontWeight.Bold) },
                                text = {
                                    Text(
                                        "الوضع الكامل ينفذ كل الخطط دون سؤالك، بما فيها إجراءات قد " +
                                        "تنفق أموالاً أو لا يمكن التراجع عنها مثل حذف الملفات أو " +
                                        "تثبيت التطبيقات أو إعادة تشغيل الجهاز. " +
                                        "لن تكون هناك خطوة موافقة قبل التنفيذ.",
                                        color = TextPrimary
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showYoloWarning = false
                                        viewModel.setAutoMode(AutoMode.YOLO)
                                    }) { Text("فاهم، فعّل الوضع", color = AccentRed) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showYoloWarning = false }) {
                                        Text("إلغاء", color = TextSecondary)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val grantedActions = config.effectiveGrantedActions()
                        Text(
                            text = "الإجراءات المسموح بها (${grantedActions.size})",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val dateFormat = remember { java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()) }
                        grantedActions.entries
                            .sortedBy { it.key }
                            .forEach { (action, grantedAt) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = action, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                                        Text(
                                            text = if (grantedAt == 0L) "افتراضي" else "تم السماح في ${dateFormat.format(java.util.Date(grantedAt))}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    TextButton(onClick = { viewModel.revokeGrant(action) }) {
                                        Text("إلغاء السماح", color = AccentRed, fontSize = 12.sp)
                                    }
                                }
                            }

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(BorderColor)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "وضع التخطيط متعدد الوكلاء",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "استخدم وكلاء مراجعة ودمج الخطط لتحسين الأمان وجودة التنفيذ.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = config.multiAgentModeEnabled,
                                onCheckedChange = { viewModel.updateMultiAgentMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = TextPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(BorderColor)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "إظهار الزر العائم",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "إظهار فقاعة عائمة صغيرة لفتح التطبيق أو تسجيل الأوامر مباشرة.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = config.showFloatingButton,
                                onCheckedChange = { viewModel.updateShowFloatingButton(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = TextPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(BorderColor)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (config.isDarkMode) "الوضع الداكن" else "الوضع الفاتح",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "التبديل بين المظهر الداكن والفاتح.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = config.isDarkMode,
                                onCheckedChange = { viewModel.updateDarkMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = TextPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }
                        }
                        }
                    }
                }
            }

            // Auto-Reply Settings Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AccentPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToAutoReply() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🤖", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إعدادات الرد التلقائي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentPurple
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إعداد الرد التلقائي الذكي لواتساب والرسائل والبريد.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Notification History Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToNotificationHistory() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔔", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سجل الإشعارات",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "عرض الإشعارات المحفوظة وسجل الردود التلقائية.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Permissions link card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToPermissions() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "الصلاحيات",
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الصلاحيات",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "مراجعة صلاحيات الميكروفون والتخزين وإمكانية الوصول وغيرها.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Habits & Routines link card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToRoutines() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "العادات والروتين",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "اكتشاف عاداتك المتكررة وأتمتة المهام اليومية.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Crash Log link card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AccentRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToCrashLog() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💥", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سجل الأعطال",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "عرض الأعطال المسجلة على الجهاز ومشاركتها.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Privacy Policy link card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToPrivacyPolicy() },
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
                            contentDescription = "سياسة الخصوصية",
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سياسة الخصوصية",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "كيف تتعامل NOVA مع بياناتك وخصوصيتك.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Terms of Use link card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToTermsOfUse() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "شروط الاستخدام",
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "شروط الاستخدام",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "شروط وأحكام استخدام NOVA.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Help Center link card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToHelpCenter() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "مركز المساعدة",
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مركز المساعدة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "شروحات وإجابات للأسئلة الشائعة وحلول للمشكلات.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // License link card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToLicense() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "التراخيص",
                            tint = AccentPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التراخيص",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentPurple
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ترخيص المصدر المفتوح واعتمادات المكتبات الخارجية.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // About link card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToAbout() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "عن التطبيق",
                            tint = AccentPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "عن NOVA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentPurple
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "معلومات الإصدار والمميزات والتقنيات المستخدمة.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "فتح",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // System integration info card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "صلاحيات تكامل النظام",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "To allow OpenDroid to operate other applications autonomously (e.g. WhatsApp, Calendar), verify that the accessibility service 'OpenDroid' is active in Settings -> Accessibility -> Installed Services.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    val localImportStatus by viewModel.localImportStatus.collectAsState()

    if (showAuthRequiredDialog != null) {
        AlertDialog(
            onDismissRequest = { showAuthRequiredDialog = null },
            title = { Text("مطلوب تسجيل الدخول", color = TextPrimary) },
            text = {
                Text(
                    text = "هذا النموذج مقيّد على Hugging Face ويحتاج إلى رمز وصول للتنزيل.\n\n" +
                        "النماذج المصنفة PUBLIC مثل Qwen 2.5 ونسخ Gemma 4 المجتمعية لا تحتاج إلى رمز؛ الرمز مطلوب فقط للنماذج المصنفة GATED. " +
                        "أضف رمز قراءة فقط في قسم Hugging Face أو اختر نموذجاً عاماً.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAuthRequiredDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("حسناً", color = AppTheme.colors.onAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthRequiredDialog = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (showCellularWarningDialog != null) {
        val modelIdToDownload = showCellularWarningDialog!!
        AlertDialog(
            onDismissRequest = { showCellularWarningDialog = null },
            title = { Text("تنبيه استخدام بيانات الهاتف", color = TextPrimary) },
            text = {
                Text(
                    text = "أنت تقوم بتنزيل نموذج باستخدام بيانات الهاتف وقد تُحتسب رسوم.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCellularWarningDialog = null
                        viewModel.downloadModel(modelIdToDownload)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("تنزيل", color = AppTheme.colors.onAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCellularWarningDialog = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (pendingCellularResumeModelId != null) {
        val modelIdToResume = pendingCellularResumeModelId!!
        AlertDialog(
            onDismissRequest = { pendingCellularResumeModelId = null },
            title = { Text("تنبيه استخدام بيانات الهاتف", color = TextPrimary) },
            text = {
                Text(
                    text = "أنت تقوم بتنزيل نموذج باستخدام بيانات الهاتف وقد تُحتسب رسوم.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingCellularResumeModelId = null
                        viewModel.resumeDownload(modelIdToResume)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("استئناف", color = AppTheme.colors.onAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCellularResumeModelId = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (localImportStatus != null) {
        val isImporting = localImportStatus == "جاري الاستيراد..."
        val isSuccess = localImportStatus == "نجاح"
        AlertDialog(
            onDismissRequest = {
                if (!isImporting) {
                    viewModel.clearImportStatus()
                }
            },
            title = {
                Text(
                    text = when {
                        isImporting -> "جاري استيراد النموذج"
                        isSuccess -> "تم الاستيراد بنجاح"
                        else -> "فشل الاستيراد"
                    },
                    color = TextPrimary
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    when {
                        isImporting -> {
                            CircularProgressIndicator(color = AccentOrange)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("جاري نسخ ملف النموذج والتحقق منه. قد يستغرق ذلك دقيقة...", color = TextSecondary)
                        }
                        isSuccess -> {
                            Text("تم استيراد النموذج والتحقق منه بنجاح. يمكنك تحميله الآن.", color = TextSecondary)
                        }
                        else -> {
                            Text(
                                text = localImportStatus
                                    ?: "فشل استيراد النموذج. تأكد أن الملف بصيغة LiteRT صحيحة وغير تالف.",
                                color = AccentRed
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isImporting) {
                    Button(
                        onClick = { viewModel.clearImportStatus() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                    ) {
                        Text("حسناً", color = AppTheme.colors.onAccent)
                    }
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

private fun connectionStatusLabel(state: ConnectionTestState?): String = when (state) {
    is ConnectionTestState.Testing -> "جاري الاختبار…"
    is ConnectionTestState.Connected ->
        "متصل بالنموذج ${state.model} · ${state.latencyMs} مللي ثانية"
    is ConnectionTestState.Failed -> when (state.error) {
        LLMError.AuthInvalid -> "تم رفض المفتاح"
        LLMError.AuthMissing -> "مطلوب مفتاح API"
        LLMError.QuotaExhausted -> "تم استهلاك الحصة المتاحة"
        LLMError.RateLimited -> "طلبات كتير في وقت قصير"
        LLMError.Network -> "مشكلة في الشبكة"
        else -> "فشل الاتصال"
    }
    is ConnectionTestState.ConfigMissing -> when (state.reason) {
        LLMError.AuthMissing -> "مطلوب مفتاح API"
        else -> "مطلوب إكمال الإعدادات"
    }
    else -> "لم يتم الاختبار"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 بايت"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(),
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}

@Composable
private fun SecureApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "إخفاء مفتاح API" else "إظهار مفتاح API",
                    tint = TextSecondary
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCyan,
            unfocusedBorderColor = BorderColor,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = modifier.fillMaxWidth()
    )
}
