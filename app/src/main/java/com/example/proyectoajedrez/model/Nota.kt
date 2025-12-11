package com.example.proyectoajedrez.model

data class Nota(
    var id: String = "",          // ID del documento en Firestore (clave para editar/borrar)
    val userId: String = "",      // ID del usuario (para privacidad)
    val titulo: String = "",
    val contenido: String = "",   // Antes era 'preview', ahora guardamos el contenido real
    val fecha: Long = 0,     // Guardamos la fecha en milisegundos para ordenar mejor
    val categoria: String = "General" // SPINNER
)