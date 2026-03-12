package com.example.proyectoajedrez.model


// CLASE 1: La respuesta general de la API
data class ExplorerResponse(
    val white: Int,
    val draws: Int,
    val black: Int,
    val moves: List<ExplorerMove>,
    val opening: ExplorerOpening? = null // <--- IMPORTANTE: Este campo trae el nombre
)

// CLASE 2: Cada jugada de la lista
data class ExplorerMove(
    val san: String,
    val white: Int,
    val draws: Int,
    val black: Int,
    val averageRating: Int?
) {
    fun getTotalGames(): Int = white + draws + black
}

// CLASE 3: El nombre de la apertura (para mostrar al final)
data class ExplorerOpening(
    val eco: String,  // Código (ej: C50)
    val name: String  // Nombre (ej: Italian Game)
)