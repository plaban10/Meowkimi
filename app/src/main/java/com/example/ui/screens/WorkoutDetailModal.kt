package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ExerciseWithSets
import com.example.data.local.WorkoutWithDetails
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailModal(
    workoutDetail: WorkoutWithDetails,
    onDismiss: () -> Unit,
    onRepeatWorkout: (WorkoutWithDetails) -> Unit,
    onEditNotes: (WorkoutWithDetails) -> Unit,
    onDeleteWorkout: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = workoutDetail.workout.title ?: "Session Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEditNotes(workoutDetail) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Notes", tint = Color(0xFF64748B))
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Workout", tint = Color(0xFFEF4444))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                onRepeatWorkout(workoutDetail)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralPrimary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🐾", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Start Similar Workout",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(BackgroundLight),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                item {
                    SessionHeaderCard(workoutDetail = workoutDetail)
                }

                // Cat Mascot Feedback Banner
                item {
                    CatMascotFeedbackBanner(workoutDetail = workoutDetail)
                }

                // Notes Card if present
                if (!workoutDetail.workout.notes.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📝", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Session Notes",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = workoutDetail.workout.notes ?: "",
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                // Exercises Section Title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Exercises Performed (${workoutDetail.exercises.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark
                        )
                        Text(
                            text = "${workoutDetail.totalSets} total sets",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Exercise Breakdown Cards
                items(workoutDetail.exercises) { exDetail ->
                    ExerciseBreakdownCard(exDetail = exDetail)
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🗑️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Workout?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete this workout log? This will remove all associated set data and PR history for this session.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteWorkout(workoutDetail.workout.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun SessionHeaderCard(workoutDetail: WorkoutWithDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workoutDetail.workout.title ?: "Meow Workout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatFullDate(workoutDetail.workout.startedAt),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (workoutDetail.prCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFFBEB))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏆", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${workoutDetail.prCount} PR${if (workoutDetail.prCount > 1) "s" else ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    emoji = "⏱️",
                    value = "${workoutDetail.durationMinutes} min",
                    label = "Duration"
                )
                MetricItem(
                    emoji = "⚡",
                    value = formatVolume(workoutDetail.totalVolume),
                    label = "Volume"
                )
                MetricItem(
                    emoji = "💪",
                    value = "${workoutDetail.totalSets}",
                    label = "Total Sets"
                )
                MetricItem(
                    emoji = "🐾",
                    value = "${workoutDetail.exercises.size}",
                    label = "Exercises"
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    emoji: String,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextDark
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun CatMascotFeedbackBanner(workoutDetail: WorkoutWithDetails) {
    val (mascotEmoji, message, gradientColors) = when {
        workoutDetail.prCount > 0 -> Triple(
            "😻",
            "PAW-SOME PR! You leveled up your feline power today with ${workoutDetail.prCount} new record${if (workoutDetail.prCount > 1) "s" else ""}!",
            listOf(Color(0xFFFFF1EB), Color(0xFFFFECE5))
        )
        workoutDetail.totalVolume > 3000 -> Triple(
            "😼",
            "Fur-ocious effort! ${formatVolume(workoutDetail.totalVolume)} lifted — your claws are forged in pure steel!",
            listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
        )
        workoutDetail.totalSets >= 12 -> Triple(
            "😽",
            "Incredible grit! Complete dedication to the grind. Paws down an amazing session!",
            listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))
        )
        else -> Triple(
            "😸",
            "Purr-fect session! Consistent workouts build unstoppable meow-scle. Keep it going!",
            listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(mascotEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Coach Whiskers says:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = CoralPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark
                )
            }
        }
    }
}

@Composable
private fun ExerciseBreakdownCard(exDetail: ExerciseWithSets) {
    val ex = exDetail.exercise
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFEDE8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ex?.icon ?: "🐾",
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = ex?.name ?: "Exercise",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ex?.muscleGroup ?: "General",
                                fontSize = 11.sp,
                                color = CoralPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            if (!ex?.equipment.isNullOrBlank()) {
                                Text(
                                    text = " • ${ex?.equipment}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⏱️ ${exDetail.workoutExercise.restSeconds}s rest",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sets Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.width(44.dp)
                )
                Text(
                    text = "WEIGHT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "REPS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "STATUS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.width(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Set Rows
            exDetail.sets.forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${set.setNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Text(
                        text = "${if (set.weight % 1.0 == 0.0) set.weight.toInt() else set.weight} kg",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${set.reps} reps",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier.width(64.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (set.isPr) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🏆 PR!",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        } else {
                            Text(
                                text = "✅ Done",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFullDate(timestampStr: String): String {
    return try {
        val longVal = timestampStr.toLongOrNull() ?: return timestampStr
        val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(longVal))
    } catch (e: Exception) {
        timestampStr
    }
}

private fun formatVolume(volume: Double): String {
    return if (volume >= 1000) {
        String.format(Locale.getDefault(), "%,.0f kg", volume)
    } else {
        "${volume.toInt()} kg"
    }
}
