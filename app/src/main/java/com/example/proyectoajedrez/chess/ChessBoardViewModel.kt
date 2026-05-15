package com.example.proyectoajedrez.chess

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Por ahora vacío — irá recibiendo lógica en las siguientes fases
class ChessBoardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChessGameUiState())
    val uiState: StateFlow<ChessGameUiState> = _uiState.asStateFlow()

    fun onEvent(event: ChessGameEvent) {
        // Se irá implementando fase por fase
    }
}