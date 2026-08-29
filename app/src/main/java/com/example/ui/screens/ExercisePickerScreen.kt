package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExerciseEntity
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.PeachLight
import com.example.viewmodel.MeowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    viewModel: MeowViewModel,
    onBackClick: () -> Unit,
    onExerciseSelected: (ExerciseEntity) -> Unit
) {
    val exerciseList by viewModel.exercises.collectAsState()
    var searchQueries by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("All") }

    var showCustomDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customMuscle by remember { mutableStateOf("Chest") }
    var customEquipment by remember { mutableStateOf("Dumbbells") }
    var customIcon by remember { mutableStateOf("🐾") }

    val muscleGroups = listOf(
        "All", "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core", "Cardio", "Full Body", "Forearms"
    )

    // Filter exercises dynamically
    val filteredList = exerciseList.filter { ex ->
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
        matchesSearch && matchesMuscle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick Exercise 🏋️", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Custom", tint = CoralPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQueries,
                onValueChange = { searchQueries = it },
                placeholder = { Text("Search exercise (e.g. Push Ups)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CoralPrimary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Muscle Filter LazyRow
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(muscleGroups) { group ->
                    val isSelected = selectedMuscle == group
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) CoralPrimary else Color(0xFFE2E2E9))
                            .clickable { selectedMuscle = group }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = group,
                            color = if (isSelected) Color.White else TextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Exercise items
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🙀", fontSize = 48.sp)
                        Text("No matching exercises found", fontWeight = FontWeight.Bold, color = Color.Gray)
                        TextButton(onClick = { showCustomDialog = true }) {
                            Text("Create a Custom Exercise", color = CoralPrimary)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { ex ->
                        Card(
                            onClick = { onExerciseSelected(ex) },
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFFECE7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(ex.icon ?: "🐾", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = ex.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "${ex.muscleGroup ?: "General"} • ${ex.equipment ?: "None"}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = CoralPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Exercise Creator Dialog
    if (showCustomDialog) {
        val muscles = listOf("Chest", "Back", "Legs", "Arms", "Core", "Cardio")
        val equipmentTypes = listOf("Dumbbells", "Barbell", "Bodyweight", "Machine", "None")
        val iconEmojis = listOf("🐾", "🐈", "🦁", "💪", "🏋️", "🧘", "🧗", "🏃🐾")

        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Create Custom Exercise") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Exercise Name") },
                        placeholder = { Text("Incline Dumbbell Flyes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Muscle selection
                    Text("Select Target Muscle:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(muscles) { m ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (customMuscle == m) CoralPrimary else Color(0xFFF0F1F5))
                                    .clickable { customMuscle = m }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(m, color = if (customMuscle == m) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Equipment selection
                    Text("Select Equipment:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(equipmentTypes) { eq ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (customEquipment == eq) LavenderAccent else Color(0xFFF0F1F5))
                                    .clickable { customEquipment = eq }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(eq, color = if (customEquipment == eq) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Icon Emojis selection
                    Text("Select Cat-Paw Icon:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(iconEmojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (customIcon == emoji) PeachLight else Color(0xFFF0F1F5))
                                    .clickable { customIcon = emoji }
                                    .padding(8.dp)
                            ) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCustomExercise(customName, customMuscle, customEquipment, customIcon)
                        showCustomDialog = false
                        customName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                ) {
                    Text("Add Exercise")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
