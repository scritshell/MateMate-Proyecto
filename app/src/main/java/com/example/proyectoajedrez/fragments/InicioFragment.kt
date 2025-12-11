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

                Log.d(TAG, "Conectando con NewsAPI...")

                // Llamada a la API buscando "ajedrez"
                val respuesta = RetrofitClient.instance.getChessNews("ajedrez", apiKey)

                // Volvemos al Hilo Principal (Main) para actualizar la pantalla
                withContext(Dispatchers.Main) {
                    if (respuesta.status == "ok") {
                        Log.d(TAG, "Noticias recibidas: ${respuesta.totalResults}")

                        // Crear y asignar el adaptador con la lista de noticias
                        val adapter = NewsAdapter(respuesta.articles)
                        binding.recyclerNoticias.adapter = adapter
                    } else {
                        Log.e(TAG, "Error en API: ${respuesta.status}")
                        Toast.makeText(context, "Error cargando noticias", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Manejo de errores (sin internet, api key mal, etc.)
                Log.e(TAG, "🔥 Excepción cargando noticias", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Fallo de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}