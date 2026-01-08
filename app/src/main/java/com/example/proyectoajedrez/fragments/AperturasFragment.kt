package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.adapters.AperturasAdapter
import com.example.proyectoajedrez.databinding.FragmentAperturasBinding
import com.example.proyectoajedrez.model.Apertura

// Fragmento para mostrar lista de aperturas de ajedrez
class AperturasFragment : Fragment() {

    // ViewBinding para el fragmento
    private var _binding: FragmentAperturasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAperturasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lista predefinida de aperturas de ajedrez
        /*
        * TODO: Añadir todas las aperturas más conocidas.
        *  Para pedir a la IA. ^^^^
        * */
        val listaAperturas = listOf(
            Apertura(getString(R.string.apertura_italiana), "1.e4 e5 2.Nf3 Nc6 3.Bc4", "♟️"),
            Apertura(getString(R.string.defensa_siciliana), "1.e4 c5", "⚔️"),
            Apertura(getString(R.string.apertura_espanola), "1.e4 e5 2.Cf3 Cc6 3.Ab5", "🏰"),
            Apertura(getString(R.string.defensa_francesa), "1.e4 e6", "🛡️"),
            Apertura(getString(R.string.gambito_dama), "1.d4 d5 2.c4", "♛"),
            Apertura(getString(R.string.defensa_india), "1.d4 Cf6 2.c4 g6", "🌄")
        )

        // Crear adaptador con lista de aperturas y callback para clicks
        val adapter = AperturasAdapter(listaAperturas) { apertura ->
            navigateToChessBoard(apertura) // Navegar al tablero al seleccionar apertura
        }

        // Configurar RecyclerView con layout linear vertical y adaptador
        binding.recyclerViewAperturas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAperturas.adapter = adapter
    }

    // Navegar al fragmento del tablero con información de la apertura seleccionada
    private fun navigateToChessBoard(apertura: Apertura) {
        val bundle = Bundle().apply {
            putString("modo", "apertura")
            putString("titulo", apertura.nombre)
            putString("secuenciaMovimientos", apertura.movimientos) // ¡IMPORTANTE! Pasamos los movimientos "1.e4 e5..."
        }
        findNavController().navigate(R.id.action_aperturasFragment_to_chessBoardFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpiar la referencia de binding para evitar memory leaks
    }
}