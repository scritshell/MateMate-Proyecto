package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.databinding.FragmentInicioBinding

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private companion object {
        const val TAG = "InicioFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "🚀 onCreateView: Iniciando creación de vista")
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        Log.d(TAG, "✅ onCreateView: Binding creado exitosamente")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "🎬 onViewCreated: Vista creada, configurando listeners")

        // CONFIGURAR LOS TEXTOS DE LOS BOTONES
        setupButtonTexts()

        setupClickListeners()
        Log.d(TAG, "🎉 Configuración completada - LISTO PARA CLICKS")
    }

    private fun setupButtonTexts() {
        // Esperar a que el layout esté listo
        binding.root.post {
            // Configurar cada botón con su emoji y texto
            val botonesConfig = listOf(
                Triple(binding.btnTacticas, "🎯", "Tácticas"),
                Triple(binding.btnAperturas, "♟️", "Aperturas"),
                Triple(binding.btnBlocNotas, "📝", "Bloc de Notas"),
                Triple(binding.btnComunidad, "👥", "Comunidad"),
                Triple(binding.btnPracticaLibre, "♜", "Práctica Libre")
            )

            botonesConfig.forEach { (cardView, emoji, titulo) ->
                try {
                    // Buscar recursivamente en el CardView
                    findAndSetTextViews(cardView, emoji, titulo)
                    Log.d(TAG, "✅ Configurado: $emoji $titulo")
                } catch (e: Exception) {
                    Log.e(TAG, "Error configurando $titulo: ${e.message}")
                }
            }
        }
    }

    private fun findAndSetTextViews(view: View, emoji: String, titulo: String) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is TextView) {
                    when (child.id) {
                        R.id.emoji -> child.text = emoji
                        R.id.titulo -> child.text = titulo
                    }
                } else if (child is ViewGroup) {
                    findAndSetTextViews(child, emoji, titulo)
                }
            }
        }
    }

    private fun setupClickListeners() {
        Log.d(TAG, "🔄 Configurando listeners de botones...")

        // VERIFICAR ESTADO DE CADA BOTÓN
        val botones = listOf(
            "btnTacticas" to binding.btnTacticas,
            "btnAperturas" to binding.btnAperturas,
            "btnBlocNotas" to binding.btnBlocNotas,
            "btnComunidad" to binding.btnComunidad,
            "btnPracticaLibre" to binding.btnPracticaLibre
        )

        botones.forEach { (nombre, boton) ->
            Log.d(TAG, "📋 Estado de $nombre: " +
                    "clickable=${boton.isClickable}, " +
                    "enabled=${boton.isEnabled}, " +
                    "visible=${boton.visibility == View.VISIBLE}")
        }

        // BOTÓN TÁCTICAS
        binding.btnTacticas.setOnClickListener {
            Log.d(TAG, "🎯🎯🎯 CLICK CONFIRMADO: Botón Tácticas")
            android.widget.Toast.makeText(
                requireContext(),
                "¡Navegando a Tácticas!",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            try {
                findNavController().navigate(R.id.action_inicioFragment_to_tacticasFragment)
                Log.d(TAG, "✅✅✅ NAVEGACIÓN EXITOSA A TacticasFragment")
            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ ERROR CRÍTICO EN NAVEGACIÓN:", e)
            }
        }

        // BOTÓN APERTURAS
        binding.btnAperturas.setOnClickListener {
            Log.d(TAG, "🎯🎯🎯 CLICK CONFIRMADO: Botón Aperturas")
            android.widget.Toast.makeText(
                requireContext(),
                "¡Navegando a Aperturas!",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            try {
                findNavController().navigate(R.id.action_inicioFragment_to_aperturasFragment)
                Log.d(TAG, "✅✅✅ NAVEGACIÓN EXITOSA A AperturasFragment")
            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ ERROR CRÍTICO EN NAVEGACIÓN:", e)
            }
        }

        // BOTÓN BLOC DE NOTAS
        binding.btnBlocNotas.setOnClickListener {
            Log.d(TAG, "🎯🎯🎯 CLICK CONFIRMADO: Botón Bloc de Notas")
            android.widget.Toast.makeText(
                requireContext(),
                "¡Navegando a Bloc de Notas!",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            try {
                findNavController().navigate(R.id.action_inicioFragment_to_blocNotasFragment)
                Log.d(TAG, "✅✅✅ NAVEGACIÓN EXITOSA A BlocNotasFragment")
            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ ERROR CRÍTICO EN NAVEGACIÓN:", e)
            }
        }

        // BOTÓN COMUNIDAD
        binding.btnComunidad.setOnClickListener {
            Log.d(TAG, "🎯🎯🎯 CLICK CONFIRMADO: Botón Comunidad")
            android.widget.Toast.makeText(
                requireContext(),
                "¡Navegando a Comunidad!",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            try {
                findNavController().navigate(R.id.action_inicioFragment_to_socialFragment)
                Log.d(TAG, "✅✅✅ NAVEGACIÓN EXITOSA A SocialFragment")
            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ ERROR CRÍTICO EN NAVEGACIÓN:", e)
            }
        }

        // BOTÓN PRÁCTICA LIBRE
        binding.btnPracticaLibre.setOnClickListener {
            Log.d(TAG, "🎯🎯🎯 CLICK CONFIRMADO: Botón Práctica Libre")
            android.widget.Toast.makeText(
                requireContext(),
                "¡Navegando a Práctica Libre!",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            try {
                findNavController().navigate(R.id.action_inicioFragment_to_chessBoardFragment)
                Log.d(TAG, "✅✅✅ NAVEGACIÓN EXITOSA A ChessBoardFragment")
            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ ERROR CRÍTICO EN NAVEGACIÓN:", e)
            }
        }

        Log.d(TAG, "🎊 Todos los listeners configurados - AHORA HAZ CLIC EN LOS BOTONES!")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "🧹 onDestroyView: Binding limpiado")
    }
}