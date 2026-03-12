package com.example.proyectoajedrez.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Los campos tienen valores por defecto vacíos porque Firestore necesita
// un constructor sin argumentos para deserializar los documentos.
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp val timestamp: Date? = null // Firebase rellena esto automáticamente
)