package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.*
import com.example.data.remote.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PrEvaluationResult(
    val isPr: Boolean = false,
    val isWeightPr: Boolean = false,
    val isRepPr: Boolean = false,
    val prTypeDescription: String = "",
    val previousMaxWeight: Double = 0.0,
    val previousMaxReps: Int = 0
)

class WorkoutRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val profileDao = db.profileDao()
    private val exerciseDao = db.exerciseDao()
    private val workoutDao = db.workoutDao()
    private val workoutExerciseDao = db.workoutExerciseDao()
    private val workoutSetDao = db.workoutSetDao()
    private val offlineQueueDao = db.offlineSyncQueueDao()
    private val routineDao = db.routineDao()
    private val routineExerciseDao = db.routineExerciseDao()

    private val prefs: SharedPreferences = context.getSharedPreferences("gym_cat_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<MeowUser?>(null)
    val currentUser: StateFlow<MeowUser?> = _currentUser

    private val _syncStatus = MutableStateFlow<String>("Initializing...")
    val syncStatus: StateFlow<String> = _syncStatus

    private val _lastSyncedTime = MutableStateFlow<String?>(null)
    val lastSyncedTime: StateFlow<String?> = _lastSyncedTime

    init {
        // 1. Initialize Firebase with programmatic fallback support
        FirebaseClient.initFirebase(context)

        // 2. Restore cached user from SharedPreferences
        val savedUid = prefs.getString("saved_user_uid", null)
        val savedEmail = prefs.getString("saved_user_email", "anonymous@meowmuscle.app")
        val savedLastSync = prefs.getString("last_synced_timestamp", null)
        _lastSyncedTime.value = savedLastSync

        val initialUid = savedUid ?: FirebaseClient.getAuth(context)?.currentUser?.uid ?: run {
            val generated = "anon_" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString("saved_user_uid", generated).apply()
            generated
        }

        _currentUser.value = MeowUser(initialUid, savedEmail)
        _syncStatus.value = "Local Cache Ready"

        // 3. Launch async initialization (Auth + Exercises + Cloud Sync)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure profile exists in local DB
                val current = _currentUser.value
                if (current != null) {
                    val p = profileDao.getProfile(current.id).first()
                    if (p == null) {
                        profileDao.insertProfile(ProfileEntity(current.id, "Gym Cat 🐾", null))
                    }
                }

                // Prepopulate exercise catalog if empty
                val exercises = exerciseDao.getAllExercises().first()
                if (exercises.size < ExerciseCatalog.ALL_DEFAULT_EXERCISES.size) {
                    prepopulateExercises()
                }

                // Ensure Firebase Anonymous Auth
                val authResult = FirebaseClient.ensureAnonymousAuth(context)
                authResult.onSuccess { fUid ->
                    _currentUser.value = MeowUser(fUid, "anonymous@meowmuscle.app")
                    prefs.edit().putString("saved_user_uid", fUid).apply()
                    _syncStatus.value = "Connected to Firebase"
                    // Trigger initial cloud sync in background
                    performTwoWaySync()
                }.onFailure { err ->
                    Log.w("WorkoutRepository", "Initial Firebase Auth skipped/offline: ${err.message}")
                    _syncStatus.value = "Offline Mode (Local Active)"
                }
            } catch (e: Exception) {
                Log.e("WorkoutRepository", "Error during repository init: ${e.message}", e)
                prepopulateExercises()
            }
        }
    }

    suspend fun prepopulateExercises() = withContext(Dispatchers.IO) {
        try {
            exerciseDao.insertExercises(ExerciseCatalog.ALL_DEFAULT_EXERCISES)
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Failed to insert default exercise catalog: ${e.message}")
        }
    }

    suspend fun syncRemoteExercisesIfAvailable() = withContext(Dispatchers.IO) {
        if (!FirebaseClient.isConfigured(context)) return@withContext
        try {
            val remoteExercises = FirebaseClient.fetchFirestoreExercises(context)
            if (remoteExercises.isNotEmpty()) {
                exerciseDao.insertExercises(remoteExercises)
            }
        } catch (e: Exception) {
            Log.d("WorkoutRepository", "Remote exercises fetch skipped: ${e.message}")
        }
    }

    // --- Authentication ---
    suspend fun signInAnonymously(): Result<MeowUser> = withContext(Dispatchers.IO) {
        val authResult = FirebaseClient.ensureAnonymousAuth(context)
        authResult.fold(
            onSuccess = { uid ->
                val user = MeowUser(uid, "anonymous@meowmuscle.app")
                _currentUser.value = user
                prefs.edit().putString("saved_user_uid", uid).putString("saved_user_email", user.email).apply()

                val profile = ProfileEntity(uid, "Gym Cat 🐾", null)
                profileDao.insertProfile(profile)
                try {
                    FirebaseClient.upsertProfile(context, profile)
                    _syncStatus.value = "Connected to Firebase"
                } catch (pe: Exception) {
                    Log.d("WorkoutRepository", "Profile sync skipped: ${pe.message}")
                    _syncStatus.value = "Connected (Local Cache)"
                }
                Result.success(user)
            },
            onFailure = { err ->
                Log.w("WorkoutRepository", "Firebase auth failed, falling back to persistent local UID: ${err.message}")
                val localId = prefs.getString("saved_user_uid", null) ?: ("anon_" + UUID.randomUUID().toString().take(12))
                prefs.edit().putString("saved_user_uid", localId).apply()
                val user = MeowUser(localId, "anonymous@meowmuscle.app")
                _currentUser.value = user
                profileDao.insertProfile(ProfileEntity(localId, "Gym Cat 🐾", null))
                _syncStatus.value = "Local / Offline Mode"
                Result.success(user)
            }
        )
    }

    suspend fun logout() {
        val newAnonId = "anon_" + UUID.randomUUID().toString().take(12)
        prefs.edit().putString("saved_user_uid", newAnonId).apply()
        _currentUser.value = MeowUser(newAnonId, "anonymous@meowmuscle.app")
        try {
            FirebaseClient.getAuth(context)?.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        _syncStatus.value = "Offline / Local Mode"
    }

    // --- Profile ---
    fun getProfileFlow(userId: String): Flow<ProfileEntity?> {
        return profileDao.getProfile(userId)
    }

    suspend fun updateProfileName(userId: String, name: String) = withContext(Dispatchers.IO) {
        val currentProfile = profileDao.getProfile(userId).first()
        val updated = currentProfile?.copy(displayName = name) ?: ProfileEntity(userId, name, null)
        profileDao.insertProfile(updated)
        if (FirebaseClient.isConfigured(context)) {
            FirebaseClient.upsertProfile(context, updated)
        }
    }

    // --- Exercise Library ---
    fun getAllExercises(): Flow<List<ExerciseEntity>> {
        return exerciseDao.getAllExercises()
    }

    suspend fun createCustomExercise(name: String, muscleGroup: String, equipment: String, icon: String): ExerciseEntity = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id
        val exercise = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            muscleGroup = muscleGroup,
            equipment = equipment,
            icon = icon,
            isCustom = true,
            createdBy = userId
        )
        exerciseDao.insertExercise(exercise)
        if (FirebaseClient.isConfigured(context)) {
            try {
                FirebaseClient.upsertExercise(context, exercise)
            } catch (e: Exception) {
                Log.e("WorkoutRepository", "Remote custom exercise sync failed: ${e.message}")
            }
        }
        exercise
    }

    // --- Workouts & Exercises Saving with Immediate Local Write + Cloud Sync ---
    suspend fun saveWorkout(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        setsMap: Map<String, List<WorkoutSetEntity>>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Immediately persist in local Room database (Guaranteed instant save)
            workoutDao.insertWorkout(workout.copy(isSynced = false))
            for (ex in exercises) {
                workoutExerciseDao.insertWorkoutExercise(ex)
            }
            for ((_, sets) in setsMap) {
                for (set in sets) {
                    workoutSetDao.insertSet(set.copy(isSynced = false))
                }
            }

            // 2. Attempt immediate cloud push if online
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val currentUid = _currentUser.value?.id ?: workout.userId
                    val pushOk = FirebaseClient.pushUserWorkout(context, currentUid, workout, exercises, setsMap)
                    if (pushOk) {
                        workoutDao.insertWorkout(workout.copy(isSynced = true))
                        for ((_, sets) in setsMap) {
                            for (set in sets) {
                                workoutSetDao.insertSet(set.copy(isSynced = true))
                            }
                        }
                        updateLastSyncTimestamp()
                    }
                } catch (e: Exception) {
                    Log.w("WorkoutRepository", "Background cloud workout push deferred: ${e.message}")
                }
            }

            true
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Failed to save workout locally: ${e.message}", e)
            false
        }
    }

    // --- Exercise History & PR Evaluation ---
    suspend fun getAllSetsForExercise(exerciseId: String): List<WorkoutSetEntity> = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: return@withContext emptyList()
        workoutSetDao.getAllSetsForExercise(exerciseId, userId)
    }

    suspend fun getBestSetForExercise(exerciseId: String): WorkoutSetEntity? = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: return@withContext null
        workoutSetDao.getBestSetForExercise(exerciseId, userId)
    }

    suspend fun evaluatePrForExercise(exerciseId: String, currentWeight: Double, currentReps: Int): PrEvaluationResult = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: return@withContext PrEvaluationResult()
        val allPastSets = workoutSetDao.getAllSetsForExercise(exerciseId, userId)

        if (allPastSets.isEmpty()) {
            return@withContext PrEvaluationResult(
                isPr = true,
                isWeightPr = true,
                isRepPr = true,
                prTypeDescription = "First time logging this exercise! 🥇",
                previousMaxWeight = 0.0,
                previousMaxReps = 0
            )
        }

        val maxWeightPast = allPastSets.maxOfOrNull { it.weight } ?: 0.0
        val maxRepsAtWeight = allPastSets.filter { it.weight == currentWeight }.maxOfOrNull { it.reps } ?: 0

        val isWeightPr = currentWeight > maxWeightPast
        val isRepPr = !isWeightPr && currentWeight == maxWeightPast && currentReps > maxRepsAtWeight

        if (isWeightPr) {
            return@withContext PrEvaluationResult(
                isPr = true,
                isWeightPr = true,
                isRepPr = false,
                prTypeDescription = "All-Time Weight PR! 🏆",
                previousMaxWeight = maxWeightPast,
                previousMaxReps = maxRepsAtWeight
            )
        }

        if (isRepPr) {
            return@withContext PrEvaluationResult(
                isPr = true,
                isWeightPr = false,
                isRepPr = true,
                prTypeDescription = "Rep PR at this weight! 🔥",
                previousMaxWeight = maxWeightPast,
                previousMaxReps = maxRepsAtWeight
            )
        }

        PrEvaluationResult(
            isPr = false,
            previousMaxWeight = maxWeightPast,
            previousMaxReps = maxRepsAtWeight
        )
    }

    // --- Workouts List with Full Relational Details ---
    fun getAllWorkouts(userId: String): Flow<List<WorkoutEntity>> {
        return workoutDao.getAllWorkouts(userId)
    }

    suspend fun getAllWorkoutsWithDetails(userId: String): List<WorkoutWithDetails> = withContext(Dispatchers.IO) {
        val workouts = workoutDao.getAllWorkouts(userId).first()
        val allExercises = exerciseDao.getAllExercises().first().associateBy { it.id }

        workouts.map { workout ->
            val workoutExercises = workoutExerciseDao.getExercisesForWorkoutList(workout.id)
            val exerciseDetails = workoutExercises.map { we ->
                val sets = workoutSetDao.getSetsForWorkoutExerciseList(we.id)
                ExerciseWithSets(
                    workoutExercise = we,
                    exercise = allExercises[we.exerciseId],
                    sets = sets
                )
            }

            val allSets = exerciseDetails.flatMap { it.sets }
            val totalVolume = allSets.sumOf { it.weight * it.reps }
            val totalSets = allSets.size
            val prCount = allSets.count { it.isPr }

            val startMs = workout.startedAt.toLongOrNull() ?: 0L
            val endMs = workout.endedAt?.toLongOrNull() ?: startMs
            val durationMin = if (startMs > 0L && endMs >= startMs) (endMs - startMs) / 60000L else 0L

            WorkoutWithDetails(
                workout = workout,
                exercises = exerciseDetails,
                totalVolume = totalVolume,
                totalSets = totalSets,
                prCount = prCount,
                durationMinutes = durationMin
            )
        }
    }

    suspend fun getWorkoutWithDetails(workoutId: String): WorkoutWithDetails? = withContext(Dispatchers.IO) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext null
        val allExercises = exerciseDao.getAllExercises().first().associateBy { it.id }
        val workoutExercises = workoutExerciseDao.getExercisesForWorkoutList(workout.id)

        val exerciseDetails = workoutExercises.map { we ->
            val sets = workoutSetDao.getSetsForWorkoutExerciseList(we.id)
            ExerciseWithSets(
                workoutExercise = we,
                exercise = allExercises[we.exerciseId],
                sets = sets
            )
        }

        val allSets = exerciseDetails.flatMap { it.sets }
        val totalVolume = allSets.sumOf { it.weight * it.reps }
        val totalSets = allSets.size
        val prCount = allSets.count { it.isPr }

        val startMs = workout.startedAt.toLongOrNull() ?: 0L
        val endMs = workout.endedAt?.toLongOrNull() ?: startMs
        val durationMin = if (startMs > 0L && endMs >= startMs) (endMs - startMs) / 60000L else 0L

        WorkoutWithDetails(
            workout = workout,
            exercises = exerciseDetails,
            totalVolume = totalVolume,
            totalSets = totalSets,
            prCount = prCount,
            durationMinutes = durationMin
        )
    }

    suspend fun updateWorkoutDetails(workoutId: String, title: String, notes: String?) = withContext(Dispatchers.IO) {
        workoutDao.updateWorkoutTitleAndNotes(workoutId, title, notes)
    }

    suspend fun deleteWorkout(workoutId: String) = withContext(Dispatchers.IO) {
        workoutDao.deleteWorkout(workoutId)
        workoutExerciseDao.deleteExercisesForWorkout(workoutId)
        workoutSetDao.deleteSetsForWorkout(workoutId)
    }

    // --- Resilient Two-Way Cloud Synchronization ---
    suspend fun performTwoWaySync(): Result<Int> = withContext(Dispatchers.IO) {
        _syncStatus.value = "Connecting to Cloud..."

        // Ensure Firebase is initialized
        if (!FirebaseClient.isConfigured(context)) {
            FirebaseClient.initFirebase(context)
        }

        // Ensure user is authenticated
        var user = _currentUser.value
        val authResult = FirebaseClient.ensureAnonymousAuth(context)
        if (authResult.isSuccess) {
            val fUid = authResult.getOrNull()!!
            user = MeowUser(fUid, "anonymous@meowmuscle.app")
            _currentUser.value = user
            prefs.edit().putString("saved_user_uid", fUid).apply()
        }

        val userId = user?.id ?: prefs.getString("saved_user_uid", null)
        if (userId == null) {
            _syncStatus.value = "Sign in required to sync"
            return@withContext Result.failure(Exception("Not logged in. Please sign in first."))
        }

        _syncStatus.value = "Syncing with Firestore..."

        try {
            // 1. Fetch Cloud Workouts from Firestore (Cloud -> Local)
            val cloudSessions = FirebaseClient.fetchUserWorkouts(context, userId)
            for (session in cloudSessions) {
                workoutDao.insertWorkout(session.workout.copy(isSynced = true))
                for (ex in session.exercises) {
                    workoutExerciseDao.insertWorkoutExercise(ex)
                }
                for (set in session.sets) {
                    workoutSetDao.insertSet(set.copy(isSynced = true))
                }
            }

            // 2. Push Local Workouts to Firestore (Local -> Cloud)
            val localWorkouts = workoutDao.getAllWorkouts(userId).first()
            for (w in localWorkouts) {
                val exercises = workoutExerciseDao.getExercisesForWorkoutList(w.id)
                val setsMap = mutableMapOf<String, List<WorkoutSetEntity>>()
                for (ex in exercises) {
                    setsMap[ex.id] = workoutSetDao.getSetsForWorkoutExerciseList(ex.id)
                }
                val pushSuccess = FirebaseClient.pushUserWorkout(context, userId, w, exercises, setsMap)
                if (pushSuccess) {
                    workoutDao.insertWorkout(w.copy(isSynced = true))
                }
            }

            // 3. Sync profile and custom routines
            val currentProfile = profileDao.getProfile(userId).first()
            if (currentProfile != null) {
                FirebaseClient.upsertProfile(context, currentProfile)
            }

            // 4. Update sync timestamp and status
            updateLastSyncTimestamp()
            val finalCount = workoutDao.getAllWorkouts(userId).first().size
            _syncStatus.value = "Connected to Firebase ($finalCount workouts synced)"
            Result.success(finalCount)
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Two-way sync failed: ${e.message}", e)
            val errorMsg = when {
                e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                    "Permission Denied: Check Firestore Security Rules"
                e.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
                    "Offline: Saved locally, will sync when connected"
                else ->
                    "Offline Mode (Local Cache Active)"
            }
            _syncStatus.value = errorMsg
            Result.failure(e)
        }
    }

    private fun updateLastSyncTimestamp() {
        val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        val formatted = timeFormat.format(Date())
        _lastSyncedTime.value = formatted
        prefs.edit().putString("last_synced_timestamp", formatted).apply()
    }

    // --- Custom Routines Operations ---
    fun getAllRoutines(userId: String): Flow<List<RoutineEntity>> {
        return routineDao.getAllRoutines(userId)
    }

    suspend fun getAllRoutinesWithDetails(userId: String): List<RoutineWithDetails> = withContext(Dispatchers.IO) {
        val routines = routineDao.getAllRoutines(userId).first()
        val allExercises = exerciseDao.getAllExercises().first().associateBy { it.id }

        routines.map { routine ->
            val routineExercises = routineExerciseDao.getExercisesForRoutineList(routine.id)
            val exerciseDetails = routineExercises.map { re ->
                RoutineExerciseWithDetails(
                    routineExercise = re,
                    exercise = allExercises[re.exerciseId]
                )
            }
            RoutineWithDetails(
                routine = routine,
                exercises = exerciseDetails
            )
        }
    }

    suspend fun saveRoutine(
        name: String,
        targetDays: String,
        exercises: List<RoutineExerciseEntity>,
        routineIdToEdit: String? = null
    ): Result<RoutineWithDetails> = withContext(Dispatchers.IO) {
        try {
            val userId = _currentUser.value?.id ?: UUID.randomUUID().toString()
            val routineId = routineIdToEdit ?: UUID.randomUUID().toString()
            val now = System.currentTimeMillis().toString()

            val routine = RoutineEntity(
                id = routineId,
                userId = userId,
                name = name,
                targetDays = targetDays,
                createdAt = now,
                updatedAt = now
            )

            routineDao.insertRoutine(routine)
            routineExerciseDao.deleteExercisesForRoutine(routineId)
            val mappedExercises = exercises.mapIndexed { index, item ->
                item.copy(
                    id = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id,
                    routineId = routineId,
                    orderIndex = index
                )
            }
            if (mappedExercises.isNotEmpty()) {
                routineExerciseDao.insertRoutineExercises(mappedExercises)
            }

            if (FirebaseClient.isConfigured(context)) {
                try {
                    FirebaseClient.upsertRoutine(context, routine)
                    FirebaseClient.upsertRoutineExercises(context, routineId, mappedExercises)
                } catch (e: Exception) {
                    Log.e("WorkoutRepository", "Remote routine sync failed: ${e.message}")
                }
            }

            val allExercises = exerciseDao.getAllExercises().first().associateBy { it.id }
            val exerciseDetails = mappedExercises.map { re ->
                RoutineExerciseWithDetails(
                    routineExercise = re,
                    exercise = allExercises[re.exerciseId]
                )
            }

            Result.success(RoutineWithDetails(routine = routine, exercises = exerciseDetails))
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Failed to save routine: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteRoutine(routineId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            routineDao.deleteRoutine(routineId)
            routineExerciseDao.deleteExercisesForRoutine(routineId)

            if (FirebaseClient.isConfigured(context)) {
                try {
                    FirebaseClient.deleteRoutine(context, routineId)
                } catch (e: Exception) {
                    Log.e("WorkoutRepository", "Remote routine deletion failed: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Failed to delete routine: ${e.message}")
            Result.failure(e)
        }
    }
}
