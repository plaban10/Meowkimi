package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String, // UUID from auth.users
    val displayName: String?,
    val avatarUrl: String?,
    val createdAt: String = System.currentTimeMillis().toString()
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String?,
    val equipment: String?,
    val icon: String?, // name of icon or drawable
    val isCustom: Boolean = false,
    val createdBy: String? = null
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String?,
    val startedAt: String = System.currentTimeMillis().toString(),
    val endedAt: String? = null,
    val notes: String? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val restSeconds: Int = 30
)

@Entity(tableName = "sets")
data class WorkoutSetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutExerciseId: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isPr: Boolean = false,
    val completedAt: String = System.currentTimeMillis().toString(),
    val isSynced: Boolean = false
)

@Entity(tableName = "offline_sync_queue")
data class OfflineSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val table: String, // "workouts", "sets", or "routines"
    val action: String, // "INSERT", "UPDATE", "DELETE"
    val recordId: String, // ID of the referenced record
    val payload: String, // JSON payload representing the record
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val targetDays: String = "", // e.g. "Mon, Wed, Fri"
    val createdAt: String = System.currentTimeMillis().toString(),
    val updatedAt: String = System.currentTimeMillis().toString()
)

@Entity(tableName = "routine_exercises")
data class RoutineExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val restSeconds: Int = 60
)
