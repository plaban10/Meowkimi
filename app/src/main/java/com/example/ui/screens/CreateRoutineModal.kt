package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ExerciseEntity
import com.example.data.local.RoutineWithDetails
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.PeachLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate50
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLight
import com.example.viewmodel.ConfiguredRoutineExercise
import com.example.viewmodel.MeowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineModal(
    viewModel: MeowViewModel,
    routineToEdit: RoutineWithDetails? = null,
    onDismiss: () -> Unit,
    onSavedSuccess: () -> Unit
) {
    var routineName by remember { mutableStateOf(routineToEdit?.routine?.name ?: "") }
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val initialDays = remember {
        routineToEdit?.routine?.targetDays?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    var selectedDays by remember { mutableStateOf(initialDays.toSet()) }

    // List of configured exercises
    val initialConfiguredExercises = remember {
        routineToEdit?.exercises?.mapNotNull { item ->
            val ex = item.exercise ?: return@mapNotNull null
            ConfiguredRoutineExercise(
                exercise = ex,
                targetSets = item.routineExercise.targetSets,
                targetReps = item.routineExercise.targetReps,
                restSeconds = item.routineExercise.restSeconds
            )
        } ?: emptyList()
    }
    val configuredExercises = remember { mutableStateListOf<ConfiguredRoutineExercise>().apply { addAll(initialConfiguredExercises) } }

    var showExercisePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFBFBFC)),
            color = Color(0xFFFBFBFC)
        ) {
            Scaffold(
                containerColor = Color(0xFFFBFBFC),
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (routineToEdit != null) "Edit MeowRoutine" else "Create New Routine",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Custom split & target workout goals",
                                    fontSize = 12.sp,
                                    color = TextLight
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark)
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    viewModel.saveCustomRoutine(
                                        name = routineName,
                                        targetDays = daysOfWeek.filter { selectedDays.contains(it) },
                                        configuredExercises = configuredExercises,
                                        routineIdToEdit = routineToEdit?.routine?.id,
                                        onSuccess = {
                                            onSavedSuccess()
                                        }
                                    )
                                }
                            ) {
                                Text(
                                    text = if (routineToEdit != null) "Update 🐾" else "Save 🐾",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = CoralPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                ) {
                    // Routine Name Input Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏷️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Routine Name",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                }

                                OutlinedTextField(
                                    value = routineName,
                                    onValueChange = { routineName = it },
                                    placeholder = { Text("e.g. Monday Push Day, Upper Body Split...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CoralPrimary,
                                        unfocusedBorderColor = Slate100,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color(0xFFFAFAFB)
                                    )
                                )
                            }
                        }
                    }

                    // Target Days of the Week Selector Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📅", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Target Days",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextDark
                                        )
                                    }
                                    Text(
                                        text = if (selectedDays.isEmpty()) "Any day" else "${selectedDays.size} days selected",
                                        fontSize = 12.sp,
                                        color = TextLight
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    daysOfWeek.forEach { day ->
                                        val isSelected = selectedDays.contains(day)
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) CoralPrimary else Slate50)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) CoralPrimary else Slate100,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    selectedDays = if (isSelected) {
                                                        selectedDays - day
                                                    } else {
                                                        selectedDays + day
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isSelected) Color.White else TextDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Configured Exercises Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🐾", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Exercises (${configuredExercises.size})",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                            }

                            Button(
                                onClick = { showExercisePicker = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PeachLight.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CoralPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add Exercise", fontWeight = FontWeight.Bold, color = CoralPrimary, fontSize = 12.sp)
                            }
                        }
                    }

                    // Empty Exercises state
                    if (configuredExercises.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showExercisePicker = true },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, CoralPrimary.copy(alpha = 0.3f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(PeachLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🐾", fontSize = 24.sp)
                                    }
                                    Text(
                                        text = "No exercises added yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Tap here or '+ Add Exercise' to choose from your catalog",
                                        fontSize = 12.sp,
                                        color = TextLight,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Configured Exercises items
                    itemsIndexed(configuredExercises) { index, itemConfig ->
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
                                // Exercise Header & Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(PeachLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(itemConfig.exercise.icon ?: "🐾", fontSize = 16.sp)
                                        }
                                        Column {
                                            Text(
                                                text = itemConfig.exercise.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${itemConfig.exercise.muscleGroup ?: "General"} • ${itemConfig.exercise.equipment ?: "Bodyweight"}",
                                                fontSize = 11.sp,
                                                color = TextLight
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { configuredExercises.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Remove Exercise",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Slate50, thickness = 1.dp)

                                // Configuration Parameters: Sets, Reps, Rest Period
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Target Sets counter
                                    NumberStepper(
                                        label = "Sets",
                                        value = itemConfig.targetSets,
                                        unit = "sets",
                                        onValueChange = { newVal ->
                                            configuredExercises[index] = itemConfig.copy(targetSets = newVal.coerceIn(1, 20))
                                        }
                                    )

                                    // Target Reps counter
                                    NumberStepper(
                                        label = "Reps",
                                        value = itemConfig.targetReps,
                                        unit = "reps",
                                        onValueChange = { newVal ->
                                            configuredExercises[index] = itemConfig.copy(targetReps = newVal.coerceIn(1, 100))
                                        }
                                    )

                                    // Rest Period stepper
                                    NumberStepper(
                                        label = "Rest",
                                        value = itemConfig.restSeconds,
                                        unit = "sec",
                                        step = 15,
                                        onValueChange = { newVal ->
                                            configuredExercises[index] = itemConfig.copy(restSeconds = newVal.coerceIn(15, 300))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Save Button Card at bottom
                    item {
                        Button(
                            onClick = {
                                viewModel.saveCustomRoutine(
                                    name = routineName,
                                    targetDays = daysOfWeek.filter { selectedDays.contains(it) },
                                    configuredExercises = configuredExercises,
                                    routineIdToEdit = routineToEdit?.routine?.id,
                                    onSuccess = {
                                        onSavedSuccess()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                            enabled = routineName.isNotBlank() && configuredExercises.isNotEmpty()
                        ) {
                            Text(
                                text = if (routineToEdit != null) "Update MeowRoutine 🐾" else "Save Custom Routine 🐾",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Nested Exercise Picker Dialog
            if (showExercisePicker) {
                RoutineExercisePickerDialog(
                    viewModel = viewModel,
                    onDismiss = { showExercisePicker = false },
                    onExerciseSelected = { ex ->
                        if (configuredExercises.none { it.exercise.id == ex.id }) {
                            configuredExercises.add(
                                ConfiguredRoutineExercise(
                                    exercise = ex,
                                    targetSets = 3,
                                    targetReps = 10,
                                    restSeconds = 60
                                )
                            )
                        }
                        showExercisePicker = false
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    unit: String,
    step: Int = 1,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextLight
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .background(Slate50, RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onValueChange(value - step) },
                contentAlignment = Alignment.Center
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
            }

            Text(
                text = "$value",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextDark,
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onValueChange(value + step) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineExercisePickerDialog(
    viewModel: MeowViewModel,
    onDismiss: () -> Unit,
    onExerciseSelected: (ExerciseEntity) -> Unit
) {
    val exerciseList by viewModel.exercises.collectAsState()
    var searchQueries by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("All") }

    val muscleGroups = listOf(
        "All", "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core", "Cardio", "Full Body"
    )

    val filteredList = remember(exerciseList, searchQueries, selectedMuscle) {
        exerciseList.filter { ex ->
            val matchesSearch = searchQueries.isBlank() ||
                    ex.name.contains(searchQueries, ignoreCase = true) ||
                    (ex.muscleGroup?.contains(searchQueries, ignoreCase = true) == true) ||
                    (ex.equipment?.contains(searchQueries, ignoreCase = true) == true)

            val matchesMuscle = when (selectedMuscle) {
                "All" -> true
                "Arms" -> ex.muscleGroup.equals("Arms", true) || ex.muscleGroup.equals("Biceps", true) || ex.muscleGroup.equals("Triceps", true)
                "Legs" -> ex.muscleGroup.equals("Legs", true) || ex.muscleGroup.equals("Quadriceps", true) || ex.muscleGroup.equals("Hamstrings", true) || ex.muscleGroup.equals("Glutes", true) || ex.muscleGroup.equals("Calves", true)
                else -> ex.muscleGroup.equals(selectedMuscle, ignoreCase = true)
            }
            matchesSearch && matchesMuscle
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Exercise 🐾",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQueries,
                    onValueChange = { searchQueries = it },
                    placeholder = { Text("Search catalog...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CoralPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoralPrimary,
                        unfocusedBorderColor = Slate100
                    )
                )

                // Muscle Group Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(muscleGroups) { muscle ->
                        val isSelected = selectedMuscle.equals(muscle, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMuscle = muscle },
                            label = { Text(muscle, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoralPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Exercise List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { ex ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExerciseSelected(ex) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(ex.icon ?: "🐾", fontSize = 16.sp)
                                    }
                                    Column {
                                        Text(
                                            text = ex.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "${ex.muscleGroup ?: "General"} • ${ex.equipment ?: "Bodyweight"}",
                                            fontSize = 11.sp,
                                            color = TextLight
                                        )
                                    }
                                }

                                Icon(
                                    Icons.Default.AddCircleOutline,
                                    contentDescription = "Select",
                                    tint = CoralPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
