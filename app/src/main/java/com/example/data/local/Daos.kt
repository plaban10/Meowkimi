package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfile(id: String): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY startedAt DESC")
    fun getAllWorkouts(userId: String): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getWorkoutById(id: String): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Query("UPDATE workouts SET title = :title, notes = :notes WHERE id = :workoutId")
    suspend fun updateWorkoutTitleAndNotes(workoutId: String, title: String, notes: String?)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: String)
}

@Dao
interface WorkoutExerciseDao {
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    fun getExercisesForWorkout(workoutId: String): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    suspend fun getExercisesForWorkoutList(workoutId: String): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE workoutId IN (:workoutIds)")
    suspend fun getExercisesForWorkoutsList(workoutIds: List<String>): List<WorkoutExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExerciseEntity)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: String)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: String)
}

@Dao
interface WorkoutSetDao {
    @Query("SELECT * FROM sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
    fun getSetsForWorkoutExercise(workoutExerciseId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
    suspend fun getSetsForWorkoutExerciseList(workoutExerciseId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM sets WHERE workoutExerciseId IN (:workoutExerciseIds) ORDER BY setNumber ASC")
    suspend fun getSetsForWorkoutExercisesList(workoutExerciseIds: List<String>): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity)

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query("DELETE FROM sets WHERE workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = :workoutId)")
    suspend fun deleteSetsForWorkout(workoutId: String)

    @Query("SELECT s.* FROM sets s INNER JOIN workout_exercises we ON s.workoutExerciseId = we.id INNER JOIN workouts w ON we.workoutId = w.id WHERE we.exerciseId = :exerciseId AND w.userId = :userId ORDER BY (s.weight * s.reps) DESC LIMIT 1")
    suspend fun getBestSetForExercise(exerciseId: String, userId: String): WorkoutSetEntity?

    @Query("SELECT s.* FROM sets s INNER JOIN workout_exercises we ON s.workoutExerciseId = we.id INNER JOIN workouts w ON we.workoutId = w.id WHERE we.exerciseId = :exerciseId AND w.userId = :userId ORDER BY s.completedAt ASC")
    suspend fun getAllSetsForExercise(exerciseId: String, userId: String): List<WorkoutSetEntity>
}

@Dao
interface OfflineSyncQueueDao {
    @Query("SELECT * FROM offline_sync_queue ORDER BY timestamp ASC")
    suspend fun getAllQueueItems(): List<OfflineSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: OfflineSyncQueueEntity)

    @Delete
    suspend fun deleteQueueItem(item: OfflineSyncQueueEntity)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllRoutines(userId: String): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: String): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity)

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: String)
}

@Dao
interface RoutineExerciseDao {
    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun getExercisesForRoutine(routineId: String): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderIndex ASC")
    suspend fun getExercisesForRoutineList(routineId: String): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_exercises WHERE routineId IN (:routineIds) ORDER BY orderIndex ASC")
    suspend fun getExercisesForRoutinesList(routineIds: List<String>): List<RoutineExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercisesForRoutine(routineId: String)
}

