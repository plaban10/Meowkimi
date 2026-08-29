package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExerciseEntity
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.PeachLight
import com.example.viewmodel.MeowViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    viewModel: MeowViewModel,
    onBackClick: () -> Unit
) {
    val exercise = viewModel.selectedExerciseForDetail.value
    val history = viewModel.selectedExerciseHistory.value
    val bestSet = viewModel.selectedExerciseBestSet.value

    if (exercise == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select an exercise first")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CoralPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Stats Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF0EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(exercise.icon ?: "🐾", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Text("${exercise.muscleGroup} • ${exercise.equipment}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        if (bestSet != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = "PR", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Personal Best", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("${bestSet.weight} kg × ${bestSet.reps}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = CoralPrimary)
                            }
                        }
                    }
                }
            }

            // Custom Line Progress Chart drawn on Canvas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Strength Volume Progress Chart", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)

                        if (history.size < 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Log at least 2 sessions to see progress chart! 📈🐾", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else {
                            // Render Stunning custom Line Chart
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(vertical = 8.dp)
                            ) {
                                val weights = history.map { it.weight * it.reps }
                                val maxW = weights.maxOrNull() ?: 1.0
                                val minW = weights.minOrNull() ?: 0.0
                                val range = (maxW - minW).coerceAtLeast(1.0)

                                val width = size.width
                                val height = size.height
                                val stepX = width / (history.size - 1)

                                val strokePath = Path()
                                val fillPath = Path()

                                history.forEachIndexed { i, set ->
                                    val x = i * stepX
                                    val volumeValue = set.weight * set.reps
                                    val y = (height - ((volumeValue - minW) / range * (height - 30.dp.toPx())) - 15.dp.toPx()).toFloat()

                                    if (i == 0) {
                                        strokePath.moveTo(x, y)
                                        fillPath.moveTo(x, height)
                                        fillPath.lineTo(x, y)
                                    } else {
                                        strokePath.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }

                                    if (i == history.lastIndex) {
                                        fillPath.lineTo(x, height)
                                        fillPath.close()
                                    }

                                    // Draw a small dot on each performance entry
                                    drawCircle(
                                        color = CoralPrimary,
                                        radius = 4.dp.toPx(),
                                        center = Offset(x.toFloat(), y.toFloat())
                                    )
                                }

                                // Fill path with transparent Peach Gradient
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(PeachLight.copy(alpha = 0.5f), Color.Transparent)
                                    )
                                )

                                // Draw bold line chart
                                drawPath(
                                    path = strokePath,
                                    color = CoralPrimary,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
            }

            // Historical Session Logs
            item {
                Text("Performance Logs 🐾", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
            }

            if (history.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No performance history logged for this exercise", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(history.reversed()) { set ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Set ${set.setNumber} • ${set.weight} kg × ${set.reps} reps",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = formatDate(set.completedAt),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            if (set.isPr) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFF3E0))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("⭐ PR", color = Color(0xFFFF9800), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestampStr: String): String {
    return try {
        val longVal = timestampStr.toLongOrNull() ?: return timestampStr
        val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        sdf.format(Date(longVal))
    } catch (e: Exception) {
        timestampStr
    }
}
