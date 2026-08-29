package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExerciseWithSets
import com.example.data.local.HistorySummary
import com.example.data.local.WorkoutWithDetails
import com.example.ui.theme.*
import com.example.viewmodel.MeowViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryViewMode {
    LIST,
    CALENDAR
}

enum class HistoryFilterChip {
    ALL,
    THIS_MONTH,
    WITH_PRS,
    HIGH_VOLUME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MeowViewModel,
    onNavigateToWorkout: () -> Unit = {}
) {
    val isLoading by viewModel.isLoadingHistory
    val detailedWorkouts by viewModel.detailedWorkouts
    var viewMode by remember { mutableStateOf(HistoryViewMode.LIST) }
    var selectedFilter by remember { mutableStateOf(HistoryFilterChip.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCalendarDate by remember { mutableStateOf<String?>(null) } // "yyyy-MM-dd"

    var activeDetailWorkout by remember { mutableStateOf<WorkoutWithDetails?>(null) }
    var workoutToEdit by remember { mutableStateOf<WorkoutWithDetails?>(null) }
    var workoutToDeleteId by remember { mutableStateOf<String?>(null) }

    val summary = remember(detailedWorkouts) {
        viewModel.calculateHistorySummary(detailedWorkouts)
    }

    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val currentMonthStr = remember { monthFormat.format(Date()) }

    // Filtered workouts list
    val filteredWorkouts = remember(detailedWorkouts, searchQuery, selectedFilter, selectedCalendarDate) {
        detailedWorkouts.filter { item ->
            val workout = item.workout
            val startTs = workout.startedAt.toLongOrNull() ?: 0L
            val workoutDateStr = if (startTs > 0) dayFormat.format(Date(startTs)) else ""
            val workoutMonthStr = if (startTs > 0) monthFormat.format(Date(startTs)) else ""

            // Calendar date filter
            if (selectedCalendarDate != null && workoutDateStr != selectedCalendarDate) {
                return@filter false
            }

            // Quick Filter chips
            val matchesFilter = when (selectedFilter) {
                HistoryFilterChip.ALL -> true
                HistoryFilterChip.THIS_MONTH -> workoutMonthStr == currentMonthStr
                HistoryFilterChip.WITH_PRS -> item.prCount > 0
                HistoryFilterChip.HIGH_VOLUME -> item.totalVolume >= 1000.0
            }
            if (!matchesFilter) return@filter false

            // Search query filter
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val titleMatch = workout.title?.lowercase()?.contains(q) == true
                val notesMatch = workout.notes?.lowercase()?.contains(q) == true
                val exerciseMatch = item.exercises.any {
                    it.exercise?.name?.lowercase()?.contains(q) == true ||
                            it.exercise?.muscleGroup?.lowercase()?.contains(q) == true
                }
                if (!titleMatch && !notesMatch && !exerciseMatch) {
                    return@filter false
                }
            }

            true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🐾", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Workout History", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshHistory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh History", tint = CoralPrimary)
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
                .background(BackgroundLight),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Summary Bar Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    HistorySummaryBar(summary = summary)
                }
            }

            // 2. View Toggle (List vs Calendar) & Search Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Segmented Toggle
                    ViewModeSegmentedToggle(
                        currentMode = viewMode,
                        onModeSelected = { viewMode = it }
                    )

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Search exercises, titles, notes...", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = CoralPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = CoralPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        item {
                            HistoryFilterChipItem(
                                label = "All (${detailedWorkouts.size})",
                                isSelected = selectedFilter == HistoryFilterChip.ALL,
                                onClick = { selectedFilter = HistoryFilterChip.ALL }
                            )
                        }
                        item {
                            HistoryFilterChipItem(
                                label = "📅 This Month",
                                isSelected = selectedFilter == HistoryFilterChip.THIS_MONTH,
                                onClick = { selectedFilter = HistoryFilterChip.THIS_MONTH }
                            )
                        }
                        item {
                            HistoryFilterChipItem(
                                label = "🏆 With PRs",
                                isSelected = selectedFilter == HistoryFilterChip.WITH_PRS,
                                onClick = { selectedFilter = HistoryFilterChip.WITH_PRS }
                            )
                        }
                        item {
                            HistoryFilterChipItem(
                                label = "⚡ Volume > 1,000kg",
                                isSelected = selectedFilter == HistoryFilterChip.HIGH_VOLUME,
                                onClick = { selectedFilter = HistoryFilterChip.HIGH_VOLUME }
                            )
                        }
                    }
                }
            }

            // 3. Calendar View (when Calendar mode selected)
            if (viewMode == HistoryViewMode.CALENDAR) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WorkoutCalendarView(
                            workouts = detailedWorkouts,
                            selectedDate = selectedCalendarDate,
                            onSelectDate = { selectedCalendarDate = it }
                        )
                    }
                }
            }

            // 4. Loading Skeleton or Empty States or Workouts List
            if (isLoading && detailedWorkouts.isEmpty()) {
                items(3) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        HistoryCardSkeleton()
                    }
                }
            } else if (filteredWorkouts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("😿", fontSize = 56.sp)
                            Text(
                                text = if (detailedWorkouts.isEmpty()) "No workouts recorded yet" else "No matching workouts found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = if (detailedWorkouts.isEmpty()) "Complete your first workout to build your history and streak!" else "Try adjusting your search query or filters.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            if (detailedWorkouts.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateToWorkout,
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("🐾 Start a Workout Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Section Title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedCalendarDate != null) "Sessions on $selectedCalendarDate" else "Past Sessions (${filteredWorkouts.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        if (selectedCalendarDate != null) {
                            TextButton(onClick = { selectedCalendarDate = null }) {
                                Text("Show All", fontSize = 12.sp, color = CoralPrimary)
                            }
                        }
                    }
                }

                // Workout Cards
                items(filteredWorkouts, key = { it.workout.id }) { workoutDetail ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WorkoutHistoryCard(
                            workoutDetail = workoutDetail,
                            onClick = { activeDetailWorkout = workoutDetail },
                            onRepeat = {
                                viewModel.repeatWorkout(workoutDetail) {
                                    onNavigateToWorkout()
                                }
                            },
                            onEditNotes = { workoutToEdit = workoutDetail },
                            onDelete = { workoutToDeleteId = workoutDetail.workout.id }
                        )
                    }
                }
            }
        }
    }

    // Detail Modal Dialog
    if (activeDetailWorkout != null) {
        WorkoutDetailModal(
            workoutDetail = activeDetailWorkout!!,
            onDismiss = { activeDetailWorkout = null },
            onRepeatWorkout = {
                viewModel.repeatWorkout(it) {
                    onNavigateToWorkout()
                }
            },
            onEditNotes = {
                workoutToEdit = it
            },
            onDeleteWorkout = { workoutId ->
                viewModel.deleteWorkout(workoutId)
                activeDetailWorkout = null
            }
        )
    }

    // Edit Notes Dialog
    if (workoutToEdit != null) {
        val target = workoutToEdit!!
        EditWorkoutNotesDialog(
            initialTitle = target.workout.title ?: "Meow Workout",
            initialNotes = target.workout.notes ?: "",
            onDismiss = { workoutToEdit = null },
            onSave = { newTitle, newNotes ->
                viewModel.updateWorkoutDetails(target.workout.id, newTitle, newNotes)
                workoutToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (workoutToDeleteId != null) {
        val idToDelete = workoutToDeleteId!!
        AlertDialog(
            onDismissRequest = { workoutToDeleteId = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🗑️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Workout Session?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete this workout? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkout(idToDelete)
                        workoutToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDeleteId = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun HistorySummaryBar(summary: HistorySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(listOf(CoralPrimary, PeachLight))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐾", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Activity Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Your feline fitness journey",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Streak Flame Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF7ED))
                        .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🐾🔥", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (summary.currentStreakDays > 0) "${summary.currentStreakDays}-Day Streak" else "Start Streak",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFEA580C)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryStatItem(
                    emoji = "🏋️",
                    value = "${summary.totalWorkouts}",
                    label = "Workouts"
                )
                SummaryStatItem(
                    emoji = "⚡",
                    value = formatSummaryVolume(summary.totalVolumeKg),
                    label = "Volume"
                )
                SummaryStatItem(
                    emoji = "🏆",
                    value = "${summary.totalPrCount}",
                    label = "PRs Set"
                )
            }
        }
    }
}

