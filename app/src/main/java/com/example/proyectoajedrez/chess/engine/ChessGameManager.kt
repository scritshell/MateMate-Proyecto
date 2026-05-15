package com.example.proyectoajedrez.chess.engine

import com.example.proyectoajedrez.chess.GameStatus
import com.example.proyectoajedrez.utils.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

class ChessGameManager {

    val board = Board()
    private val undoStack = mutableListOf<Move>()

    // Intenta realizar un movimiento. Retorna el resultado.
    fun attemptMove(fromIdx: Int, toIdx: Int): MoveResult {
        val fromSq = squareFromIndex(fromIdx)
        val toSq = squareFromIndex(toIdx)
        val piece = board.getPiece(fromSq)

        val promo = when {
            piece == Piece.WHITE_PAWN && toSq.rank == Rank.RANK_8 -> Piece.WHITE_QUEEN
            piece == Piece.BLACK_PAWN && toSq.rank == Rank.RANK_1 -> Piece.BLACK_QUEEN
            else -> Piece.NONE
        }

        val move = Move(fromSq, toSq, promo)
        if (!board.legalMoves().contains(move)) return MoveResult.Illegal

        return executeMove(move)
    }

    fun executeMove(move: Move): MoveResult {
        val wasCapture = board.getPiece(move.to) != Piece.NONE
        val san = move.toString()
        val moveNumber = board.moveCounter
        val wasWhiteTurn = board.sideToMove == Side.WHITE

        board.doMove(move)
        undoStack.clear()

        val historyEntry = if (wasWhiteTurn) "$moveNumber. $san" else san
        val status = evaluateGameStatus()

        return MoveResult.Success(
            san = san,
            moveNumber = moveNumber,
            wasWhiteTurn = wasWhiteTurn,
            historyEntry = historyEntry,
            wasCapture = wasCapture,
            gameStatus = status
        )
    }

    fun undoMove(): Boolean {
        if (board.backup.isEmpty) return false
        val lastMove = board.backup.last.move
        undoStack.add(lastMove)
        board.undoMove()
        return true
    }

    fun redoMove(): Boolean {
        if (undoStack.isEmpty()) return false
        val move = undoStack.removeAt(undoStack.size - 1)
        board.doMove(move)
        return true
    }

    val isInReviewMode: Boolean get() = undoStack.isNotEmpty()

    fun getLegalMovesFor(squareIdx: Int): List<Int> {
        val square = squareFromIndex(squareIdx)
        return board.legalMoves()
            .filter { it.from == square }
            .map { indexFromSquare(it.to) }
    }

    private fun evaluateGameStatus(): GameStatus = when {
        board.isMated -> if (board.sideToMove == Side.WHITE) GameStatus.BLACK_WINS else GameStatus.WHITE_WINS
        board.isStaleMate -> GameStatus.DRAW_STALEMATE
        board.isDraw -> GameStatus.DRAW_REPETITION
        else -> GameStatus.PLAYING
    }

    fun squareFromIndex(idx: Int): Square =
        Square.encode(Rank.values()[7 - idx / 8], com.github.bhlangonijr.chesslib.File.values()[idx % 8])

    fun indexFromSquare(sq: Square): Int =
        (7 - sq.rank.ordinal) * 8 + sq.file.ordinal
}

sealed class MoveResult {
    object Illegal : MoveResult()
    data class Success(
        val san: String,
        val moveNumber: Int,
        val wasWhiteTurn: Boolean,
        val historyEntry: String,
        val wasCapture: Boolean,
        val gameStatus: GameStatus
    ) : MoveResult()
}
