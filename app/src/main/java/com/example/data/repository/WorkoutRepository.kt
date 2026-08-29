package com.example.data.repository

import android.content.Context
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
import kotlinx.coroutines.tasks.await
import java.util.UUID

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


    private val _currentUser = MutableStateFlow<MeowUser?>(null)
    val currentUser: StateFlow<MeowUser?> = _currentUser

    private val _syncStatus = MutableStateFlow<String>("Offline / Local Mode")
    val syncStatus: StateFlow<String> = _syncStatus

    init {
        // Prepopulate default exercises if database has fewer than full catalog
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val exercises = exerciseDao.getAllExercises().first()
                if (exercises.size < ExerciseCatalog.ALL_DEFAULT_EXERCISES.size) {
                    prepopulateExercises()
                }
                syncRemoteExercisesIfAvailable()
            } catch (e: Exception) {
                Log.e("WorkoutRepository", "Error checking exercises: ${e.message}")
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
        if (!FirebaseClient.isConfigured(context) || !FirebaseClient.isAuthenticated()) return@withContext
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
    suspend fun signUp(email: String, password: String, displayName: String): Result<MeowUser> = withContext(Dispatchers.IO) {
        val auth = FirebaseClient.getAuth(context)
        if (auth == null) {
            // Local persistence fallback
            val mockId = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
            val user = MeowUser(mockId, email)
            _currentUser.value = user
            val profile = ProfileEntity(mockId, displayName.ifBlank { email.substringBefore("@") }, null)
            profileDao.insertProfile(profile)
            _syncStatus.value = "Local / Offline Mode"
            return@withContext Result.success(user)
        }

        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val fUser = authResult.user
            if (fUser != null) {
                val user = MeowUser(fUser.uid, fUser.email)
                _currentUser.value = user
                val profile = ProfileEntity(fUser.uid, displayName, null)
                profileDao.insertProfile(profile)
                try {
                    FirebaseClient.upsertProfile(context, profile)
                    _syncStatus.value = "Connected to Firebase"
                } catch (pe: Exception) {
                    Log.d("WorkoutRepository", "Profile sync skipped: ${pe.message}")
                    _syncStatus.value = "Connected (Local Cache)"
                }
                Result.success(user)
            } else {
                Result.failure(Exception("Registration succeeded but no user data was returned."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<MeowUser> = withContext(Dispatchers.IO) {
        val auth = FirebaseClient.getAuth(context)
        if (auth == null) {
            // Local persistence fallback
            val mockId = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
            val user = MeowUser(mockId, email)
            _currentUser.value = user
            val existingProfile = profileDao.getProfile(mockId).first()
            if (existingProfile == null) {
                profileDao.insertProfile(ProfileEntity(mockId, email.substringBefore("@"), null))
            }
            _syncStatus.value = "Local / Offline Mode"
            return@withContext Result.success(user)
        }

        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val fUser = authResult.user
            if (fUser != null) {
                val user = MeowUser(fUser.uid, fUser.email)
                _currentUser.value = user
                val displayName = fUser.displayName ?: fUser.email?.substringBefore("@") ?: "Climber"
                val profile = ProfileEntity(fUser.uid, displayName, null)
                profileDao.insertProfile(profile)
                try {
                    FirebaseClient.upsertProfile(context, profile)
                    _syncStatus.value = "Connected to Firebase"
                } catch (pe: Exception) {
                    Log.d("WorkoutRepository", "Profile sync skipped: ${pe.message}")
                    _syncStatus.value = "Connected (Local Cache)"
                }
                Result.success(user)
            } else {
                Result.failure(Exception("Login succeeded but no user data was returned."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginAsGuest(guestName: String = "Guest Climber"): Result<MeowUser> = withContext(Dispatchers.IO) {
        val guestId = "guest_user_${System.currentTimeMillis()}"
        val user = MeowUser(guestId, "guest@meowmuscle.app")
        _currentUser.value = user
        val profile = ProfileEntity(guestId, guestName, null)
        profileDao.insertProfile(profile)
        _syncStatus.value = "Local / Offline Mode"
        Result.success(user)
    }

    suspend fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential): Result<MeowUser> = withContext(Dispatchers.IO) {
        val auth = FirebaseClient.getAuth(context)
        if (auth == null) {
            val mockEmail = "google.cat@example.com"
            val mockId = UUID.nameUUIDFromBytes(mockEmail.toByteArray()).toString()
            val user = MeowUser(mockId, mockEmail)
            _currentUser.value = user
            val profile = ProfileEntity(mockId, "Google Climber", null)
            profileDao.insertProfile(profile)
            _syncStatus.value = "Local / Offline Mode"
            return@withContext Result.success(user)
        }

        try {
            val authResult = auth.signInWithCredential(credential).await()
            val fUser = authResult.user
            if (fUser != null) {
                val user = MeowUser(fUser.uid, fUser.email)
                _currentUser.value = user
                val displayName = fUser.displayName ?: fUser.email?.substringBefore("@") ?: "Climber"
                val profile = ProfileEntity(fUser.uid, displayName, fUser.photoUrl?.toString())
                profileDao.insertProfile(profile)
                try {
                    FirebaseClient.upsertProfile(context, profile)
                    _syncStatus.value = "Connected to Firebase"
                } catch (pe: Exception) {
                    Log.d("WorkoutRepository", "Profile sync skipped: ${pe.message}")
                    _syncStatus.value = "Connected (Local Cache)"
                }
                Result.success(user)
            } else {
                Result.failure(Exception("Sign-In succeeded but no user data was returned."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        _currentUser.value = null
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
            try {
                FirebaseClient.upsertProfile(context, updated)
                _syncStatus.value = "Connected to Firebase"
            } catch (e: Exception) {
                Log.e("WorkoutRepository", "Failed to sync profile update: ${e.message}")
                if (e.message?.contains("PERMISSION_DENIED") == true || e.message?.contains("permission") == true) {
                    _syncStatus.value = "Sync locked: Set Firestore rules!"
                } else {
                    _syncStatus.value = "Sync Failed (Offline Mode)"
                }
            }
        }
    }

    // --- Exercises ---
    fun getAllExercises(): Flow<List<ExerciseEntity>> {
        return exerciseDao.getAllExercises()
    }

    suspend fun createCustomExercise(name: String, muscleGroup: String, equipment: String, icon: String) = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id
        val exercise = ExerciseEntity(
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
                Log.e("WorkoutRepository", "Failed to sync custom exercise to Firestore: ${e.message}")
            }
        }
    }

    // --- Workouts & Exercises & Sets Persistence ---
    fun getAllWorkouts(userId: String): Flow<List<WorkoutEntity>> {
        return workoutDao.getAllWorkouts(userId)
    }

    fun getExercisesForWorkout(workoutId: String): Flow<List<WorkoutExerciseEntity>> {
        return workoutExerciseDao.getExercisesForWorkout(workoutId)
    }

    fun getSetsForWorkoutExercise(workoutExerciseId: String): Flow<List<WorkoutSetEntity>> {
        return workoutSetDao.getSetsForWorkoutExercise(workoutExerciseId)
    }

    suspend fun getBestSetForExercise(exerciseId: String): WorkoutSetEntity? = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: return@withContext null
        workoutSetDao.getBestSetForExercise(exerciseId, userId)
    }

    suspend fun getAllSetsForExercise(exerciseId: String): List<WorkoutSetEntity> = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: return@withContext emptyList()
        workoutSetDao.getAllSetsForExercise(exerciseId, userId)
    }

    /**
     * Complete Workflow of saving a complete workout session
     */
    suspend fun saveWorkout(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        setsMap: Map<String, List<WorkoutSetEntity>>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Save Workout Row
            workoutDao.insertWorkout(workout)

            // 2. Save Workout Exercises & sets
            for (ex in exercises) {
                workoutExerciseDao.insertWorkoutExercise(ex)
                val sets = setsMap[ex.id] ?: emptyList()
                for (set in sets) {
                    // AUTO PR DETECTION: Calculate maximum weight * reps
                    val isPr = calculateIfPr(ex.exerciseId, set.weight, set.reps)
                    val setWithPr = set.copy(isPr = isPr)
                    workoutSetDao.insertSet(setWithPr)
                }
            }

            // 3. Attempt Live Sync if online
            if (FirebaseClient.isConfigured(context)) {
                syncWorkoutSession(workout, exercises, setsMap)
            } else {
                _syncStatus.value = "Saved Locally (Offline Mode)"
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error saving workout locally: ${e.message}")
            return@withContext false
        }
    }

    suspend fun deleteWorkout(workoutId: String) = withContext(Dispatchers.IO) {
        try {
            workoutSetDao.deleteSetsForWorkout(workoutId)
            workoutExerciseDao.deleteExercisesForWorkout(workoutId)
            workoutDao.deleteWorkout(workoutId)
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error deleting workout cascade: ${e.message}")
        }
    }

    suspend fun updateWorkoutDetails(workoutId: String, title: String, notes: String?) = withContext(Dispatchers.IO) {
        try {
            workoutDao.updateWorkoutTitleAndNotes(workoutId, title, notes)
            val currentWorkout = workoutDao.getWorkoutById(workoutId)
            if (currentWorkout != null && FirebaseClient.isConfigured(context)) {
                FirebaseClient.upsertWorkout(context, currentWorkout)
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error updating workout: ${e.message}")
        }
    }

    suspend fun getWorkoutWithDetails(workoutId: String): WorkoutWithDetails? = withContext(Dispatchers.IO) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext null
        val workoutExercises = workoutExerciseDao.getExercisesForWorkoutList(workoutId)
        val allExercises = exerciseDao.getAllExercises().first().associateBy { it.id }

        var totalVolume = 0.0
        var totalSets = 0
        var prCount = 0

        val exerciseDetails = workoutExercises.map { we ->
            val sets = workoutSetDao.getSetsForWorkoutExerciseList(we.id)
            sets.forEach { set ->
                totalVolume += (set.weight * set.reps)
                totalSets++
                if (set.isPr) prCount++
            }
            ExerciseWithSets(
                workoutExercise = we,
                exercise = allExercises[we.exerciseId],
                sets = sets
            )
        }

        val startTs = workout.startedAt.toLongOrNull() ?: 0L
        val endTs = workout.endedAt?.toLongOrNull() ?: (startTs + (totalSets.coerceAtLeast(1) * 150_000L))
        val durationMins = if (endTs > startTs) (endTs - startTs) / 60000L else (totalSets.coerceAtLeast(1) * 3L)

        WorkoutWithDetails(
            workout = workout,
            exercises = exerciseDetails,
            totalVolume = totalVolume,
            totalSets = totalSets,
            prCount = prCount,
            durationMinutes = durationMins.coerceAtLeast(1L)
        )
    }

    suspend fun getAllWorkoutsWithDetails(userId: String): List<WorkoutWithDetails> = withContext(Dispatchers.IO) {
        val workoutsList = workoutDao.getAllWorkouts(userId).first()
        if (workoutsList.isEmpty()) return@withContext emptyList()

        val allExercises = exerciseDao.getAllExercises().first().associateBy { it.id }
        val workoutIds = workoutsList.map { it.id }
        val allWorkoutExercises = workoutExerciseDao.getExercisesForWorkoutsList(workoutIds)
        val workoutExMap = allWorkoutExercises.groupBy { it.workoutId }
        val weIds = allWorkoutExercises.map { it.id }
        val allSets = workoutSetDao.getSetsForWorkoutExercisesList(weIds)
        val setMap = allSets.groupBy { it.workoutExerciseId }

        workoutsList.map { workout ->
            val wExercises = workoutExMap[workout.id] ?: emptyList()
            var totalVolume = 0.0
            var totalSets = 0
            var prCount = 0

            val exerciseDetails = wExercises.sortedBy { it.orderIndex }.map { we ->
                val sets = setMap[we.id] ?: emptyList()
                sets.forEach { set ->
                    totalVolume += (set.weight * set.reps)
                    totalSets++
                    if (set.isPr) prCount++
                }
                ExerciseWithSets(
                    workoutExercise = we,
                    exercise = allExercises[we.exerciseId],
                    sets = sets
                )
            }

            val startTs = workout.startedAt.toLongOrNull() ?: 0L
            val endTs = workout.endedAt?.toLongOrNull() ?: (startTs + (totalSets.coerceAtLeast(1) * 150_000L))
            val durationMins = if (endTs > startTs) (endTs - startTs) / 60000L else (totalSets.coerceAtLeast(1) * 3L)

            WorkoutWithDetails(
                workout = workout,
                exercises = exerciseDetails,
                totalVolume = totalVolume,
                totalSets = totalSets,
                prCount = prCount,
                durationMinutes = durationMins.coerceAtLeast(1L)
            )
        }
    }

    private suspend fun calculateIfPr(exerciseId: String, weight: Double, reps: Int): Boolean {
        val userId = _currentUser.value?.id ?: return false
        val bestSet = workoutSetDao.getBestSetForExercise(exerciseId, userId) ?: return true
        val currentMaxVolume = weight * reps
        val historicalMaxVolume = bestSet.weight * bestSet.reps
        return currentMaxVolume > historicalMaxVolume
    }

    // --- Sync Engine ---
    private suspend fun syncWorkoutSession(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        setsMap: Map<String, List<WorkoutSetEntity>>
    ) {
        try {
            // Upload Workout Row to Firestore
            val wSuccess = FirebaseClient.upsertWorkout(context, workout)
            if (!wSuccess) throw Exception("Workout row sync failed")

            // Upload Exercises mapping to Firestore
            val weSuccess = FirebaseClient.upsertWorkoutExercises(context, exercises)
            if (!weSuccess) throw Exception("Workout exercise rows sync failed")

            // Upload sets to Firestore
            val flatSets = setsMap.values.flatten()
            if (flatSets.isNotEmpty()) {
                val sSuccess = FirebaseClient.upsertSets(context, flatSets)
                if (!sSuccess) throw Exception("Set rows sync failed")
            }

            // Mark Synced locally
            workoutDao.insertWorkout(workout.copy(isSynced = true))
            _syncStatus.value = "Synced with Firebase Live"
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Sync failed: ${e.message}")
            if (e.message?.contains("PERMISSION_DENIED") == true || e.message?.contains("permission") == true) {
                _syncStatus.value = "Sync locked: Set Firestore rules!"
            } else {
                _syncStatus.value = "Pending Sync (Offline Mode)"
            }
            // Queue workout for offline sync retry
            queueOfflineSync(workout, exercises, setsMap)
        }
    }

    private suspend fun queueOfflineSync(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        setsMap: Map<String, List<WorkoutSetEntity>>
    ) {
        // Queue the workout id for sync
        offlineQueueDao.insertQueueItem(
            OfflineSyncQueueEntity(
                table = "workouts",
                action = "INSERT",
                recordId = workout.id,
                payload = workout.id // simple payload refers to the ID in local DB
            )
        )
    }

    suspend fun retryOfflineSync() = withContext(Dispatchers.IO) {
        if (!FirebaseClient.isConfigured(context)) return@withContext

        val queue = offlineQueueDao.getAllQueueItems()
        if (queue.isEmpty()) {
            _syncStatus.value = "All Workouts Synced"
            return@withContext
        }

        _syncStatus.value = "Syncing ${queue.size} pending items..."

        for (item in queue) {
            try {
                if (item.table == "workouts") {
                    val workoutId = item.recordId
                    val workout = workoutDao.getWorkoutById(workoutId)
                    if (workout != null) {
                        val exercises = workoutExerciseDao.getExercisesForWorkoutList(workoutId)
                        val setsMap = mutableMapOf<String, List<WorkoutSetEntity>>()
                        for (ex in exercises) {
                            setsMap[ex.id] = workoutSetDao.getSetsForWorkoutExerciseList(ex.id)
                        }
                        // Re-try upload
                        syncWorkoutSession(workout, exercises, setsMap)
                    }
                    offlineQueueDao.deleteQueueItem(item)
                }
            } catch (e: Exception) {
                Log.e("WorkoutRepository", "Offline item retry failed: ${e.message}")
                _syncStatus.value = "Sync interrupted. Will retry later."
                return@withContext
            }
        }
        _syncStatus.value = "Sync completed successfully!"
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

            // Save Routine
            routineDao.insertRoutine(routine)

            // Clear old exercises for this routine and insert updated ones
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

            // Sync to Firestore if online
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

