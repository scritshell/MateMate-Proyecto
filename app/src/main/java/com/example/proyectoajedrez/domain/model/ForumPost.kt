package com.example.proyectoajedrez.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ForumPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: UserRole = UserRole.USER,  // Rol del autor para auditoría
    val title: String = "",
    val content: String = "",
    val category: ForumCategory = ForumCategory.GENERAL,
    val likes: Int = 0,
    val repliesCount: Int = 0,
    @ServerTimestamp val createdAt: Date? = null
)

enum class ForumCategory {
    GENERAL, APERTURA, TACTICA, PARTIDA, TORNEO
}