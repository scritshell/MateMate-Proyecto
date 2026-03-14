package com.example.proyectoajedrez.ui.chat

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.proyectoajedrez.model.ChatMessage
import com.example.proyectoajedrez.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(chatViewModel: ChatViewModel = viewModel()) {
    val messages by chatViewModel.messages.collectAsState()
    val inputText by chatViewModel.inputText.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val context = LocalContext.current

    val listState = rememberLazyListState()

    // --- ESTADOS PARA LA CÁMARA Y GALERÍA ---
    var showDialog by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher para seleccionar de la galería
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { chatViewModel.sendImageMessage(context, it) }
    }

    // Launcher para hacer Foto con la Cámara
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { chatViewModel.sendImageMessage(context, it) }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // --- DIÁLOGO DE SELECCIÓN ---
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enviar archivo multimedia") },
            text = { Text("¿Desde dónde quieres obtener la imagen?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Galería")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    // Crear un archivo temporal para la cámara usando FileProvider
                    val file = context.createImageFile()
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider", // Debe coincidir con el authorities del Manifest
                        file
                    )
                    tempImageUri = uri
                    cameraLauncher.launch(uri)
                }) {
                    Text("Cámara")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isOwnMessage = message.senderId == currentUserId
                )
            }
        }

        // Barra de input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón para abrir el diálogo de multimedia
            IconButton(onClick = { showDialog = true }) {
                Text("📷", fontSize = 24.sp) // Puedes cambiarlo por un Icon de Material si lo prefieres
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { chatViewModel.onInputChange(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Comenta la partida...") },
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { chatViewModel.sendMessage() },
                enabled = inputText.isNotBlank()
            ) {
                Text("♟")
            }
        }
    }
}

// Actualización de la burbuja para renderizar la imagen con Coil
@Composable
fun MessageBubble(message: ChatMessage, isOwnMessage: Boolean) {
    val alignment = if (isOwnMessage) Arrangement.End else Arrangement.Start
    val bubbleColor = if (isOwnMessage) Color(0xFF263238) else Color(0xFFEEEEEE)
    val textColor = if (isOwnMessage) Color.White else Color.Black

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!isOwnMessage) {
                Text(
                    text = message.senderName,
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Si el mensaje tiene una URL de imagen, la mostramos usando Coil
            if (!message.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = message.imageUrl,
                    contentDescription = "Imagen compartida",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .padding(bottom = if (message.text.isNotBlank()) 4.dp else 0.dp)
                )
            }

            // Si hay texto, lo mostramos debajo de la imagen (o solo si no hay imagen)
            if (message.text.isNotBlank()) {
                Text(text = message.text, color = textColor, fontSize = 14.sp)
            }
        }
    }
}

// Función de extensión para crear un archivo temporal para la cámara
fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    // CAMBIO CRÍTICO: Usamos cacheDir (interno) en lugar de externalCacheDir
    return File.createTempFile(
        imageFileName,
        ".jpg",
        cacheDir
    )
}