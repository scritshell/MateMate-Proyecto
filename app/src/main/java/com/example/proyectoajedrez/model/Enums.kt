package com.example.proyectoajedrez.model

enum class GameMode {
    LIBRE, LOCAL_2P, APERTURA, DAILY_PUZZLE
}

// Extensión útil para convertir String a Enum sin crash
fun String.toGameMode(): GameMode = try {
    GameMode.valueOf(this.uppercase())
} catch (e: Exception) { GameMode.LIBRE }