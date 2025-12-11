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

class AperturasFragment : Fragment() {

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

        val listaAperturas = listOf(
            Apertura(getString(R.string.apertura_italiana), "1.e4 e5 2.Cf3 Cc6 3.Ac4", "♟️"),
            Apertura(getString(R.string.defensa_siciliana), "1.e4 c5", "⚔️"),
            Apertura(getString(R.string.apertura_espanola), "1.e4 e5 2.Cf3 Cc6 3.Ab5", "🏰"),
            Apertura(getString(R.string.defensa_francesa), "1.e4 e6", "🛡️"),
            Apertura(getString(R.string.gambito_dama), "1.d4 d5 2.c4", "♛"),
            Apertura(getString(R.string.defensa_india), "1.d4 Cf6 2.c4 g6", "🌄")
        )

        val adapter = AperturasAdapter(listaAperturas) { apertura ->
            navigateToChessBoard(apertura)
        }

        binding.recyclerViewAperturas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAperturas.adapter = adapter
    }

    private fun navigateToChessBoard(apertura: Apertura) {
        try {
            // Navegación simple sin Safe Args
            val bundle = Bundle().apply {
                putString("modo", "apertura")
                putString("aperturaNombre", apertura.nombre)
            }
            findNavController().navigate(R.id.action_aperturasFragment_to_chessBoardFragment, bundle)

        } catch (e: Exception) {
            // Fallback si hay error
            android.widget.Toast.makeText(
                requireContext(),
                "Abriendo tablero para: ${apertura.nombre}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}