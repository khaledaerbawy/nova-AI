package com.opendroid.ai.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendroid.ai.R
import com.opendroid.ai.ui.theme.AppTheme
import com.opendroid.ai.ui.theme.TextPrimary
import com.opendroid.ai.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onNavigateNext: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.88f) }
    val pulse = remember { Animatable(0.35f) }

    LaunchedEffect(Unit) {
        launch {
            pulse.animateTo(
                1f,
                infiniteRepeatable(
                    animation = tween(1100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
        launch {
            scale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 190f))
        }
        alpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        delay(1250)
        alpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        onNavigateNext()
    }

    val cyan = AppTheme.colors.accentCyan
    val violet = AppTheme.colors.accentPurple

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(AppTheme.colors.surface, AppTheme.colors.background, AppTheme.colors.surface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(310.dp)
                .offset(y = (-95).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(cyan.copy(alpha = 0.13f), violet.copy(alpha = 0.045f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value)
                .padding(horizontal = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(172.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AppTheme.colors.cardBackground, AppTheme.colors.surface, AppTheme.colors.cardBackground)
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(cyan.copy(alpha = 0.8f), violet.copy(alpha = 0.65f))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = 0.045f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bot),
                        contentDescription = "شعار نوفا",
                        modifier = Modifier.size(132.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "NOVA",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 7.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "مساعدك الذكي.. أقرب مما تتخيل",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(38.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(AppTheme.colors.surface.copy(alpha = 0.92f))
                    .border(1.dp, AppTheme.colors.borderColor, RoundedCornerShape(30.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = pulse.value))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "نوفا جاهزة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary
                )
            }
        }

        Text(
            text = "ذكاء يساعدك تنجز",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp),
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}
