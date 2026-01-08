package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.databinding.FragmentPuzzleDiarioBinding

class PuzzleDiarioFragment : Fragment() { // Anteriormente Tacticas.

    private var _binding: FragmentPuzzleDiarioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPuzzleDiarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*
        * TODO: Aquí podríamos cargar estadísticas de SharedPreferences en el futuro!!!
        *  SharedPreferences: Rachas de puzzles completados seguidos, cantidad de puzzles
        *  resueltos totales, tiempo promedio de resolución, etc...
        * */

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnJugarDiario.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("modo", "daily_puzzle")
            findNavController().navigate(R.id.action_puzzleDiarioFragment_to_chessBoardFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/*
* Cambios mas importantes: Sustitucion de las Tacticas por los puzzles diarios
* Motivos: Los puzzles diarios estan alimentados gracias a la API de Lichess, no tendre
* que comerme la cabeza con las tacticas que se debian hacer a mano. Era tedioso, muy repetitido
* y complicado de realizar. Los puzzles diarios seran automaticos.
* */

/*
* TODO: Agregar modo oscuro
* */