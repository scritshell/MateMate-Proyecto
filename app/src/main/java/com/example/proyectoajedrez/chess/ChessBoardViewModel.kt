package com.example.proyectoajedrez.chess

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyectoajedrez.chess.engine.ChessGameManager
import com.example.proyectoajedrez.chess.engine.MoveResult
import com.example.proyectoajedrez.chess.engine.StockfishController
import com.example.proyectoajedrez.chess.puzzle.PuzzleController
import com.example.proyectoajedrez.chess.puzzle.PuzzleMoveResult
import com.example.proyectoajedrez.chess.timer.ChessTimerController
import com.example.proyectoajedrez.model.GameMode
import com.example.proyectoajedrez.model.ChessPiece
import com.example.proyectoajedrez.utils.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChessBoardViewModel(
    private val gameMode: GameMode,
    private val playerSide: Side,
    private val difficulty: Int,
    private val title: String,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChessGameUiState())
    val uiState: StateFlow<ChessGameUiState> = _uiState.asStateFlow()

    // Controllers - inicializados en init block
    private lateinit var gameManager: ChessGameManager
    private lateinit var timerController: ChessTimerController
    private lateinit var stockfishController: StockfishController
    private lateinit var puzzleController: PuzzleController

    // Estado local
    private var selectedSquareIdx: Int? = null
    private var moveHistoryList = mutableListOf<String>()

    init {
        // Crear controllers con callbacks
        gameManager = ChessGameManager()
        timerController = ChessTimerController(
            onTick = { side, millis -> updateTimerDisplay(side, millis) },
            onTimeExpired = { side -> onTimeExpired(side) }
        )
        stockfishController = StockfishController(
            context, viewModelScope,
            onMoveReady = { moveStr -> onEngineMove(moveStr) },
            onThinkingChanged = { isThinking -> _uiState.value = _uiState.value.copy(isEngineThinking = isThinking) }
        )
        puzzleController = PuzzleController(context)
        
        initializeGame()
    }

    private fun initializeGame() {
        // Configurar timers
        val timeMinutes = when {
            gameMode in listOf(GameMode.APERTURA, GameMode.DAILY_PUZZLE) -> -1
            else -> 5 // Por defecto 5 minutos
        }
        timerController.configure(timeMinutes)

        // Configurar Stockfish si es necesario
        if (gameMode == GameMode.LIBRE || gameMode == GameMode.APERTURA) {
            viewModelScope.launch {
                stockfishController.initialize()
                stockfishController.setDifficulty(difficulty)
                if (playerSide == Side.BLACK) {
                    requestEngineMove()
                }
            }
        }

        // Cargar puzzle si es necesario
        if (gameMode == GameMode.DAILY_PUZZLE) {
            viewModelScope.launch {
                val result = puzzleController.loadDailyPuzzle(gameManager.board)
                when (result) {
                    is com.example.proyectoajedrez.chess.puzzle.PuzzleLoadResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            title = "Puzzle Diario (${result.rating})"
                        )
                        updateBoardDisplay()
                    }
                    is com.example.proyectoajedrez.chess.puzzle.PuzzleLoadResult.Error -> {
                        // Error loading puzzle
                    }
                }
            }
        }

        updateBoardDisplay()
        _uiState.value = _uiState.value.copy(
            title = title,
            isTimerVisible = !timerController.isUnlimited
        )
    }

    fun onEvent(event: ChessGameEvent) {
        when (event) {
            is ChessGameEvent.SquareTapped -> onSquareTapped(event.visualPosition)
            ChessGameEvent.UndoRequested -> onUndoMove()
            ChessGameEvent.RedoRequested -> onRedoMove()
            ChessGameEvent.ExitRequested -> onExit()
        }
    }

    private fun onSquareTapped(visualPosition: Int) {
        // Si estamos en modo review, no permitir tocar piezas
        if (gameManager.isInReviewMode) return

        // Si está pensando Stockfish, no permitir
        if (_uiState.value.isEngineThinking) return

        // Si es modo apertura, no permitir
        if (gameMode == GameMode.APERTURA) return

        // Si no es turno del jugador en modo LIBRE/PUZZLE, no permitir
        if ((gameMode == GameMode.LIBRE || gameMode == GameMode.DAILY_PUZZLE) && gameManager.board.sideToMove != playerSide) return

        val logicalIdx = convertVisualToLogical(visualPosition)

        if (selectedSquareIdx == null) {
            // Seleccionar pieza
            selectSquare(logicalIdx)
        } else {
            if (logicalIdx == selectedSquareIdx) {
                // Deseleccionar
                deselectSquare()
            } else {
                // Intentar mover
                attemptMove(selectedSquareIdx!!, logicalIdx)
            }
        }
    }

    private fun selectSquare(idx: Int) {
        val piece = boardPieceAt(idx)
        if (piece != ChessPiece.EMPTY) {
            val isMyPiece = (gameManager.board.sideToMove == Side.WHITE && piece.isWhite) ||
                           (gameManager.board.sideToMove == Side.BLACK && piece.isBlack)
            if (isMyPiece) {
                selectedSquareIdx = idx
                val moves = gameManager.getLegalMovesFor(idx)
                _uiState.value = _uiState.value.copy(
                    selectedSquare = idx,
                    legalMoveSquares = moves
                )
            }
        }
    }

    private fun deselectSquare() {
        selectedSquareIdx = null
        _uiState.value = _uiState.value.copy(
            selectedSquare = null,
            legalMoveSquares = emptyList()
        )
    }

    private fun attemptMove(fromIdx: Int, toIdx: Int) {
        val moveResult = gameManager.attemptMove(fromIdx, toIdx)

        when (moveResult) {
            MoveResult.Illegal -> {
                deselectSquare()
            }
            is MoveResult.Success -> {
                // En modo puzzle, validar primero
                if (gameMode == GameMode.DAILY_PUZZLE) {
                    val move = gameManager.board.backup.last.move
                    val puzzleResult = puzzleController.validateMove(move)
                    
                    when (puzzleResult) {
                        PuzzleMoveResult.Incorrect -> {
                            // Deshacer movimiento inválido
                            gameManager.undoMove()
                        }
                        is PuzzleMoveResult.CorrectContinue -> {
                            moveHistoryList.add(moveResult.historyEntry)
                            playMoveSound(moveResult.wasCapture)
                            deselectSquare()
                            updateBoardDisplay()
                            startTimer(gameManager.board.sideToMove)
                            
                            // Engine juega siguiente movimiento
                            viewModelScope.launch {
                                delay(800)
                                val engineMove = puzzleResult.nextEngineMove
                                val engineMoveObj = ChessUtils.sanToMove(engineMove, gameManager.board)
                                if (engineMoveObj != null) {
                                    gameManager.executeMove(engineMoveObj)
                                    puzzleController.consumeEngineMove()
                                    updateBoardDisplay()
                                }
                            }
                            return
                        }
                        PuzzleMoveResult.Solved -> {
                            moveHistoryList.add(moveResult.historyEntry)
                            playMoveSound(moveResult.wasCapture)
                            viewModelScope.launch {
                                puzzleController.saveProgress()
                            }
                            _uiState.value = _uiState.value.copy(gameStatus = GameStatus.PUZZLE_SOLVED)
                            updateBoardDisplay()
                            deselectSquare()
                            return
                        }
                    }
                }

                moveHistoryList.add(moveResult.historyEntry)
                playMoveSound(moveResult.wasCapture)
                deselectSquare()
                updateBoardDisplay()
                startTimer(gameManager.board.sideToMove)

                // Si el motor debe jugar
                if (gameMode == GameMode.LIBRE && gameManager.board.sideToMove != playerSide) {
                    requestEngineMove()
                }
            }
        }
    }

    private fun onUndoMove() {
        if (gameManager.undoMove()) {
            updateBoardDisplay()
        }
    }

    private fun onRedoMove() {
        if (gameManager.redoMove()) {
            updateBoardDisplay()
        }
    }

    private fun onExit() {
        timerController.cancelAll()
        stockfishController.close()
    }

    private fun requestEngineMove() {
        stockfishController.requestMove(gameManager.board)
    }

    private fun onEngineMove(uciMove: String) {
        viewModelScope.launch {
            try {
                val legalMoves = gameManager.board.legalMoves()
                val move = legalMoves.firstOrNull { it.toString().lowercase() == uciMove.lowercase() }
                
                if (move != null) {
                    val result = gameManager.executeMove(move)
                    if (result is MoveResult.Success) {
                        moveHistoryList.add(result.historyEntry)
                        playMoveSound(result.wasCapture)
                        updateBoardDisplay()
                        startTimer(gameManager.board.sideToMove)
                    }
                }
            } catch (e: Exception) {
                // Error procesando movimiento del motor
            }
        }
    }

    private fun onTimeExpired(side: Side) {
        _uiState.value = _uiState.value.copy(gameStatus = GameStatus.DRAW_REPETITION) // Placeholder
    }

    private fun startTimer(side: Side) {
        if (!timerController.isUnlimited) {
            timerController.startFor(side)
        }
    }

    private fun updateTimerDisplay(side: Side, millis: Long) {
        val formatted = timerController.formatTime(millis)
        _uiState.value = if (side == Side.WHITE) {
            _uiState.value.copy(timerWhiteText = formatted, activeTimer = Side.WHITE)
        } else {
            _uiState.value.copy(timerBlackText = formatted, activeTimer = Side.BLACK)
        }
    }

    private fun updateBoardDisplay() {
        val newPieces = Array(64) { idx -> boardPieceAt(idx) }
        val newStatus = when (gameManager.board) {
            gameManager.board -> when {
                gameManager.board.isMated -> {
                    if (gameManager.board.sideToMove == playerSide) GameStatus.BLACK_WINS else GameStatus.WHITE_WINS
                }
                gameManager.board.isStaleMate -> GameStatus.DRAW_STALEMATE
                gameManager.board.isDraw -> GameStatus.DRAW_REPETITION
                else -> GameStatus.PLAYING
            }
            else -> GameStatus.PLAYING
        }

        _uiState.value = _uiState.value.copy(
            pieces = newPieces,
            moveHistory = moveHistoryList.toList(),
            currentTurn = gameManager.board.sideToMove,
            gameStatus = newStatus,
            isInReviewMode = gameManager.isInReviewMode
        )
    }

    private fun boardPieceAt(idx: Int): ChessPiece {
        val square = gameManager.squareFromIndex(idx)
        val piece = gameManager.board.getPiece(square)
        return ChessPiece.fromChessLibPiece(piece)
    }

    private fun convertVisualToLogical(visual: Int): Int {
        // Si el tablero está flipped, invertir
        return if (_uiState.value.isFlipped) {
            63 - visual
        } else {
            visual
        }
    }

    private fun playMoveSound(wasCapture: Boolean) {
        // Este callback irá a Fragment
    }

    private fun moveToUci(move: com.github.bhlangonijr.chesslib.move.Move): String {
        val from = move.from.toString().lowercase()
        val to = move.to.toString().lowercase()
        val promotion = if (move.promotion != com.github.bhlangonijr.chesslib.Piece.NONE) {
            move.promotion.toString().lowercase().last().toString()
        } else ""
        return "$from$to$promotion"
    }
}

class ChessBoardViewModelFactory(
    private val context: Context,
    private val gameMode: GameMode,
    private val playerSide: Side,
    private val difficulty: Int,
    private val titleArg: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChessBoardViewModel(
            gameMode = gameMode,
            playerSide = playerSide,
            difficulty = difficulty,
            title = titleArg,
            context = context
        ) as T
    }
}