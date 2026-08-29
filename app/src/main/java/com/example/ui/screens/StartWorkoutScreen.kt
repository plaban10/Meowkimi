package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RoutineWithDetails
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.PeachLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate50
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLight
import com.example.viewmodel.ActiveExercise
import com.example.viewmodel.ActiveSet
import com.example.viewmodel.MeowViewModel
import com.example.viewmodel.PrCelebrationEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartWorkoutScreen(
    viewModel: MeowViewModel,
    onAddExerciseClick: () -> Unit,
    onFinishWorkoutSuccess: () -> Unit
) {
    val isActive = viewModel.isActiveWorkoutInProgress.value
    val title = viewModel.activeWorkoutTitle.value
    val activeExList = viewModel.activeExercises
    val setsMap = viewModel.activeSets
    val timerRemaining = viewModel.timerRemaining.value
    val timerActive = viewModel.isTimerActive.value
    val prCelebration = viewModel.prCelebrationText.value
    val prCelebrationEvent = viewModel.prCelebrationEvent.value

    val detailedRoutines by viewModel.detailedRoutines
    val isLoadingRoutines by viewModel.isLoadingRoutines

    var showFinishDialog by remember { mutableStateOf(false) }
    var workoutNotes by remember { mutableStateOf("") }

    var showCreateRoutineModal by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<RoutineWithDetails?>(null) }
    var routineToDelete by remember { mutableStateOf<RoutineWithDetails?>(null) }

    var focusedTarget by remember { mutableStateOf<FocusedSetTarget?>(null) }

    fun moveToNextTarget() {
        val current = focusedTarget ?: return
        val currentExerciseSets = setsMap[current.exerciseId] ?: emptyList()
        if (current.fieldType == SetFieldType.WEIGHT) {
            // Move to Reps on the same set
            focusedTarget = current.copy(fieldType = SetFieldType.REPS)
        } else {
            // Move to Weight on the next set of this exercise
            if (current.setIndex + 1 < currentExerciseSets.size) {
                focusedTarget = FocusedSetTarget(current.exerciseId, current.setIndex + 1, SetFieldType.WEIGHT)
            } else {
                // Move to next exercise
                val exIdx = activeExList.indexOfFirst { it.id == current.exerciseId }
                if (exIdx != -1 && exIdx + 1 < activeExList.size) {
                    val nextEx = activeExList[exIdx + 1]
                    focusedTarget = FocusedSetTarget(nextEx.id, 0, SetFieldType.WEIGHT)
                } else {
                    focusedTarget = null
                }
            }
        }
    }

    val currentTargetValue = remember(focusedTarget, setsMap) {
        val target = focusedTarget
        if (target != null) {
            val sets = setsMap[target.exerciseId] ?: emptyList()
            if (target.setIndex in sets.indices) {
                val set = sets[target.setIndex]
                if (target.fieldType == SetFieldType.WEIGHT) set.weight else set.reps
            } else ""
        } else ""
    }

    if (!isActive) {
        // Workout not active: Show Custom Routines & Empty Starter
        Scaffold(
            containerColor = Color(0xFFF7F8FA)
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {
                // Header section
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "🐾 MeowSession 🐾",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = CoralPrimary
                        )
                        Text(
                            text = "Ready to build some meowscle? Start an empty session or pick a custom routine!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                // Action Buttons: Start Empty Session & + Create New Routine
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Start Empty Session Button
                        Button(
                            onClick = { viewModel.startNewWorkout("Empty MeowSession") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Empty MeowWorkout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        // + Create New Routine Button
                        Button(
                            onClick = {
                                routineToEdit = null
                                showCreateRoutineModal = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PeachLight.copy(alpha = 0.25f),
                                contentColor = CoralPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, CoralPrimary.copy(alpha = 0.5f)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CoralPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ Create New Routine", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = CoralPrimary)
                        }
                    }
                }

                // Section Header: My Routines
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "My Routines",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDark
                            )
                            if (detailedRoutines.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PeachLight.copy(alpha = 0.4f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${detailedRoutines.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralPrimary
                                    )
                                }
                            }
                        }

                        if (detailedRoutines.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    routineToEdit = null
                                    showCreateRoutineModal = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+ Add New", fontWeight = FontWeight.Bold, color = CoralPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Empty State for Custom Routines
                if (detailedRoutines.isEmpty() && !isLoadingRoutines) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    routineToEdit = null
                                    showCreateRoutineModal = true
                                },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(PeachLight.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📝🐾", fontSize = 24.sp)
                                }
                                Text(
                                    text = "No custom routines yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Create a custom split (e.g. Monday Push Day) to automatically load target exercises, sets, and rest intervals!",
                                    fontSize = 12.sp,
                                    color = TextLight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        routineToEdit = null
                                        showCreateRoutineModal = true
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create Routine", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Render dynamic Custom Routine Cards
                items(detailedRoutines, key = { it.routine.id }) { routineDetail ->
                    var menuExpanded by remember { mutableStateOf(false) }
                    val routine = routineDetail.routine
                    val exCount = routineDetail.exercises.size
                    val exNamesPreview = routineDetail.exercises.take(3).mapNotNull { it.exercise?.name }.joinToString(", ")
                    val targetDays = routine.targetDays.ifBlank { "Flexible split" }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Row: Title, Target Days Badge & 3-Dot Menu
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PeachLight.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🐾", fontSize = 16.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = routine.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "📅 $targetDays",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CoralPrimary
                                            )
                                        }
                                    }
                                }

                                // 3-dot options menu
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = TextLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Routine ✏️", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CoralPrimary) },
                                            onClick = {
                                                menuExpanded = false
                                                routineToEdit = routineDetail
                                                showCreateRoutineModal = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Routine 🗑️", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444)) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                            onClick = {
                                                menuExpanded = false
                                                routineToDelete = routineDetail
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Slate50, thickness = 1.dp)

                            // Bottom Row: Exercise summary preview & Chevron Play Button to launch directly
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$exCount exercises",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextDark
                                    )
                                    if (exNamesPreview.isNotBlank()) {
                                        Text(
                                            text = exNamesPreview + if (exCount > 3) ", +${exCount - 3} more" else "",
                                            fontSize = 11.sp,
                                            color = TextLight,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Play / Start Routine Button
                                Button(
                                    onClick = {
                                        viewModel.startRoutineWorkout(routineDetail)
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Start", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Start Routine",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Active workout logging UI
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.discardActiveWorkout() }) {
                            Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color.Red)
                        }
                    },
                    actions = {
                        TextButton(onClick = { showFinishDialog = true }) {
                            Text("Finish 🐾", fontWeight = FontWeight.Black, color = CoralPrimary, fontSize = 16.sp)
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = if (focusedTarget != null) 360.dp else 120.dp)
                ) {
                    // Active workout timer
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PeachLight.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⏱️", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Active Timer:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Logging live sets", fontSize = 12.sp, fontWeight = FontWeight.Black, color = CoralPrimary)
                            }
                        }
                    }

                    // Empty State if no exercises added
                    if (activeExList.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🐾", fontSize = 48.sp)
                                Text("No exercises added yet!", fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("Tap below to add your first workout exercise.", fontSize = 12.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // Display exercises list
                    items(activeExList) { activeEx ->
                        val sets = setsMap[activeEx.id] ?: emptyList()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Exercise Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(PeachLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(activeEx.exercise.icon ?: "🐾", fontSize = 16.sp)
                                        }
                                        Column {
                                            Text(
                                                text = activeEx.exercise.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = TextDark
                                            )
                                            Text(
                                                text = "${activeEx.exercise.muscleGroup ?: "General"} • ${activeEx.exercise.equipment ?: "Bodyweight"}",
                                                fontSize = 11.sp,
                                                color = TextLight
                                            )
                                        }
                                    }

                                    IconButton(onClick = { viewModel.removeExerciseFromActiveWorkout(activeEx.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }

                                // Rest Timer selection for this exercise
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Rest Timer:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextLight)
                                    listOf(30, 60, 90, 120).forEach { sec ->
                                        val isSelected = activeEx.restSeconds == sec
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) CoralPrimary else Slate100)
                                                .clickable {
                                                    viewModel.updateExerciseRestSeconds(activeEx.id, sec)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${sec}s",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextDark
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = Slate50, thickness = 1.dp)

                                // Sets Table Header
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SET", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextLight, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                    Text("KG", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextLight, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("REPS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextLight, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("DONE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextLight, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                                }

                                // Sets rows with SetRowComponent
                                sets.forEachIndexed { idx, setItem ->
                                    SetRowComponent(
                                        exerciseId = activeEx.id,
                                        setIndex = idx,
                                        setItem = setItem,
                                        focusedTarget = focusedTarget,
                                        onFocusField = { target ->
                                            focusedTarget = target
                                        },
                                        onWeightChange = { newWeight ->
                                            viewModel.updateSetWeight(activeEx.id, idx, newWeight)
                                        },
                                        onRepsChange = { newReps ->
                                            viewModel.updateSetReps(activeEx.id, idx, newReps)
                                        },
                                        onToggleCompleted = {
                                            viewModel.toggleSetCompleted(activeEx.id, idx)
                                        }
                                    )
                                }

                                // Add / Remove Set Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = { viewModel.addSetToActiveExercise(activeEx.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = CoralPrimary)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Add Set", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    if (sets.isNotEmpty()) {
                                        TextButton(
                                            onClick = { viewModel.removeSetFromActiveExercise(activeEx.id) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                                        ) {
                                            Text("- Remove Set", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Exercise Action Button
                    item {
                        OutlinedButton(
                            onClick = onAddExerciseClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, CoralPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CoralPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ Add Exercise to Session", color = CoralPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Docked Bottom Section: Rest Timer + Numeric Keypad
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    // Rest Timer Floating Bar
                    if (timerActive && timerRemaining > 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CoralPrimary),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("🐱💤", fontSize = 20.sp)
                                    Column {
                                        Text("REST PERIOD", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
                                        Text(
                                            text = "${timerRemaining}s Remaining",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { viewModel.adjustRestTimer(15) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("+15s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.stopRestTimer() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Skip", color = CoralPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Custom Numeric Keypad
                    AnimatedVisibility(
                        visible = focusedTarget != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        if (focusedTarget != null) {
                            WorkoutNumericKeypad(
                                focusedTarget = focusedTarget!!,
                                currentValue = currentTargetValue,
                                onValueChange = { newVal ->
                                    val target = focusedTarget ?: return@WorkoutNumericKeypad
                                    if (target.fieldType == SetFieldType.WEIGHT) {
                                        viewModel.updateSetWeight(target.exerciseId, target.setIndex, newVal)
                                    } else {
                                        viewModel.updateSetReps(target.exerciseId, target.setIndex, newVal)
                                    }
                                },
                                onNextField = { moveToNextTarget() },
                                onCloseKeypad = { focusedTarget = null }
                            )
                        }
                    }
                }

                // PR Toast Celebration Full-screen overlay with Confetti & Animation
                AnimatedVisibility(
                    visible = prCelebrationEvent != null || prCelebration != null,
                    enter = scaleIn(animationSpec = spring()) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val event = prCelebrationEvent ?: PrCelebrationEvent(
                        exerciseName = "Exercise",
                        weight = 0.0,
                        reps = 0,
                        prDescription = prCelebration ?: "",
                        isWeightPr = true,
                        isRepPr = false
                    )
                    PrCelebrationOverlay(
                        event = event,
                        onDismiss = {
                            viewModel.prCelebrationEvent.value = null
                            viewModel.prCelebrationText.value = null
                        }
                    )
                }
            }
        }
    }

    // Finish Workout Notes Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Complete Workout Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add some notes/thoughts about your lifting today!")
                    OutlinedTextField(
                        value = workoutNotes,
                        onValueChange = { workoutNotes = it },
                        placeholder = { Text("Felt amazing, hit my push targets! 🐾") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        viewModel.finishActiveWorkout(workoutNotes, onFinishWorkoutSuccess)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                ) {
                    Text("Save & Purr! 😸")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Routine Confirmation Dialog
    if (routineToDelete != null) {
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Delete Routine 🐾", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${routineToDelete?.routine?.name}'? This action will remove the routine from your split.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        routineToDelete?.routine?.id?.let { id ->
                            viewModel.deleteCustomRoutine(id)
                        }
                        routineToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create / Edit Routine Modal
    if (showCreateRoutineModal) {
        CreateRoutineModal(
            viewModel = viewModel,
            routineToEdit = routineToEdit,
            onDismiss = {
                showCreateRoutineModal = false
                routineToEdit = null
            },
            onSavedSuccess = {
                showCreateRoutineModal = false
                routineToEdit = null
            }
        )
    }
}
