package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.BuildConfig
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.adapters.NewsAdapter
import com.example.proyectoajedrez.databinding.FragmentInicioBinding
import com.example.proyectoajedrez.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var firestoreListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "InicioFragment"
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

        Log.d(TAG, "InicioFragment iniciado")

        setupNoticias()
        refrescarUsuario()
    }

    override fun onResume() {
        super.onResume()
        refrescarUsuario()
    }

    // =========================
    // USUARIO FIREBASE
    // =========================

    private fun refrescarUsuario() {

        firestoreListener?.remove()
        firestoreListener = null

        val userId = auth.currentUser?.uid

        if (userId == null) {

            binding.tvBienvenidaSubtitulo.text =
                getString(R.string.inicio_modo_invitado)

            binding.textElo.text = "-"
            binding.textPorcentajeTacticas.text = "-"
            binding.textAmigos.text = "-"

            return
        }

        firestoreListener = db.collection("usuarios")
            .document(userId)
            .addSnapshotListener { document, error ->

                if (error != null) {
                    Log.e(TAG, "Error cargando usuario", error)
                    return@addSnapshotListener
                }

                if (!isAdded || _binding == null) return@addSnapshotListener

                if (document != null && document.exists()) {

                    val username =
                        document.getString("username")
                            ?: getString(R.string.default_player_name)

                    val elo =
                        document.getLong("elo") ?: 1200

                    binding.tvBienvenidaSubtitulo.text =
                        getString(R.string.inicio_saludo_usuario, username)

                    binding.textElo.text = elo.toString()

                    binding.textPorcentajeTacticas.text =
                        getString(R.string.no_disponible)

                    binding.textAmigos.text =
                        getString(R.string.no_disponible)

                    binding.textAmigos.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    )

                }
            }
    }

    // =========================
    // NOTICIAS
    // =========================

    private fun setupNoticias() {
        binding.recyclerNoticias.layoutManager =
            LinearLayoutManager(requireContext())

        cargarNoticias()
    }

    private fun cargarNoticias() {

        mostrarEstadoNoticias(EstadoNoticias.CARGANDO)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            try {

                val apiKey = BuildConfig.NEWS_API_KEY

                if (apiKey.isBlank()) {
                    Log.e(TAG, "NEWS_API_KEY vacía")
                    withContext(Dispatchers.Main) {
                        mostrarEstadoNoticias(EstadoNoticias.ERROR)
                    }
                    return@launch
                }

                val idiomaActual = java.util.Locale.getDefault().language
                val idiomaApi = if (idiomaActual == "en") "en" else "es"

                val queryBusqueda =
                    if (idiomaActual == "en") "chess"
                    else "ajedrez"

                Log.d(TAG, "Noticias — idioma=$idiomaApi query=$queryBusqueda")

                val respuesta = RetrofitClient.instance.getChessNews(
                    query = queryBusqueda,
                    apiKey = apiKey,
                    language = idiomaApi,
                    sortBy = "publishedAt",
                    pageSize = 20
                )

                Log.d(
                    TAG,
                    "API: status=${respuesta.status}, total=${respuesta.totalResults}, artículos=${respuesta.articles.size}"
                )

                withContext(Dispatchers.Main) {

                    if (!isAdded || _binding == null) return@withContext

                    if (respuesta.status != "ok") {
                        mostrarEstadoNoticias(EstadoNoticias.ERROR)
                        return@withContext
                    }

                    // 🔥 FILTRO REAL DE AJEDREZ (mucho más estricto)
                    val keywords = listOf(
                        "chess",
                        "ajedrez",
                        "fide",
                        "grandmaster",
                        "tournament",
                        "torneo",
                        "opening",
                        "apertura",
                        "checkmate",
                        "blitz"
                    )

                    val noticiasFiltradas = respuesta.articles.filter { articulo ->

                        val texto =
                            "${articulo.title} ${articulo.description}".lowercase()

                        val score = keywords.count { texto.contains(it) }

                        !articulo.title.isNullOrBlank() &&
                                !articulo.urlToImage.isNullOrBlank() &&
                                score >= 2
                    }

                    Log.d(TAG, "Filtradas: ${noticiasFiltradas.size}")

                    if (noticiasFiltradas.isEmpty()) {
                        mostrarEstadoNoticias(EstadoNoticias.VACIO)
                    } else {
                        mostrarEstadoNoticias(EstadoNoticias.CON_DATOS)
                        binding.recyclerNoticias.adapter =
                            NewsAdapter(noticiasFiltradas)
                    }
                }

            } catch (e: Exception) {

                Log.e(TAG, "Error noticias", e)

                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext
                    mostrarEstadoNoticias(EstadoNoticias.ERROR)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        firestoreListener?.remove()
        firestoreListener = null

        _binding = null
    }

    private enum class EstadoNoticias {
        CARGANDO, CON_DATOS, VACIO, ERROR
    }

    private fun mostrarEstadoNoticias(estado: EstadoNoticias) {

        binding.progressNoticias?.isVisible =
            estado == EstadoNoticias.CARGANDO

        binding.recyclerNoticias.isVisible =
            estado == EstadoNoticias.CON_DATOS

        binding.tvSinNoticias?.isVisible =
            estado == EstadoNoticias.VACIO || estado == EstadoNoticias.ERROR

        binding.tvSinNoticias?.text =
            when (estado) {
                EstadoNoticias.ERROR -> getString(R.string.noticias_error_red)
                else -> getString(R.string.noticias_no_disponibles)
            }
    }
}