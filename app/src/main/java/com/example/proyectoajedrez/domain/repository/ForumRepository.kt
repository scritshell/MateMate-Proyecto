package com.example.proyectoajedrez.domain.repository

import com.example.proyectoajedrez.domain.model.ForumPost
import com.example.proyectoajedrez.domain.model.ForumReply
import kotlinx.coroutines.flow.Flow

interface ForumRepository {
    fun getPosts(): Flow<List<ForumPost>>
    suspend fun createPost(post: ForumPost): Result<Unit>
    suspend fun likePost(postId: String): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
    fun getReplies(postId: String): Flow<List<ForumReply>>
    suspend fun addReply(postId: String, replyContent: String): Result<Unit>
}