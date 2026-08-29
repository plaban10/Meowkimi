package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.PeachLight
import com.example.ui.theme.TextDark
import com.example.viewmodel.PrCelebrationEvent
import kotlin.math.cos
import kotlin.math.sin

private data class ConfettiParticle(
    val angle: Double,
    val distance: Float,
    val size: Float,
    val color: Color
)

@Composable
fun PrCelebrationOverlay(
    event: PrCelebrationEvent,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(event) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }

    // Bounce and pulsing animation for the medal
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Confetti burst progress (0f to 1f)
    val confettiProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        confettiProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
    }

    // Generate fixed particle positions for this burst
    val particles = remember {
        val colors = listOf(
            Color(0xFFFF5722),
            Color(0xFFFF9800),
            Color(0xFFFFC107),
            Color(0xFF4CAF50),
            Color(0xFF2196F3),
            Color(0xFFE91E63),
            Color(0xFF9C27B0)
        )
        (0 until 36).map { i ->
            ConfettiParticle(
                angle = Math.toRadians((i * 10.0) + (i % 3 * 5)),
                distance = 180f + (i % 5) * 35f,
                size = 6f + (i % 4) * 3f,
                color = colors[i % colors.size]
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Confetti Canvas in background
        Canvas(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.Center)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val progress = confettiProgress.value
            val alpha = (1f - (progress - 0.4f).coerceAtLeast(0f) / 0.6f).coerceIn(0f, 1f)

            particles.forEach { particle ->
                val currentDistance = particle.distance * progress
                val x = center.x + (currentDistance * cos(particle.angle)).toFloat()
                val y = center.y + (currentDistance * sin(particle.angle)).toFloat() + (progress * 50f)
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = particle.size * (1f - progress * 0.3f),
                    center = Offset(x, y)
                )
            }
        }

        // Celebratory Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp)
                .clickable { /* consume card click */ },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🏆", fontSize = 14.sp)
                        Text(
                            text = if (event.isWeightPr && event.isRepPr) "WEIGHT & REPS PR"
                            else if (event.isWeightPr) "WEIGHT PR BEATEN"
                            else if (event.isRepPr) "REPS PR ACHIEVED"
                            else "NEW PERSONAL RECORD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100),
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Glowing Cat Paw Badge
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFFB74D), Color(0xFFFF7043))
                            )
                        )
                        .shadow(8.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐾", fontSize = 46.sp)
                }

                // Exercise Title & Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = event.exerciseName,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        color = TextDark
                    )

                    if (event.prDescription.isNotBlank()) {
                        Text(
                            text = event.prDescription,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // PR Stat Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PeachLight.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WEIGHT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                            Text(
                                text = "${if (event.weight % 1.0 == 0.0) event.weight.toInt().toString() else event.weight.toString()} kg",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark
                            )
                        }

                        Text("×", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("REPS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                            Text(
                                text = "${event.reps} reps",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark
                            )
                        }
                    }
                }

                Text(
                    text = "Purr-fect strength gains unlocked! Your inner lion is flexing! 🐱⚡",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                // Dismiss / Continue Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                ) {
                    Text(
                        text = "Keep Purring! 🐾",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
