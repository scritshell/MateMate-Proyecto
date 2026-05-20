package com.example.proyectoajedrez.domain.model

/**
 * Enum de roles de usuario para control de permisos
 */
enum class UserRole {
    USER,      // Usuario normal
    ADMIN      // Administrador (puede moderar foro, etc)
}

/**
 * Modelo de Usuario coherente con autenticación y roles
 */
data class User(
    val uid: String = "",                    // UID de Firebase
    val username: String = "",               // Nombre de usuario
    val email: String = "",                  // Email
    val role: UserRole = UserRole.USER,      // Rol por defecto: USER
    val createdAt: Long = System.currentTimeMillis(),
    val lichessUsername: String? = null      // Username de Lichess para sincronización
)
