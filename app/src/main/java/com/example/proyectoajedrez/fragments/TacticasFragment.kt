package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.databinding.FragmentTacticasBinding

class TacticasFragment : Fragment() {

    private var _binding: FragmentTacticasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTacticasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() {
        // Mate en 1
        binding.cardMate1.setOnClickListener {
            navigateToBoard("mate1")
        }

        // Mate en 2
        binding.cardMate2.setOnClickListener {
            navigateToBoard("mate2")
        }

        // Avanzados
        binding.cardAvanzados.setOnClickListener {
            navigateToBoard("avanzado")
        }

        // Estadísticas (Podemos mostrar un toast o ir a perfil si existiera)
        binding.cardEstadisticas.setOnClickListener {
            Toast.makeText(context, "Estadísticas: 85% victorias", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToBoard(modoJuego: String) {
        try {
            val bundle = Bundle()
            bundle.putString("modo", modoJuego)
            findNavController().navigate(R.id.action_tacticasFragment_to_chessBoardFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al navegar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}