package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.databinding.FragmentInicioBinding
import com.example.proyectoajedrez.network.RetrofitClient
import com.example.proyectoajedrez.adapters.NewsAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Fragmento principal de inicio con información de usuario y noticias
class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    // Instancias para autenticación y base de datos Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private companion object {
        const val TAG = "InicioFragment"  // Etiqueta para logs
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "Vista creada: Iniciando...")

        // 1. Configurar RecyclerView para noticias
        binding.recyclerNoticias.layoutManager = LinearLayoutManager(context)

        // 2. Cargar datos del usuario desde Firebase
        cargarDatosUsuario()

        // 3. Cargar noticias de ajedrez desde API
        cargarNoticias()
    }

    // Cargar y mostrar datos del usuario actual desde Firestore
    private fun cargarDatosUsuario() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Escuchar cambios en tiempo real del documento de usuario
            db.collection("usuarios").document(userId)
                .addSnapshotListener { document, e ->
                    if (e != null) {
                        Log.e(TAG, "Error al escuchar datos de usuario", e)
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        // Extraer datos del documento Firestore
                        val username = document.getString("username") ?: "Jugador"
                        val elo = document.getLong("elo") ?: 1200

                        // Actualizar UI con datos del usuario
                        binding.tvBienvenidaSubtitulo.text = "¡Hola, $username!"
                        binding.textElo.text = elo.toString()

                        // Valores por defecto para características pendientes
                        binding.textPorcentajeTacticas.text = "0%"  // Por implementar
                        binding.textAmigos.text = "0"  // Por implementar
                    }
                }
        } else {
            // Mostrar datos para modo invitado (usuario no logueado)
            binding.tvBienvenidaSubtitulo.text = "Modo Invitado"
            binding.textElo.text = "-"
        }
    }

    // Obtener noticias de ajedrez desde News API
    private fun cargarNoticias() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiKey = "fd08253831f2472582d2a03585f4f834"
                val idiomaActual = java.util.Locale.getDefault().language

                // Definir búsqueda según idioma del dispositivo
                val queryBusqueda = if (idiomaActual == "en") "chess" else "ajedrez"
                val idiomaApi = if (idiomaActual == "en") "en" else "es"

                // Realizar petición a la API
                val respuesta = RetrofitClient.instance.getChessNews(
                    query = queryBusqueda,
                    apiKey = apiKey,
                    language = idiomaApi
                )

                withContext(Dispatchers.Main) {
                    // Verificar que el fragmento esté activo y la respuesta sea válida
                    if (isAdded && respuesta.status == "ok") {
                        val adapter = NewsAdapter(respuesta.articles)
                        binding.recyclerNoticias.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando noticias", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Limpiar binding para evitar memory leaks
    }
}