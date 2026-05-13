package com.example.proyectoajedrez

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MateMateApp : Application() {

    override fun onCreate() {
        super.onCreate()
        aplicarTemaGuardado()
    }

    private fun aplicarTemaGuardado() {
        val prefs = getSharedPreferences("AjedrezPrefs", MODE_PRIVATE)
        val modoOscuro = prefs.getBoolean("modo_oscuro", false)
        AppCompatDelegate.setDefaultNightMode(
            if (modoOscuro) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}