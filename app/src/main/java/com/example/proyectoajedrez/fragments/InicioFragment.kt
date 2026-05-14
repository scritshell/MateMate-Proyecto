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
import com.example.proyectoajedrez.BuildConfig
import com.example.proyectoajedrez.utils.PrefKeys
import com.google.firebase.firestore.FirebaseFirestore
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
        val lichessUser = sharedPref.getString(PrefKeys.KEY_LICHESS_USERNAME, null)

        if (lichessUser != null) {
            obtenerDatosLichess(lichessUser)
        } else {
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
                    if (_binding != null && isAdded && document != null && document.exists()) {
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
            if (_binding != null && isAdded) {
                binding.tvBienvenidaSubtitulo.text = "Modo Invitado"
                binding.textElo.text = "-"
                binding.textPorcentajeTacticas.text = "-"
                binding.textAmigos.text = "-"
            }
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
            putString(PrefKeys.KEY_LICHESS_USERNAME, username)
            apply()
        }
        Toast.makeText(context, getString(R.string.msg_usuario_lichess_guardado), Toast.LENGTH_SHORT).show()
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

    // --- LÓGICA DE NOTICIAS ---

    // Obtener noticias de ajedrez exclusivas desde News API
    private fun cargarNoticias() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.NEWS_API_KEY
                val idiomaActual = java.util.Locale.getDefault().language

                // ✅ Query más específica con términos relacionados
                val queryBusqueda = if (idiomaActual == "en") {
                    "chess AND (tournament OR grandmaster OR opening OR tactics OR FIDE OR Carlsen OR Kasparov)"
                } else {
                    "ajedrez AND (torneo OR gran maestro OR apertura OR táctica OR FIDE OR partida)"
                }
                val idiomaApi = if (idiomaActual == "en") "en" else "es"

                // ✅ Dominios de confianza mantenidos (buena decisión previa)
                val dominiosAjedrez = "chess.com,lichess.org,chess24.com,fide.com,chessbase.com"

                val respuesta = RetrofitClient.instance.getChessNews(
                    query = queryBusqueda,
                    apiKey = apiKey,
                    language = idiomaApi,
                    sortBy = "publishedAt",
                    domains = dominiosAjedrez
                )

                withContext(Dispatchers.Main) {
                    if (isAdded && _binding != null && respuesta.status == "ok") {
                        val noticiasLimpias = respuesta.articles.filter { articulo ->
                            // Filtro adicional para mayor precisión
                            val textoCompleto = "${articulo.title} ${articulo.description}".lowercase()
                            val terminosAjedrez = if (idiomaActual == "en") {
                                listOf("chess", "grandmaster", "tournament", "checkmate",
                                    "opening", "tactics", "fide", "blitz", "puzzle")
                            } else {
                                listOf("ajedrez", "gran maestro", "torneo", "jaque",
                                    "apertura", "táctica", "fide", "partida")
                            }
                            // El artículo debe contener al menos un término de ajedrez
                            // Y tener imagen y descripción
                            !articulo.urlToImage.isNullOrEmpty() &&
                                    !articulo.description.isNullOrEmpty() &&
                                    terminosAjedrez.any { termino -> textoCompleto.contains(termino) }
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