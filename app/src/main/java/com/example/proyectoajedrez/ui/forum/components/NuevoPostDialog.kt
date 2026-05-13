package com.example.proyectoajedrez.ui.forum

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.domain.model.ForumCategory

@Composable
fun NuevoPostDialog(
    onConfirm: (titulo: String, contenido: String, categoria: ForumCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var contenido by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf(ForumCategory.GENERAL) }
    var mostrarError by remember { mutableStateOf(false) }
    var expandirMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.forum_titulo_dialogo),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Campo título
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        mostrarError = false
                    },
                    label = { Text(stringResource(R.string.forum_hint_titulo)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = mostrarError && titulo.isBlank()
                )

                // Campo contenido
                OutlinedTextField(
                    value = contenido,
                    onValueChange = {
                        contenido = it
                        mostrarError = false
                    },
                    label = { Text(stringResource(R.string.forum_hint_contenido)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = mostrarError && contenido.isBlank()
                )

                // Selector de categoría (100% estable)
                Text(
                    text = stringResource(R.string.forum_label_categoria),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box {
                    OutlinedTextField(
                        value = categoriaSeleccionada.name,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandirMenu = true },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = expandirMenu,
                        onDismissRequest = { expandirMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ForumCategory.entries.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.name) },
                                onClick = {
                                    categoriaSeleccionada = categoria
                                    expandirMenu = false
                                }
                            )
                        }
                    }
                }

                // Mensaje de error
                if (mostrarError) {
                    Text(
                        text = stringResource(R.string.forum_error_campos),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isBlank() || contenido.isBlank()) {
                        mostrarError = true
                    } else {
                        onConfirm(
                            titulo.trim(),
                            contenido.trim(),
                            categoriaSeleccionada
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.forum_btn_publicar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.forum_btn_cancelar))
            }
        }
    )
}