package com.example.proyectoajedrez.fragments

import android.graphics.Color.parseColor
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.adapters.ChessBoardAdapter
import com.example.proyectoajedrez.adapters.MovesAdapter
import com.example.proyectoajedrez.databinding.FragmentChessBoardBinding
import com.example.proyectoajedrez.engine.StockfishClient
import com.example.proyectoajedrez.model.ChessPiece
import com.example.proyectoajedrez.network.LichessClient
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Constants
import com.github.bhlangonijr.chesslib.File
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ====================================================================== //
// ChessBoardFragment: TABLERO. Funcionará del estilo Camaleon.           //
// Dependiendo de si el usuario accede al tablero desde Puzzle diario,    //
// apertura o modo libre, el tablero tendrá que actuar de una forma       //
// u otra.                                                                //
// ====================================================================== //

class ChessBoardFragment : Fragment() {

    private var _binding: FragmentChessBoardBinding? = null
    private val binding get() = _binding!!

    // Adaptadores y Lógica
    private lateinit var boardAdapter: ChessBoardAdapter
    private lateinit var historyAdapter: MovesAdapter
    private val chessBoard = Board()
    private lateinit var stockfishClient: StockfishClient

    // Variables de Estado y Configuración
    private var timerWhite: android.os.CountDownTimer? = null
    private var timerBlack: android.os.CountDownTimer? = null
    private var timeLeftWhite: Long = 300000 // 5 min por defecto
    private var timeLeftBlack: Long = 300000
    private var initialTime: Long = 300000
    private var isTimeUnlimited = false

    private var modoJuego: String = "libre" // "libre", "local_2p", "apertura", "daily_puzzle"
    private var playerSide: Side = Side.WHITE // Bando del usuario (Vs IA o Puzzle)
    private var difficultyDepth = 1
    private var isEngineThinking = false

    // Puzzles diarios
    private var puzzleSolution: MutableList<String> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChessBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. RECOGER ARGUMENTOS
        modoJuego = arguments?.getString("modo") ?: "libre"
        val sideStr = arguments?.getString("side") ?: "WHITE"
        playerSide = if (sideStr == "BLACK") Side.BLACK else Side.WHITE

        val dialogDifficulty = arguments?.getInt("difficulty", -1) ?: -1
        if (dialogDifficulty != -1) difficultyDepth = dialogDifficulty

        val secuenciaApertura = arguments?.getString("secuenciaMovimientos") ?: ""
        val tituloApertura = arguments?.getString("titulo")

        // 2. CONFIGURAR UI
        setupUI()
        configurarInterfazSegunModo(tituloApertura)

        // Recoger tiempo del diálogo
        val timeIndex = arguments?.getInt("timeIndex", 3) ?: 3
        configurarTiempoInicial(timeIndex)

