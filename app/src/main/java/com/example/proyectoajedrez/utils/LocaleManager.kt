package com.example.proyectoajedrez.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

/**
 * Gestor centralizado para la persistencia y aplicación del idioma
 * Responsable de:
 * - Guardar el idioma seleccionado en SharedPreferences
 * - Aplicar el idioma al arrancar la app
 * - Cambiar idioma en tiempo de ejecución
 */
class LocaleManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PrefKeys.AJEDREZ_PREFS, 
        Context.MODE_PRIVATE
    )
    private val appContext = context.applicationContext
    
    /**
     * Obtiene el idioma guardado en SharedPreferences.
     * Si no hay idioma guardado, retorna el idioma del sistema.
     * @return código de idioma ("es", "en", etc)
     */
    fun getLanguage(): String {
        val saved = prefs.getString(PrefKeys.KEY_IDIOMA, null)
        return normalizeLanguage(saved ?: getSystemLanguage())
    }
    
    /**
     * Guarda el idioma seleccionado en SharedPreferences
     * @param languageCode código de idioma ("es", "en", etc)
     */
    fun setLanguage(languageCode: String) {
        prefs.edit().putString(PrefKeys.KEY_IDIOMA, normalizeLanguage(languageCode)).apply()
    }
    
    /**
     * Obtiene el idioma del sistema operativo
     * @return código de idioma del dispositivo
     */
    private fun getSystemLanguage(): String {
        return Locale.getDefault().language
    }

    private fun normalizeLanguage(languageCode: String): String {
        return if (languageCode == "en") "en" else "es"
    }
    
    /**
     * Aplica el idioma guardado (o del sistema) a la aplicación.
     * Se llama desde MateMateApp.onCreate()
     */
    fun applyLanguage() {
        val language = getLanguage()
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = appContext.resources.configuration
        config.setLocale(locale)
        appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)
    }
}
