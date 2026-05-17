package com.example.proyectoajedrez.utils

/**
 * Centralización de constantes para SharedPreferences
 * Evita hardcodear strings de claves en todo el proyecto
 */
object PrefKeys {
    
    // ==================== NOMBRES DE PREFERENCIAS ====================
    
    /** SharedPreferences para configuración de sesión y autenticación */
    const val MATE_SESSION_PREFS = "MateMateSesion"
    
    /** SharedPreferences para configuración general de la aplicación */
    const val AJEDREZ_PREFS = "AjedrezPrefs"
    
    
    // ==================== CLAVES DE SESIÓN ====================
    
    /** Indica si el usuario está actualmente logueado */
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    /** Nombre de usuario almacenado en sesión */
    const val KEY_USERNAME = "username"
    
    /** UID de Firebase del usuario autenticado */
    const val KEY_USER_UID = "user_uid"
    
    /** Rol del usuario (USER o ADMIN) */
    const val KEY_USER_ROLE = "user_role"
    
    
    // ==================== CLAVES DE PREFERENCIAS GENERALES ====================
    
    /** Indica si el modo oscuro está activado */
    const val KEY_MODO_OSCURO = "modo_oscuro"
    
    /** Idioma seleccionado (ej: "es", "en") */
    const val KEY_IDIOMA = "idioma"
    
    /** Indica si se usa skin alternativo para las piezas de ajedrez */
    const val KEY_USAR_SKIN_ALT = "usar_skin_alt"
    
    /** Nombre de usuario en Lichess para sincronizar puzzles */
    const val KEY_LICHESS_USERNAME = "lichess_username"
    
    /** Número de días consecutivos resolviendo puzzles */
    const val KEY_PUZZLE_STREAK_DAYS = "puzzle_streak_days"
    
    /** Cantidad de puzzles resueltos localmente */
    const val KEY_LOCAL_PUZZLES_SOLVED = "local_puzzles_solved"
}
