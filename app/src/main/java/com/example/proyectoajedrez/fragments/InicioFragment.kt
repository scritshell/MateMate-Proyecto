package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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

        // 🔥 evitar listeners duplicados
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

                } else {

                    binding.tvBienvenidaSubtitulo.text =
                        getString(R.string.default_player_name)
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

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            try {

                val apiKey = BuildConfig.NEWS_API_KEY

                val idiomaActual =
                    java.util.Locale.getDefault().language

                val queryBusqueda =
                    if (idiomaActual == "en") {
                        "chess AND (tournament OR grandmaster OR opening OR tactics OR FIDE)"
                    } else {
                        "ajedrez AND (torneo OR gran maestro OR apertura OR táctica OR FIDE)"
                    }

                val idiomaApi =
                    if (idiomaActual == "en") "en" else "es"

                val dominiosAjedrez =
                    "chess.com,lichess.org,chess24.com,fide.com,chessbase.com"

                val respuesta = RetrofitClient.instance.getChessNews(
                    query = queryBusqueda,
                    apiKey = apiKey,
                    language = idiomaApi,
                    sortBy = "publishedAt",
                    domains = dominiosAjedrez
                )

                withContext(Dispatchers.Main) {

                    if (!isAdded || _binding == null) return@withContext

                    if (respuesta.status == "ok") {

                        val noticiasFiltradas =
                            respuesta.articles.filter { articulo ->

                                val texto =
                                    "${articulo.title} ${articulo.description}"
                                        .lowercase()

                                val keywords =
                                    if (idiomaActual == "en") {
                                        listOf(
                                            "chess",
                                            "grandmaster",
                                            "opening",
                                            "tournament",
                                            "fide",
                                            "blitz",
                                            "checkmate"
                                        )
                                    } else {
                                        listOf(
                                            "ajedrez",
                                            "gran maestro",
                                            "apertura",
                                            "torneo",
                                            "fide",
                                            "partida",
                                            "jaque"
                                        )
                                    }

                                !articulo.urlToImage.isNullOrEmpty() &&
                                        !articulo.description.isNullOrEmpty() &&
                                        keywords.any { texto.contains(it) }
                            }

                        binding.recyclerNoticias.adapter =
                            NewsAdapter(noticiasFiltradas)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando noticias", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        firestoreListener?.remove()
        firestoreListener = null

        _binding = null
    }
}
