package com.example.proyectoajedrez.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.adapters.ChessBoardAdapter
import com.example.proyectoajedrez.adapters.ExplorerAdapter
import com.example.proyectoajedrez.adapters.MovesAdapter
import com.example.proyectoajedrez.chess.ChessBoardViewModel
import com.example.proyectoajedrez.chess.ChessBoardViewModelFactory
import com.example.proyectoajedrez.chess.ChessGameEvent
import com.example.proyectoajedrez.chess.GameStatus
import com.example.proyectoajedrez.databinding.FragmentChessBoardBinding
import com.example.proyectoajedrez.model.GameMode
import com.example.proyectoajedrez.model.toGameMode
import com.example.proyectoajedrez.network.ExplorerClient
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ChessBoardFragment - UI Layer (PHASE 5 REFACTORED)
 * Displays chess board and collects user input, delegating all logic to ChessBoardViewModel.
 * Responsibilities: Board rendering, user input collection, reactive UI updates from ViewModel state.
 */
class ChessBoardFragment : Fragment() {

    private var _binding: FragmentChessBoardBinding? = null
    private val binding get() = _binding!!

    // ViewModel with dependency injection
    private val viewModel: ChessBoardViewModel by viewModels {
        val gameMode = (arguments?.getString("modo") ?: "libre").toGameMode()
        val playerSide = if (arguments?.getString("side") == "BLACK") Side.BLACK else Side.WHITE
        val difficulty = arguments?.getInt("difficulty", 1) ?: 1
        val title = arguments?.getString("titulo") ?: "Tablero"
        
        ChessBoardViewModelFactory(
            requireContext(),
            gameMode,
            playerSide,
            difficulty,
            title
        )
    }

    // UI Adapters
    private lateinit var boardAdapter: ChessBoardAdapter
    private val historyAdapter = MovesAdapter()
    private val explorerAdapter = ExplorerAdapter()

