package com.example.proyectoajedrez.model

// Clase de datos que representa una casilla del tablero de ajedrez
data class ChessSquare(
    val col: Int,                         // Columna (0-7 donde 0 = columna 'a')
    val row: Int,                         // Fila (0-7 donde 0 = fila 8 del tablero)
    var piece: ChessPiece = ChessPiece.EMPTY  // Pieza en la casilla (vacía por defecto)
) {
    // Propiedad calculada: notación de ajedrez de la casilla (ej: "a1", "e4")
    val position: String
        get() = "${'a' + col}${8 - row}"  // Convierte coordenadas a notación estándar

    // Propiedad calculada: determina si la casilla es clara u oscura
    val isLightSquare: Boolean
        get() = (row + col) % 2 == 0      // Casilla clara si suma par, oscura si impar
}