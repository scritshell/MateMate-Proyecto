package com.example.proyectoajedrez.fragments

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.databinding.FragmentGameSetupDialogBinding

class GameSetupDialogFragment(
    // Callback para enviar los datos seleccionados al MainActivity
    private val onGameStart: (modo: String, side: String, dificultad: Int, tiempoIndex: Int) -> Unit
) : DialogFragment() {

    private var _binding: FragmentGameSetupDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        _binding = FragmentGameSetupDialogBinding.inflate(inflater)
        builder.setView(binding.root)

        setupUI()

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    private fun setupUI() {
        // 1. Spinner Tiempos
        val tiempos = arrayOf("Sin Límite", "1 min (Bullet)", "3 min (Blitz)", "5 min (Blitz)", "10 min (Rápida)", "30 min (Clásica)")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tiempos)
        binding.spinnerTimeDialog.adapter = adapter
        binding.spinnerTimeDialog.setSelection(3)

        // 2. RadioButtons: Ocultar opciones de IA si es 2 jugadores
        binding.rgGameMode.setOnCheckedChangeListener { _, checkedId ->
            val esContraIA = checkedId == R.id.rbVsAI
            binding.containerSideSelection.isVisible = esContraIA
            binding.containerDifficulty.isVisible = esContraIA
        }

        // 3. SeekBar Dificultad
        binding.seekBarDifficultyDialog.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val nivel = if (progress < 1) 1 else progress
                binding.tvDifficultyValue.text = " $nivel"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 4. Botón Jugar
        binding.btnStartGame.setOnClickListener {
            // Recoger datos
            val esContraIA = binding.rbVsAI.isChecked

            val modoJuego = if (esContraIA) "libre" else "local_2p"

            // Si es contra IA, miramos el radiobutton. Si es local, "BOTH" significa ambos humanos.
            val side = if (esContraIA) {
                if (binding.rbWhite.isChecked) "WHITE" else "BLACK"
            } else {
                "BOTH"
            }

            val dificultad = if (esContraIA) binding.seekBarDifficultyDialog.progress.coerceAtLeast(1) else 0
            val tiempoIndex = binding.spinnerTimeDialog.selectedItemPosition

            // Enviamos los datos y cerramos
            onGameStart(modoJuego, side, dificultad, tiempoIndex)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/*
* TODO: Agregar modo oscuro
* */