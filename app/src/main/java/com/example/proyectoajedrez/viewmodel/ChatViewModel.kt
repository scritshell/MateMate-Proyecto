package com.example.proyectoajedrez.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectoajedrez.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Estado de la lista de mensajes
    val messages: StateFlow<List<ChatMessage>> = getMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estado del campo de texto
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    fun onInputChange(text: String) { _inputText.value = text }

    // callbackFlow convierte el listener de Firestore en un Flow de Kotlin,
    // que es la forma idiomática de trabajar con streams de datos en corrutinas.
    private fun getMessagesFlow(): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("chess_chat")
            .document("general")
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(msgs) // Emite la nueva lista al Flow
            }
        // Cuando el Flow se cancela, removemos el listener
        awaitClose { listener.remove() }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val user = auth.currentUser ?: return
        if (text.isEmpty()) return

        val message = hashMapOf(
            "senderId" to user.uid,
            "senderName" to (user.displayName ?: user.email?.substringBefore("@") ?: "Jugador"),
            "text" to text,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("chess_chat").document("general").collection("messages").add(message)
        _inputText.value = "" // Limpiar el campo tras enviar
    }
}