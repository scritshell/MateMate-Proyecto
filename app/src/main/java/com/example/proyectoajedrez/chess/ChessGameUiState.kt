package com.example.proyectoajedrez.chess

import com.example.proyectoajedrez.model.ChessPiece
import com.github.bhlangonijr.chesslib.Side

data class ChessGameUiState(
    // Estado del tablero
    val pieces: Array<ChessPiece> = Array(64) { ChessPiece.EMPTY },
    val selectedSquare: Int? = null,
    val legalMoveSquares: List<Int> = emptyList(),
    val errorSquare: Int? = null,
    val isFlipped: Boolean = false,

    // Estado de la partida
    val currentTurn: Side = Side.WHITE,
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val isEngineThinking: Boolean = false,
    val isInReviewMode: Boolean = false,

    // Historial
    val moveHistory: List<String> = emptyList(),

    // Temporizadores (texto ya formateado para la UI)
    val timerWhiteText: String = "05:00",
    val timerBlackText: String = "05:00",
    val isTimerVisible: Boolean = false,
    val activeTimer: Side = Side.WHITE,

    // Título y modo
    val title: String = "Tablero de Ajedrez",
)

enum class GameStatus {
    PLAYING,
    WHITE_WINS,
    BLACK_WINS,
    DRAW_STALEMATE,
    DRAW_REPETITION,
    PUZZLE_SOLVED,
    PUZZLE_FAILED
}

// Eventos que el Fragment envía al ViewModel (en lugar de lógica directa)
sealed class ChessGameEvent {
    data class SquareTapped(val visualPosition: Int) : ChessGameEvent()
    object UndoRequested : ChessGameEvent()
    object RedoRequested : ChessGameEvent()
    object ExitRequested : ChessGameEvent()
}