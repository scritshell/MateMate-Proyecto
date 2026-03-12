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
            // APERTURAS ABIERTAS (1.e4 e5)
            Apertura(getString(R.string.apertura_italiana), "1.e4 e5 2.Nf3 Nc6 3.Bc4", "🍕"),
            Apertura(getString(R.string.apertura_espanola), "1.e4 e5 2.Nf3 Nc6 3.Bb5", "🏰"),
            Apertura(getString(R.string.apertura_escocesa), "1.e4 e5 2.Nf3 Nc6 3.d4", "🏴󠁧󠁢󠁳󠁣󠁴󠁿"),
            Apertura(getString(R.string.gambito_rey), "1.e4 e5 2.f4", "👑"),
            Apertura(getString(R.string.apertura_viena), "1.e4 e5 2.Nc3", "🎻"),
            Apertura(getString(R.string.defensa_petrov), "1.e4 e5 2.Nf3 Nf6", "🪆"),
            Apertura(getString(R.string.defensa_filidor), "1.e4 e5 2.Nf3 d6", "🛡️"),
            Apertura(getString(R.string.gambito_evans), "1.e4 e5 2.Nf3 Nc6 3.Bc4 Bc5 4.b4", "🦅"),
            Apertura(getString(R.string.cuatro_caballos), "1.e4 e5 2.Nf3 Nc6 3.Nc3 Nf6", "🐎"),

            // DEFENSAS CONTRA 1.e4 (Semi-abiertas)
            Apertura(getString(R.string.defensa_siciliana), "1.e4 c5", "⚔️"),
            Apertura(getString(R.string.siciliana_najdorf), "1.e4 c5 2.Nf3 d6 3.d4 cxd4 4.Nxd4 Nf6 5.Nc3 a6", "⚡"),
            Apertura(getString(R.string.siciliana_dragon), "1.e4 c5 2.Nf3 d6 3.d4 cxd4 4.Nxd4 Nf6 5.Nc3 g6", "🐉"),
            Apertura(getString(R.string.defensa_francesa), "1.e4 e6", "🥐"),
            Apertura(getString(R.string.defensa_caro_kann), "1.e4 c6", "🧱"),
            Apertura(getString(R.string.defensa_escandinava), "1.e4 d5", "❄️"),
            Apertura(getString(R.string.defensa_alekhine), "1.e4 Nf6", "🏃"),
            Apertura(getString(R.string.defensa_pirc), "1.e4 d6 2.d4 Nf6 3.Nc3 g6", "⛺"),
            Apertura(getString(R.string.defensa_moderna), "1.e4 g6 2.d4 Bg7", "🏗️"),

            // APERTURAS CERRADAS (1.d4)
            Apertura(getString(R.string.gambito_dama), "1.d4 d5 2.c4 dxc4", "👸"),
            Apertura(getString(R.string.gambito_dama_rehusado), "1.d4 d5 2.c4 e6", "🛡️"),
            Apertura(getString(R.string.defensa_slava), "1.d4 d5 2.c4 c6", "🗡️"),
            Apertura(getString(R.string.defensa_semislava), "1.d4 d5 2.c4 c6 3.Nf3 Nf6 4.Nc3 e6", "🏰"),
            Apertura(getString(R.string.sistema_londres), "1.d4 d5 2.Nf3 Nf6 3.Bf4", "💂"),
            Apertura(getString(R.string.sistema_colle), "1.d4 d5 2.Nf3 Nf6 3.e3 e6 4.Bd3 c5 5.c3", "🏛️"),
            Apertura(getString(R.string.ataque_trompowsky), "1.d4 Nf6 2.Bg5", "🎯"),

            // DEFENSAS INDIAS (1.d4 Nf6)
            Apertura(getString(R.string.defensa_india_rey), "1.d4 Nf6 2.c4 g6 3.Nc3 Bg7", "🐅"),
            Apertura(getString(R.string.defensa_nimzoindia), "1.d4 Nf6 2.c4 e6 3.Nc3 Bb4", "🐘"),
            Apertura(getString(R.string.defensa_india_dama), "1.d4 Nf6 2.c4 e6 3.Nf3 b6", "👑"),
            Apertura(getString(R.string.defensa_bogo_india), "1.d4 Nf6 2.c4 e6 3.Nf3 Bb4+", "🏹"),
            Apertura(getString(R.string.defensa_grunfeld), "1.d4 Nf6 2.c4 g6 3.Nc3 d5", "🌪️"),
            Apertura(getString(R.string.defensa_benoni), "1.d4 Nf6 2.c4 c5 3.d5 e6", "🏜️"),
            Apertura(getString(R.string.gambito_benko), "1.d4 Nf6 2.c4 c5 3.d5 b5", "🌊"),

            // APERTURAS DE FLANCO Y OTRAS
            Apertura(getString(R.string.apertura_inglesa), "1.c4", "☕"),
            Apertura(getString(R.string.apertura_reti), "1.Nf3", "🕸️"),
            Apertura(getString(R.string.defensa_holandesa), "1.d4 f5", "🌷"),
            Apertura(getString(R.string.ataque_indio_rey), "1.Nf3 Nf6 2.g3 g6 3.Bg2 Bg7 4.O-O O-O 5.d3", "🐍"),
            Apertura(getString(R.string.apertura_bird), "1.f4", "🕊️"),
            Apertura(getString(R.string.apertura_polaca), "1.b4", "🦧")
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