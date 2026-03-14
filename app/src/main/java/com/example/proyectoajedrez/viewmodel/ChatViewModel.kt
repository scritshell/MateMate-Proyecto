package com.example.proyectoajedrez.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectoajedrez.BuildConfig
import com.example.proyectoajedrez.model.ChatMessage
import com.example.proyectoajedrez.network.ImgBbClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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
        _inputText.value = ""
    }

    fun sendImageMessage(context: Context, uri: Uri) {
        val user = auth.currentUser ?: return

        // Lanzamos una corrutina en el hilo de IO (Entrada/Salida) para no bloquear la UI
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("ChatViewModel", "Iniciando subida a ImgBB...")

                // 1. Convertimos la URI de la imagen a un array de Bytes de forma segura
                val inputStream = context.contentResolver.openInputStream(uri)
                val imageBytes = inputStream?.readBytes() ?: return@launch
                inputStream.close()

                // 2. Preparamos el cuerpo de la petición (Multipart)
                val requestBody = imageBytes.toRequestBody("image/*".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData("image", "mate_image.jpg", requestBody)

                // 3. Llamamos a la API de ImgBB inyectando la clave ofuscada y segura
                val apiKey = BuildConfig.IMGBB_API_KEY

                val response = ImgBbClient.apiService.uploadImage(apiKey, multipartBody)

                // 4. Si ha ido bien, guardamos el mensaje en Firestore
                if (response.success) {
                    val publicImageUrl = response.data.url
                    android.util.Log.d("ChatViewModel", "¡Subida exitosa! URL: $publicImageUrl")

                    val message = hashMapOf(
                        "senderId" to user.uid,
                        "senderName" to (user.displayName ?: user.email?.substringBefore("@") ?: "Jugador"),
                        "text" to "",
                        "imageUrl" to publicImageUrl,
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    db.collection("chess_chat").document("general").collection("messages").add(message)
                } else {
                    android.util.Log.e("ChatViewModel", "Error de ImgBB: Falló la subida")
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Excepción subiendo imagen", e)
            }
        }
    }
}