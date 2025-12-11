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
    }

    // Configurar interruptor de modo oscuro/claro
    private fun setupDarkMode() {
        // Obtener estado actual del modo oscuro
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()

        // Marcar switch según el estado actual
        binding.switchDarkMode.isChecked = currentNightMode == AppCompatDelegate.MODE_NIGHT_YES

        // Listener para cambios en el switch
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)  // Modo oscuro
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)   // Modo claro
            }
        }
    }

    // Configurar selección de idioma (español/inglés)
    private fun setupLanguage() {
        // Detectar idioma actual de la aplicación
        val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()

        // Marcar el radio button correspondiente
        if (currentLocale.contains("en")) {
            binding.rbEnglish.isChecked = true  // Inglés seleccionado
        } else {
            binding.rbSpanish.isChecked = true  // Español seleccionado
        }

        // Listener para cambios en el grupo de radio buttons
        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            val idioma = when (checkedId) {
                R.id.rbEnglish -> "en"  // Código para inglés
                else -> "es"            // Código para español
            }

            // Aplicar nuevo idioma (reinicia automáticamente la actividad)
            val appLocale = LocaleListCompat.forLanguageTags(idioma)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}