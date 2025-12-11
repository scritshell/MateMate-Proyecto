package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.databinding.FragmentInicioBinding
import com.example.proyectoajedrez.network.RetrofitClient
import com.example.proyectoajedrez.adapters.NewsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private companion object {
        const val TAG = "InicioFragment"
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
        Log.d(TAG, "🎬 Vista creada: Iniciando carga de noticias...")

        // 1. Configurar el RecyclerView (Diseño vertical)
        binding.recyclerNoticias.layoutManager = LinearLayoutManager(context)

        // 2. Llamar a la API para cargar datos
        cargarNoticias()
    }

    private fun cargarNoticias() {
        // Usamos Corrutinas para hacer la petición en segundo plano (Hilo IO)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiKey = "fd08253831f2472582d2a03585f4f834"

                // 1. DETECTAR IDIOMA DEL MÓVIL
                // Obtenemos el código de idioma actual (ej: "es", "en", "fr")
                val idiomaActual = java.util.Locale.getDefault().language

                // 2. CONFIGURAR BÚSQUEDA SEGÚN IDIOMA
                // Si el móvil está en inglés, buscamos "chess" y pedimos noticias en inglés ("en")
                // Para cualquier otro caso, buscamos "ajedrez" en español ("es")
                val queryBusqueda = if (idiomaActual == "en") "chess" else "ajedrez"
                val idiomaApi = if (idiomaActual == "en") "en" else "es"

                Log.d(TAG, "🌍 Conectando con NewsAPI... Buscando: $queryBusqueda ($idiomaApi)")

                // 3. LLAMADA A LA API CON PARÁMETROS DINÁMICOS
                val respuesta = RetrofitClient.instance.getChessNews(
                    query = queryBusqueda,
                    apiKey = apiKey,
                    language = idiomaApi
                )

                // 4. ACTUALIZAR PANTALLA (Hilo Principal)
                withContext(Dispatchers.Main) {
                    // Verificamos 'isAdded' para evitar crashes si el usuario sale rápido de la pantalla
                    if (isAdded) {
                        if (respuesta.status == "ok") {
                            Log.d(TAG, "✅ Noticias recibidas: ${respuesta.totalResults}")

                            // Crear y asignar el adaptador con la lista de noticias
                            val adapter = NewsAdapter(respuesta.articles)
                            binding.recyclerNoticias.adapter = adapter
                        } else {
                            Log.e(TAG, "❌ Error en API: ${respuesta.status}")
                            Toast.makeText(context, "Error cargando noticias", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                // Manejo de errores (sin internet, api key mal, etc.)
                Log.e(TAG, "🔥 Excepción cargando noticias", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(context, "Fallo de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}