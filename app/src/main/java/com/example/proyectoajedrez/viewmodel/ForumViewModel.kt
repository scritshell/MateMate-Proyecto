package com.example.proyectoajedrez.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectoajedrez.domain.model.ForumCategory
import com.example.proyectoajedrez.domain.model.ForumPost
import com.example.proyectoajedrez.domain.repository.ForumRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.proyectoajedrez.domain.model.ForumReply
import kotlinx.coroutines.Dispatchers

data class ForumUiState(
    val posts: List<ForumPost> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String = "",
    val currentUserIsAdmin: Boolean = false
)

class ForumViewModel(
    private val repository: ForumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumUiState(isLoading = true))
    val uiState: StateFlow<ForumUiState> = _uiState.asStateFlow()

    init {
        cargarPosts()
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val doc = FirebaseFirestore.getInstance()
                        .collection("usuarios").document(uid).get().await()
                    val role = doc.getString("role") ?: "USER"
                    _uiState.update { it.copy(
                        currentUserId = uid,
                        currentUserIsAdmin = role.uppercase() == "ADMIN"
                    )}
                } catch (e: Exception) {
                    _uiState.update { it.copy(currentUserId = uid, currentUserIsAdmin = false) }
                }
            }
        } else {
            _uiState.update { it.copy(currentUserId = "", currentUserIsAdmin = false) }
        }
    }

    private fun cargarPosts() {
        viewModelScope.launch {
            repository.getPosts()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { posts ->
                    _uiState.update {
                        it.copy(posts = posts, isLoading = false, error = null)
                    }
                }
        }
    }

    fun crearPost(titulo: String, contenido: String, categoria: ForumCategory) {
        viewModelScope.launch {
            val post = ForumPost(title = titulo, content = contenido, category = categoria)
            repository.createPost(post).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun darLike(postId: String) {
        viewModelScope.launch {
            repository.likePost(postId)
        }
    }
    
    fun eliminarPost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }

    // Replies
    fun getRepliesFlow(postId: String): Flow<List<ForumReply>> = repository.getReplies(postId)

    fun enviarReply(postId: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val reply = ForumReply(content = content)
            repository.addReply(postId, reply).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
