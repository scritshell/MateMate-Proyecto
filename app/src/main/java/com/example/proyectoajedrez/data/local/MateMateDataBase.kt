package com.example.proyectoajedrez.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PuzzleProgressEntity::class], version = 1, exportSchema = false)
abstract class MateMateDataBase : RoomDatabase() {

    abstract fun puzzleProgressDao(): PuzzleProgressDao

    companion object {
        @Volatile private var INSTANCE: MateMateDataBase? = null

        fun getInstance(context: Context): MateMateDataBase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MateMateDataBase::class.java,
                    "matemate_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}