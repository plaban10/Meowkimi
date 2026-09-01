package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.ProfileEntity
import com.example.data.local.WorkoutEntity
import com.example.data.local.WorkoutExerciseEntity
import com.example.data.local.WorkoutSetEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.tasks.await

data class MeowUser(
    val id: String,
    val email: String? = null
)

data class CloudWorkoutSession(
    val workout: WorkoutEntity,
    val exercises: List<WorkoutExerciseEntity>,
    val sets: List<WorkoutSetEntity>
)

object FirebaseClient {
    private const val TAG = "FirebaseClient"

    // Default fallback project configuration (allows app to function if google-services.json is omitted in git)
    private const val FALLBACK_APP_ID = "1:797877783285:android:e36878d63438cedf1293bc"
    private const val FALLBACK_API_KEY = "AIzaSyBUkX8-8mJSbhXHDr0WT6LVLPvLmigikBw"
    private const val FALLBACK_PROJECT_ID = "meowkimi-c9a59"
    private const val FALLBACK_STORAGE_BUCKET = "meowkimi-c9a59.firebasestorage.app"

    private var isFirestoreSettingsConfigured = false

    /**
     * Initializes Firebase safely. Tries default resource configuration from google-services.json first,
     * and seamlessly falls back to programmatic FirebaseOptions if missing.
     */
    fun initFirebase(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                    Log.d(TAG, "Firebase auto-initialized successfully via google-services.json")
                } catch (e: Exception) {
                    Log.w(TAG, "Default Firebase initialization skipped: ${e.message}. Using fallback options.")
                    try {
                        val options = FirebaseOptions.Builder()
                            .setApplicationId(FALLBACK_APP_ID)
                            .setApiKey(FALLBACK_API_KEY)
                            .setProjectId(FALLBACK_PROJECT_ID)
                            .setStorageBucket(FALLBACK_STORAGE_BUCKET)
                            .build()
                        FirebaseApp.initializeApp(context, options)
                        Log.d(TAG, "Firebase successfully initialized with fallback FirebaseOptions")
                    } catch (fe: Exception) {
                        Log.e(TAG, "Firebase fallback options initialization failed: ${fe.message}", fe)
                    }
                }
            }

            // Configure Firestore offline persistence
            configureFirestoreSettings(context)

            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Throwable) {
            Log.e(TAG, "Firebase initialization error: ${e.message}", e)
            false
        }
    }

    private fun configureFirestoreSettings(context: Context) {
        if (isFirestoreSettingsConfigured) return
        try {
            val firestore = getFirestore(context) ?: return
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                firestore.firestoreSettings = settings
                isFirestoreSettingsConfigured = true
                Log.d(TAG, "Firestore offline persistence configured with PersistentCacheSettings")
            } catch (e: Throwable) {
                @Suppress("DEPRECATION")
                val fallbackSettings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = fallbackSettings
                isFirestoreSettingsConfigured = true
                Log.d(TAG, "Firestore offline persistence configured with setPersistenceEnabled(true)")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to configure Firestore settings: ${e.message}")
        }
    }

    fun isConfigured(context: Context): Boolean {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                true
            } else {
                initFirebase(context)
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Firebase not configured: ${e.message}")
            false
        }
    }

    fun getAuth(context: Context): FirebaseAuth? {
        if (!isConfigured(context)) return null
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.d(TAG, "FirebaseAuth unavailable: ${e.message}")
            null
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        if (!isConfigured(context)) return null
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.d(TAG, "FirebaseFirestore unavailable: ${e.message}")
            null
        }
    }

    fun getCurrentUserId(context: Context): String? {
        return getAuth(context)?.currentUser?.uid
    }

    fun isAuthenticated(context: Context? = null): Boolean {
        return try {
            val auth = if (context != null) getAuth(context) else FirebaseAuth.getInstance()
            auth?.currentUser != null
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Ensures the user is authenticated anonymously with Firebase.
     * Returns the Firebase User ID.
     */
    suspend fun ensureAnonymousAuth(context: Context): Result<String> {
        val auth = getAuth(context) ?: return Result.failure(Exception("Firebase Auth not initialized"))
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return Result.success(currentUser.uid)
        }
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: return Result.failure(Exception("Anonymous login succeeded but user is null"))
            Log.d(TAG, "Anonymous user signed in with UID: ${user.uid}")
            Result.success(user.uid)
        } catch (e: Exception) {
            Log.e(TAG, "Failed anonymous auth: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun upsertProfile(context: Context, profile: ProfileEntity): Boolean {
        val db = getFirestore(context) ?: return false
        val userId = getCurrentUserId(context) ?: profile.id
        return try {
            val data = mapOf(
                "id" to profile.id,
                "display_name" to (profile.displayName ?: "Gym Cat 🐾"),
                "avatar_url" to (profile.avatarUrl ?: ""),
                "updated_at" to System.currentTimeMillis().toString()
            )
            // Upsert in user subcollection and root profiles
            db.collection("users").document(userId).collection("profile").document("main").set(data).await()
            db.collection("profiles").document(profile.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Profile sync skipped: ${e.message}")
            false
        }
    }

    suspend fun fetchProfile(context: Context, userId: String): ProfileEntity? {
        val db = getFirestore(context) ?: return null
        return try {
            val doc = db.collection("users").document(userId).collection("profile").document("main").get().await()
            if (doc.exists()) {
                val displayName = doc.getString("display_name") ?: "Gym Cat 🐾"
                val avatarUrl = doc.getString("avatar_url")
                ProfileEntity(id = userId, displayName = displayName, avatarUrl = avatarUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch profile skipped: ${e.message}")
            null
        }
    }

    suspend fun upsertWorkout(context: Context, workout: WorkoutEntity): Boolean {
        val db = getFirestore(context) ?: return false
        return try {
            val data = mapOf(
                "id" to workout.id,
                "user_id" to workout.userId,
                "title" to (workout.title ?: "Workout Session"),
                "started_at" to workout.startedAt,
                "ended_at" to (workout.endedAt ?: ""),
                "notes" to (workout.notes ?: "")
            )
            db.collection("workouts").document(workout.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Workout sync skipped: ${e.message}")
            false
        }
    }

    suspend fun upsertWorkoutExercises(context: Context, workoutExercises: List<WorkoutExerciseEntity>): Boolean {
        val db = getFirestore(context) ?: return false
        if (workoutExercises.isEmpty()) return true
        return try {
            val batch = db.batch()
            for (we in workoutExercises) {
                val docRef = db.collection("workout_exercises").document(we.id)
                val data = mapOf(
                    "id" to we.id,
                    "workout_id" to we.workoutId,
                    "exercise_id" to we.exerciseId,
                    "order_index" to we.orderIndex,
                    "rest_seconds" to we.restSeconds
                )
                batch.set(docRef, data)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Workout exercises sync skipped: ${e.message}")
            false
        }
    }

    suspend fun upsertSets(context: Context, sets: List<WorkoutSetEntity>): Boolean {
        val db = getFirestore(context) ?: return false
        if (sets.isEmpty()) return true
        return try {
            val batch = db.batch()
            for (set in sets) {
                val docRef = db.collection("sets").document(set.id)
                val data = mapOf(
                    "id" to set.id,
                    "workout_exercise_id" to set.workoutExerciseId,
                    "set_number" to set.setNumber,
                    "weight" to set.weight,
                    "reps" to set.reps,
                    "is_pr" to set.isPr,
                    "completed_at" to set.completedAt
                )
                batch.set(docRef, data)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Sets sync skipped: ${e.message}")
            false
        }
    }

    suspend fun upsertExercise(context: Context, exercise: com.example.data.local.ExerciseEntity): Boolean {
        val db = getFirestore(context) ?: return false
        return try {
            val data = mapOf(
                "id" to exercise.id,
                "name" to exercise.name,
                "muscleGroup" to (exercise.muscleGroup ?: ""),
                "equipment" to (exercise.equipment ?: ""),
                "icon" to (exercise.icon ?: "🐾"),
                "isCustom" to exercise.isCustom,
                "createdBy" to (exercise.createdBy ?: "")
            )
            db.collection("exercises").document(exercise.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Exercise sync skipped: ${e.message}")
            false
        }
    }

    suspend fun fetchFirestoreExercises(context: Context): List<com.example.data.local.ExerciseEntity> {
        val db = getFirestore(context) ?: return emptyList()
        return try {
            val snapshot = db.collection("exercises").get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val muscleGroup = doc.getString("muscleGroup")
                val equipment = doc.getString("equipment")
                val icon = doc.getString("icon") ?: "🐾"
                val isCustom = doc.getBoolean("isCustom") ?: false
                val createdBy = doc.getString("createdBy")
                com.example.data.local.ExerciseEntity(
                    id = id,
                    name = name,
                    muscleGroup = muscleGroup,
                    equipment = equipment,
                    icon = icon,
                    isCustom = isCustom,
                    createdBy = createdBy
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exercises fetch skipped: ${e.message}")
            emptyList()
        }
    }

    suspend fun upsertRoutine(context: Context, routine: com.example.data.local.RoutineEntity): Boolean {
        val db = getFirestore(context) ?: return false
        return try {
            val data = mapOf(
                "id" to routine.id,
                "user_id" to routine.userId,
                "name" to routine.name,
                "target_days" to routine.targetDays,
                "created_at" to routine.createdAt,
                "updated_at" to routine.updatedAt
            )
            db.collection("routines").document(routine.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Routine sync skipped: ${e.message}")
            false
        }
    }

    suspend fun upsertRoutineExercises(context: Context, routineId: String, routineExercises: List<com.example.data.local.RoutineExerciseEntity>): Boolean {
        val db = getFirestore(context) ?: return false
        if (routineExercises.isEmpty()) return true
        return try {
            val batch = db.batch()
            for (re in routineExercises) {
                val docRef = db.collection("routine_exercises").document(re.id)
                val data = mapOf(
                    "id" to re.id,
                    "routine_id" to re.routineId,
                    "exercise_id" to re.exerciseId,
                    "order_index" to re.orderIndex,
                    "target_sets" to re.targetSets,
                    "target_reps" to re.targetReps,
                    "rest_seconds" to re.restSeconds
                )
                batch.set(docRef, data)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Routine exercises sync skipped: ${e.message}")
            false
        }
    }

    suspend fun deleteRoutine(context: Context, routineId: String): Boolean {
        val db = getFirestore(context) ?: return false
        return try {
            db.collection("routines").document(routineId).delete().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Routine deletion skipped: ${e.message}")
            false
        }
    }

    /**
     * Pushes a full workout session to users/{userId}/workouts/{workoutId} with sub-data
     */
    suspend fun pushUserWorkout(
        context: Context,
        userId: String,
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        setsMap: Map<String, List<WorkoutSetEntity>>
    ): Boolean {
        val db = getFirestore(context) ?: return false
        return try {
            val exercisesList = exercises.map { we ->
                val sets = setsMap[we.id] ?: emptyList()
                mapOf(
                    "id" to we.id,
                    "workoutId" to we.workoutId,
                    "exerciseId" to we.exerciseId,
                    "orderIndex" to we.orderIndex,
                    "restSeconds" to we.restSeconds,
                    "sets" to sets.map { s ->
                        mapOf(
                            "id" to s.id,
                            "workoutExerciseId" to s.workoutExerciseId,
                            "setNumber" to s.setNumber,
                            "weight" to s.weight,
                            "reps" to s.reps,
                            "isPr" to s.isPr,
                            "completedAt" to s.completedAt
                        )
                    }
                )
            }

            val workoutData = mapOf(
                "id" to workout.id,
                "userId" to userId,
                "title" to (workout.title ?: "Meow Workout"),
                "startedAt" to workout.startedAt,
                "endedAt" to (workout.endedAt ?: ""),
                "notes" to (workout.notes ?: ""),
                "isSynced" to true,
                "updatedAt" to System.currentTimeMillis().toString(),
                "exercises" to exercisesList
            )

            // Save under users/{userId}/workouts/{workoutId}
            db.collection("users")
                .document(userId)
                .collection("workouts")
                .document(workout.id)
                .set(workoutData)
                .await()

            // Also keep top-level tables updated for queries
            upsertWorkout(context, workout)
            upsertWorkoutExercises(context, exercises)
            val flatSets = setsMap.values.flatten()
            if (flatSets.isNotEmpty()) {
                upsertSets(context, flatSets)
            }

            Log.d(TAG, "Successfully synced workout ${workout.id} to Firestore under user $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push workout to users/$userId/workouts: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches all workouts saved for the given user from Firestore
     */
    suspend fun fetchUserWorkouts(context: Context, userId: String): List<CloudWorkoutSession> {
        val db = getFirestore(context) ?: return emptyList()
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("workouts")
                .get()
                .await()

            val sessions = mutableListOf<CloudWorkoutSession>()

            for (doc in snapshot.documents) {
                val workoutId = doc.getString("id") ?: doc.id
                val docUserId = doc.getString("userId") ?: doc.getString("user_id") ?: userId
                val title = doc.getString("title") ?: "Meow Workout"
                val startedAt = doc.get("startedAt")?.toString()
                    ?: doc.get("started_at")?.toString()
                    ?: System.currentTimeMillis().toString()
                val endedAt = doc.get("endedAt")?.toString()
                    ?: doc.get("ended_at")?.toString()
                val notes = doc.getString("notes")

                val workoutEntity = WorkoutEntity(
                    id = workoutId,
                    userId = docUserId,
                    title = title,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    notes = notes,
                    isSynced = true
                )

                val exercisesList = mutableListOf<WorkoutExerciseEntity>()
                val setsList = mutableListOf<WorkoutSetEntity>()

                val rawExercises = doc.get("exercises") as? List<*>
                if (rawExercises != null) {
                    for ((index, item) in rawExercises.withIndex()) {
                        if (item is Map<*, *>) {
                            val weId = item["id"]?.toString() ?: java.util.UUID.randomUUID().toString()
                            val exId = item["exerciseId"]?.toString() ?: item["exercise_id"]?.toString() ?: continue
                            val orderIndex = (item["orderIndex"] as? Number)?.toInt()
                                ?: (item["order_index"] as? Number)?.toInt()
                                ?: index
                            val restSec = (item["restSeconds"] as? Number)?.toInt()
                                ?: (item["rest_seconds"] as? Number)?.toInt()
                                ?: 30

                            val weEntity = WorkoutExerciseEntity(
                                id = weId,
                                workoutId = workoutId,
                                exerciseId = exId,
                                orderIndex = orderIndex,
                                restSeconds = restSec
                            )
                            exercisesList.add(weEntity)

                            val rawSets = item["sets"] as? List<*>
                            if (rawSets != null) {
                                for ((setIdx, setObj) in rawSets.withIndex()) {
                                    if (setObj is Map<*, *>) {
                                        val sId = setObj["id"]?.toString() ?: java.util.UUID.randomUUID().toString()
                                        val sNum = (setObj["setNumber"] as? Number)?.toInt()
                                            ?: (setObj["set_number"] as? Number)?.toInt()
                                            ?: (setIdx + 1)
                                        val weight = (setObj["weight"] as? Number)?.toDouble() ?: 0.0
                                        val reps = (setObj["reps"] as? Number)?.toInt() ?: 0
                                        val isPr = (setObj["isPr"] as? Boolean)
                                            ?: (setObj["is_pr"] as? Boolean)
                                            ?: false
                                        val completedAt = setObj["completedAt"]?.toString()
                                            ?: setObj["completed_at"]?.toString()
                                            ?: startedAt

                                        val setEntity = WorkoutSetEntity(
                                            id = sId,
                                            workoutExerciseId = weId,
                                            setNumber = sNum,
                                            weight = weight,
                                            reps = reps,
                                            isPr = isPr,
                                            completedAt = completedAt,
                                            isSynced = true
                                        )
                                        setsList.add(setEntity)
                                    }
                                }
                            }
                        }
                    }
                }

                sessions.add(
                    CloudWorkoutSession(
                        workout = workoutEntity,
                        exercises = exercisesList,
                        sets = setsList
                    )
                )
            }

            Log.d(TAG, "Fetched ${sessions.size} cloud workout sessions for user $userId")
            sessions
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user workouts: ${e.message}", e)
            emptyList()
        }
    }
}
