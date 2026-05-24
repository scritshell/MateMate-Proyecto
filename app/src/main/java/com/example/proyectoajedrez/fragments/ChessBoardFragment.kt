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
import com.example.proyectoajedrez.adapters.MovesAdapter
import com.example.proyectoajedrez.chess.ChessBoardViewModel
import com.example.proyectoajedrez.chess.ChessBoardViewModelFactory
import com.example.proyectoajedrez.chess.ChessGameEvent
import com.example.proyectoajedrez.chess.ChessUiEvent
import com.example.proyectoajedrez.chess.GameStatus
import com.example.proyectoajedrez.databinding.FragmentChessBoardBinding
import com.example.proyectoajedrez.model.GameMode
import com.example.proyectoajedrez.model.toGameMode
import com.github.bhlangonijr.chesslib.Side
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChessBoardFragment : Fragment() {

    private var _binding: FragmentChessBoardBinding? = null
    private val binding get() = _binding!!

    // ──────────────────────────────────────────────────────────────────────────
    // FIX BUG 2: Guardia para que el popup de fin de partida se muestre
    // UNA SOLA VEZ, sin importar cuántos estados emita el StateFlow después.
    // El StateFlow emite ante cualquier cambio de campo (timer, piezas, etc.),
    // y sin esta bandera cada emisión con gameStatus != PLAYING abría un diálogo.
    // ──────────────────────────────────────────────────────────────────────────
    private var gameEndHandled = false

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

        setupBoardInteraction()
    }

    private fun setupBoardInteraction() {
        binding.chessBoard.setOnItemClickListener { _, _, position, _ ->
            viewModel.onEvent(ChessGameEvent.SquareTapped(position))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Actualizar piezas
                boardAdapter.updateBoard(state.pieces)

                // Aplicar orientación del tablero al adapter
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

                // ──────────────────────────────────────────────────────────
                // FIX BUG 2: Solo procesar el fin de partida una vez.
                //
                // PUZZLE_SOLVED se gestiona enteramente a través del evento
                // ChessUiEvent.PuzzleSolved (Toast + navegación automática),
                // por lo que aquí se excluye para evitar doble notificación.
                // ──────────────────────────────────────────────────────────
                if (state.gameStatus != GameStatus.PLAYING
                    && state.gameStatus != GameStatus.PUZZLE_SOLVED
                    && !gameEndHandled) {
                    gameEndHandled = true
                    handleGameEnd(state.gameStatus)
                }

                binding.chessBoard.isEnabled = !state.isEngineThinking
            }
        }

        lifecycleScope.launch {
            viewModel.uiEvents.collect { event ->
                when (event) {
                    ChessUiEvent.IncorrectPuzzleMove -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_jugada_incorrecta),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    ChessUiEvent.PuzzleSolved -> {
                        // ──────────────────────────────────────────────────
                        // FIX BUG 1 (parte UI): Mostrar victoria y navegar
                        // de vuelta automáticamente tras 3 segundos.
                        // El puzzle queda bloqueado (chessBoard.isEnabled = false
                        // ya se aplica desde el estado PUZZLE_SOLVED).
                        // ──────────────────────────────────────────────────
                        binding.chessBoard.isEnabled = false
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_reto_completado),
                            Toast.LENGTH_LONG
                        ).show()
                        lifecycleScope.launch {
                            delay(3_000)
                            if (isAdded) findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    /**
     * Muestra el diálogo de fin de partida UNA SOLA VEZ y se auto-descarta
     * a los 3 segundos. Solo se invoca para modos no-puzzle (2P, Libre, Apertura).
     */
    private fun handleGameEnd(status: GameStatus) {
        val mensaje = when (status) {
            GameStatus.WHITE_WINS      -> getString(R.string.game_status_white_wins)
            GameStatus.BLACK_WINS      -> getString(R.string.game_status_black_wins)
            GameStatus.DRAW_STALEMATE  -> getString(R.string.game_status_draw_stalemate)
            GameStatus.DRAW_REPETITION -> getString(R.string.game_status_draw_repetition)
            GameStatus.PUZZLE_FAILED   -> getString(R.string.game_status_puzzle_failed)
            else                       -> getString(R.string.game_status_finished)
        }

        binding.chessBoard.isEnabled = false

        // ──────────────────────────────────────────────────────────────────
        // FIX BUG 2: El diálogo se puede cancelar (cancelable = true) y
        // se auto-descarta a los 3 segundos para no bloquear la UI.
        // La bandera gameEndHandled garantiza que este método solo se llama
        // una vez por partida, aunque el StateFlow emita más estados.
        // ──────────────────────────────────────────────────────────────────
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_partida_finalizada))
            .setMessage(mensaje)
            .setPositiveButton(getString(R.string.dialog_btn_aceptar), null)
            .setCancelable(true)
            .show()

        lifecycleScope.launch {
            delay(3_000)
            if (isAdded && dialog.isShowing) dialog.dismiss()
        }
    }

    private fun switchTab(showHistory: Boolean) {
        binding.recyclerHistory.isVisible = showHistory

        val activeColor   = ContextCompat.getColor(requireContext(), R.color.tab_active)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.tab_inactive)
        binding.btnTabHistory?.backgroundTintList  =
            ColorStateList.valueOf(if (showHistory) activeColor else inactiveColor)
    }

    private fun highlightActiveTimer(side: Side) {
        val active   = ContextCompat.getColor(requireContext(), R.color.timer_active)
        val inactive = ContextCompat.getColor(requireContext(), R.color.timer_inactive)
        binding.tvTimerWhite.setBackgroundColor(if (side == Side.WHITE) active else inactive)
        binding.tvTimerBlack.setBackgroundColor(if (side == Side.BLACK) active else inactive)
    }

    private fun setupShakeSensor() {
        sensorManager  = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        shakeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val aceleracion = Math.sqrt((x * x + y * y + z * z).toDouble()) -
                        SensorManager.GRAVITY_EARTH
                val ahora = System.currentTimeMillis()
                if (aceleracion > 12f && ahora - lastShakeTime > 1000) {
                    lastShakeTime = ahora
                    requireActivity().runOnUiThread {
                        viewModel.onEvent(ChessGameEvent.UndoRequested)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_jugada_deshecha),
                            Toast.LENGTH_SHORT
                        ).show()
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