package com.example.proyectoajedrez.model

// Clase de datos que representa una apertura de ajedrez
data class Apertura(
    val nombre: String,       // Nombre de la apertura (ej: "Defensa Siciliana")
    val descripcion: String,  // Descripción o movimientos principales
    val emoji: String         // Emoji representativo de la apertura
)