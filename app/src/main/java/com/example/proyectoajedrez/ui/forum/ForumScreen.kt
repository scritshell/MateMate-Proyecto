package com.example.proyectoajedrez.ui.forum

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.data.repository.ForumRepositoryImpl
import com.example.proyectoajedrez.ui.forum.components.ForumTopBar
import com.example.proyectoajedrez.viewmodel.ForumViewModel

@Composable
fun ForumScreen() {

    val factory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ForumViewModel(ForumRepositoryImpl()) as T
            }
        }
    }

    val forumViewModel: ForumViewModel = viewModel(factory = factory)

    val uiState by forumViewModel.uiState.collectAsState()
    var mostrarDialogo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ForumTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogo = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.forum_nuevo_post)
                )
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.posts.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.forum_sin_posts),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.posts,
                            key = { it.id }
                        ) { post ->
                            PostCard(
                                post = post,
                                onLike = { forumViewModel.darLike(post.id) }
                            )
                        }
                    }
                }
            }
        }

        if (mostrarDialogo) {
            NuevoPostDialog(
                onConfirm = { titulo, contenido, categoria ->
                    forumViewModel.crearPost(titulo, contenido, categoria)
                    mostrarDialogo = false
                },
                onDismiss = {
                    mostrarDialogo = false
                }
            )
        }

        uiState.error?.let {
            LaunchedEffect(it) {
                forumViewModel.limpiarError()
            }
        }
    }
}