package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.proyectoajedrez.adapters.ChessBoardAdapter
import com.example.proyectoajedrez.databinding.FragmentChessBoardBinding

class ChessBoardFragment : Fragment() {

    private lateinit var binding: FragmentChessBoardBinding
    private lateinit var adapter: ChessBoardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChessBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChessBoardAdapter(requireContext())
        binding.chessBoard.adapter = adapter

        setupControls()
        updateGameInfo()
    }

    private fun setupControls() {
        binding.btnReset.setOnClickListener {
            adapter.resetBoard()
            updateGameInfo("Tablero reiniciado")
        }

        binding.btnUndo.setOnClickListener {
            // TODO: Implementar funcionalidad de deshacer
            updateGameInfo("Deshacer - Próximamente")
        }

        binding.btnSave.setOnClickListener {
            // TODO: Implementar funcionalidad de guardar
            updateGameInfo("Guardar - Próximamente")
        }
    }

    private fun updateGameInfo(message: String? = null) {
        val defaultText = "Modo: Práctica - Tablero interactivo"
        binding.gameInfoTextView.text = message ?: defaultText
    }

    companion object {
        fun newInstance(): ChessBoardFragment {
            return ChessBoardFragment()
        }
    }
}