package com.example.proyectoajedrez.fragments

import android.content.Context
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
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
import com.example.proyectoajedrez.chess.ChessUiEvent
import com.example.proyectoajedrez.chess.GameStatus
import com.example.proyectoajedrez.databinding.FragmentChessBoardBinding
import com.example.proyectoajedrez.model.GameMode
import com.example.proyectoajedrez.model.toGameMode
import com.example.proyectoajedrez.network.ExplorerClient
import com.github.bhlangonijr.chesslib.Side
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChessBoardFragment : Fragment() {

    private var _binding: FragmentChessBoardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChessBoardViewModel by viewModels {
        val gameMode = (arguments?.getString("modo") ?: "libre").toGameMode()
        val playerSide = if (arguments?.getString("side") == "BLACK") Side.BLACK else Side.WHITE
        val difficulty = arguments?.getInt("difficulty", 1) ?: 1
        val title = arguments?.getString("titulo") ?: getString(R.string.titulo_tablero)
        val openingMoves = arguments?.getString("secuenciaMovimientos") ?: ""

        ChessBoardViewModelFactory(
            requireContext(),
            gameMode,
            playerSide,
            difficulty,
            title,
            openingMoves
        )
    }

    private lateinit var boardAdapter: ChessBoardAdapter
    private val historyAdapter = MovesAdapter()
    private val explorerAdapter = ExplorerAdapter()

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

    private fun setupUI() {
        boardAdapter = ChessBoardAdapter(requireContext())
        binding.chessBoard.adapter = boardAdapter

        binding.recyclerHistory.layoutManager = LinearLayoutManager(context)
        binding.recyclerHistory.adapter = historyAdapter

        binding.recyclerExplorer?.layoutManager = LinearLayoutManager(context)
        binding.recyclerExplorer?.adapter = explorerAdapter

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

        setupBoardInteraction()
    }

    /**
     * FIX 5: pasar la posición visual cruda sin conversión previa
     */
    private fun setupBoardInteraction() {
        binding.chessBoard.setOnItemClickListener { _, _, position, _ ->
            // Antes: val visualPosition = boardAdapter.getLogicalIndex(position)
            // Ahora: enviamos la posición visual directamente
            viewModel.onEvent(ChessGameEvent.SquareTapped(position))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Actualizar piezas
                boardAdapter.updateBoard(state.pieces)

                // FIX 6: aplicar orientación del tablero al adapter
                boardAdapter.setFlipped(state.isFlipped)

                // Selección y movimientos legales
                if (state.selectedSquare != null) {
                    boardAdapter.setSelectedPosition(state.selectedSquare)
                    boardAdapter.setLegalMoves(state.legalMoveSquares)
                } else {
                    boardAdapter.setSelectedPosition(-1)
                    boardAdapter.clearLegalMoves()
                }

                // Historial
                historyAdapter.submitList(state.moveHistory)
                if (state.moveHistory.isNotEmpty()) {
                    binding.recyclerHistory.scrollToPosition(state.moveHistory.size - 1)
                }

                // Temporizadores
                binding.tvTimerWhite.text = state.timerWhiteText
                binding.tvTimerBlack.text = state.timerBlackText
                highlightActiveTimer(state.activeTimer)

                binding.titleTextView.text = state.title
                binding.layoutTimers.isVisible = state.isTimerVisible

                if (state.gameStatus != GameStatus.PLAYING) {
                    handleGameEnd(state.gameStatus)
                }

                binding.chessBoard.isEnabled = !state.isEngineThinking
            }
        }

        lifecycleScope.launch {
            viewModel.uiEvents.collect { event ->
                when (event) {
                    ChessUiEvent.IncorrectPuzzleMove -> {
                        Toast.makeText(requireContext(), getString(R.string.msg_jugada_incorrecta), Toast.LENGTH_SHORT).show()
                    }
                    ChessUiEvent.PuzzleSolved -> {
                        Toast.makeText(requireContext(), getString(R.string.msg_reto_completado), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun handleGameEnd(status: GameStatus) {
        val mensaje = when (status) {
            GameStatus.WHITE_WINS -> getString(R.string.game_status_white_wins)
            GameStatus.BLACK_WINS -> getString(R.string.game_status_black_wins)
            GameStatus.DRAW_STALEMATE -> getString(R.string.game_status_draw_stalemate)
            GameStatus.DRAW_REPETITION -> getString(R.string.game_status_draw_repetition)
            GameStatus.PUZZLE_SOLVED -> getString(R.string.msg_reto_completado)
            GameStatus.PUZZLE_FAILED -> getString(R.string.game_status_puzzle_failed)
            else -> getString(R.string.game_status_finished)
        }

        binding.chessBoard.isEnabled = false
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_partida_finalizada))
            .setMessage(mensaje)
            .setPositiveButton(getString(R.string.dialog_btn_aceptar), null)
            .setCancelable(false)
            .show()
    }

    private fun switchTab(showHistory: Boolean) {
        binding.recyclerHistory.isVisible = showHistory
        binding.recyclerExplorer?.isVisible = !showHistory
        if (!showHistory && explorerAdapter.itemCount == 0) binding.tvExplorerStatus?.isVisible = true
        else binding.tvExplorerStatus?.isVisible = false

        val activeColor = ContextCompat.getColor(requireContext(), R.color.tab_active)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.tab_inactive)
        binding.btnTabHistory?.backgroundTintList = ColorStateList.valueOf(if (showHistory) activeColor else inactiveColor)
        binding.btnTabExplorer?.backgroundTintList = ColorStateList.valueOf(if (!showHistory) activeColor else inactiveColor)

        if (!showHistory) fetchOpeningData()
    }

    private fun fetchOpeningData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
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
                binding.tvExplorerStatus?.text = getString(R.string.explorer_book_end, opening.name, opening.eco)
                binding.tvExplorerStatus?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
            } else {
                binding.tvExplorerStatus?.text = getString(R.string.explorer_label_empty)
            }
        }
    }

    private fun highlightActiveTimer(side: Side) {
        val active = ContextCompat.getColor(requireContext(), R.color.timer_active)
        val inactive = ContextCompat.getColor(requireContext(), R.color.timer_inactive)
        binding.tvTimerWhite.setBackgroundColor(if (side == Side.WHITE) active else inactive)
        binding.tvTimerBlack.setBackgroundColor(if (side == Side.BLACK) active else inactive)
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        shakeListener?.let { sensorManager.unregisterListener(it) }
    }
}