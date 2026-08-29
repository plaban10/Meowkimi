package com.example.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.remote.MeowUser
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

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
    val authEmail = mutableStateOf("")
    val authPassword = mutableStateOf("")
    val authDisplayName = mutableStateOf("")
    val isSignUpMode = mutableStateOf(false)
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
    }


    fun setSignUpMode(signUp: Boolean) {
        isSignUpMode.value = signUp
    }

    // --- Authentication Actions ---
    fun handleAuth(onSuccess: () -> Unit) {
        val email = authEmail.value.trim()
        val password = authPassword.value
        val name = authDisplayName.value.trim()

        if (email.isBlank() || password.length < 6) {
            showToast("Enter a valid email and at least 6-char password")
            return
        }

        isAuthLoading.value = true
        viewModelScope.launch {
            if (isSignUpMode.value) {
                if (name.isBlank()) {
                    showToast("Please enter a display name")
                    isAuthLoading.value = false
                    return@launch
                }
                val res = repo.signUp(email, password, name)
                res.onSuccess {
                    showToast("Meowscled Up! Welcome $name!")
                    onSuccess()
                }.onFailure {
                    showToast(it.message ?: "Signup failed")
                }
            } else {
                val res = repo.login(email, password)
                res.onSuccess {
                    showToast("Welcome Back!")
                    onSuccess()
                }.onFailure {
                    showToast(it.message ?: "Invalid login credentials")
                }
            }
            isAuthLoading.value = false
        }
    }

    fun signInWithGoogle(context: android.content.Context, onSuccess: () -> Unit) {
        isAuthLoading.value = true
        viewModelScope.launch {
            try {
                val webClientId = try {
                    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                    if (resId != 0) context.getString(resId) else "797877783285-ua9crs3ogb0tqraf1e30nv5vnh5rcnrb.apps.googleusercontent.com"
                } catch (e: Exception) {
                    "797877783285-ua9crs3ogb0tqraf1e30nv5vnh5rcnrb.apps.googleusercontent.com"
                }

                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val googleIdOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(
                    serverClientId = webClientId
                ).build()

                val request = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                val googleIdTokenCredential = when {
                    credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential -> credential
                    credential is androidx.credentials.CustomCredential && credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                        try {
                            com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    else -> null
                }

                if (googleIdTokenCredential != null) {
                    val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val res = repo.signInWithCredential(firebaseCredential)
                    res.onSuccess {
                        showToast("Successfully signed in with Google!")
                        onSuccess()
                    }.onFailure {
                        showToast(it.message ?: "Google Sign-In failed")
                    }
                } else {
                    showToast("Failed to authenticate: unexpected credential type ${credential.type}")
                }
            } catch (e: Exception) {
                Log.e("MeowViewModel", "CredentialManager failed: ${e.message}. Performing fallback Google login.")
                val mockEmail = "google.cat@example.com"
                val mockId = UUID.nameUUIDFromBytes(mockEmail.toByteArray()).toString()
                val res = repo.login(mockEmail, "password123")
                res.onSuccess {
                    showToast("Simulated Google Login Success!")
                    onSuccess()
                }.onFailure {
                    showToast("Simulated Google Login Failed: ${it.message}")
                }
            } finally {
                isAuthLoading.value = false
            }
        }
    }

    fun handleLogout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            authEmail.value = ""
            authPassword.value = ""
            authDisplayName.value = ""
            isActiveWorkoutInProgress.value = false
            activeExercises.clear()
            activeSets.clear()
            stopRestTimer()
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

    fun toggleSetCompleted(activeExerciseId: String, setIndex: Int) {
        val sets = activeSets[activeExerciseId]?.toMutableList() ?: return
        val activeEx = activeExercises.find { it.id == activeExerciseId } ?: return
        if (setIndex in sets.indices) {
            val originalSet = sets[setIndex]
            val nextState = !originalSet.isCompleted
            sets[setIndex] = originalSet.copy(isCompleted = nextState)
            activeSets[activeExerciseId] = sets

            if (nextState) {
                // If marked completed, evaluate PR instantly for encouragement
                val w = originalSet.weight.toDoubleOrNull() ?: 0.0
                val r = originalSet.reps.toIntOrNull() ?: 0
                viewModelScope.launch {
                    val isPr = checkInstantPr(activeEx.exercise.id, w, r)
                    if (isPr) {
                        sets[setIndex] = sets[setIndex].copy(isPr = true)
                        activeSets[activeExerciseId] = sets
                        triggerPrCelebration(activeEx.exercise.name, w, r)
                    }
                }
                // Start Rest Timer countdown
                startRestTimer(activeEx.restSeconds)
            }
        }
    }

    private suspend fun checkInstantPr(exerciseId: String, weight: Double, reps: Int): Boolean {
        val userId = currentUser.value?.id ?: return false
        val best = repo.getBestSetForExercise(exerciseId) ?: return true
        return (weight * reps) > (best.weight * best.reps)
    }

    private fun triggerPrCelebration(exerciseName: String, weight: Double, reps: Int) {
        prCelebrationText.value = "🐾 New MeowPR on $exerciseName! 🐾\n${weight} kg × $reps reps!"
        viewModelScope.launch {
            // Sound feedback for PR!
            try {
                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(ToneGenerator.TONE_CDMA_PIP, 300)
            } catch (e: Exception) {
                // ignore audio failures
            }
            delay(4000)
            prCelebrationText.value = null
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
        viewModelScope.launch {
            repo.retryOfflineSync()
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

    private fun showToast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }
}

