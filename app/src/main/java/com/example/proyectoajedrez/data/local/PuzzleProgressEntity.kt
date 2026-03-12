package com.example.proyectoajedrez.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "puzzle_progress")
data class PuzzleProgressEntity(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val totalSolved: Int = 0,
    val lastSolvedDate: String = ""
)