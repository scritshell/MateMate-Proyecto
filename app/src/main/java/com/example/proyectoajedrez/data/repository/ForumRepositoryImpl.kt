package com.example.proyectoajedrez.data.repository

import android.content.Context
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.activities.SessionManager
import com.example.proyectoajedrez.domain.model.ForumPost
import com.example.proyectoajedrez.domain.model.UserRole
import com.example.proyectoajedrez.domain.repository.ForumRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.proyectoajedrez.domain.model.ForumReply
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue

class ForumRepositoryImpl(
    context: Context? = null,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ForumRepository {

    private val postsCollection = db.collection("forum_posts")
    
    // SessionManager para acceso a rol del usuario actual
    private val sessionManager: SessionManager? = context?.let { SessionManager(it) }
    private val defaultAuthorName = context?.getString(R.string.default_player_name) ?: "Jugador"

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
        
        // Obtener el rol del usuario actual (por defecto USER si no está disponible)
        val userRole = sessionManager?.getUserRole() ?: UserRole.USER
        
        val newPost = post.copy(
            authorId = user.uid,
            authorName = user.displayName ?: user.email?.substringBefore("@") ?: defaultAuthorName,
            authorRole = userRole  // Guardar rol del autor para auditoría
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
        val user = auth.currentUser ?: error("Usuario no autenticado")
        
        // Obtener el post para verificar permisos
        val post = postsCollection.document(postId).get().await().toObject(ForumPost::class.java)
            ?: error("Post no encontrado")
        
        // Validar: Solo admin u autor pueden borrar
        val isAuthor = post.authorId == user.uid
        var isAdmin = sessionManager?.isAdmin() ?: false
        
        // Fallback: check post's authorRole if sessionManager is null
        if (!isAdmin && sessionManager == null) {
            isAdmin = post.authorRole.name.uppercase() == "ADMIN" && post.authorId == user.uid
        }
        
        val canDelete = isAdmin || isAuthor
        
        if (!canDelete) {
            error("No tienes permisos para eliminar este post")
        }
        
        postsCollection.document(postId).delete().await()
        Unit
    }

    override fun getReplies(postId: String) = callbackFlow {
        val repliesCol = postsCollection.document(postId).collection("replies")
            .orderBy("createdAt", Query.Direction.ASCENDING)
        val listener = repliesCol.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val replies = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ForumReply::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(replies)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun addReply(postId: String, reply: ForumReply): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        val repliesCol = postsCollection.document(postId).collection("replies")
        val newReply = reply.copy(authorId = user.uid, authorName = user.displayName ?: user.email?.substringBefore("@") ?: defaultAuthorName)
        repliesCol.add(newReply).await()
        // increment repliesCount on post document
        postsCollection.document(postId).update("repliesCount", FieldValue.increment(1)).await()
        Unit
    }
}