    // Sensors
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private var shakeListener: SensorEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChessBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupShakeSensor()
        observeViewModel()
    }

    /**
     * Initialize UI components: adapters, button listeners, board interaction
     */
    private fun setupUI() {
        // Setup adapters
        boardAdapter = ChessBoardAdapter(requireContext())
        binding.chessBoard.adapter = boardAdapter

        binding.recyclerHistory.layoutManager = LinearLayoutManager(context)
        binding.recyclerHistory.adapter = historyAdapter

        binding.recyclerExplorer?.layoutManager = LinearLayoutManager(context)
        binding.recyclerExplorer?.adapter = explorerAdapter

        // Button listeners - dispatch events to ViewModel
        binding.btnExit.setOnClickListener {
            viewModel.onEvent(ChessGameEvent.ExitRequested)
            findNavController().popBackStack()
        }

        binding.btnUndo.setOnClickListener {
            viewModel.onEvent(ChessGameEvent.UndoRequested)
        }

        binding.btnRedo?.setOnClickListener {
            viewModel.onEvent(ChessGameEvent.RedoRequested)
        }

        binding.btnTabHistory?.setOnClickListener { switchTab(showHistory = true) }
        binding.btnTabExplorer?.setOnClickListener { switchTab(showHistory = false) }

        // Board interaction
        setupBoardInteraction()
    }

    /**
     * Setup board click listener - dispatches square taps to ViewModel
     */
    private fun setupBoardInteraction() {
        binding.chessBoard.setOnItemClickListener { _, _, position, _ ->
            val visualPosition = boardAdapter.getLogicalIndex(position)
            viewModel.onEvent(ChessGameEvent.SquareTapped(visualPosition))
        }
    }

    /**
     * Observe ViewModel state and update UI reactively
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update board pieces
                boardAdapter.updateBoard(state.pieces)

                // Update selection and legal moves highlight
                if (state.selectedSquare != null) {
                    boardAdapter.setSelectedPosition(state.selectedSquare)
                    boardAdapter.setLegalMoves(state.legalMoveSquares)
                } else {
                    boardAdapter.setSelectedPosition(-1)
                    boardAdapter.clearLegalMoves()
                }

                // Update move history
                historyAdapter.submitList(state.moveHistory)
                if (state.moveHistory.isNotEmpty()) {
                    binding.recyclerHistory.scrollToPosition(state.moveHistory.size - 1)
                }

                // Update timers
                binding.tvTimerWhite.text = state.timerWhiteText
                binding.tvTimerBlack.text = state.timerBlackText
                highlightActiveTimer(state.activeTimer)

                // Update title
                binding.titleTextView.text = state.title

                // Show/hide timers based on game mode
                binding.layoutTimers.isVisible = state.isTimerVisible

                // Handle game end
                if (state.gameStatus != GameStatus.PLAYING) {
                    handleGameEnd(state.gameStatus)
                }

                // Disable board while engine thinks
                binding.chessBoard.isEnabled = !state.isEngineThinking
            }
        }
    }

    /**
     * Display game end dialog and disable board
     */
    private fun handleGameEnd(status: GameStatus) {
        val mensaje = when (status) {
            GameStatus.WHITE_WINS -> "¡Victoria! Ganan las Blancas"
            GameStatus.BLACK_WINS -> "¡Victoria! Ganan las Negras"
            GameStatus.DRAW_STALEMATE -> "Empate por ahogado"
            GameStatus.DRAW_REPETITION -> "Empate por repetición"
            GameStatus.PUZZLE_SOLVED -> getString(R.string.msg_reto_completado)
            GameStatus.PUZZLE_FAILED -> "Puzzle fallido"
            else -> "Partida finalizada"
        }

        binding.chessBoard.isEnabled = false
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_partida_finalizada))
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .setCancelable(false)
            .show()
    }

    /**
     * Switch between history and opening explorer tabs
     */
    private fun switchTab(showHistory: Boolean) {
        binding.recyclerHistory.isVisible = showHistory
        binding.recyclerExplorer?.isVisible = !showHistory
        if (!showHistory && explorerAdapter.itemCount == 0) binding.tvExplorerStatus?.isVisible = true
        else binding.tvExplorerStatus?.isVisible = false

        val activeColor = parseColor("#FFD700")
        val inactiveColor = parseColor("#E0E0E0")
        binding.btnTabHistory?.backgroundTintList = android.content.res.ColorStateList.valueOf(if (showHistory) activeColor else inactiveColor)
        binding.btnTabExplorer?.backgroundTintList = android.content.res.ColorStateList.valueOf(if (!showHistory) activeColor else inactiveColor)

        if (!showHistory) fetchOpeningData()
    }

    /**
     * Fetch opening moves from Lichess Explorer API
     */
    private fun fetchOpeningData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" // Placeholder
                val response = ExplorerClient.instance.getOpeningMoves(fen)
                withContext(Dispatchers.Main) {
                    explorerAdapter.submitList(response.moves)
                    updateExplorerStatus(response.moves.isEmpty(), response.opening)
                }
            } catch (e: Exception) { }
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

    /**
     * Highlight which timer is active (white or black)
     */
    private fun highlightActiveTimer(side: Side) {
        val active = parseColor("#81C784")
        val inactive = parseColor("#E0E0E0")
        binding.tvTimerWhite.setBackgroundColor(if (side == Side.WHITE) active else inactive)
        binding.tvTimerBlack.setBackgroundColor(if (side == Side.BLACK) active else inactive)
    }

    /**
     * Setup accelerometer for shake gesture undo
     */
    private fun setupShakeSensor() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        shakeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val aceleracion = Math.sqrt((x*x + y*y + z*z).toDouble()) - SensorManager.GRAVITY_EARTH
                val ahora = System.currentTimeMillis()
                if (aceleracion > 12f && ahora - lastShakeTime > 1000) {
                    lastShakeTime = ahora
                    requireActivity().runOnUiThread {
                        viewModel.onEvent(ChessGameEvent.UndoRequested)
                        Toast.makeText(requireContext(), getString(R.string.msg_jugada_deshecha), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        shakeListener?.let {
            sensorManager.registerListener(it, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun parseColor(hex: String) = Color.parseColor(hex)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        shakeListener?.let { sensorManager.unregisterListener(it) }
    }
}
