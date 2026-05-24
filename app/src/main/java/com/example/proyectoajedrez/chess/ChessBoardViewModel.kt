package com.example.proyectoajedrez.chess

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val context: Context,
    private val openingMoves: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChessGameUiState())
    val uiState: StateFlow<ChessGameUiState> = _uiState.asStateFlow()

    private lateinit var gameManager: ChessGameManager
    private lateinit var timerController: ChessTimerController
    private lateinit var stockfishController: StockfishController
    private lateinit var puzzleController: PuzzleController

    private val _uiEvents = MutableSharedFlow<ChessUiEvent>(extraBufferCapacity = 4)
    val uiEvents: SharedFlow<ChessUiEvent> = _uiEvents.asSharedFlow()

    private var selectedSquareIdx: Int? = null
    private var moveHistoryList = mutableListOf<String>()

    // Lado efectivo actualizable tras cargar el puzzle
    private var effectivePlayerSide: Side = playerSide

    init {
        gameManager = ChessGameManager()
        timerController = ChessTimerController(
            onTick = { side, millis -> updateTimerDisplay(side, millis) },
            onTimeExpired = { side -> onTimeExpired(side) }
        )
        stockfishController = StockfishController(
            context, viewModelScope,
            onMoveReady = { moveStr -> onEngineMove(moveStr) },
            onThinkingChanged = { isThinking ->
                _uiState.value = _uiState.value.copy(isEngineThinking = isThinking)
            }
        )
        puzzleController = PuzzleController(context)

        initializeGame()
    }

    private fun initializeGame() {
        val timeMinutes = when {
            gameMode in listOf(GameMode.APERTURA, GameMode.DAILY_PUZZLE) -> -1
            else -> 5
        }
        timerController.configure(timeMinutes)

        if (gameMode == GameMode.APERTURA) {
            viewModelScope.launch {
                replayOpeningMoves(openingMoves)
            }
        } else if (gameMode == GameMode.LIBRE || gameMode == GameMode.LOCAL_2P) {
            viewModelScope.launch {
                stockfishController.initialize()
                stockfishController.setDifficulty(difficulty)
                if (gameMode == GameMode.LIBRE && playerSide == Side.BLACK) {
                    requestEngineMove()
                }
            }
        }

        if (gameMode == GameMode.DAILY_PUZZLE) {
            viewModelScope.launch {
                val result = puzzleController.loadDailyPuzzle(gameManager.board)
                when (result) {
                    is com.example.proyectoajedrez.chess.puzzle.PuzzleLoadResult.Success -> {
                        effectivePlayerSide = result.playerSide
                        _uiState.value = _uiState.value.copy(
                            title = "Puzzle Diario (${result.rating})",
                            isFlipped = result.playerSide == Side.BLACK
                        )
                        updateBoardDisplay()
                    }
                    is com.example.proyectoajedrez.chess.puzzle.PuzzleLoadResult.Error -> { }
                }
            }
        }

        updateBoardDisplay()
        _uiState.value = _uiState.value.copy(
            title = title,
            isTimerVisible = !timerController.isUnlimited
        )
    }

    private suspend fun replayOpeningMoves(movesString: String) {
        if (movesString.isBlank()) return
        val sanMoves = movesString
            .replace(Regex("\\d+\\."), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        for (san in sanMoves) {
            delay(700L)
            val move = ChessUtils.sanToMove(san, gameManager.board) ?: continue
            gameManager.executeMove(move)
            updateBoardDisplay()
        }
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
        if (gameManager.isInReviewMode) return
        if (_uiState.value.isEngineThinking) return
        if (gameMode == GameMode.APERTURA) return

        if ((gameMode == GameMode.LIBRE || gameMode == GameMode.DAILY_PUZZLE)
            && gameManager.board.sideToMove != effectivePlayerSide) return

        val logicalIdx = convertVisualToLogical(visualPosition)

        if (selectedSquareIdx == null) {
            selectSquare(logicalIdx)
        } else {
            if (logicalIdx == selectedSquareIdx) {
                deselectSquare()
            } else {
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
                if (gameMode == GameMode.DAILY_PUZZLE) {
                    val move = gameManager.board.backup.last.move
                    val puzzleResult = puzzleController.validateMove(move)

                    when (puzzleResult) {
                        PuzzleMoveResult.Incorrect -> {
                            gameManager.board.undoMove()
                            deselectSquare()
                            updateBoardDisplay()
                            viewModelScope.launch { _uiEvents.emit(ChessUiEvent.IncorrectPuzzleMove) }
                            return
                        }

                        is PuzzleMoveResult.CorrectContinue -> {
                            moveHistoryList.add(moveResult.historyEntry)
                            playMoveSound(moveResult.wasCapture)
                            deselectSquare()
                            updateBoardDisplay()
                            startTimer(gameManager.board.sideToMove)
                            viewModelScope.launch {
                                delay(600)
                                // ──────────────────────────────────────────────────
                                // FIX BUG 1: La solución de Lichess usa formato UCI
                                // (e.g. "g1f3"), NO SAN.
                                // ChessUtils.sanToMove() espera SAN → siempre retorna
                                // null con UCI → el rival nunca movía → puzzle bloqueado.
                                //
                                // Solución: buscar el movimiento legal cuya
                                // representación UCI coincida directamente.
                                // ──────────────────────────────────────────────────
                                val uciMove = puzzleResult.nextEngineMove
                                val legalMoves = gameManager.board.legalMoves()
                                val engineMoveObj = legalMoves.firstOrNull {
                                    it.toString().lowercase() == uciMove.lowercase()
                                }
                                if (engineMoveObj != null) {
                                    gameManager.executeMove(engineMoveObj)
                                    puzzleController.consumeEngineMove()
                                    updateBoardDisplay()
                                }
                            }
                            return
                        }

                        PuzzleMoveResult.Solved -> {
                            // ──────────────────────────────────────────────────────
                            // FIX BUG 3: Antes se llamaba updateBoardDisplay() antes
                            // de setear PUZZLE_SOLVED. Si el tablero estaba en jaque
                            // mate, updateBoardDisplay() emitía WHITE_WINS/BLACK_WINS,
                            // generando dos estados no-PLAYING → dos popups.
                            //
                            // Solución: una sola emisión de estado con PUZZLE_SOLVED
                            // directamente, sin pasar por updateBoardDisplay().
                            // ──────────────────────────────────────────────────────
                            moveHistoryList.add(moveResult.historyEntry)
                            playMoveSound(moveResult.wasCapture)
                            selectedSquareIdx = null   // Limpiar selección sin emitir
                            val solvedPieces = Array(64) { idx -> boardPieceAt(idx) }
                            _uiState.value = _uiState.value.copy(
                                pieces = solvedPieces,
                                moveHistory = moveHistoryList.toList(),
                                selectedSquare = null,
                                legalMoveSquares = emptyList(),
                                currentTurn = gameManager.board.sideToMove,
                                gameStatus = GameStatus.PUZZLE_SOLVED,
                                isInReviewMode = gameManager.isInReviewMode
                            )
                            viewModelScope.launch {
                                puzzleController.saveProgress()
                                _uiEvents.emit(ChessUiEvent.PuzzleSolved)
                            }
                            return
                        }
                    }
                }

                // ── Modo no-puzzle (LIBRE / LOCAL_2P / APERTURA) ──
                moveHistoryList.add(moveResult.historyEntry)
                playMoveSound(moveResult.wasCapture)
                deselectSquare()
                updateBoardDisplay()
                startTimer(gameManager.board.sideToMove)

                if (gameMode == GameMode.LOCAL_2P) {
                    _uiState.value = _uiState.value.copy(
                        isFlipped = gameManager.board.sideToMove == Side.BLACK
                    )
                }

                if (gameMode == GameMode.LIBRE && gameManager.board.sideToMove != effectivePlayerSide) {
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
                val move = legalMoves.firstOrNull {
                    it.toString().lowercase() == uciMove.lowercase()
                }
                if (move != null) {
                    val result = gameManager.executeMove(move)
                    if (result is MoveResult.Success) {
                        moveHistoryList.add(result.historyEntry)
                        playMoveSound(result.wasCapture)
                        updateBoardDisplay()
                        startTimer(gameManager.board.sideToMove)
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun onTimeExpired(side: Side) {
        _uiState.value = _uiState.value.copy(gameStatus = GameStatus.DRAW_REPETITION)
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
        val newStatus = when {
            gameManager.board.isMated -> {
                if (gameManager.board.sideToMove == playerSide) GameStatus.BLACK_WINS
                else GameStatus.WHITE_WINS
            }
            gameManager.board.isStaleMate -> GameStatus.DRAW_STALEMATE
            gameManager.board.isDraw -> GameStatus.DRAW_REPETITION
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
        return if (_uiState.value.isFlipped) 63 - visual else visual
    }

    private fun playMoveSound(wasCapture: Boolean) { }
}

class ChessBoardViewModelFactory(
    private val context: Context,
    private val gameMode: GameMode,
    private val playerSide: Side,
    private val difficulty: Int,
    private val titleArg: String,
    private val openingMoves: String = ""
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChessBoardViewModel(
            gameMode = gameMode,
            playerSide = playerSide,
            difficulty = difficulty,
            title = titleArg,
            context = context,
            openingMoves = openingMoves
        ) as T
    }
}