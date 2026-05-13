package com.example.proyectoajedrez.ui.forum.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.proyectoajedrez.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumTopBar() {
    TopAppBar(
        title = {
            Text(text = stringResource(R.string.forum_titulo_pantalla))
        }
    )
}