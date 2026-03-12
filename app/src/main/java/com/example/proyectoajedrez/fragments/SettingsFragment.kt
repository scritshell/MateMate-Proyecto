package com.example.proyectoajedrez.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.databinding.FragmentSettingsBinding

// Fragmento de configuración para ajustes de la aplicación
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding  // ViewBinding para el fragmento

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDarkMode()   // Configurar toggle de modo oscuro
        setupLanguage()   // Configurar selector de idioma
        setupSkins()      // NUEVO: Configurar selector de estilo de piezas
    }

    // Configurar interruptor de modo oscuro/claro
    private fun setupDarkMode() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        binding.switchDarkMode.isChecked = currentNightMode == AppCompatDelegate.MODE_NIGHT_YES

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    // Configurar selección de idioma (español/inglés)
    private fun setupLanguage() {
        val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()

        if (currentLocale.contains("en")) {
            binding.rbEnglish.isChecked = true
        } else {
            binding.rbSpanish.isChecked = true
        }

        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            val idioma = when (checkedId) {
                R.id.rbEnglish -> "en"
                else -> "es"
            }

            val appLocale = LocaleListCompat.forLanguageTags(idioma)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    // ALTERNAR SKIN
    private fun setupSkins() {
        val sharedPref = requireContext().getSharedPreferences("AjedrezPrefs", Context.MODE_PRIVATE)

        binding.switchSkinAlt.isChecked = sharedPref.getBoolean("usar_skin_alt", false)

        binding.switchSkinAlt.setOnCheckedChangeListener { _, isChecked ->
            with(sharedPref.edit()) {
                putBoolean("usar_skin_alt", isChecked)
                apply()
            }
            Toast.makeText(context, "Estilo de piezas actualizado", Toast.LENGTH_SHORT).show()
        }
    }
}