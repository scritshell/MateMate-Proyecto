package com.example.proyectoajedrez.activities

import android.content.Context
import android.content.SharedPreferences

// Clase para gestionar la sesión de usuario usando SharedPreferences
class SessionManager(context: Context) {
    // SharedPreferences para almacenar datos de sesión persistentes
    // SharedPreferences servirá para almacenar datos en Android.
    private val prefs: SharedPreferences = context.getSharedPreferences("MateMateSesion", Context.MODE_PRIVATE)

    // Constantes para las claves de almacenamiento
    companion object {
        const val KEY_IS_LOGGED_IN = "is_logged_in"  // Estado de login
        const val KEY_USERNAME = "username"          // Nombre de usuario
    }

    // Crear sesión de usuario después de login exitoso
    fun createLoginSession(username: String) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)  // Marcar como logueado
        editor.putString(KEY_USERNAME, username)   // Guardar nombre de usuario
        editor.apply()  // Guardar cambios (asíncrono)
    }

    // Cerrar sesión eliminando todos los datos
    fun logoutUser() {
        val editor = prefs.edit()
        editor.clear()  // Eliminar todas las preferencias
        editor.apply()  // Aplicar cambios
    }

    // Verificar si hay una sesión activa
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)  // Valor por defecto: false
    }

    // Obtener el nombre de usuario guardado
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)  // Retorna null si no existe
    }
}