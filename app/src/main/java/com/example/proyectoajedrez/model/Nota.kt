package com.example.proyectoajedrez.model

// Clase de datos que representa una nota personal del usuario
data class Nota(
    var id: String = "",               // ID único del documento en Firestore
    val userId: String = "",           // ID del usuario propietario
    val titulo: String = "",           // Título de la nota
    val contenido: String = "",        // Contenido completo de la nota
    val fecha: Long = 0,               // Fecha en milisegundos para ordenamiento
    val categoria: String = "General"  // Categoría
)