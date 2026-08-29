package com.example.data.local

data class ExerciseWithSets(
    val workoutExercise: WorkoutExerciseEntity,
    val exercise: ExerciseEntity?,
    val sets: List<WorkoutSetEntity>
)

data class WorkoutWithDetails(
    val workout: WorkoutEntity,
    val exercises: List<ExerciseWithSets>,
    val totalVolume: Double,
    val totalSets: Int,
    val prCount: Int,
    val durationMinutes: Long
)

data class HistorySummary(
    val totalWorkouts: Int,
    val totalVolumeKg: Double,
    val currentStreakWeeks: Int,
    val currentStreakDays: Int,
    val totalPrCount: Int
)

data class RoutineExerciseWithDetails(
    val routineExercise: RoutineExerciseEntity,
    val exercise: ExerciseEntity?
)

data class RoutineWithDetails(
    val routine: RoutineEntity,
    val exercises: List<RoutineExerciseWithDetails>
)

