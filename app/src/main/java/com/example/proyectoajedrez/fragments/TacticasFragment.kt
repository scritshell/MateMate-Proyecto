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

// Fragmento para sección de tácticas y ejercicios de ajedrez
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
        setupNavigation()  // Configurar navegación a ejercicios
    }

    // Configurar click listeners para las tarjetas de ejercicios
    private fun setupNavigation() {
        // Ejercicios de mate en 1 movimiento
        binding.cardMate1.setOnClickListener {
            navigateToBoard("mate1")
        }

        // Ejercicios de mate en 2 movimientos
        binding.cardMate2.setOnClickListener {
            navigateToBoard("mate2")
        }

        // Ejercicios avanzados
        binding.cardAvanzados.setOnClickListener {
            navigateToBoard("avanzado")
        }

        // Mostrar estadísticas de tácticas
        binding.cardEstadisticas.setOnClickListener {
            Toast.makeText(context, "Estadísticas: 85% victorias", Toast.LENGTH_SHORT).show()
        }
    }

    // Navegar al tablero con modo de juego específico
    private fun navigateToBoard(modoJuego: String) {
        try {
            val bundle = Bundle()
            bundle.putString("modo", modoJuego)  // Pasar modo de juego como parámetro
            findNavController().navigate(R.id.action_tacticasFragment_to_chessBoardFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al navegar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Limpiar binding para evitar memory leaks
    }
}