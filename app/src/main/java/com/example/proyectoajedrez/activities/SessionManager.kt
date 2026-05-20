package com.example.proyectoajedrez.activities

import android.content.Context
import android.content.SharedPreferences
import com.example.proyectoajedrez.domain.model.UserRole
import com.example.proyectoajedrez.utils.PrefKeys

// Clase para gestionar la sesión de usuario usando SharedPreferences.
class SessionManager(context: Context) {
    // SharedPreferences para almacenar datos de sesión persistentes
    // SharedPreferences servirá para almacenar datos en Android.
    private val prefs: SharedPreferences = context.getSharedPreferences(PrefKeys.MATE_SESSION_PREFS, Context.MODE_PRIVATE)

    // Crear sesión de usuario después de login exitoso
    fun createLoginSession(username: String, uid: String = "", role: UserRole = UserRole.USER) {
        val editor = prefs.edit()
        editor.putBoolean(PrefKeys.KEY_IS_LOGGED_IN, true)  // Marcar como logueado
        editor.putString(PrefKeys.KEY_USERNAME, username)   // Guardar nombre de usuario
        editor.putString(PrefKeys.KEY_USER_UID, uid)        // Guardar UID de Firebase
        editor.putString(PrefKeys.KEY_USER_ROLE, role.name) // Guardar rol (USER o ADMIN)
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
    
    // Obtener el UID de Firebase del usuario
    fun getUserUid(): String? {
        return prefs.getString(PrefKeys.KEY_USER_UID, null)
    }
    
    // Obtener el rol del usuario actual
    fun getUserRole(): UserRole {
        val roleStr = prefs.getString(PrefKeys.KEY_USER_ROLE, UserRole.USER.name)
        return try {
            UserRole.valueOf(roleStr ?: UserRole.USER.name)
        } catch (e: Exception) {
            UserRole.USER  // Valor por defecto si hay error
        }
    }
    
    // Verificar si el usuario es administrador
    fun isAdmin(): Boolean {
        return getUserRole() == UserRole.ADMIN
    }
}