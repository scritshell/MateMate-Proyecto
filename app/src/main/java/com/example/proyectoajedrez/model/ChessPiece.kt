package com.example.proyectoajedrez.model

import com.example.proyectoajedrez.R

// Enumeración que representa las piezas de ajedrez con sus recursos gráficos
enum class ChessPiece(val drawableRes: Int) {
    EMPTY(0),  // Casilla vacía

    // PIEZAS BLANCAS
    WHITE_PAWN(R.drawable.ic_w_pawn),    // Peón blanco
    WHITE_ROOK(R.drawable.ic_w_rook),    // Torre blanca
    WHITE_KNIGHT(R.drawable.ic_w_knight),// Caballo blanco
    WHITE_BISHOP(R.drawable.ic_w_bishop),// Alfil blanco
    WHITE_QUEEN(R.drawable.ic_w_queen),  // Dama blanca
    WHITE_KING(R.drawable.ic_w_king),    // Rey blanco

    // PIEZAS NEGRAS
    BLACK_PAWN(R.drawable.ic_b_pawn),    // Peón negro
    BLACK_ROOK(R.drawable.ic_b_rook),    // Torre negra
    BLACK_KNIGHT(R.drawable.ic_b_knight),// Caballo negro
    BLACK_BISHOP(R.drawable.ic_b_bishop),// Alfil negro
    BLACK_QUEEN(R.drawable.ic_b_queen),  // Dama negra
    BLACK_KING(R.drawable.ic_b_king);    // Rey negro

    // Propiedad que indica si la pieza es blanca
    val isWhite: Boolean
        get() = this.name.startsWith("WHITE")

    // Propiedad que indica si la pieza es negra
    val isBlack: Boolean
        get() = this.name.startsWith("BLACK") // bcspn (ay noo)

    companion object {
        fun fromChessLibPiece(piece: com.github.bhlangonijr.chesslib.Piece): ChessPiece = when (piece) {
            com.github.bhlangonijr.chesslib.Piece.WHITE_PAWN -> WHITE_PAWN
            com.github.bhlangonijr.chesslib.Piece.WHITE_ROOK -> WHITE_ROOK
            com.github.bhlangonijr.chesslib.Piece.WHITE_KNIGHT -> WHITE_KNIGHT
            com.github.bhlangonijr.chesslib.Piece.WHITE_BISHOP -> WHITE_BISHOP
            com.github.bhlangonijr.chesslib.Piece.WHITE_QUEEN -> WHITE_QUEEN
            com.github.bhlangonijr.chesslib.Piece.WHITE_KING -> WHITE_KING
            com.github.bhlangonijr.chesslib.Piece.BLACK_PAWN -> BLACK_PAWN
            com.github.bhlangonijr.chesslib.Piece.BLACK_ROOK -> BLACK_ROOK
            com.github.bhlangonijr.chesslib.Piece.BLACK_KNIGHT -> BLACK_KNIGHT
            com.github.bhlangonijr.chesslib.Piece.BLACK_BISHOP -> BLACK_BISHOP
            com.github.bhlangonijr.chesslib.Piece.BLACK_QUEEN -> BLACK_QUEEN
            com.github.bhlangonijr.chesslib.Piece.BLACK_KING -> BLACK_KING
            else -> EMPTY
        }
    }
}