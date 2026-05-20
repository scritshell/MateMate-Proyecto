package com.example.proyectoajedrez.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectoajedrez.domain.model.ForumCategory
import com.example.proyectoajedrez.domain.model.ForumPost
import com.example.proyectoajedrez.domain.repository.ForumRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ForumUiState(
    val posts: List<ForumPost> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ForumViewModel(
    private val repository: ForumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumUiState(isLoading = true))
    val uiState: StateFlow<ForumUiState> = _uiState.asStateFlow()

    init {
        cargarPosts()
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
}
