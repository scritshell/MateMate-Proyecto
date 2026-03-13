package com.example.proyectoajedrez.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.adapters.ChessBoardAdapter
import com.example.proyectoajedrez.adapters.ExplorerAdapter
import com.example.proyectoajedrez.adapters.MovesAdapter
import com.example.proyectoajedrez.databinding.FragmentChessBoardBinding
import com.example.proyectoajedrez.engine.StockfishClient
import com.example.proyectoajedrez.model.ChessPiece
import com.example.proyectoajedrez.model.GameMode
import com.example.proyectoajedrez.model.toGameMode
import com.example.proyectoajedrez.network.ExplorerClient
import com.example.proyectoajedrez.network.LichessClient
import com.example.proyectoajedrez.utils.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Constants
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.proyectoajedrez.data.local.MateMateDataBase
import com.example.proyectoajedrez.data.local.PuzzleProgressEntity
import kotlinx.coroutines.flow.firstOrNull
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager


class ChessBoardFragment : Fragment() {

    private var _binding: FragmentChessBoardBinding? = null
    private val binding get() = _binding!!

    // Componentes Core
    private val chessBoard = Board()
    private lateinit var stockfishClient: StockfishClient
    private lateinit var boardAdapter: ChessBoardAdapter
    private val historyAdapter = MovesAdapter()
    private val explorerAdapter = ExplorerAdapter()

    // Configuración de Partida
    private var gameMode = GameMode.LIBRE
    private var playerSide = Side.WHITE
    private var difficultyDepth = 1

    // Estado del Juego
    private var isEngineThinking = false
    private var puzzleSolution: MutableList<String> = mutableListOf()

    // MÁQUINA DEL TIEMPO (Historial del futuro para el botón Rehacer)
    private val movimientosDeshechos = mutableListOf<Move>()

    // Temporizadores
    private var timerWhite: CountDownTimer? = null
    private var timerBlack: CountDownTimer? = null
    private var timeLeftWhite = 300_000L
    private var timeLeftBlack = 300_000L
    private var isTimeUnlimited = false

    // Sensores
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private var shakeListener: SensorEventListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChessBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        extractArguments()
        setupUI()
        setupTimers()
        initializeGameLogic()

