package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDarkMode()
        setupLanguage()
    }

    private fun setupDarkMode() {
        // 1. Leer estado actual
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        binding.switchDarkMode.isChecked = currentNightMode == AppCompatDelegate.MODE_NIGHT_YES

        // 2. Escuchar cambios
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun setupLanguage() {
        // 1. Detectar idioma actual
        val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (currentLocale.contains("en")) {
            binding.rbEnglish.isChecked = true
        } else {
            binding.rbSpanish.isChecked = true
        }

        // 2. Escuchar cambios
        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            val idioma = when (checkedId) {
                R.id.rbEnglish -> "en"
                else -> "es"
            }
            // Esto cambia el idioma y REINICIA la actividad automáticamente
            val appLocale = LocaleListCompat.forLanguageTags(idioma)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}