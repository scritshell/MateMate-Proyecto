package com.example.proyectoajedrez.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ForumReply(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)
