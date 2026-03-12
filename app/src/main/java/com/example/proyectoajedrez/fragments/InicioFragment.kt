package com.example.proyectoajedrez.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.databinding.FragmentInicioBinding
import com.example.proyectoajedrez.network.LichessClient
import com.example.proyectoajedrez.network.RetrofitClient
import com.example.proyectoajedrez.adapters.NewsAdapter
import com.example.proyectoajedrez.model.LichessUserResponse
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

        // 2. Cargar datos del usuario (Combinación Firebase + Lichess)
        verificarYcargarDatosUsuario()

        // 3. Cargar noticias de ajedrez desde API
        cargarNoticias()

        // Listener: Cambiar usuario Lichess al tocar el nombre
        binding.tvBienvenidaSubtitulo.setOnClickListener {
            mostrarDialogoConfigurarUsuario()
        }
    }

    // --- LÓGICA DE USUARIO (Firebase + Lichess) ---

    private fun verificarYcargarDatosUsuario() {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val lichessUser = sharedPref.getString("lichess_username", null)

        if (lichessUser != null) {
            // Si ya vinculó Lichess en este dispositivo, mandamos pedir los datos a la API
            obtenerDatosLichess(lichessUser)
        } else {
            // Si no hay Lichess vinculado, cargamos los datos base de Firebase
            cargarDatosFirebase()
        }
    }

    private fun cargarDatosFirebase() {
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

                        // Actualizar UI con datos básicos de Firebase
                        binding.tvBienvenidaSubtitulo.text = "¡Hola, $username!"
                        binding.textElo.text = elo.toString()
                        binding.textPorcentajeTacticas.text = "-"
                        binding.textAmigos.text = "0"
                        binding.textAmigos.setTextColor(Color.GRAY)
                    }
                }
        } else {
            // Mostrar datos para modo invitado
            binding.tvBienvenidaSubtitulo.text = "Modo Invitado"
            binding.textElo.text = "-"
            binding.textPorcentajeTacticas.text = "-"
            binding.textAmigos.text = "-"
        }
    }

    private fun mostrarDialogoConfigurarUsuario() {
        val input = EditText(requireContext())
        input.hint = "Usuario Lichess (ej: MagnusCarlsen)"

        // Contenedor para márgenes
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 60; params.rightMargin = 60
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Vincular Cuenta Lichess")
            .setMessage("Introduce tu usuario para ver tu ELO real y estadísticas.")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val username = input.text.toString().trim()
                if (username.isNotEmpty()) {
                    guardarUsuarioLichess(username)
                    obtenerDatosLichess(username)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarUsuarioLichess(username: String) {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("lichess_username", username)
            apply()
        }
        Toast.makeText(context, "Usuario de Lichess guardado", Toast.LENGTH_SHORT).show()
    }

    private fun obtenerDatosLichess(username: String) {
        // Mostrar estado de carga
        binding.tvBienvenidaSubtitulo.text = "Cargando $username..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Llamada a la API de Lichess
                val user = LichessClient.instance.getUserPublicData(username)
                withContext(Dispatchers.Main) {
                    actualizarInterfazConLichess(user)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvBienvenidaSubtitulo.text = username
                    binding.textElo.text = "-"
                    Log.e(TAG, "Error conectando con Lichess", e)
                    // Si falla Lichess, intentamos recuperar los datos de Firebase como respaldo
                    cargarDatosFirebase()
                }
            }
        }
    }

    private fun actualizarInterfazConLichess(user: LichessUserResponse) {
        // 1. Nombre y Título
        val displayName = if (user.title != null) "[${user.title}] ${user.username}" else user.username
        binding.tvBienvenidaSubtitulo.text = displayName

        // 2. ELO (Prioridad: Blitz -> Rapid -> Puzzle)
        val eloBlitz = user.perfs?.blitz?.rating
        val eloRapid = user.perfs?.rapid?.rating
        val eloPuzzle = user.perfs?.puzzle?.rating
        val eloMostrado = eloBlitz ?: eloRapid ?: eloPuzzle ?: "?"

        binding.textElo.text = eloMostrado.toString()

        // 3. ELO Tácticas
        binding.textPorcentajeTacticas.text = (eloPuzzle?.toString() ?: "-")

        // 4. Estado Online
        if (user.online) {
            binding.textAmigos.text = "En línea"
            binding.textAmigos.setTextColor(Color.parseColor("#4CAF50")) // Verde
        } else {
            binding.textAmigos.text = "Offline"
            binding.textAmigos.setTextColor(Color.GRAY)
        }
    }

    // --- LÓGICA DE NOTICIAS (Intacta) ---

    // Obtener noticias de ajedrez exclusivas desde News API
    private fun cargarNoticias() {
        // Usamos viewLifecycleOwner.lifecycleScope porque es más seguro en Fragmentos
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiKey = "fd08253831f2472582d2a03585f4f834"
                val idiomaActual = java.util.Locale.getDefault().language

                // Mantenemos tu lógica de idiomas original
                val queryBusqueda = if (idiomaActual == "en") "+chess" else "+ajedrez"
                val idiomaApi = if (idiomaActual == "en") "en" else "es"

                // 1. Definimos los dominios de confianza (Ajedrez 100%)
                val dominiosAjedrez = "chess.com,lichess.org,chess24.com,fide.com,chessbase.com"

                // 2. Llamada a la API con los dominios incluidos
                val respuesta = RetrofitClient.instance.getChessNews(
                    query = queryBusqueda,
                    apiKey = apiKey,
                    language = idiomaApi,
                    sortBy = "publishedAt",   // Traerá lo más reciente
                    domains = dominiosAjedrez // <-- EL FILTRO MÁGICO
                )

                withContext(Dispatchers.Main) {
                    // Verificamos que el fragmento siga vivo antes de tocar la vista
                    if (isAdded && _binding != null && respuesta.status == "ok") {

                        // 3. Tu filtro original intacto: asegurarnos de que tengan imagen y texto
                        val noticiasLimpias = respuesta.articles.filter {
                            !it.urlToImage.isNullOrEmpty() && !it.description.isNullOrEmpty()
                        }

                        val adapter = NewsAdapter(noticiasLimpias)
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
        _binding = null
    }
}