@Composable
private fun SummaryStatItem(
    emoji: String,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextDark
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun ViewModeSegmentedToggle(
    currentMode: HistoryViewMode,
    onModeSelected: (HistoryViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE2E8F0).copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (currentMode == HistoryViewMode.LIST) Color.White else Color.Transparent)
                .clickable { onModeSelected(HistoryViewMode.LIST) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📋", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "List View",
                    fontWeight = if (currentMode == HistoryViewMode.LIST) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (currentMode == HistoryViewMode.LIST) TextDark else Color(0xFF64748B)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (currentMode == HistoryViewMode.CALENDAR) Color.White else Color.Transparent)
                .clickable { onModeSelected(HistoryViewMode.CALENDAR) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Calendar View",
                    fontWeight = if (currentMode == HistoryViewMode.CALENDAR) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (currentMode == HistoryViewMode.CALENDAR) TextDark else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun HistoryFilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CoralPrimary else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) CoralPrimary else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF475569)
        )
    }
}

@Composable
private fun WorkoutHistoryCard(
    workoutDetail: WorkoutWithDetails,
    onClick: () -> Unit,
    onRepeat: () -> Unit,
    onEditNotes: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title, Date, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF1EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏋️", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = workoutDetail.workout.title ?: "Meow Workout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatCardDate(workoutDetail.workout.startedAt),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // 3-Dot Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(14.dp),
                        containerColor = Color.White
                    ) {
                        DropdownMenuItem(
                            text = { Text("🔁 Repeat Workout", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            onClick = {
                                menuExpanded = false
                                onRepeat()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("✏️ Edit Notes & Title", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            onClick = {
                                menuExpanded = false
                                onEditNotes()
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        DropdownMenuItem(
                            text = { Text("🗑️ Delete Workout", fontSize = 13.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardBadge(emoji = "⏱️", text = "${workoutDetail.durationMinutes}m")
                CardBadge(emoji = "⚡", text = formatSummaryVolume(workoutDetail.totalVolume))
                CardBadge(emoji = "💪", text = "${workoutDetail.totalSets} sets")

                if (workoutDetail.prCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏆", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${workoutDetail.prCount} PR${if (workoutDetail.prCount > 1) "s" else ""}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            // Inline Exercises Preview Chips
            if (workoutDetail.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(workoutDetail.exercises.take(5)) { exDetail ->
                        val ex = exDetail.exercise
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ex?.icon ?: "🐾", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = ex?.name ?: "Exercise",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (workoutDetail.exercises.size > 5) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+${workoutDetail.exercises.size - 5} more",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Notes snippet if present
            if (!workoutDetail.workout.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${workoutDetail.workout.notes}\"",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CardBadge(emoji: String, text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                color = Color(0xFF475569),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HistoryCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF1F5F9))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF8FAFC))
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                )
            }
        }
    }
}

@Composable
private fun EditWorkoutNotesDialog(
    initialTitle: String,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var notes by remember { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✏️", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Session Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Workout Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Session Notes") },
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("How did this workout feel? Claws sharp?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes 🐾", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

private fun formatCardDate(timestampStr: String): String {
    return try {
        val longVal = timestampStr.toLongOrNull() ?: return timestampStr
        val date = Date(longVal)
        val now = Calendar.getInstance()
        val workoutCal = Calendar.getInstance().apply { time = date }

        val isToday = now.get(Calendar.YEAR) == workoutCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == workoutCal.get(Calendar.DAY_OF_YEAR)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        if (isToday) {
            "Today at ${timeFormat.format(date)}"
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            sdf.format(date)
        }
    } catch (e: Exception) {
        timestampStr
    }
}

private fun formatSummaryVolume(volume: Double): String {
    return if (volume >= 1000) {
        String.format(Locale.getDefault(), "%,.0f kg", volume)
    } else {
        "${volume.toInt()} kg"
    }
}
