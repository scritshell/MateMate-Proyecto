package com.example.proyectoajedrez.model

// Clase de datos que representa una apertura de ajedrez
data class Apertura(
    val nombre: String,
    val movimientos: String,
    val emoji: String,
    val descripcion: String = ""
)