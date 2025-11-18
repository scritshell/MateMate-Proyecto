package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.adapters.NotasAdapter
import com.example.proyectoajedrez.databinding.FragmentBlocNotasBinding
import com.example.proyectoajedrez.model.Nota

class BlocNotasFragment : Fragment() {

    private var _binding: FragmentBlocNotasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlocNotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listaNotas = listOf(
            Nota(
                titulo = "Análisis partida vs Carlos",
                fecha = "Hoy, 15:30",
                preview = "1.e4 e5 2.Cf3 Cc6 3.Ac4 - Buena posición en el medio juego..."
            ),
            Nota(
                titulo = "Ideas para la Siciliana",
                fecha = "Ayer, 20:15",
                preview = "Variación del Dragón: 1.e4 c5 2.Cf3 d6 3.d4 cxd4 4.Cxd4 Cf6 5.Cc3 g6"
            ),
            Nota(
                titulo = "Errores en finales",
                fecha = "12 Nov, 10:45",
                preview = "Practicar finales de torres y peones. Recordar la oposición..."
            ),
            Nota(
                titulo = "Partida memorable",
                fecha = "10 Nov, 18:20",
                preview = "Gané con un sacrificio de dama en la jugada 25..."
            ),
            Nota(
                titulo = "Aperturas para estudiar",
                fecha = "8 Nov, 14:10",
                preview = "Priorizar: Italiana, Española, Gambito de Dama rechazado"
            )
        )

        setupRecyclerView(listaNotas)

        binding.cardNuevaNota.setOnClickListener {
            mostrarCrearNota()
        }
    }

    private fun setupRecyclerView(notas: List<Nota>) {
        val adapter = NotasAdapter(notas) { nota ->
            // Navegar al editor de nota (para implementar después)
            android.widget.Toast.makeText(
                requireContext(),
                "Editando nota: ${nota.titulo}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        binding.recyclerViewNotas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotas.adapter = adapter
    }

    private fun mostrarCrearNota() {
        // Navegar al editor de nueva nota
        android.widget.Toast.makeText(
            requireContext(),
            "Creando nueva nota - Editor próximamente",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}