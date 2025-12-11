package com.example.proyectoajedrez.model

import com.example.proyectoajedrez.R

enum class ChessPiece(val drawableRes: Int) {
    EMPTY(0),

    // Piezas Blancas
    WHITE_PAWN(R.drawable.ic_w_pawn),
    WHITE_ROOK(R.drawable.ic_w_rook),
    WHITE_KNIGHT(R.drawable.ic_w_knight),
    WHITE_BISHOP(R.drawable.ic_w_bishop),
    WHITE_QUEEN(R.drawable.ic_w_queen),
    WHITE_KING(R.drawable.ic_w_king),

    // Piezas Negras
    BLACK_PAWN(R.drawable.ic_b_pawn),
    BLACK_ROOK(R.drawable.ic_b_rook),
    BLACK_KNIGHT(R.drawable.ic_b_knight),
    BLACK_BISHOP(R.drawable.ic_b_bishop),
    BLACK_QUEEN(R.drawable.ic_b_queen),
    BLACK_KING(R.drawable.ic_b_king);

    val isWhite: Boolean
        get() = this.name.startsWith("WHITE")

    val isBlack: Boolean
        get() = this.name.startsWith("BLACK")
}