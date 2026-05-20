package com.example.proyectoajedrez.data.repository

import android.content.Context
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

class ForumRepositoryImpl(
    context: Context? = null,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ForumRepository {

    private val postsCollection = db.collection("forum_posts")
    
    // SessionManager para acceso a rol del usuario actual
    private val sessionManager: SessionManager? = context?.let { SessionManager(it) }

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
            authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Jugador",
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
        val isAdmin = sessionManager?.isAdmin() ?: false
        
        // Obtener el post para verificar permisos
        val post = postsCollection.document(postId).get().await().toObject(ForumPost::class.java)
            ?: error("Post no encontrado")
        
        // Validar: Solo admin u autor pueden borrar
        val isAuthor = post.authorId == user.uid
        val canDelete = isAdmin || isAuthor
        
        if (!canDelete) {
            error("No tienes permisos para eliminar este post")
        }
        
        postsCollection.document(postId).delete().await()
        Unit
    }
}