package com.opendroid.ai.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendroid.ai.data.models.HabitRoutine
import com.opendroid.ai.data.models.PlanStep
import com.opendroid.ai.data.models.RoutineStatus
import com.opendroid.ai.ui.theme.*
import com.opendroid.ai.ui.viewmodel.RoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    viewModel: RoutineViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val routinesUiState by viewModel.routinesUiState.collectAsState()
    val suggestedRoutines = routinesUiState.suggested
    val activeRoutines = routinesUiState.active
    val recentEvents by viewModel.recentEvents.collectAsState()

    var isExecuting by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "العادات والروتين",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        letterSpacing = 1.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.triggerDetection()
                        Toast.makeText(context, "جاري تحليل العادات...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحليل العادات",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
            )
        },
        containerColor = AppTheme.colors.background,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── 1. SUGGESTED ROUTINES SECTION (AI DISCOVERED) ──────────
            if (suggestedRoutines.isNotEmpty()) {
                item {
                    Text(
                        text = "العادات والاقتراحات المكتشفة",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.accentCyan,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(suggestedRoutines, key = { "s-${it.id}" }) { routine ->
                    SuggestedRoutineCard(
                        routine = routine,
                        onApprove = {
                            viewModel.approveRoutine(routine.id)
                            Toast.makeText(context, "تم تفعيل الروتين ${routineNameArabic(routine.name)} تلقائياً!", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = {
                            viewModel.dismissRoutine(routine.id)
                            Toast.makeText(context, "تم تجاهل الاقتراح", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // ── 2. AUTOMATED ACTIVE ROUTINES ───────────────────────────
            item {
                Text(
                    text = "الروتينات المفعّلة (${activeRoutines.size})",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.accentCyan,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (activeRoutines.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppTheme.colors.borderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = AppTheme.colors.textSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد روتينات مفعّلة بعد",
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "أثناء استخدامك لتطبيقات مثل Gmail وCalendar وSlack، تكتشف NOVA الأنماط المتكررة وتقترح أتمتتها هنا.",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                items(activeRoutines, key = { "a-${it.id}" }) { routine ->
                    ActiveRoutineCard(
                        routine = routine,
                        isExecuting = isExecuting == routine.id,
                        onToggle = { isEnabled ->
                            viewModel.toggleRoutine(routine.id, isEnabled)
                        },
                        onExecute = {
                            isExecuting = routine.id
                            viewModel.executeRoutine(routine.id, context) { success, msg ->
                                isExecuting = null
                                Toast.makeText(
                                    context,
                                    if (success) "اكتمل الروتين: $msg" else "فشل التنفيذ: $msg",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onDelete = {
                            viewModel.deleteRoutine(routine.id)
                        }
                    )
                }
            }

            // ── 3. PRE-BUILT TEMPLATES ──────────────────────────────────
            item {
                Text(
                    text = "قوالب روتين جاهزة",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                RoutineTemplateCard(
                    title = "🌅 روتين الصباح",
                    description = "راجع التقويم ← لخص اجتماعات اليوم ← راجع الإشعارات ← قائمة المهام ← ملخص الصباح",
                    trigger = "كل يوم عمل الساعة 9 صباحاً",
                    onActivate = {
                        viewModel.triggerDetection()
                        Toast.makeText(context, "تم تفعيل قالب روتين الصباح!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                RoutineTemplateCard(
                    title = "💼 روتين التركيز في العمل",
                    description = "افتح Slack ← راجع التقويم ← اقرأ الإشعارات المهمة",
                    trigger = "كل يوم عمل الساعة 9:30 صباحاً",
                    onActivate = {
                        viewModel.triggerDetection()
                        Toast.makeText(context, "تم تفعيل قالب التركيز في العمل!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                RoutineTemplateCard(
                    title = "🌙 روتين نهاية اليوم",
                    description = "راجع تقويم الغد ← راجع الإشعارات غير المقروءة ← ملخص اليوم",
                    trigger = "يومياً الساعة 9 مساءً",
                    onActivate = {
                        viewModel.triggerDetection()
                        Toast.makeText(context, "تم تفعيل روتين نهاية اليوم!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // ── 4. HABIT LEARNING ANALYTICS ────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppTheme.colors.borderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = AppTheme.colors.accentPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "محرك تعلّم العادات",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tracked Events: ${recentEvents.size} recent activities logged.\nOpenDroid securely analyzes on-device app switches to learn your daily routines without cloud data transfer.",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedRoutineCard(
    routine: HabitRoutine,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppTheme.colors.borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = AppTheme.colors.accentCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "💡 تم اكتشاف روتين",
                        color = AppTheme.colors.accentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = AppTheme.colors.surface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "نسبة التطابق ${(routine.confidence * 100).toInt()}%",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = routine.suggestionMessage.ifBlank { "لاحظت إنك بتكرر المهام دي. تحب أفعّلها تلقائياً؟" },
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "⚡ ${routineTriggerArabic(routine.triggerLabel)}",
                color = AppTheme.colors.accentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )

            if (routine.detectedActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    routine.detectedActions.forEach { action ->
                        Surface(
                            color = AppTheme.colors.surface,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.borderColor)
                        ) {
                            Text(
                                text = action,
                                color = AppTheme.colors.textSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable suggested steps preview
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "الأتمتة المقترحة (${routine.suggestedSteps.size} خطوات)",
                    color = AppTheme.colors.accentPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTheme.colors.accentPurple,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    routine.suggestedSteps.forEachIndexed { idx, step ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${idx + 1}.",
                                color = AppTheme.colors.accentCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(
                                text = step.description,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.textPrimary,
                        contentColor = AppTheme.colors.background
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = AppTheme.colors.background,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "موافقة وتفعيل",
                        color = AppTheme.colors.background,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.borderColor)
                ) {
                    Text(
                        text = "تجاهل",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveRoutineCard(
    routine: HabitRoutine,
    isExecuting: Boolean,
    onToggle: (Boolean) -> Unit,
    onExecute: () -> Unit,
    onDelete: () -> Unit
) {
    val isEnabled = routine.status == RoutineStatus.ACTIVE || routine.status == RoutineStatus.APPROVED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isEnabled) AppTheme.colors.accentCyan else AppTheme.colors.borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routineNameArabic(routine.name),
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "⚡ ${routineTriggerArabic(routine.triggerLabel)}",
                        color = AppTheme.colors.accentCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppTheme.colors.textPrimary,
                        checkedTrackColor = AppTheme.colors.textPrimary.copy(alpha = 0.5f),
                        uncheckedThumbColor = AppTheme.colors.textSecondary,
                        uncheckedTrackColor = AppTheme.colors.surface
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = routine.description,
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onExecute,
                    enabled = !isExecuting,
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surface),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.accentCyan),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = AppTheme.colors.accentCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جاري التنفيذ...", color = AppTheme.colors.accentCyan, fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AppTheme.colors.accentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تشغيل الروتين الآن",
                            color = AppTheme.colors.accentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = AppTheme.colors.accentRed
                    )
                }
            }
        }
    }
}

@Composable
fun RoutineTemplateCard(
    title: String,
    description: String,
    trigger: String,
    onActivate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppTheme.colors.borderColor, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = AppTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⏰ $trigger",
                    color = AppTheme.colors.accentCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onActivate) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "تفعيل",
                    tint = AppTheme.colors.accentCyan
                )
            }
        }
    }
}

private fun routineNameArabic(name: String): String = when (name) {
    "Morning Routine" -> "روتين الصباح"
    "Work Focus Routine" -> "روتين التركيز في العمل"
    "Evening Wrap-up" -> "روتين نهاية اليوم"
    else -> name
}

private fun routineTriggerArabic(trigger: String): String = when (trigger) {
    "Every weekday at 9:00 AM" -> "كل يوم عمل الساعة 9 صباحاً"
    "Every weekday at 9:30 AM" -> "كل يوم عمل الساعة 9:30 صباحاً"
    "Daily at 9:00 PM" -> "يومياً الساعة 9 مساءً"
    else -> trigger
}
