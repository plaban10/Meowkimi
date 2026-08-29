package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ExerciseEntity
import com.example.ui.theme.*
import com.example.viewmodel.MeowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MeowViewModel,
    onStartWorkoutClick: () -> Unit,
    onExploreExercisesClick: () -> Unit,
    onExerciseClick: (ExerciseEntity) -> Unit
) {
    val profileState by viewModel.profile.collectAsState()
    val exercisesState by viewModel.exercises.collectAsState()
    val username = profileState?.displayName ?: "Alex"
    val syncStatusState by viewModel.syncStatus.collectAsState()
    val detailedWorkouts by viewModel.detailedWorkouts

    // Calculate today's workout activity dynamically
    val todayDateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val todayString = remember { todayDateFormat.format(java.util.Date()) }

    val todayWorkout = remember(detailedWorkouts) {
        detailedWorkouts.firstOrNull { item ->
            val ts = item.workout.startedAt.toLongOrNull() ?: 0L
            ts > 0 && todayDateFormat.format(java.util.Date(ts)) == todayString
        }
    }
    val todayExercises = todayWorkout?.exercises ?: emptyList()
    val overallProgress = if (todayWorkout != null && todayExercises.isNotEmpty()) 1.0f else 0.0f
    val todayDurationMinutes = todayWorkout?.durationMinutes ?: 0L
    val todayDurationText = if (todayDurationMinutes > 0) "${todayDurationMinutes}:00" else "00:00"
    val todayCalories = if (todayWorkout != null) {
        ((todayDurationMinutes * 6.5) + (todayWorkout.totalVolume * 0.05)).toInt().coerceAtLeast(45)
    } else 0

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PeachLight)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "MeowMuscle Mascot",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(
                                text = "MeowMuscle",
                                style = androidx.compose.ui.text.TextStyle(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(CoralPrimary, Color(0xFFFFAB91))
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Slate50)
                                .clickable { viewModel.retryDatabaseSync() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔔", fontSize = 18.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
                Divider(color = Slate100, thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sync status sub-text
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status: $syncStatusState",
                        fontSize = 11.sp,
                        color = TextLight,
                        fontWeight = FontWeight.Medium
                    )
                    if (syncStatusState.contains("Pending")) {
                        Text(
                            text = "Retry Sync 🔄",
                            fontSize = 11.sp,
                            color = CoralPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.retryDatabaseSync() }
                        )
                    }
                }
            }

            // Hero Banner Card with Peach/Coral Gradient and Generated Cat Mascot holding dumbbells
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CoralPrimary, Color(0xFFFFB088))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Good morning, $username!",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ready to Meowscle\nup today?",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 24.sp
                            )
                        }

                        // Display the gym cats mascot
                        Box(
                            modifier = Modifier
                                .size(95.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "MeowMuscle Gym Cats Mascot",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Section Header: "Today's MeowSession"
            item {
                Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                    Text(
                        text = "TODAY'S MEOWSESSION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Circular Progress Ring & Side-by-Side Exercises Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Circular Progress Card
                    Card(
                        modifier = Modifier
                            .size(112.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = overallProgress,
                                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
                                label = "progress"
                            )

                            Canvas(modifier = Modifier.size(80.dp)) {
                                // Background circle
                                drawCircle(
                                    color = Slate100,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                // Active progress sweep: LavenderAccent (#A0A6FF)
                                drawArc(
                                    color = LavenderAccent,
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${(overallProgress * 100).toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "PROGRESS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight
                                )
                            }

                            // Paw decor in top right corner
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Text("🐾", fontSize = 10.sp)
                            }
                        }
                    }

                    // Side-by-Side Exercises List or Clean Empty State
                    if (todayExercises.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(112.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFF1EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🐾", fontSize = 15.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No exercises logged today",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextDark,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tap below to start your session",
                                        fontSize = 9.sp,
                                        color = TextLight,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            todayExercises.take(2).forEach { exDetail ->
                                val exName = exDetail.exercise?.name ?: "Exercise"
                                val exIcon = exDetail.exercise?.icon ?: "🐾"
                                val setsCount = exDetail.sets.size
                                val totalReps = exDetail.sets.sumOf { it.reps }
                                val exDetailsText = if (setsCount > 0) "$setsCount sets • $totalReps reps" else "${exDetail.workoutExercise.restSeconds}s rest"

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate50),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFFF1EB)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(exIcon, fontSize = 14.sp)
                                            }

                                            Column {
                                                Text(
                                                    text = exName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = TextDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = exDetailsText,
                                                    fontSize = 10.sp,
                                                    color = TextLight,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        if (exDetail.sets.any { it.isPr }) {
                                            Text(
                                                text = "🏆 PR",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706)
                                            )
                                        } else {
                                            Text(
                                                text = "✅",
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stat Cards: Duration and Calories Burned side by side
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Duration card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DURATION",
                                fontSize = 10.sp,
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = todayDurationText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                if (todayDurationMinutes > 0) {
                                    Text(
                                        text = " min",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextLight
                                    )
                                }
                            }
                        }
                    }

                    // Calories Burned card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CALORIES",
                                fontSize = 10.sp,
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "$todayCalories",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = " kcal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextLight
                                )
                            }
                        }
                    }
                }
            }

            // Primary CTA Button: Start Next MeowWorkout with Tactile 3D Bottom Border
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clickable { onStartWorkoutClick() }
                ) {
                    // Under shadow / bottom border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Slate950)
                    )
                    // Core active button plate with content offset upwards to reveal the bottom plate
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Slate900),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "▶️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "START NEXT MEOWWORKOUT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "🐾", fontSize = 18.sp)
                        }
                    }
                }
            }

            // Quick Explore All Exercises Card
            item {
                Card(
                    onClick = { onExploreExercisesClick() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFFECE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📚", fontSize = 22.sp)
                            }
                            Column {
                                Text(
                                    text = "Explore All Exercises",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Browse ${exercisesState.size} workouts & target muscles",
                                    fontSize = 11.sp,
                                    color = TextLight
                                )
                            }
                        }
                        Text("➔", color = CoralPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

val TextDark = com.example.ui.theme.TextDark
val TextLight = com.example.ui.theme.TextLight

