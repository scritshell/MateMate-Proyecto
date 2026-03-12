package com.example.proyectoajedrez.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.databinding.FragmentPuzzleDiarioBinding
import com.example.proyectoajedrez.network.LichessClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PuzzleDiarioFragment : Fragment() {

    private var _binding: FragmentPuzzleDiarioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPuzzleDiarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    // --- REFRESCAR AL VOLVER ---
    override fun onResume() {
        super.onResume()
        cargarEstadisticas()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
    }

    private fun cargarEstadisticas() {
        // IMPORTANTE: Usar el mismo nombre "AjedrezPrefs"
        val sharedPref = requireContext().getSharedPreferences("AjedrezPrefs", Context.MODE_PRIVATE)

        // --- A. RACHA LOCAL ---
        val racha = sharedPref.getInt("puzzle_streak_days", 0)
        binding.tvRacha.text = racha.toString()

        // --- B. RESUELTOS TOTALES ---
        val usuarioLichess = sharedPref.getString("lichess_username", null)

        if (usuarioLichess != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val userResponse = LichessClient.instance.getUserPublicData(usuarioLichess)
                    // Sumamos los de Lichess + los que haya hecho en local en tu app
                    val totalLichess = userResponse.perfs?.puzzle?.games ?: 0
                    val localSolved = sharedPref.getInt("local_puzzles_solved", 0)

                    withContext(Dispatchers.Main) {
                        binding.tvTotalResueltos.text = (totalLichess + localSolved).toString()
                    }
                } catch (e: Exception) {
                    mostrarDatosLocales(sharedPref)
                }
            }
        } else {
            mostrarDatosLocales(sharedPref)
        }
    }

    private fun mostrarDatosLocales(sharedPref: android.content.SharedPreferences) {
        val localSolved = sharedPref.getInt("local_puzzles_solved", 0)
        binding.tvTotalResueltos.text = localSolved.toString()
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