package com.example.proyectoajedrez.model

enum class ChessPiece {
    EMPTY,
    WHITE_PAWN, WHITE_ROOK, WHITE_KNIGHT, WHITE_BISHOP, WHITE_QUEEN, WHITE_KING,
    BLACK_PAWN, BLACK_ROOK, BLACK_KNIGHT, BLACK_BISHOP, BLACK_QUEEN, BLACK_KING;

    val isWhite: Boolean
        get() = this.name.startsWith("WHITE")

    val isBlack: Boolean
        get() = this.name.startsWith("BLACK")

    val pieceType: String
        get() = when (this) {
            EMPTY -> "Empty"
            WHITE_PAWN, BLACK_PAWN -> "Pawn"
            WHITE_ROOK, BLACK_ROOK -> "Rook"
            WHITE_KNIGHT, BLACK_KNIGHT -> "Knight"
            WHITE_BISHOP, BLACK_BISHOP -> "Bishop"
            WHITE_QUEEN, BLACK_QUEEN -> "Queen"
            WHITE_KING, BLACK_KING -> "King"
        }

    val symbol: String
        get() = when (this) {
            EMPTY -> ""
            WHITE_PAWN -> "♙"
            BLACK_PAWN -> "♟"
            WHITE_ROOK -> "♖"
            BLACK_ROOK -> "♜"
            WHITE_KNIGHT -> "♘"
            BLACK_KNIGHT -> "♞"
            WHITE_BISHOP -> "♗"
            BLACK_BISHOP -> "♝"
            WHITE_QUEEN -> "♕"
            BLACK_QUEEN -> "♛"
            WHITE_KING -> "♔"
            BLACK_KING -> "♚"
        }

    companion object {
        fun fromString(pieceName: String): ChessPiece {
            return try {
                valueOf(pieceName)
            } catch (e: IllegalArgumentException) {
                EMPTY
            }
        }
    }
}