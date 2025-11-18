package com.example.proyectoajedrez.models

import com.example.proyectoajedrez.model.ChessPiece

data class ChessSquare(
    val col: Int,
    val row: Int,
    var piece: ChessPiece = ChessPiece.EMPTY
) {
    val position: String
        get() = "${'a' + col}${8 - row}" // Notación ajedrez: a1, e4, etc.

    val isLightSquare: Boolean
        get() = (row + col) % 2 == 0
}