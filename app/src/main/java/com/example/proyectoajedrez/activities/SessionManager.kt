package com.example.proyectoajedrez.activities

import android.content.Context
import android.content.SharedPreferences
import com.example.proyectoajedrez.utils.PrefKeys

// Clase para gestionar la sesión de usuario usando SharedPreferences.
class SessionManager(context: Context) {
    // SharedPreferences para almacenar datos de sesión persistentes
    // SharedPreferences servirá para almacenar datos en Android.
    private val prefs: SharedPreferences = context.getSharedPreferences(PrefKeys.MATE_SESSION_PREFS, Context.MODE_PRIVATE)

    // Crear sesión de usuario después de login exitoso
    fun createLoginSession(username: String) {
        val editor = prefs.edit()
        editor.putBoolean(PrefKeys.KEY_IS_LOGGED_IN, true)  // Marcar como logueado
        editor.putString(PrefKeys.KEY_USERNAME, username)   // Guardar nombre de usuario
        editor.apply()  // Guardar cambios
    }

    // Cerrar sesión eliminando todos los datos
    fun logoutUser() {
        val editor = prefs.edit()
        editor.clear()  // Eliminar todas las preferencias
        editor.apply()  // Aplicar cambios
    }

    // Verificar si hay una sesión activa
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(PrefKeys.KEY_IS_LOGGED_IN, false)  // Valor por defecto: false
    }

    // Obtener el nombre de usuario guardado
    fun getUsername(): String? {
        return prefs.getString(PrefKeys.KEY_USERNAME, null)  // Retorna null si no existe
    }
}