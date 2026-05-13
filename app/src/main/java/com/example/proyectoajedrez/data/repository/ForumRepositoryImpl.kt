package com.example.proyectoajedrez.data.repository

import com.example.proyectoajedrez.domain.model.ForumPost
import com.example.proyectoajedrez.domain.repository.ForumRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ForumRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ForumRepository {

    private val postsCollection = db.collection("forum_posts")

    override fun getPosts(): Flow<List<ForumPost>> = callbackFlow {
        val listener = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ForumPost::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createPost(post: ForumPost): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        val newPost = post.copy(
            authorId = user.uid,
            authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Jugador"
        )
        postsCollection.add(newPost).await()
        Unit
    }

    override suspend fun likePost(postId: String): Result<Unit> = runCatching {
        postsCollection.document(postId)
            .update("likes", com.google.firebase.firestore.FieldValue.increment(1))
            .await()
        Unit
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        postsCollection.document(postId).delete().await()
        Unit
    }
}