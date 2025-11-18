package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
        // Navegación desde las tarjetas de ejercicios
        // (Asumiendo que tienes IDs para cada CardView)

        // Ejercicio Mate en 1
        binding.cardMate1.setOnClickListener {
            showToast("Iniciando ejercicios de Mate en 1")
            // findNavController().navigate(R.id.action_tacticas_to_ejercicio1) // Para implementar después
        }

        // Ejercicio Mate en 2
        binding.cardMate2.setOnClickListener {
            showToast("Iniciando ejercicios de Mate en 2")
        }

        // Ejercicios Avanzados
        binding.cardAvanzados.setOnClickListener {
            showToast("Iniciando ejercicios avanzados")
        }

        // Estadísticas
        binding.cardEstadisticas.setOnClickListener {
            showToast("Mostrando estadísticas")
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}