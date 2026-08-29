package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import com.example.data.local.ExerciseEntity
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate50
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLight
import com.example.viewmodel.MeowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    viewModel: MeowViewModel,
    onExerciseClick: (ExerciseEntity) -> Unit,
    onStartWorkoutWithExercise: (ExerciseEntity) -> Unit
) {
    val exerciseList by viewModel.exercises.collectAsState()
    var searchQueries by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("All") }
    var selectedEquipment by remember { mutableStateOf("All") }

    var showCustomDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customMuscle by remember { mutableStateOf("Chest") }
    var customEquipment by remember { mutableStateOf("Dumbbells") }
    var customIcon by remember { mutableStateOf("🐾") }

    val muscleGroups = listOf(
        "All", "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core", "Cardio", "Full Body", "Forearms"
    )

    val equipmentList = listOf(
        "All", "Barbell", "Dumbbells", "Bodyweight", "Cable", "Machine", "Kettlebell", "None"
    )

    // Filter exercises dynamically
    val filteredList = remember(exerciseList, searchQueries, selectedMuscle, selectedEquipment) {
        exerciseList.filter { ex ->
            val matchesSearch = searchQueries.isBlank() ||
                    ex.name.contains(searchQueries, ignoreCase = true) ||
                    (ex.muscleGroup?.contains(searchQueries, ignoreCase = true) == true) ||
                    (ex.equipment?.contains(searchQueries, ignoreCase = true) == true)

            val matchesMuscle = when (selectedMuscle) {
                "All" -> true
                "Arms" -> ex.muscleGroup.equals("Arms", true) || ex.muscleGroup.equals("Biceps", true) || ex.muscleGroup.equals("Triceps", true) || ex.muscleGroup.equals("Forearms", true)
                "Legs" -> ex.muscleGroup.equals("Legs", true) || ex.muscleGroup.equals("Quadriceps", true) || ex.muscleGroup.equals("Hamstrings", true) || ex.muscleGroup.equals("Glutes", true) || ex.muscleGroup.equals("Calves", true)
                "Back" -> ex.muscleGroup.equals("Back", true) || ex.muscleGroup.equals("Lats", true) || ex.muscleGroup.equals("Traps", true)
                else -> ex.muscleGroup.equals(selectedMuscle, ignoreCase = true)
            }

            val matchesEquip = when (selectedEquipment) {
                "All" -> true
                else -> ex.equipment?.contains(selectedEquipment, ignoreCase = true) == true
            }

            matchesSearch && matchesMuscle && matchesEquip
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📚", fontSize = 22.sp)
                            Text(
                                text = "Exercise Library",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = TextDark
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshAllExercises() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload Catalog", tint = CoralPrimary)
                        }
                        IconButton(onClick = { showCustomDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Custom Exercise", tint = CoralPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                Divider(color = Slate100, thickness = 1.dp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQueries,
                onValueChange = { searchQueries = it },
                placeholder = { Text("Search ${exerciseList.size} exercises...", color = TextLight, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CoralPrimary) },
                trailingIcon = {
                    if (searchQueries.isNotEmpty()) {
                        IconButton(onClick = { searchQueries = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextLight)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = CoralPrimary,
                    unfocusedIndicatorColor = Slate100
                )
            )

            // Muscle Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(muscleGroups) { group ->
                    val isSelected = selectedMuscle == group
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) CoralPrimary else Color.White)
                            .border(1.dp, if (isSelected) CoralPrimary else Slate100, RoundedCornerShape(14.dp))
                            .clickable { selectedMuscle = group }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = group,
                            color = if (isSelected) Color.White else TextDark,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Equipment Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(equipmentList) { equip ->
                    val isSelected = selectedEquipment == equip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF475569) else Color.White)
                            .border(1.dp, if (isSelected) Color(0xFF475569) else Slate100, RoundedCornerShape(12.dp))
                            .clickable { selectedEquipment = equip }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = equip,
                            color = if (isSelected) Color.White else TextLight,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Results summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredList.size} EXERCISES FOUND",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    letterSpacing = 1.sp
                )
                if (selectedMuscle != "All" || selectedEquipment != "All" || searchQueries.isNotEmpty()) {
                    Text(
                        text = "Reset filters",
                        fontSize = 11.sp,
                        color = CoralPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            selectedMuscle = "All"
                            selectedEquipment = "All"
                            searchQueries = ""
                        }
                    )
                }
            }

            // Exercise items list
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🙀", fontSize = 48.sp)
                        Text("No matching exercises found", fontWeight = FontWeight.Bold, color = TextDark)
                        Text("Try a different search keyword or filter", fontSize = 12.sp, color = TextLight)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                selectedMuscle = "All"
                                selectedEquipment = "All"
                                searchQueries = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Show All Exercises")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredList, key = { it.id }) { ex ->
                        Card(
                            onClick = { onExerciseClick(ex) },
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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Icon Box
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFFECE7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = ex.icon ?: "🐾", fontSize = 20.sp)
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = ex.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextDark
                                            )
                                            if (ex.isCustom) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFE0E7FF))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text("CUSTOM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4338CA))
                                                }
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Muscle pill
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFF1F5F9))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = ex.muscleGroup ?: "General",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF475569)
                                                )
                                            }

                                            // Equipment pill
                                            ex.equipment?.let { equip ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFFFF7ED))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = equip,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFFC2410C)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quick action / View detail arrow
                                Text("➔", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Custom Exercise Dialog
        if (showCustomDialog) {
            AlertDialog(
                onDismissRequest = { showCustomDialog = false },
                title = { Text("🐾 Create Custom Exercise", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Exercise Name") },
                            placeholder = { Text("e.g. Cat Stretches") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Muscle selection
                        Text("Target Muscle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core", "Cardio", "Full Body")) { m ->
                                val sel = customMuscle == m
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) CoralPrimary else Color(0xFFE2E8F0))
                                        .clickable { customMuscle = m }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(m, color = if (sel) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Equipment selection
                        Text("Equipment", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Barbell", "Dumbbells", "Bodyweight", "Cable", "Machine", "Kettlebell", "None")) { eq ->
                                val sel = customEquipment == eq
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) CoralPrimary else Color(0xFFE2E8F0))
                                        .clickable { customEquipment = eq }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(eq, color = if (sel) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Icon selector
                        Text("Select Icon", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("🐾", "💪", "🏋️", "🐈", "🧗", "🏃", "⚡", "🧘", "🔥", "⭐", "🦾", "🤸")) { ic ->
                                val sel = customIcon == ic
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (sel) Color(0xFFFFCCBC) else Color(0xFFF1F5F9))
                                        .clickable { customIcon = ic },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(ic, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customName.isNotBlank()) {
                                viewModel.addCustomExercise(customName, customMuscle, customEquipment, customIcon)
                                customName = ""
                                showCustomDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                    ) {
                        Text("Save Exercise")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}
