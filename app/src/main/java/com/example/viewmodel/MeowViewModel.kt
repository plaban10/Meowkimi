@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.remote.MeowUser
import com.example.data.repository.PrEvaluationResult
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Event data for celebratory PR banner & confetti animation
data class PrCelebrationEvent(
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val prDescription: String,
    val isWeightPr: Boolean,
    val isRepPr: Boolean
)

// Struct to represent an ongoing, unsaved active workout exercise
data class ActiveExercise(
    val id: String = UUID.randomUUID().toString(),
    val exercise: ExerciseEntity,
    var restSeconds: Int = 30
)

// Struct to represent an ongoing, unsaved active set
data class ActiveSet(
    val id: String = UUID.randomUUID().toString(),
    val setNumber: Int,
    var weight: String = "0",
    var reps: String = "0",
    var isCompleted: Boolean = false,
    var isPr: Boolean = false
)

// Struct to represent a configured exercise inside routine creator
data class ConfiguredRoutineExercise(
    val exercise: ExerciseEntity,
    var targetSets: Int = 3,
    var targetReps: Int = 10,
    var restSeconds: Int = 60
)


class MeowViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = WorkoutRepository(application)
    val currentUser = repo.currentUser
    val syncStatus = repo.syncStatus

    // Auth fields
    val isAuthLoading = mutableStateOf(false)

    // User Profile
    val profile = currentUser.flatMapLatest { user ->
        if (user != null) repo.getProfileFlow(user.id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Exercises library
    val exercises = repo.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved workouts list
    val workouts = currentUser.flatMapLatest { user ->
        if (user != null) repo.getAllWorkouts(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved custom routines list
    val routines = currentUser.flatMapLatest { user ->
        if (user != null) repo.getAllRoutines(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Detailed Routines State
    val detailedRoutines = mutableStateOf<List<RoutineWithDetails>>(emptyList())
    val isLoadingRoutines = mutableStateOf(false)

    // Detailed Workouts & History States
    val isLoadingHistory = mutableStateOf(false)
    val detailedWorkouts = mutableStateOf<List<WorkoutWithDetails>>(emptyList())
    val selectedWorkoutDetail = mutableStateOf<WorkoutWithDetails?>(null)

    // Sync State
    val isSyncing = mutableStateOf(false)
    val lastSyncedTimestamp = mutableStateOf<String?>(null)


    // --- Active Workout State ---
    val isActiveWorkoutInProgress = mutableStateOf(false)
    val activeWorkoutTitle = mutableStateOf("Today's MeowSession")
    val activeExercises = mutableStateListOf<ActiveExercise>()
    val activeSets = mutableStateMapOf<String, List<ActiveSet>>() // ActiveExercise.id -> List of sets

    // --- Rest Timer State ---
    val isTimerActive = mutableStateOf(false)
    val timerRemaining = mutableStateOf(0)
    val timerTotal = mutableStateOf(30)
    private var timerJob: Job? = null

    // --- PR Congratulation Celebration State ---
    val prCelebrationText = mutableStateOf<String?>(null)
    val prCelebrationEvent = mutableStateOf<PrCelebrationEvent?>(null)

    // Selected exercise details (for stats charting)
    val selectedExerciseForDetail = mutableStateOf<ExerciseEntity?>(null)
    val selectedExerciseHistory = mutableStateOf<List<WorkoutSetEntity>>(emptyList())
    val selectedExerciseBestSet = mutableStateOf<WorkoutSetEntity?>(null)

    init {
        viewModelScope.launch {
            workouts.collectLatest { list ->
                val userId = currentUser.value?.id
                if (userId != null && list.isNotEmpty()) {
                    isLoadingHistory.value = true
                    try {
                        val details = repo.getAllWorkoutsWithDetails(userId)
                        detailedWorkouts.value = details
                    } catch (e: Exception) {
                        Log.e("MeowViewModel", "Error fetching detailed history: ${e.message}")
                    } finally {
                        isLoadingHistory.value = false
                    }
                } else {
                    detailedWorkouts.value = emptyList()
                    isLoadingHistory.value = false
                }
            }
        }

        viewModelScope.launch {
            routines.collectLatest { list ->
                val userId = currentUser.value?.id
                if (userId != null) {
                    isLoadingRoutines.value = true
                    try {
                        val details = repo.getAllRoutinesWithDetails(userId)
                        detailedRoutines.value = details
                    } catch (e: Exception) {
                        Log.e("MeowViewModel", "Error fetching routines: ${e.message}")
                    } finally {
                        isLoadingRoutines.value = false
                    }
                } else {
                    detailedRoutines.value = emptyList()
                    isLoadingRoutines.value = false
                }
            }
        }
        // Collect last synced time from repository
        viewModelScope.launch {
            repo.lastSyncedTime.collect { time ->
                if (time != null) {
                    lastSyncedTimestamp.value = time
                }
            }
        }
    }


    // --- Authentication Actions ---
    fun signInAnonymously(onSuccess: () -> Unit) {
        isAuthLoading.value = true
        viewModelScope.launch {
            val res = repo.signInAnonymously()
            res.onSuccess {
                showToast("Welcome to MeowMuscle! 🐾")
                onSuccess()
            }.onFailure { err ->
                Log.e("MeowMuscleAuth", "Anonymous Sign-In failed: ${err.message}", err)
                showToast("Sign-in error: ${err.message ?: "Authentication failed"}")
            }
            isAuthLoading.value = false
        }
    }

    fun handleLogout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            isActiveWorkoutInProgress.value = false
            activeExercises.clear()
            activeSets.clear()
            stopRestTimer()
            showToast("Logged out")
            onSuccess()
        }
    }

    // --- Active Workout Management ---
    fun startNewWorkout(title: String = "Today's MeowSession") {
        activeWorkoutTitle.value = title
        activeExercises.clear()
        activeSets.clear()
        isActiveWorkoutInProgress.value = true
        stopRestTimer()
    }

    fun addExerciseToActiveWorkout(exercise: ExerciseEntity) {
        val activeEx = ActiveExercise(exercise = exercise)
        activeExercises.add(activeEx)
        // Add one empty set by default
        activeSets[activeEx.id] = listOf(ActiveSet(setNumber = 1))
    }

    fun removeExerciseFromActiveWorkout(activeExerciseId: String) {
        activeExercises.removeAll { it.id == activeExerciseId }
        activeSets.remove(activeExerciseId)
    }

    fun addSetToActiveExercise(activeExerciseId: String) {
        val sets = activeSets[activeExerciseId]?.toMutableList() ?: mutableListOf()
        val nextNumber = sets.size + 1
        val lastSet = sets.lastOrNull()
        val newSet = ActiveSet(
            setNumber = nextNumber,
            weight = lastSet?.weight ?: "0",
            reps = lastSet?.reps ?: "0"
        )
        sets.add(newSet)
        activeSets[activeExerciseId] = sets
    }

    fun removeSetFromActiveExercise(activeExerciseId: String) {
        val sets = activeSets[activeExerciseId]?.toMutableList() ?: return
        if (sets.isNotEmpty()) {
            sets.removeLast()
            activeSets[activeExerciseId] = sets
        }
    }

    fun updateSetValues(activeExerciseId: String, setIndex: Int, weight: String, reps: String) {
        val sets = activeSets[activeExerciseId]?.toMutableList() ?: return
        if (setIndex in sets.indices) {
            sets[setIndex] = sets[setIndex].copy(weight = weight, reps = reps)
            activeSets[activeExerciseId] = sets
        }
    }

    fun updateSetWeight(activeExerciseId: String, setIndex: Int, weight: String) {
        val sets = activeSets[activeExerciseId]?.toMutableList() ?: return
        if (setIndex in sets.indices) {
            sets[setIndex] = sets[setIndex].copy(weight = weight)
            activeSets[activeExerciseId] = sets
        }
    }

    fun updateSetReps(activeExerciseId: String, setIndex: Int, reps: String) {
        val sets = activeSets[activeExerciseId]?.toMutableList() ?: return
        if (setIndex in sets.indices) {
            sets[setIndex] = sets[setIndex].copy(reps = reps)
            activeSets[activeExerciseId] = sets
        }
    }

    fun toggleSetCompleted(activeExerciseId: String, setIndex: Int) {
        val sets = activeSets[activeExerciseId]?.toMutableList() ?: return
        val activeEx = activeExercises.find { it.id == activeExerciseId } ?: return
        if (setIndex in sets.indices) {
            val originalSet = sets[setIndex]
            val nextState = !originalSet.isCompleted
            sets[setIndex] = originalSet.copy(isCompleted = nextState, isPr = if (!nextState) false else originalSet.isPr)
            activeSets[activeExerciseId] = sets

            if (nextState) {
                // If marked completed, evaluate PR instantly for encouragement
                val w = originalSet.weight.toDoubleOrNull() ?: 0.0
                val r = originalSet.reps.toIntOrNull() ?: 0
                if (w > 0.0 && r > 0) {
                    viewModelScope.launch {
                        val prEval = repo.evaluatePrForExercise(activeEx.exercise.id, w, r)
                        if (prEval.isPr) {
                            val currentSets = activeSets[activeExerciseId]?.toMutableList()
                            if (currentSets != null && setIndex in currentSets.indices) {
                                currentSets[setIndex] = currentSets[setIndex].copy(isPr = true)
                                activeSets[activeExerciseId] = currentSets
                            }
                            triggerPrCelebration(activeEx.exercise.name, w, r, prEval)
                        }
                    }
                }
                // Start Rest Timer countdown
                startRestTimer(activeEx.restSeconds)
            }
        }
    }

    private fun triggerPrCelebration(exerciseName: String, weight: Double, reps: Int, eval: PrEvaluationResult) {
        val event = PrCelebrationEvent(
            exerciseName = exerciseName,
            weight = weight,
            reps = reps,
            prDescription = eval.prTypeDescription,
            isWeightPr = eval.isWeightPr,
            isRepPr = eval.isRepPr
        )
        prCelebrationEvent.value = event
        prCelebrationText.value = "🐾 New Personal Record on $exerciseName! 🐾\n${if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()} kg × $reps reps!"

        // Haptic feedback vibration
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 200), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(250)
            }
        } catch (e: Exception) {
            Log.e("MeowViewModel", "Vibrator error: ${e.message}")
        }

        // Sound feedback for PR!
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
        } catch (e: Exception) {
            // ignore audio failures
        }

        viewModelScope.launch {
            delay(4000)
            if (prCelebrationEvent.value == event) {
                prCelebrationEvent.value = null
                prCelebrationText.value = null
            }
        }
    }

    fun finishActiveWorkout(notes: String, onFinished: () -> Unit) {
        val userId = currentUser.value?.id ?: return
        if (activeExercises.isEmpty()) {
            showToast("Add some exercises first!")
            return
        }

        viewModelScope.launch {
            val workoutId = UUID.randomUUID().toString()
            val workout = WorkoutEntity(
                id = workoutId,
                userId = userId,
                title = activeWorkoutTitle.value,
                notes = notes,
                startedAt = System.currentTimeMillis().toString(),
                endedAt = System.currentTimeMillis().toString()
            )

            val localExercises = mutableListOf<WorkoutExerciseEntity>()
            val localSetsMap = mutableMapOf<String, List<WorkoutSetEntity>>()

            for ((index, activeEx) in activeExercises.withIndex()) {
                val workoutExerciseId = UUID.randomUUID().toString()
                localExercises.add(
                    WorkoutExerciseEntity(
                        id = workoutExerciseId,
                        workoutId = workoutId,
                        exerciseId = activeEx.exercise.id,
                        orderIndex = index,
                        restSeconds = activeEx.restSeconds
                    )
                )

                val sets = activeSets[activeEx.id] ?: emptyList()
                val setEntities = sets.map {
                    WorkoutSetEntity(
                        id = UUID.randomUUID().toString(),
                        workoutExerciseId = workoutExerciseId,
                        setNumber = it.setNumber,
                        weight = it.weight.toDoubleOrNull() ?: 0.0,
                        reps = it.reps.toIntOrNull() ?: 0,
                        isPr = it.isPr,
                        completedAt = System.currentTimeMillis().toString()
                    )
                }
                localSetsMap[workoutExerciseId] = setEntities
            }

            val success = repo.saveWorkout(workout, localExercises, localSetsMap)
            if (success) {
                showToast("Workout Saved! Purr-fect job! 😸🐾")
                isActiveWorkoutInProgress.value = false
                activeExercises.clear()
                activeSets.clear()
                stopRestTimer()
                onFinished()
            } else {
                showToast("Failed to save workout session")
            }
        }
    }

    fun discardActiveWorkout() {
        isActiveWorkoutInProgress.value = false
        activeExercises.clear()
        activeSets.clear()
        stopRestTimer()
        showToast("Workout discarded")
    }

    // --- Rest Timer ---
    fun updateExerciseRestSeconds(activeExerciseId: String, restSeconds: Int) {
        val index = activeExercises.indexOfFirst { it.id == activeExerciseId }
        if (index != -1) {
            val current = activeExercises[index]
            activeExercises[index] = current.copy(restSeconds = restSeconds)
        }
    }

    fun adjustRestTimer(secondsToAdd: Int) {
        if (isTimerActive.value) {
            timerRemaining.value = (timerRemaining.value + secondsToAdd).coerceAtLeast(0)
            timerTotal.value = (timerTotal.value + secondsToAdd).coerceAtLeast(0)
        }
    }

    private fun startRestTimer(seconds: Int) {
        timerJob?.cancel()
        timerTotal.value = seconds
        timerRemaining.value = seconds
        isTimerActive.value = true

        timerJob = viewModelScope.launch {
            while (timerRemaining.value > 0) {
                delay(1000)
                timerRemaining.value -= 1
            }
            // Rest finished!
            isTimerActive.value = false
            try {
                // Beep sound for complete rest timer
                val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            } catch (e: Exception) {
                // ignore audio errors
            }
            showToast("Rest time is over! Meowscle up!")
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        isTimerActive.value = false
        timerRemaining.value = 0
    }

    // --- Exercise Picker / Creation ---
    fun addCustomExercise(name: String, muscle: String, equip: String, iconSymbol: String) {
        if (name.isBlank()) {
            showToast("Exercise name cannot be empty")
            return
        }
        viewModelScope.launch {
            repo.createCustomExercise(name, muscle, equip, iconSymbol)
            showToast("Custom exercise added!")
        }
    }

    // --- Exercise Stats & Detail Screen Loader ---
    fun loadExerciseStats(exercise: ExerciseEntity) {
        selectedExerciseForDetail.value = exercise
        viewModelScope.launch {
            val history = repo.getAllSetsForExercise(exercise.id)
            selectedExerciseHistory.value = history
            selectedExerciseBestSet.value = repo.getBestSetForExercise(exercise.id)
        }
    }

    // --- History & Workout Detail Operations ---
    fun refreshHistory() {
        val userId = currentUser.value?.id ?: return
        isLoadingHistory.value = true
        viewModelScope.launch {
            try {
                detailedWorkouts.value = repo.getAllWorkoutsWithDetails(userId)
            } catch (e: Exception) {
                Log.e("MeowViewModel", "Failed to refresh history: ${e.message}")
            } finally {
                isLoadingHistory.value = false
            }
        }
    }

    fun loadWorkoutDetail(workoutId: String) {
        viewModelScope.launch {
            val detail = repo.getWorkoutWithDetails(workoutId)
            selectedWorkoutDetail.value = detail
        }
    }

    fun updateWorkoutDetails(workoutId: String, newTitle: String, newNotes: String?) {
        viewModelScope.launch {
            repo.updateWorkoutDetails(workoutId, newTitle.trim(), newNotes?.trim())
            showToast("Workout updated 🐾")
            refreshHistory()
            if (selectedWorkoutDetail.value?.workout?.id == workoutId) {
                selectedWorkoutDetail.value = repo.getWorkoutWithDetails(workoutId)
            }
        }
    }

    fun repeatWorkout(workoutDetail: WorkoutWithDetails, onReady: () -> Unit) {
        val baseTitle = workoutDetail.workout.title ?: "Meow Workout"
        val newTitle = if (baseTitle.contains("(Repeat)")) baseTitle else "$baseTitle (Repeat)"
        activeWorkoutTitle.value = newTitle
        activeExercises.clear()
        activeSets.clear()

        for (exDetail in workoutDetail.exercises) {
            val exEntity = exDetail.exercise ?: continue
            val activeEx = ActiveExercise(
                exercise = exEntity,
                restSeconds = exDetail.workoutExercise.restSeconds
            )
            activeExercises.add(activeEx)

            val sets = if (exDetail.sets.isNotEmpty()) {
                exDetail.sets.mapIndexed { idx, set ->
                    ActiveSet(
                        setNumber = idx + 1,
                        weight = if (set.weight % 1.0 == 0.0) set.weight.toInt().toString() else set.weight.toString(),
                        reps = set.reps.toString(),
                        isCompleted = false,
                        isPr = false
                    )
                }
            } else {
                listOf(ActiveSet(setNumber = 1))
            }
            activeSets[activeEx.id] = sets
        }

        isActiveWorkoutInProgress.value = true
        stopRestTimer()
        showToast("Loaded ${activeExercises.size} exercises for your new session! 😸")
        onReady()
    }

    fun calculateHistorySummary(list: List<WorkoutWithDetails>): HistorySummary {
        val totalWorkouts = list.size
        val totalVolume = list.sumOf { it.totalVolume }
        val totalPrs = list.sumOf { it.prCount }

        if (list.isEmpty()) {
            return HistorySummary(0, 0.0, 0, 0, 0)
        }

        // Calculate consecutive active days & weeks
        val cal = java.util.Calendar.getInstance()
        val uniqueDays = mutableSetOf<String>()
        val dayFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        list.forEach { item ->
            val ts = item.workout.startedAt.toLongOrNull()
            if (ts != null) {
                uniqueDays.add(dayFormat.format(java.util.Date(ts)))
            }
        }

        // Check active days streak counting backward from today
        var dayStreak = 0
        val checkCal = java.util.Calendar.getInstance()
        
        // If no workout today, check if yesterday had one to continue streak
        val todayStr = dayFormat.format(checkCal.time)
        if (!uniqueDays.contains(todayStr)) {
            checkCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val dateStr = dayFormat.format(checkCal.time)
            if (uniqueDays.contains(dateStr)) {
                dayStreak++
                checkCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        // Estimate active weeks
        val weekStreak = (dayStreak / 3).coerceAtLeast(if (dayStreak > 0) 1 else 0)

        return HistorySummary(
            totalWorkouts = totalWorkouts,
            totalVolumeKg = totalVolume,
            currentStreakWeeks = weekStreak,
            currentStreakDays = dayStreak,
            totalPrCount = totalPrs
        )
    }

    // --- Utilities ---
    fun updateProfileName(newName: String) {
        val userId = currentUser.value?.id ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            repo.updateProfileName(userId, newName.trim())
            showToast("Profile updated!")
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            repo.deleteWorkout(workoutId)
            showToast("Workout deleted")
            if (selectedWorkoutDetail.value?.workout?.id == workoutId) {
                selectedWorkoutDetail.value = null
            }
            refreshHistory()
        }
    }

    fun refreshAllExercises() {
        viewModelScope.launch {
            repo.prepopulateExercises()
            repo.syncRemoteExercisesIfAvailable()
            showToast("Exercise catalog refreshed!")
        }
    }

    fun retryDatabaseSync() {
        performTwoWaySync()
    }

    fun performTwoWaySync() {
        if (isSyncing.value) return
        isSyncing.value = true
        viewModelScope.launch {
            val result = repo.performTwoWaySync()
            result.onSuccess { count ->
                val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                lastSyncedTimestamp.value = timeFormat.format(Date())
                showToast("Synced $count workouts to Cloud! 🐾")
                refreshHistory()
            }.onFailure { err ->
                Log.e("MeowViewModel", "Sync failed: ${err.message}", err)
                val friendly = when {
                    err.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                        "Firestore Rules blocked access. Check Firebase console rules."
                    err.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
                        "Offline mode: Workouts saved locally."
                    else ->
                        "Sync Notice: ${err.localizedMessage ?: "Workouts cached locally"}"
                }
                showToast(friendly)
            }
            isSyncing.value = false
        }
    }

    // --- Custom Routine Actions ---
    fun refreshRoutines() {
        val userId = currentUser.value?.id ?: return
        viewModelScope.launch {
            isLoadingRoutines.value = true
            try {
                detailedRoutines.value = repo.getAllRoutinesWithDetails(userId)
            } catch (e: Exception) {
                Log.e("MeowViewModel", "Failed refreshing routines: ${e.message}")
            } finally {
                isLoadingRoutines.value = false
            }
        }
    }

    fun saveCustomRoutine(
        name: String,
        targetDays: List<String>,
        configuredExercises: List<ConfiguredRoutineExercise>,
        routineIdToEdit: String? = null,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank()) {
            showToast("Please enter a routine name")
            return
        }
        if (configuredExercises.isEmpty()) {
            showToast("Please select at least one exercise")
            return
        }

        viewModelScope.launch {
            val daysString = targetDays.joinToString(", ")
            val routineExercises = configuredExercises.mapIndexed { index, item ->
                RoutineExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    routineId = routineIdToEdit ?: "",
                    exerciseId = item.exercise.id,
                    orderIndex = index,
                    targetSets = item.targetSets.coerceAtLeast(1),
                    targetReps = item.targetReps.coerceAtLeast(1),
                    restSeconds = item.restSeconds.coerceAtLeast(10)
                )
            }

            val result = repo.saveRoutine(
                name = name.trim(),
                targetDays = daysString,
                exercises = routineExercises,
                routineIdToEdit = routineIdToEdit
            )

            result.onSuccess {
                showToast(if (routineIdToEdit != null) "Routine updated! 🐾" else "Routine created! 🐾")
                refreshRoutines()
                onSuccess()
            }.onFailure {
                showToast(it.message ?: "Failed to save routine")
            }
        }
    }

    fun deleteCustomRoutine(routineId: String) {
        viewModelScope.launch {
            val result = repo.deleteRoutine(routineId)
            result.onSuccess {
                showToast("Routine deleted 🐾")
                refreshRoutines()
            }.onFailure {
                showToast(it.message ?: "Failed to delete routine")
            }
        }
    }

    fun startRoutineWorkout(routineDetail: RoutineWithDetails) {
        startNewWorkout(routineDetail.routine.name)
        activeExercises.clear()
        activeSets.clear()

        for (reWithDetail in routineDetail.exercises) {
            val exercise = reWithDetail.exercise ?: continue
            val activeEx = ActiveExercise(
                exercise = exercise,
                restSeconds = reWithDetail.routineExercise.restSeconds
            )
            activeExercises.add(activeEx)

            val sets = (1..reWithDetail.routineExercise.targetSets.coerceAtLeast(1)).map { setNum ->
                ActiveSet(
                    setNumber = setNum,
                    weight = "0",
                    reps = reWithDetail.routineExercise.targetReps.toString(),
                    isCompleted = false,
                    isPr = false
                )
            }
            activeSets[activeEx.id] = sets
        }
    }

    fun showToast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }
}

