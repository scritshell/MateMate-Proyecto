package com.example.proyectoajedrez.ui.chat

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyectoajedrez.model.ChatMessage
import com.example.proyectoajedrez.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatScreen(chatViewModel: ChatViewModel = viewModel()) {
    val messages by chatViewModel.messages.collectAsState()
    val inputText by chatViewModel.inputText.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Para hacer autoscroll al último mensaje
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Lista de mensajes — LazyColumn
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = messages,
                key = { it.id } // La key mejora el rendimiento del diff
            ) { message ->
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

// Componente individual de burbuja de mensaje
@Composable
fun MessageBubble(message: ChatMessage, isOwnMessage: Boolean) {
    // Los mensajes propios van a la derecha, los ajenos a la izquierda
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
            Text(text = message.text, color = textColor, fontSize = 14.sp)
        }
    }
}