        when (gameMode) {
            GameMode.APERTURA -> loadOpening(arguments?.getString("secuenciaMovimientos") ?: "")
            GameMode.DAILY_PUZZLE -> loadDailyPuzzle()
            else -> { }
        }
    }

    // --- SETUP & CONFIGURACIÓN ---

    private fun extractArguments() {
        arguments?.let { args ->
            gameMode = (args.getString("modo") ?: "libre").toGameMode()
            playerSide = if (args.getString("side") == "BLACK") Side.BLACK else Side.WHITE
            difficultyDepth = args.getInt("difficulty", 1).coerceAtLeast(1)

            val title = args.getString("titulo") ?: when(gameMode) {
                GameMode.LOCAL_2P -> "2 Jugadores"
                GameMode.DAILY_PUZZLE -> "Cargando..."
                else -> "Vs Stockfish (${if(playerSide == Side.WHITE) "Blancas" else "Negras"})"
            }
            binding.titleTextView.text = title
        }
    }

    private fun setupUI() {
        boardAdapter = ChessBoardAdapter(requireContext())
        binding.chessBoard.adapter = boardAdapter

        binding.recyclerHistory.layoutManager = LinearLayoutManager(context)
        binding.recyclerHistory.adapter = historyAdapter

        binding.recyclerExplorer?.layoutManager = LinearLayoutManager(context)
        binding.recyclerExplorer?.adapter = explorerAdapter

        binding.btnExit.setOnClickListener { findNavController().popBackStack() }

        // BOTONES DE LA MÁQUINA DEL TIEMPO
        binding.btnUndo.setOnClickListener { undoMove() }
        binding.btnRedo?.setOnClickListener { redoMove() }

        binding.btnTabHistory?.setOnClickListener { switchTab(showHistory = true) }
        binding.btnTabExplorer?.setOnClickListener { switchTab(showHistory = false) }

        setupBoardInteraction()

        if (playerSide == Side.BLACK && gameMode == GameMode.LIBRE) boardAdapter.setFlipped(true)
    }

    private fun initializeGameLogic() {
        if (gameMode == GameMode.LIBRE) {
            stockfishClient = StockfishClient(requireContext())
            lifecycleScope.launch {
                stockfishClient.inicializar()
                stockfishClient.readOutput { line ->
                    if (line.startsWith("bestmove")) {
                        line.split(" ").getOrNull(1)?.let { processEngineMove(it) }
                    }
                }
                if (playerSide == Side.BLACK) requestEngineMove()
            }
        }
    }

    // --- LÓGICA DEL JUEGO ---

    private fun setupBoardInteraction() {
        binding.chessBoard.setOnItemClickListener { _, _, position, _ ->

            // BLOQUEO MODO REVISIÓN: No deja tocar piezas en el pasado
            if (movimientosDeshechos.isNotEmpty()) {
                Toast.makeText(context, "Modo revisión: Ve al último movimiento (>) para seguir jugando", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            if (isEngineThinking || gameMode == GameMode.APERTURA) return@setOnItemClickListener
            if ((gameMode == GameMode.LIBRE || gameMode == GameMode.DAILY_PUZZLE) && chessBoard.sideToMove != playerSide) return@setOnItemClickListener

            val logicalPos = boardAdapter.getLogicalIndex(position)
            val selectedPos = boardAdapter.getSelectedPosition()

            if (selectedPos == -1) {
                val piece = boardAdapter.getItem(position).piece
                if (piece != ChessPiece.EMPTY) {
                    val isMyTurn = (chessBoard.sideToMove == Side.WHITE && piece.isWhite) || (chessBoard.sideToMove == Side.BLACK && piece.isBlack)
                    if (isMyTurn) selectPiece(logicalPos)
                }
            } else {
                if (logicalPos == selectedPos) deseleccionar()
                else attemptMove(selectedPos, logicalPos)
            }
        }
    }

    private fun attemptMove(fromIdx: Int, toIdx: Int) {
        val fromSq = getSquare(fromIdx)
        val toSq = getSquare(toIdx)
        val piece = chessBoard.getPiece(fromSq)

        // 1. CORONACIÓN AUTOMÁTICA
        var promo = Piece.NONE
        if (piece == Piece.WHITE_PAWN && toSq.rank == Rank.RANK_8) {
            promo = Piece.WHITE_QUEEN
        } else if (piece == Piece.BLACK_PAWN && toSq.rank == Rank.RANK_1) {
            promo = Piece.BLACK_QUEEN
        }

        val move = Move(fromSq, toSq, promo)

        // 2. COMPROBACIÓN ESTRICTA
        val isLegal = chessBoard.legalMoves().contains(move)

        if (isLegal) {
            if (gameMode == GameMode.DAILY_PUZZLE) validatePuzzleMove(move)
            else executeMove(move)
        } else {
            deseleccionar()
        }
    }

    private fun executeMove(move: Move) {
        val san = move.toString()
        val moveCount = chessBoard.moveCounter
        val isWhite = chessBoard.sideToMove == Side.WHITE

        // Ejecutamos la jugada
        chessBoard.doMove(move)

        // Limpiamos el futuro si hacemos un movimiento nuevo
        movimientosDeshechos.clear()

        // Repintamos el tablero
        boardAdapter.updateBoard(chessBoard)
        deseleccionar()

        if (isWhite) historyAdapter.addMove("$moveCount. $san")
        else historyAdapter.updateLastItem("${historyAdapter.getLastMove()} $san")
        binding.recyclerHistory.scrollToPosition(historyAdapter.itemCount - 1)

        startTimer(chessBoard.sideToMove == Side.WHITE)

        if (gameMode == GameMode.LOCAL_2P) rotateBoard()
        if (binding.recyclerExplorer?.isVisible == true) fetchOpeningData()

        checkGameOver()

        if (!chessBoard.isMated && !chessBoard.isDraw && !chessBoard.isStaleMate) {
            if (gameMode == GameMode.LIBRE && chessBoard.sideToMove != playerSide) requestEngineMove()
        }
    }

    private fun checkGameOver() {
        if (chessBoard.isMated) {
            // El sideToMove es el que ha recibido el mate (el que pierde)
            val turnoActual = chessBoard.sideToMove
            val ganador = if (turnoActual == Side.WHITE) "Negras" else "Blancas"

            val mensaje = when (gameMode) {
                GameMode.LOCAL_2P -> "Partida finalizada.\n¡Ganan las $ganador!"
                GameMode.LIBRE -> {
                    if (turnoActual == playerSide) {
                        "Derrota.\nStockfish te ha dado Jaque Mate"
                    } else {
                        "¡Victoria!\nHas derrotado a Stockfish"
                    }
                }
                else -> "¡Jaque Mate! Ganan las $ganador."
            }
            mostrarDialogoFin(mensaje)

        } else if (chessBoard.isStaleMate) {
            mostrarDialogoFin("Empate por ahogado.\nEl Rey no tiene movimientos legales.")
        } else if (chessBoard.isDraw) {
            mostrarDialogoFin("Empate.\n(Regla de los 50 movimientos o repetición).")
        }
    }

    private fun mostrarDialogoFin(mensaje: String) {
        binding.chessBoard.isEnabled = false
        timerWhite?.cancel()
        timerBlack?.cancel()

        AlertDialog.Builder(requireContext())
            .setTitle("Partida Finalizada")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .setCancelable(false)
            .show()
    }

    // --- MÓDULOS ESPECÍFICOS ---

    private fun fetchOpeningData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fen = chessBoard.fen
                val response = ExplorerClient.instance.getOpeningMoves(fen)
                withContext(Dispatchers.Main) {
                    explorerAdapter.submitList(response.moves)
                    updateExplorerStatus(response.moves.isEmpty(), response.opening)
                }
            } catch (e: Exception) { Log.e("Explorer", "Error API", e) }
        }
    }

    private fun updateExplorerStatus(isEmpty: Boolean, opening: com.example.proyectoajedrez.model.ExplorerOpening? = null) {
        binding.recyclerExplorer?.isVisible = !isEmpty
        binding.tvExplorerStatus?.isVisible = isEmpty

        if (isEmpty) {
            if (opening != null) {
                binding.tvExplorerStatus?.text = "Fin del libro.\n\n${opening.name}\n(${opening.eco})"
                binding.tvExplorerStatus?.setTextColor(Color.BLACK)
            } else {
                binding.tvExplorerStatus?.text = getString(R.string.explorer_label_empty)
            }
        }
    }

    private fun loadDailyPuzzle() {
        binding.chessBoard.isEnabled = false
        binding.layoutTimers.isVisible = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = LichessClient.instance.getDailyPuzzle()
                withContext(Dispatchers.Main) {
                    chessBoard.loadFromFen(Constants.startStandardFENPosition)

                    val cleanPgn = response.game.pgn
                        .replace(Regex("\\[.*?\\]"), " ")
                        .replace(Regex("\\{.*?\\}"), " ")
                        .replace(Regex("\\d+\\.+"), " ")
                        .replace("0-0-0", "O-O-O")
                        .replace("0-0", "O-O")
                        .trim()

                    cleanPgn.split(Regex("\\s+")).forEach { san ->
                        if (san.isNotBlank()) {
                            ChessUtils.sanToMove(san, chessBoard)?.let { chessBoard.doMove(it) }
                        }
                    }

                    puzzleSolution = response.puzzle.solution.toMutableList()
                    binding.titleTextView.text = "Puzzle Diario (${response.puzzle.rating})"
                    boardAdapter.updateBoard(chessBoard)

                    playerSide = chessBoard.sideToMove
                    boardAdapter.setFlipped(playerSide == Side.BLACK)
                    binding.chessBoard.isEnabled = true
                }
            } catch (e: Exception) { Log.e("Puzzle", "Error carga", e) }
        }
    }

    private fun validatePuzzleMove(move: Move) {
        if (puzzleSolution.isNotEmpty() && move.toString().lowercase() == puzzleSolution[0]) {
            executeMove(move)
            puzzleSolution.removeAt(0)

            if (puzzleSolution.isEmpty()) {
                actualizarProgresoPuzzle()
                mostrarDialogoFin(getString(R.string.msg_reto_completado))
            } else {
                lifecycleScope.launch {
                    delay(800)
                    processEngineMove(puzzleSolution[0])
                    puzzleSolution.removeAt(0)
                }
            }
        } else {

        }
    }

    private fun actualizarProgresoPuzzle() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = MateMateDataBase.getInstance(requireContext())
            val dao = db.puzzleProgressDao()

            // Leemos el progreso actual (si no hay, crea uno nuevo por defecto)
            val progressActual = dao.getProgress().firstOrNull() ?: PuzzleProgressEntity()

            val hoy = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                .format(java.util.Date())

            val nuevaRacha = when {
                progressActual.lastSolvedDate == hoy -> progressActual.currentStreak
                esAyer(progressActual.lastSolvedDate) -> progressActual.currentStreak + 1
                else -> 1 // Si falló un día, vuelve a 1
            }

            // Guardamos el nuevo progreso en la base de datos
            dao.saveProgress(
                progressActual.copy(
                    currentStreak = nuevaRacha,
                    totalSolved = progressActual.totalSolved + 1,
                    lastSolvedDate = hoy
                )
            )
        }
    }

    // Función auxiliar para saber si la última vez fue ayer
    private fun esAyer(fechaAntigua: String?): Boolean {
        if (fechaAntigua.isNullOrEmpty()) return false
        val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, -1)
        val ayer = sdf.format(cal.time)
        return fechaAntigua == ayer
    }

    private fun requestEngineMove() {
        isEngineThinking = true
        lifecycleScope.launch(Dispatchers.IO) {
            delay((500..1500).random().toLong())
            stockfishClient.sendCommand("position fen ${chessBoard.fen}")
            stockfishClient.sendCommand("go depth $difficultyDepth")
        }
    }

    private fun processEngineMove(moveStr: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            isEngineThinking = false
            try {
                val from = Square.valueOf(moveStr.take(2).uppercase())
                val to = Square.valueOf(moveStr.substring(2, 4).uppercase())
                val promo = if (moveStr.length == 5) {
                    if (chessBoard.sideToMove == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
                } else Piece.NONE

                executeMove(Move(from, to, promo))
            } catch (e: Exception) { Log.e("Stockfish", "Error move: $moveStr", e) }
        }
    }

    // --- MÁQUINA DEL TIEMPO ---

    private fun undoMove() {
        if (gameMode == GameMode.APERTURA) return

        if (chessBoard.backup.isNotEmpty()) {
            // 1. Guardamos el movimiento en el futuro
            val lastMove = chessBoard.backup.last.move
            movimientosDeshechos.add(lastMove)

            // 2. Deshacemos
            chessBoard.undoMove()

            // 3. Repintamos
            boardAdapter.updateBoard(chessBoard)
        }
    }

    private fun redoMove() {
        if (movimientosDeshechos.isNotEmpty()) {
            // 1. Recuperamos del futuro
            val move = movimientosDeshechos.removeAt(movimientosDeshechos.size - 1)

            // 2. Aplicamos
            chessBoard.doMove(move)

            // 3. Repintamos
            boardAdapter.updateBoard(chessBoard)
        }
    }

    // --- UI HELPERS ---

    private fun switchTab(showHistory: Boolean) {
        binding.recyclerHistory.isVisible = showHistory
        binding.recyclerExplorer?.isVisible = !showHistory
        if (!showHistory && explorerAdapter.itemCount == 0) binding.tvExplorerStatus?.isVisible = true
        else binding.tvExplorerStatus?.isVisible = false

        val activeColor = parseColor("#FFD700"); val inactiveColor = parseColor("#E0E0E0")
        binding.btnTabHistory?.backgroundTintList = android.content.res.ColorStateList.valueOf(if (showHistory) activeColor else inactiveColor)
        binding.btnTabExplorer?.backgroundTintList = android.content.res.ColorStateList.valueOf(if (!showHistory) activeColor else inactiveColor)

        if (!showHistory) fetchOpeningData()
    }

    private fun selectPiece(idx: Int) {
        boardAdapter.setSelectedPosition(idx)
        val moves = chessBoard.legalMoves().filter { it.from == getSquare(idx) }.map { getIndex(it.to) }
        boardAdapter.setLegalMoves(moves)
    }

    private fun deseleccionar() {
        boardAdapter.setSelectedPosition(-1)
        boardAdapter.clearLegalMoves()
    }

    private fun loadOpening(seq: String) {
        lifecycleScope.launch {
            binding.chessBoard.isEnabled = false
            seq.split(" ").filter { it.isNotBlank() }.forEach { san ->
                ChessUtils.sanToMove(san, chessBoard)?.let {
                    executeMove(it)
                    delay(800)
                }
            }
            binding.chessBoard.isEnabled = true
        }
    }

    private fun setupTimers() {
        val minutes = when(arguments?.getInt("timeIndex", 3)) {
            1 -> 1; 2 -> 3; 3 -> 5; 4 -> 10; 5 -> 30; else -> -1
        }
        isTimeUnlimited = (minutes == -1)
        binding.layoutTimers.isVisible = !isTimeUnlimited

        if (!isTimeUnlimited) {
            timeLeftWhite = minutes * 60000L
            timeLeftBlack = timeLeftWhite
            updateTimerText(true, timeLeftWhite)
            updateTimerText(false, timeLeftBlack)
        }
    }

    private fun startTimer(isWhite: Boolean) {
        if (isTimeUnlimited || gameMode == GameMode.APERTURA || gameMode == GameMode.DAILY_PUZZLE) return
        timerWhite?.cancel(); timerBlack?.cancel()

        val timer = object : CountDownTimer(if(isWhite) timeLeftWhite else timeLeftBlack, 1000) {
            override fun onTick(millis: Long) {
                if(isWhite) timeLeftWhite = millis else timeLeftBlack = millis
                updateTimerText(isWhite, millis)
            }
            override fun onFinish() { binding.chessBoard.isEnabled = false; mostrarDialogoFin("Tiempo Agotado") }
        }

        if(isWhite) timerWhite = timer else timerBlack = timer
        timer.start()
        highlightActiveTimer(isWhite)
    }

    private fun updateTimerText(isWhite: Boolean, millis: Long) {
        val text = String.format("%02d:%02d", (millis/1000)/60, (millis/1000)%60)
        if(isWhite) binding.tvTimerWhite.text = text else binding.tvTimerBlack.text = text
    }

    private fun highlightActiveTimer(isWhite: Boolean) {
        val active = parseColor("#81C784"); val inactive = parseColor("#E0E0E0")
        binding.tvTimerWhite.setBackgroundColor(if(isWhite) active else inactive)
        binding.tvTimerBlack.setBackgroundColor(if(!isWhite) active else inactive)
    }

    private fun getSquare(idx: Int) = Square.encode(Rank.values()[7 - idx / 8], com.github.bhlangonijr.chesslib.File.values()[idx % 8])
    private fun getIndex(sq: Square) = (7 - sq.rank.ordinal) * 8 + sq.file.ordinal
    private fun parseColor(hex: String) = Color.parseColor(hex)
    private fun rotateBoard() = lifecycleScope.launch { delay(300); boardAdapter.setFlipped(chessBoard.sideToMove == Side.BLACK) }

    private fun setupShakeSensor() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        shakeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Calcular la fuerza del movimiento
                val aceleracion = Math.sqrt((x*x + y*y + z*z).toDouble()) - SensorManager.GRAVITY_EARTH
                val ahora = System.currentTimeMillis()
                if (aceleracion > 12f && ahora - lastShakeTime > 1000) {
                    lastShakeTime = ahora
                    requireActivity().runOnUiThread {
                        undoMove()
                        Toast.makeText(requireContext(), "↩ Jugada deshecha por agitación", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        shakeListener?.let {
            sensorManager.registerListener(it, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shakeListener?.let { sensorManager.unregisterListener(it) }
        timerWhite?.cancel(); timerBlack?.cancel()
        if (gameMode == GameMode.LIBRE) stockfishClient.close()
        _binding = null
    }
}