        // Iniciar reloj si es partida jugable (no puzzle ni apertura)
        if (modoJuego == "libre" || modoJuego == "local_2p") {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(500)
                iniciarReloj(true) // Empiezan blancas
            }
        }

        // 3. INICIALIZAR LÓGICA DE JUEGO
        // Si el usuario eligió Negras vs IA, giramos el tablero
        if (modoJuego == "libre" && playerSide == Side.BLACK) {
            boardAdapter.setFlipped(true)
        }

        // Inicializar Stockfish (Solo en modo libre)
        if (modoJuego == "libre") {
            inicializarStockfish()
            // Si el usuario juega con NEGRAS, la IA (Blancas) debe mover primero
            if (playerSide == Side.BLACK) {
                solicitarMovimientoIA()
            }
        }

        // Lógica específica de modos
        when (modoJuego) {
            "apertura" -> iniciarReproduccionApertura(secuenciaApertura)
            "daily_puzzle" -> cargarPuzzleDiarioLichess()
            else -> { /* Modo Libre */ }
        }
    }

    private fun setupUI() {
        boardAdapter = ChessBoardAdapter(requireContext())
        binding.chessBoard.adapter = boardAdapter

        historyAdapter = MovesAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(context)
        binding.recyclerHistory.adapter = historyAdapter

        setupControls()
        setupBoardInteraction()
    }

    private fun setupControls() {
        // Spinner solo visual para compatibilidad
        val tiempos = arrayOf("Rápido", "Normal", "Difícil")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiempos)
        binding.spinnerTime.adapter = adapter

        binding.btnExit.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnUndo.setOnClickListener {
            undoMove()
        }
        binding.btnRedo.isEnabled = false
        binding.btnRedo.alpha = 0.3f
    }

    private fun undoMove() {
        if (modoJuego == "apertura") return
        if (modoJuego == "daily_puzzle") {
            Toast.makeText(context, "No puedes deshacer en modo Puzzle", Toast.LENGTH_SHORT).show()
            return
        }

        if (modoJuego == "local_2p") {
            if (chessBoard.history.size > 0) {
                chessBoard.undoMove()
                boardAdapter.updateBoard(chessBoard)
                historyAdapter.removeLastItem()
                actualizarOrientacionTablero()
            }
        } else {
            // Vs IA
            if (chessBoard.history.size >= 2) {
                chessBoard.undoMove()
                chessBoard.undoMove()
                boardAdapter.updateBoard(chessBoard)
                historyAdapter.removeLastItem()
                historyAdapter.removeLastItem()
            } else if (chessBoard.history.size == 1) {
                chessBoard.undoMove()
                boardAdapter.updateBoard(chessBoard)
                historyAdapter.removeLastItem()
            }
        }
    }

    private fun configurarInterfazSegunModo(titulo: String?) {
        binding.layoutAjustesPartida.isVisible = false

        val tituloMostrar = titulo ?: when (modoJuego) {
            "local_2p" -> "Modo 2 Jugadores"
            "libre" -> if (playerSide == Side.WHITE) "Vs Stockfish (Blancas)" else "Vs Stockfish (Negras)"
            "apertura" -> "Apertura"
            "daily_puzzle" -> "Cargando Puzzle..."
            else -> "Ajedrez"
        }
        binding.titleTextView.text = tituloMostrar
    }

    // INTERACCIÓN
    private fun setupBoardInteraction() {
        binding.chessBoard.setOnItemClickListener { _, _, position, _ ->
            if (modoJuego == "apertura") return@setOnItemClickListener
            if (isEngineThinking) return@setOnItemClickListener

            val logicalPos = boardAdapter.getLogicalIndex(position)

            // Validación de turno
            if (modoJuego == "libre") {
                if (chessBoard.sideToMove != playerSide) return@setOnItemClickListener
            }
            if (modoJuego == "daily_puzzle") {
                if (chessBoard.sideToMove != playerSide) return@setOnItemClickListener
            }

            val selectedPos = boardAdapter.getSelectedPosition()

            if (selectedPos == -1) {
                val piece = boardAdapter.getItem(position).piece
                if (piece == ChessPiece.EMPTY) return@setOnItemClickListener

                val isWhiteTurn = chessBoard.sideToMove == Side.WHITE
                if ((isWhiteTurn && piece.isWhite) || (!isWhiteTurn && piece.isBlack)) {
                    boardAdapter.setSelectedPosition(logicalPos)
                    mostrarMovimientosPosibles(logicalPos)
                }
            } else {
                if (logicalPos == selectedPos) {
                    deseleccionar()
                } else {
                    val fromSquare = getSquareFromIndex(selectedPos)
                    val toSquare = getSquareFromIndex(logicalPos)
                    val move = Move(fromSquare, toSquare)

                    if (chessBoard.isMoveLegal(move, true)) {
                        if (modoJuego == "daily_puzzle") {
                            validarMovimientoTactica(move)
                        } else {
                            realizarMovimiento(move)
                        }
                    } else {
                        val piece = boardAdapter.getItem(position).piece
                        val isWhiteTurn = chessBoard.sideToMove == Side.WHITE
                        if ((isWhiteTurn && piece.isWhite) || (!isWhiteTurn && piece.isBlack)) {
                            boardAdapter.setSelectedPosition(logicalPos)
                            mostrarMovimientosPosibles(logicalPos)
                        } else {
                            deseleccionar()
                        }
                    }
                }
            }
        }
    }

    private fun realizarMovimiento(move: Move) {
        val fromPos = getIndexFromSquare(move.from)
        val toPos = getIndexFromSquare(move.to)
        val moveCounter = chessBoard.moveCounter
        val isWhite = chessBoard.sideToMove == Side.WHITE
        val san = move.toString()

        chessBoard.doMove(move)
        boardAdapter.movePiece(fromPos, toPos)
        deseleccionar()

        val nextTurnIsWhite = chessBoard.sideToMove == Side.WHITE
        iniciarReloj(nextTurnIsWhite)

        if (isWhite) {
            historyAdapter.addMove("$moveCounter. $san")
        } else {
            val prevText = historyAdapter.getLastMove()
            historyAdapter.updateLastItem("$prevText $san")
        }
        binding.recyclerHistory.scrollToPosition(historyAdapter.itemCount - 1)

        if (modoJuego == "local_2p") {
            actualizarOrientacionTablero()
        } else if (modoJuego == "libre") {
            if (chessBoard.sideToMove != playerSide) {
                solicitarMovimientoIA()
            }
        }
    }

    // --- PUZZLE LICHESS ---
    private fun validarMovimientoTactica(move: Move) {
        val moveString = move.toString().lowercase()

        if (puzzleSolution.isNotEmpty() && moveString == puzzleSolution[0]) {
            chessBoard.doMove(move)
            boardAdapter.movePiece(getIndexFromSquare(move.from), getIndexFromSquare(move.to))
            deseleccionar()
            puzzleSolution.removeAt(0)

            if (puzzleSolution.isEmpty()) {
                Toast.makeText(context, getString(R.string.msg_reto_completado), Toast.LENGTH_LONG).show()
                binding.chessBoard.isEnabled = false
            } else {
                realizarRespuestaPuzzle()
            }
        } else {
            boardAdapter.highlightError(move.from, move.to)
            deseleccionar()
            Toast.makeText(context, getString(R.string.msg_jugada_incorrecta), Toast.LENGTH_SHORT).show()
        }
    }

    private fun realizarRespuestaPuzzle() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.chessBoard.isEnabled = false
            delay(800)

            if (puzzleSolution.isNotEmpty()) {
                val nextMoveStr = puzzleSolution[0]
                try {
                    val fromSq = Square.valueOf(nextMoveStr.substring(0, 2).uppercase())
                    val toSq = Square.valueOf(nextMoveStr.substring(2, 4).uppercase())

                    val color = chessBoard.sideToMove
                    val promotionPiece = if (nextMoveStr.length == 5) {
                        when (nextMoveStr[4]) {
                            'q' -> if (color == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
                            'r' -> if (color == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
                            'b' -> if (color == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                            'n' -> if (color == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                            else -> Piece.NONE
                        }
                    } else {
                        Piece.NONE
                    }

                    val cpuMove = Move(fromSq, toSq, promotionPiece)
                    chessBoard.doMove(cpuMove)
                    boardAdapter.updateBoard(chessBoard)

                    puzzleSolution.removeAt(0)
                    binding.chessBoard.isEnabled = true

                } catch (e: Exception) {
                    Log.e("Puzzle", "Error CPU Move: ${e.message}")
                }
            }
        }
    }

    private fun cargarPuzzleDiarioLichess() {
        binding.chessBoard.isEnabled = false
        binding.layoutTimers.isVisible = false
        Toast.makeText(context, "Cargando Puzzle...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = LichessClient.instance.getDailyPuzzle()
                val rawPgn = response.game.pgn.trim() // "e4 c6 d4..."

                withContext(Dispatchers.Main) {
                    try {
                        // 1. Resetear tablero
                        chessBoard.loadFromFen(Constants.startStandardFENPosition)

                        // 2. Procesar lista de movimientos manualmente
                        if (rawPgn.isNotEmpty()) {
                            val movesList = rawPgn.split(" ")
                            Log.d("AJEDREZ_DEBUG", "Procesando ${movesList.size} movimientos...")

                            for (sanMove in movesList) {
                                if (sanMove.isBlank()) continue

                                // USAMOS LA NUEVA FUNCIÓN TRADUCTORA
                                val move = convertirSanAMove(sanMove, chessBoard)

                                if (move != null) {
                                    chessBoard.doMove(move)
                                } else {
                                    Log.e("AJEDREZ_DEBUG", "ERROR FATAL: No pude traducir '$sanMove' en turno ${chessBoard.sideToMove}")
                                    break // Paramos para no corromper más
                                }
                            }
                        }

                        // 3. Guardar solución y Configurar UI
                        puzzleSolution = response.puzzle.solution.toMutableList()
                        binding.titleTextView.text = "Puzzle Diario (Elo ${response.puzzle.rating})"

                        binding.chessBoard.post {
                            boardAdapter.updateBoard(chessBoard)
                        }

                        playerSide = chessBoard.sideToMove
                        boardAdapter.setFlipped(playerSide == Side.BLACK)
                        binding.chessBoard.isEnabled = true

                        val bando = if (playerSide == Side.WHITE) "Blancas" else "Negras"
                        Toast.makeText(context, "¡Juegan $bando!", Toast.LENGTH_LONG).show()

                    } catch (e: Exception) {
                        Log.e("AJEDREZ_DEBUG", "Error procesando movimientos", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("AJEDREZ_DEBUG", "Error de red", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
/*
* TODO: Quitar los Ajedrez_DEBUG
* */

    // UTILIDADES
    private fun actualizarOrientacionTablero() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(300)
            val tocaNegras = chessBoard.sideToMove == Side.BLACK
            boardAdapter.setFlipped(tocaNegras)
        }
    }

    private fun inicializarStockfish() {
        stockfishClient = StockfishClient(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            stockfishClient.inicializar()
            stockfishClient.readOutput { linea ->
                if (linea.startsWith("bestmove")) {
                    val parts = linea.split(" ")
                    if (parts.size > 1) {
                        procesarMovimientoIA(parts[1])
                    }
                }
            }
        }
    }

    private fun solicitarMovimientoIA() {
        if (isEngineThinking) return
        isEngineThinking = true
        lifecycleScope.launch(Dispatchers.IO) {
            val delayMillis = (1000..2500).random().toLong()
            delay(delayMillis)
            val currentFen = chessBoard.fen
            stockfishClient.sendCommand("position fen $currentFen")
            stockfishClient.sendCommand("go depth $difficultyDepth")
        }
    }

    private fun procesarMovimientoIA(moveString: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            isEngineThinking = false
            try {
                val fromString = moveString.substring(0, 2).uppercase()
                val toString = moveString.substring(2, 4).uppercase()
                val promotion = if (moveString.length == 5) {
                    if (chessBoard.sideToMove == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
                } else Piece.NONE

                val move = Move(Square.valueOf(fromString), Square.valueOf(toString), promotion)
                realizarMovimiento(move)
            } catch (e: Exception) {
                Log.e("Stockfish", "Error procesando movimiento IA: $moveString", e)
            }
        }
    }

    private fun iniciarReproduccionApertura(secuencia: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.chessBoard.isEnabled = false
            binding.layoutTimers.isVisible = false
            val movimientosLimpios = secuencia.replace(Regex("\\d+\\."), "").replace("\n", " ").split(" ").filter { it.isNotBlank() }
            delay(1000)
            for (movTxt in movimientosLimpios) {
                try {
                    val move = convertirNotacionAMove(movTxt)
                    if (move != null) {
                        chessBoard.doMove(move)
                        boardAdapter.updateBoard(chessBoard)
                        val san = move.toString()
                        val isWhiteMoved = chessBoard.sideToMove == Side.BLACK
                        if (isWhiteMoved) historyAdapter.addMove(movTxt)
                        else historyAdapter.updateLastItem("${historyAdapter.getLastMove()} $movTxt")
                        binding.recyclerHistory.scrollToPosition(historyAdapter.itemCount - 1)
                        delay(1200)
                    }
                } catch (e: Exception) { Log.e("Apertura", "Error: $movTxt", e) }
            }
            binding.chessBoard.isEnabled = true
        }
    }

    private fun deseleccionar() {
        boardAdapter.setSelectedPosition(-1)
        boardAdapter.clearLegalMoves()
    }

    private fun mostrarMovimientosPosibles(logicalIndex: Int) {
        val fromSquare = getSquareFromIndex(logicalIndex)
        val legalMoves = chessBoard.legalMoves()
        val validDestinations = legalMoves
            .filter { it.from == fromSquare }
            .map { getIndexFromSquare(it.to) }
        boardAdapter.setLegalMoves(validDestinations)
    }

    private fun getSquareFromIndex(index: Int): Square {
        val col = index % 8
        val row = index / 8
        return Square.encode(Rank.values()[7 - row], File.values()[col])
    }

    private fun getIndexFromSquare(square: Square): Int {
        return (7 - square.rank.ordinal) * 8 + square.file.ordinal
    }

    private fun convertirNotacionAMove(san: String): Move? {
        val legalMoves = chessBoard.legalMoves()
        var sanCorregido = san.replace("C", "N").replace("A", "B").replace("T", "R").replace("D", "Q").replace("R", "K")
        for (move in legalMoves) {
            if (move.toString() == sanCorregido || move.san == sanCorregido) return move
            if (sanCorregido.length == 2 && move.to.toString().lowercase() == sanCorregido.lowercase()) {
                if (chessBoard.getPiece(move.from).pieceType.name == "PAWN") return move
            }
        }
        return null
    }

    // TEMPORIZADORES

    private fun configurarTiempoInicial(index: Int) {
        val minutos = when(index) {
            1 -> 1; 2 -> 3; 3 -> 5; 4 -> 10; 5 -> 30; else -> -1
        }

        if (minutos == -1) {
            isTimeUnlimited = true
            binding.layoutTimers.isVisible = false
        } else {
            isTimeUnlimited = false
            binding.layoutTimers.isVisible = true
            initialTime = minutos * 60 * 1000L
            timeLeftWhite = initialTime
            timeLeftBlack = initialTime
            actualizarTextoReloj(true, timeLeftWhite)
            actualizarTextoReloj(false, timeLeftBlack)
        }
    }

    private fun iniciarReloj(isWhiteTurn: Boolean) {
        if (isTimeUnlimited) return
        pausarRelojes()

        if (isWhiteTurn) {
            highlightTimer(true)
            timerWhite = object : CountDownTimer(timeLeftWhite, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeftWhite = millisUntilFinished
                    actualizarTextoReloj(true, millisUntilFinished)
                }
                override fun onFinish() { finalizarPartidaPorTiempo(false) }
            }.start()
        } else {
            highlightTimer(false)
            timerBlack = object : CountDownTimer(timeLeftBlack, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeftBlack = millisUntilFinished
                    actualizarTextoReloj(false, millisUntilFinished)
                }
                override fun onFinish() { finalizarPartidaPorTiempo(true) }
            }.start()
        }
    }

    private fun pausarRelojes() {
        timerWhite?.cancel()
        timerBlack?.cancel()
    }

    private fun actualizarTextoReloj(isWhite: Boolean, millis: Long) {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)
        if (isWhite) binding.tvTimerWhite.text = timeStr
        else binding.tvTimerBlack.text = timeStr
    }

    private fun highlightTimer(isWhiteActive: Boolean) {
        val activeColor = parseColor("#81C784")
        val inactiveColor = parseColor("#E0E0E0")
        if (isWhiteActive) {
            binding.tvTimerWhite.setBackgroundColor(activeColor)
            binding.tvTimerBlack.setBackgroundColor(inactiveColor)
        } else {
            binding.tvTimerWhite.setBackgroundColor(inactiveColor)
            binding.tvTimerBlack.setBackgroundColor(activeColor)
        }
    }

    private fun finalizarPartidaPorTiempo(gananBlancas: Boolean) {
        binding.chessBoard.isEnabled = false
        pausarRelojes()
        val ganador = if (gananBlancas) "BLANCAS" else "NEGRAS"
        Toast.makeText(context, "¡TIEMPO AGOTADO! Ganan $ganador", Toast.LENGTH_LONG).show()
    }

    /**
     * Busca en los movimientos legales del tablero cuál coincide con la notación SAN (ej: "Nc3", "O-O")
     */
    private fun convertirSanAMove(san: String, board: Board): Move? {
        val legalMoves = board.legalMoves()

        // 1. Limpieza: quitar jaques (+), mates (#) y capturas (x)
        // Ej: "Nxe4+" -> "Ne4"
        var text = san.replace("+", "").replace("#", "").replace("x", "")

        // 2. Enroques
        if (text == "O-O") {
            return legalMoves.find {
                it.from.file == File.FILE_E && it.to.file == File.FILE_G
            }
        }
        if (text == "O-O-O") {
            return legalMoves.find {
                it.from.file == File.FILE_E && it.to.file == File.FILE_C
            }
        }

        // 3. Manejo de Promoción (ej: a8=Q)
        var promotionPiece = Piece.NONE
        if (text.contains("=")) {
            val parts = text.split("=")
            text = parts[0] // Nos quedamos con la casilla "a8"
            val promoChar = parts[1].uppercase()
            promotionPiece = when(promoChar) {
                "Q" -> if(board.sideToMove == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
                "R" -> if(board.sideToMove == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
                "B" -> if(board.sideToMove == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                "N" -> if(board.sideToMove == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                else -> Piece.NONE
            }
        }

        // 4. Identificar Pieza y Destino
        // "Nc3" -> Pieza: 'N' (Caballo), Destino: "c3"
        // "e4"  -> Pieza: Peón (implícito), Destino: "e4"

        val destStr = text.takeLast(2) // Últimos 2 caracteres son la casilla destino
        val pieceChar = if (text.length > 2 && text.first().isUpperCase()) text.first() else 'P'

        // Filtramos: Qué movimientos van a esa casilla
        val candidates = legalMoves.filter { move ->
            move.to.toString().equals(destStr, ignoreCase = true)
        }

        // Filtramos: Qué pieza se está moviendo
        val candidatesByPiece = candidates.filter { move ->
            val piece = board.getPiece(move.from)
            when (pieceChar) {
                'N' -> piece.pieceType == PieceType.KNIGHT
                'B' -> piece.pieceType == PieceType.BISHOP
                'R' -> piece.pieceType == PieceType.ROOK
                'Q' -> piece.pieceType == PieceType.QUEEN
                'K' -> piece.pieceType == PieceType.KING
                'P' -> piece.pieceType == PieceType.PAWN
                else -> false
            }
        }

        // 5. Si hay 1 candidato, es ese.
        if (candidatesByPiece.size == 1) {
            val m = candidatesByPiece[0]
            // Reconstruir con promoción si hace falta
            return if (promotionPiece != Piece.NONE) Move(m.from, m.to, promotionPiece) else m
        }

        // 6. Desambiguación (Si hay 2 caballos que pueden ir a c3: "Nbd7")
        if (candidatesByPiece.size > 1) {
            // El caracter de desambiguación está entre la pieza y el destino
            // Ej: "Nbd7" -> 'b' está en text[1]
            // Ej: "N4d7" -> '4' está en text[1]
            for (move in candidatesByPiece) {
                val fromSq = move.from.toString().lowercase() // "b8"
                // Buscamos si "b8" contiene la pista que hay en el texto SAN
                val disambiguator = text.substring(
                    if (pieceChar == 'P') 0 else 1,
                    text.length - 2
                )
                if (fromSq.contains(disambiguator)) {
                    return if (promotionPiece != Piece.NONE) Move(move.from, move.to, promotionPiece) else move
                }
            }
        }

        return candidatesByPiece.firstOrNull()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pausarRelojes()
        if (::stockfishClient.isInitialized) stockfishClient.close()
        _binding = null
    }
}

/*
* TODO: MEJORAR LA LEGIBILIDAD DE ESTE CODIGO. Ideas:
*  Crear una clase enum para el GameMode (libre, local_2p, apertura, daily_puzzle)
*  Crear una clase enum para los colores (blancas, negras)
*  comprimir un poco más el codigo (demasiados tabuladores y espacios innecesarios)
*  Pedir sugerencias extras a la IA.
*  Reducir o juntar explicaciones comentadas
*
* Esta clase ha superado las 700 lineas totales de código, es demasiado incluso si el ChessBoardFragment
* se encarga de la parte más pesada, esto se puede simplificar un poco más.
* */

