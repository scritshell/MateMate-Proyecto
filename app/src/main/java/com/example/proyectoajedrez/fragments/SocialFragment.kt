package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.proyectoajedrez.R  // ✅ IMPORT AÑADIDO
import com.example.proyectoajedrez.databinding.FragmentSocialBinding
import com.google.android.material.tabs.TabLayout  // ✅ IMPORT AÑADIDO

class SocialFragment : Fragment() {

    private var _binding: FragmentSocialBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configuración de tabs
        binding.tabLayoutSocial.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showToast("Feed Social - Próximamente")
                    1 -> showToast("Lista de Amigos - Próximamente")
                    2 -> showToast("Mensajes - Próximamente")
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Navegación desde items del menú de la toolbar
        setupToolbarMenu()
    }

    private fun setupToolbarMenu() {
        binding.toolbarSocial?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search_social -> {
                    showToast("Búsqueda en comunidad - Próximamente")
                    true
                }
                R.id.action_notifications_social -> {
                    showToast("Notificaciones - Próximamente")
                    true
                }
                R.id.action_settings_social -> {
                    showToast("Ajustes sociales - Próximamente")
                    true
                }
                else -> false
            }
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