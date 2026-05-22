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
        
        // Obtener el rol del usuario actual directamente desde Firestore
        val roleDoc = db.collection("usuarios").document(user.uid).get().await()
        val userRole = try {
            UserRole.valueOf(roleDoc.getString("role") ?: "USER")
        } catch (_: Exception) {
            UserRole.USER
        }
        
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
        
        val postDoc = postsCollection.document(postId)
        val post = postDoc.get().await().toObject(ForumPost::class.java)
            ?: error("Post no encontrado")
        
        val isAuthor = post.authorId == user.uid
        val roleDoc = db.collection("usuarios").document(user.uid).get().await()
        val currentRole = roleDoc.getString("role") ?: "USER"
        val isAdmin = currentRole.uppercase() == "ADMIN"
        
        if (!isAuthor && !isAdmin) {
            error("No tienes permisos para eliminar este post")
        }
        
        val batch = db.batch()
        val repliesSnap = postDoc.collection("replies").get().await()
        repliesSnap.documents.forEach { batch.delete(it.reference) }
        batch.delete(postDoc)
        batch.commit().await()
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

    override suspend fun addReply(postId: String, replyContent: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        val postDoc = postsCollection.document(postId)
        val repliesCol = postDoc.collection("replies")
        val newReplyRef = repliesCol.document()
        val newReply = ForumReply(
            authorId = user.uid,
            authorName = user.displayName ?: user.email?.substringBefore("@") ?: defaultAuthorName,
            content = replyContent
        )
        val batch = db.batch()
        batch.set(newReplyRef, newReply)
        batch.update(postDoc, "repliesCount", FieldValue.increment(1))
        batch.commit().await()
        Unit
    }
}
