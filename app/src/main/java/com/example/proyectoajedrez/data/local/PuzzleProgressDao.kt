package com.example.proyectoajedrez.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleProgressDao {
    @Query("SELECT * FROM puzzle_progress WHERE id = 1")
    fun getProgress(): Flow<PuzzleProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: PuzzleProgressEntity)
}