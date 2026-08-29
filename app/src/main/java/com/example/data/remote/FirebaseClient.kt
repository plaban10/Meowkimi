package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.ProfileEntity
import com.example.data.local.WorkoutEntity
import com.example.data.local.WorkoutExerciseEntity
import com.example.data.local.WorkoutSetEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MeowUser(
    val id: String,
    val email: String?
)

object FirebaseClient {
    private const val TAG = "FirebaseClient"

    fun isConfigured(context: Context): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: IllegalStateException) {
            try {
                FirebaseApp.initializeApp(context)
                true
            } catch (ex: Exception) {
                Log.w(TAG, "Firebase not initialized: ${ex.message}")
                false
            }
        }
    }

    fun isAuthenticated(): Boolean {
        return try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun upsertProfile(context: Context, profile: ProfileEntity): Boolean {
        if (!isConfigured(context) || !isAuthenticated()) return false
        return try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to profile.id,
                "display_name" to profile.displayName,
                "avatar_url" to profile.avatarUrl
            )
            db.collection("profiles").document(profile.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Profile sync skipped: ${e.message}")
            false
        }
    }

    suspend fun upsertWorkout(context: Context, workout: WorkoutEntity): Boolean {
        if (!isConfigured(context) || !isAuthenticated()) return false
        return try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to workout.id,
                "user_id" to workout.userId,
                "title" to workout.title,
                "started_at" to workout.startedAt,
                "ended_at" to workout.endedAt,
                "notes" to workout.notes
            )
            db.collection("workouts").document(workout.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Workout sync skipped: ${e.message}")
            false
        }
    }

    suspend fun upsertWorkoutExercises(context: Context, workoutExercises: List<WorkoutExerciseEntity>): Boolean {
        if (!isConfigured(context) || !isAuthenticated() || workoutExercises.isEmpty()) return false
        return try {
            val db = FirebaseFirestore.getInstance()
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
        if (!isConfigured(context) || !isAuthenticated() || sets.isEmpty()) return false
        return try {
            val db = FirebaseFirestore.getInstance()
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
        if (!isConfigured(context) || !isAuthenticated()) return false
        return try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to exercise.id,
                "name" to exercise.name,
                "muscleGroup" to exercise.muscleGroup,
                "equipment" to exercise.equipment,
                "icon" to exercise.icon,
                "isCustom" to exercise.isCustom,
                "createdBy" to exercise.createdBy
            )
            db.collection("exercises").document(exercise.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Exercise sync skipped: ${e.message}")
            false
        }
    }

    suspend fun fetchFirestoreExercises(context: Context): List<com.example.data.local.ExerciseEntity> {
        if (!isConfigured(context) || !isAuthenticated()) return emptyList()
        return try {
            val db = FirebaseFirestore.getInstance()
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
            Log.w(TAG, "Exercises fetch skipped or unavailable: ${e.message}")
            emptyList()
        }
    }

    suspend fun upsertRoutine(context: Context, routine: com.example.data.local.RoutineEntity): Boolean {
        if (!isConfigured(context) || !isAuthenticated()) return false
        return try {
            val db = FirebaseFirestore.getInstance()
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
        if (!isConfigured(context) || !isAuthenticated()) return false
        return try {
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()
            // Delete existing or overwrite
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
        if (!isConfigured(context) || !isAuthenticated()) return false
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection("routines").document(routineId).delete().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Routine deletion skipped: ${e.message}")
            false
        }
    }
}

