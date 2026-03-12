package com.example.proyectoajedrez.utils

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move

object ChessUtils {
    // Convierte notación SAN (ej: "Nf3", "O-O", "e8=Q") a objeto Move válido para el tablero.
    fun sanToMove(san: String, board: Board): Move? {
        val legalMoves = board.legalMoves()
        var text = san.replace("+", "").replace("#", "").replace("x", "")

        // 1. Enroques
        // 1. Enroques (Blindado para que SOLO lo haga el Rey)
        if (text == "O-O" || text == "0-0") {
            return legalMoves.find {
                it.from.file == File.FILE_E &&
                        it.to.file == File.FILE_G &&
                        board.getPiece(it.from).pieceType == PieceType.KING
            }
        }
        if (text == "O-O-O" || text == "0-0-0") {
            return legalMoves.find {
                it.from.file == File.FILE_E &&
                        it.to.file == File.FILE_C &&
                        board.getPiece(it.from).pieceType == PieceType.KING
            }
        }

        // 2. Promociones (Soporta e8=Q y e8Q)
        var promoPiece = Piece.NONE
        if (text.contains("=")) {
            val parts = text.split("=")
            text = parts[0]
            val color = board.sideToMove
            promoPiece = when (parts[1].uppercase()) {
                "Q" -> if (color == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
                "R" -> if (color == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
                "B" -> if (color == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                "N" -> if (color == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                else -> Piece.NONE
            }
        } else if (text.length >= 3 && text.last() in listOf('Q', 'R', 'B', 'N') && text[text.length-2].isDigit()) {
            val color = board.sideToMove
            promoPiece = when (text.last()) {
                'Q' -> if (color == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
                'R' -> if (color == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
                'B' -> if (color == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                'N' -> if (color == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                else -> Piece.NONE
            }
            text = text.dropLast(1)
        }

        // Failsafe de seguridad por si Lichess manda basura
        if (text.length < 2) return null

        // 3. Búsqueda de candidatos
        val destStr = text.takeLast(2)
        val pieceChar = if (text.first().isUpperCase()) text.first() else 'P'

        val candidates = legalMoves.filter { move ->
            move.to.toString().equals(destStr, ignoreCase = true) &&
                    isPieceType(board.getPiece(move.from), pieceChar)
        }

        // 4. Selección final y Desambiguación robusta
        val selectedMove = if (candidates.size == 1) {
            candidates[0]
        } else {
            val disambiguator = text.substring(if (pieceChar == 'P') 0 else 1, text.length - 2)
            candidates.find { move ->
                move.from.toString().contains(disambiguator, ignoreCase = true)
            } ?: candidates.firstOrNull() // SALVAVIDAS: Si falla, coge el primero válido para no desincronizar turnos
        }

        return selectedMove?.let {
            if (promoPiece != Piece.NONE) Move(it.from, it.to, promoPiece) else it
        }
    }

    private fun isPieceType(piece: Piece, char: Char): Boolean {
        return when (char) {
            'N' -> piece.pieceType == PieceType.KNIGHT
            'B' -> piece.pieceType == PieceType.BISHOP
            'R' -> piece.pieceType == PieceType.ROOK
            'Q' -> piece.pieceType == PieceType.QUEEN
            'K' -> piece.pieceType == PieceType.KING
            'P' -> piece.pieceType == PieceType.PAWN
            else -> false
        }
    }
}