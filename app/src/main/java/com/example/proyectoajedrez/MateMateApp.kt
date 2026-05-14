package com.example.proyectoajedrez

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.proyectoajedrez.utils.PrefKeys

class MateMateApp : Application() {

    override fun onCreate() {
        super.onCreate()
        aplicarTemaGuardado()
    }

    private fun aplicarTemaGuardado() {
        val prefs = getSharedPreferences(PrefKeys.AJEDREZ_PREFS, MODE_PRIVATE)
        val modoOscuro = prefs.getBoolean(PrefKeys.KEY_MODO_OSCURO, false)
        AppCompatDelegate.setDefaultNightMode(
            if (modoOscuro) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}