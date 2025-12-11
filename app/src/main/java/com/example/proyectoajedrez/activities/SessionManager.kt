package com.example.proyectoajedrez.activities

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MateMateSesion", Context.MODE_PRIVATE)

    companion object {
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_USERNAME = "username"
    }

    // Guardar sesión
    fun createLoginSession(username: String) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    // Cerrar sesión
    fun logoutUser() {
        val editor = prefs.edit()
        editor.clear() // Borra todo
        editor.apply()
    }

    // ¿Está logueado?
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Obtener nombre
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }
}