package com.example.proyectoajedrez.chess

/**
 * Eventos de UI de un solo disparo (SharedFlow)
 */
sealed class ChessUiEvent {
    object IncorrectPuzzleMove : ChessUiEvent()
    object PuzzleSolved : ChessUiEvent()
    object EngineUnavailable : ChessUiEvent()
}
