package com.opendroid.ai.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendroid.ai.ui.theme.AppTheme
import com.opendroid.ai.ui.viewmodel.AutoReplyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoReplySettingsScreen(
    viewModel: AutoReplyViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val config = viewModel.config
    val isLoading = viewModel.isLoading

    var isNotificationPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isAccessibilityPermissionGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationPermissionGranted = isNotificationServiceEnabled(context)
                isAccessibilityPermissionGranted = isAccessibilityServiceEnabled(context)
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                // Persist the last edit as soon as the screen leaves the foreground.
                viewModel.flushPendingConfig()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.flushPendingConfig()
        }
    }

    val themeColors = AppTheme.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الرد التلقائي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.surface,
                    titleContentColor = themeColors.textPrimary,
                    navigationIconContentColor = themeColors.textPrimary
                ),
                modifier = Modifier.border(0.5.dp, themeColors.borderColor.copy(alpha = 0.5f))
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = themeColors.accentPurple)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permission Warning Card
                if (!isNotificationPermissionGranted || !isAccessibilityPermissionGranted) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.accentRed, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.accentRed.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "صلاحيات النظام مطلوبة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accentRed,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يحتاج الرد التلقائي إلى إذن الوصول للإشعارات لمتابعة الرسائل الواردة، وإذن إمكانية الوصول لكتابة الردود وإرسالها تلقائياً.",
                                fontSize = 13.sp,
                                color = themeColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isNotificationPermissionGranted) {
                                    Button(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                            } catch (e: Exception) {
                                                context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accentRed),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("السماح بالوصول إلى الإشعارات", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                                if (!isAccessibilityPermissionGranted) {
                                    Button(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                            } catch (e: Exception) {
                                                context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accentPurple),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تفعيل إمكانية الوصول", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Global Toggle Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.borderColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (config.globalEnabled) {
                            themeColors.accentPurple.copy(alpha = 0.08f)
                        } else {
                            themeColors.cardBackground
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "الرد التلقائي",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (config.globalEnabled) "سيرد الذكاء الاصطناعي على الرسائل تلقائياً بعد ${config.replyDelayMinutes} دقيقة"
                                else "الرد التلقائي متوقف",
                                fontSize = 13.sp,
                                color = themeColors.textSecondary
                            )
                        }
                        Switch(
                            checked = config.globalEnabled,
                            onCheckedChange = { viewModel.saveConfig(config.copy(globalEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = themeColors.textPrimary,
                                checkedTrackColor = themeColors.textPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                if (config.globalEnabled) {
                    // Per-App Toggles
                    Text(
                        "التطبيقات المفعّلة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.textPrimary
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.borderColor, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            AppToggleRow("واتساب", "💬", config.whatsappEnabled, themeColors) {
                                viewModel.saveConfig(config.copy(whatsappEnabled = it))
                            }
                            Divider(color = themeColors.borderColor.copy(alpha = 0.5f))
                            AppToggleRow("تيليجرام", "✈️", config.telegramEnabled, themeColors) {
                                viewModel.saveConfig(config.copy(telegramEnabled = it))
                            }
                            Divider(color = themeColors.borderColor.copy(alpha = 0.5f))
                            AppToggleRow("ديسكورد", "🎮", config.discordEnabled, themeColors) {
                                viewModel.saveConfig(config.copy(discordEnabled = it))
                            }
                            Divider(color = themeColors.borderColor.copy(alpha = 0.5f))
                            AppToggleRow("الرسائل النصية", "📱", config.smsEnabled, themeColors) {
                                viewModel.saveConfig(config.copy(smsEnabled = it))
                            }
                            Divider(color = themeColors.borderColor.copy(alpha = 0.5f))
                            AppToggleRow("البريد الإلكتروني", "📧", config.emailEnabled, themeColors) {
                                viewModel.saveConfig(config.copy(emailEnabled = it))
                            }
                        }
                    }

                    // Reply Delay Slider
                    Text(
                        "تأخير الرد",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.textPrimary
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.borderColor, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "الانتظار قبل الرد",
                                    fontSize = 14.sp,
                                    color = themeColors.textSecondary
                                )
                                Text(
                                    if (config.replyDelayMinutes <= 0) "فوراً (0 ثانية)"
                                    else "${config.replyDelayMinutes} دقيقة",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accentPurple
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = config.replyDelayMinutes.toFloat(),
                                onValueChange = {
                                    viewModel.saveConfig(config.copy(replyDelayMinutes = it.toInt()))
                                },
                                valueRange = 0f..60f,
                                steps = 59,
                                colors = SliderDefaults.colors(
                                    thumbColor = themeColors.accentPurple,
                                    activeTrackColor = themeColors.accentPurple
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("0 ثانية (فوري)", fontSize = 12.sp, color = themeColors.textSecondary.copy(alpha = 0.6f))
                                Text("60 دقيقة", fontSize = 12.sp, color = themeColors.textSecondary.copy(alpha = 0.6f))
                            }
                        }
                    }

                    // Rate Limit
                    Text(
                        "حد معدل الردود",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.textPrimary
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.borderColor, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "أقصى عدد للردود لكل جهة اتصال في الساعة",
                                    fontSize = 14.sp,
                                    color = themeColors.textSecondary
                                )
                                Text(
                                    "${config.maxRepliesPerContactPerHour}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accentPurple
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = config.maxRepliesPerContactPerHour.toFloat(),
                                onValueChange = {
                                    viewModel.saveConfig(config.copy(maxRepliesPerContactPerHour = it.toInt()))
                                },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = themeColors.accentPurple,
                                    activeTrackColor = themeColors.accentPurple
                                )
                            )
                        }
                    }

                    // Custom Prompt & Direct Reply
                    Text(
                        "محتوى الرد",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.textPrimary
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.borderColor, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "نص رد مخصص (اختياري)",
                                fontSize = 14.sp,
                                color = themeColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "عند تحديده، سيتم إرسال هذا النص كما هو بدلاً من إنشاء رد بالذكاء الاصطناعي.",
                                fontSize = 12.sp,
                                color = themeColors.textSecondary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = config.customReplyText ?: "",
                                onValueChange = {
                                    viewModel.saveConfig(config.copy(customReplyText = it.ifBlank { null }), debounce = true)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "مثال: أنا أقود السيارة حالياً، سأرد عليك قريباً!",
                                        color = themeColors.textSecondary.copy(alpha = 0.5f)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeColors.accentPurple,
                                    unfocusedBorderColor = themeColors.borderColor,
                                    focusedTextColor = themeColors.textPrimary,
                                    unfocusedTextColor = themeColors.textPrimary,
                                    cursorColor = themeColors.accentPurple
                                ),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "أسلوب الرد المخصص (اختياري)",
                                fontSize = 14.sp,
                                color = themeColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = config.customPrompt ?: "",
                                onValueChange = {
                                    viewModel.saveConfig(config.copy(customPrompt = it.ifBlank { null }), debounce = true)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "مثال: ودي وبلغة بسيطة مع استخدام الرموز التعبيرية",
                                        color = themeColors.textSecondary.copy(alpha = 0.5f)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeColors.accentPurple,
                                    unfocusedBorderColor = themeColors.borderColor,
                                    focusedTextColor = themeColors.textPrimary,
                                    unfocusedTextColor = themeColors.textPrimary,
                                    cursorColor = themeColors.accentPurple
                                ),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AppToggleRow(
    appName: String,
    emoji: String,
    isEnabled: Boolean,
    themeColors: com.opendroid.ai.ui.theme.OpenDroidColors,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(appName, fontSize = 15.sp, color = themeColors.textPrimary)
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = themeColors.textPrimary,
                checkedTrackColor = themeColors.textPrimary.copy(alpha = 0.5f)
            )
        )
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!flat.isNullOrEmpty()) {
        val names = flat.split(":")
        for (name in names) {
            val cn = android.content.ComponentName.unflattenFromString(name)
            if (cn != null) {
                if (cn.packageName == pkgName) {
                    return true
                }
            }
        }
    }
    return false
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    if (com.opendroid.ai.accessibility.OpenDroidAccessibilityService.getInstance() != null) {
        return true
    }
    val expectedComponentName = android.content.ComponentName(context, com.opendroid.ai.accessibility.OpenDroidAccessibilityService::class.java).flattenToString()
    val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
    return enabledServicesSetting.contains(expectedComponentName)
}
