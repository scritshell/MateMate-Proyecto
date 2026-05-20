package com.example.proyectoajedrez

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.proyectoajedrez.utils.LocaleManager
import com.example.proyectoajedrez.utils.PrefKeys

class MateMateApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 1. Aplicar el idioma guardado (o del sistema) al arrancar
        aplicarIdiomaGuardado()
        
        // 2. Aplicar el tema (modo oscuro) guardado al arrancar
        aplicarTemaGuardado()
    }

    /**
     * Aplica el idioma almacenado en SharedPreferences.
     * Si no hay idioma guardado, usa el idioma del sistema.
     */
    private fun aplicarIdiomaGuardado() {
        val localeManager = LocaleManager(this)
        localeManager.applyLanguage()
    }

    /**
     * Aplica el modo oscuro almacenado en SharedPreferences.
     * Si no hay preferencia guardada, usa el modo claro.
     */
    private fun aplicarTemaGuardado() {
        val prefs = getSharedPreferences(PrefKeys.AJEDREZ_PREFS, MODE_PRIVATE)
        val modoOscuro = prefs.getBoolean(PrefKeys.KEY_MODO_OSCURO, false)
        AppCompatDelegate.setDefaultNightMode(
            if (modoOscuro) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}