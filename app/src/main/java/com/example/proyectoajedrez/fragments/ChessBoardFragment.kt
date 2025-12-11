package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.adapters.ChessBoardAdapter
import com.example.proyectoajedrez.adapters.MovesAdapter
import com.example.proyectoajedrez.databinding.FragmentChessBoardBinding
import com.example.proyectoajedrez.model.ChessPiece
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

// Fragmento principal para jugar ajedrez
class ChessBoardFragment : Fragment() {

    private lateinit var binding: FragmentChessBoardBinding
    private lateinit var boardAdapter: ChessBoardAdapter     // Adaptador para tablero gráfico
    private lateinit var historyAdapter: MovesAdapter        // Adaptador para historial de movimientos
    private val chessBoard = Board()                         // Tablero lógico de la librería chesslib

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChessBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Configurar tablero gráfico
        boardAdapter = ChessBoardAdapter(requireContext())
        binding.chessBoard.adapter = boardAdapter

        // 2. Configurar historial de movimientos
        historyAdapter = MovesAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(context)
        binding.recyclerHistory.adapter = historyAdapter

        // 3. Inicializar lógica de juego y controles
        setupBoardInteraction()
        setupControls()

        // Configurar spinner para seleccionar tiempo de la IA
        val tiempos = arrayOf("1 min", "5 min", "10 min", "30 min")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiempos)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTime.adapter = spinnerAdapter

        // Configurar seekbar para ajustar dificultad de la IA
        binding.seekBarDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvDifficultyLabel.text = "Dificultad IA: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // Configurar interacción con el tablero (clicks en casillas)
    private fun setupBoardInteraction() {
        binding.chessBoard.setOnItemClickListener { _, _, position, _ ->

            val selectedPos = boardAdapter.getSelectedPosition()

            if (selectedPos == -1) {
                // Primer click: seleccionar pieza
                val piece = boardAdapter.getItem(position).piece
                if (piece == ChessPiece.EMPTY) return@setOnItemClickListener

                val sideToMove = chessBoard.sideToMove
                val isMyTurn = if (sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE) {
                    piece.isWhite
                } else {
                    piece.isBlack
                }

                if (isMyTurn) {
                    boardAdapter.setSelectedPosition(position)
                    mostrarMovimientosPosibles(position)
                } else {
                    Toast.makeText(context, "No es tu turno", Toast.LENGTH_SHORT).show()
                }

            } else {
                // Segundo click: mover pieza o cancelar selección
                if (selectedPos == position) {
                    boardAdapter.setSelectedPosition(-1)
                    boardAdapter.clearLegalMoves()
                } else {
                    intentarMover(selectedPos, position)
                }
            }
        }
    }

    // Mostrar movimientos legales para la pieza seleccionada
    private fun mostrarMovimientosPosibles(fromIndex: Int) {
        try {
            val fromSquare = getSquareFromIndex(fromIndex)
            val legalMoves = chessBoard.legalMoves()
            val validDestinations = legalMoves
                .filter { it.from == fromSquare }
                .map { getIndexFromSquare(it.to) }
            boardAdapter.setLegalMoves(validDestinations)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Intentar realizar movimiento en el tablero lógico y gráfico
    private fun intentarMover(fromPos: Int, toPos: Int) {
        try {
            val fromSquare = getSquareFromIndex(fromPos)
            val toSquare = getSquareFromIndex(toPos)
            val move = Move(fromSquare, toSquare)

            if (chessBoard.isMoveLegal(move, true)) {
                val san = move.toString()
                val esTurnoBlancas = chessBoard.sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE
                val moveCounter = chessBoard.moveCounter

                chessBoard.doMove(move)

                boardAdapter.movePiece(fromPos, toPos)
                boardAdapter.setSelectedPosition(-1)
                boardAdapter.clearLegalMoves()

                // Actualizar historial de movimientos
                if (esTurnoBlancas) {
                    historyAdapter.addMove("$moveCounter. $san")
                } else {
                    val prevText = historyAdapter.getLastMove()
                    historyAdapter.updateLastItem("$prevText      $san")
                }
                binding.recyclerHistory.scrollToPosition(historyAdapter.itemCount - 1)

            } else {
                boardAdapter.setSelectedPosition(-1)
                boardAdapter.clearLegalMoves()
            }
        } catch (e: Exception) {
            boardAdapter.setSelectedPosition(-1)
            boardAdapter.clearLegalMoves()
            e.printStackTrace()
        }
    }

    // Convertir índice de GridView a Square de chesslib
    private fun getSquareFromIndex(index: Int): Square {
        val col = index % 8
        val row = index / 8
        val rankIndex = 7 - row
        val file = com.github.bhlangonijr.chesslib.File.values()[col]
        val rank = com.github.bhlangonijr.chesslib.Rank.values()[rankIndex]
        return Square.encode(rank, file)
    }

    // Convertir Square de chesslib a índice de GridView
    private fun getIndexFromSquare(square: Square): Int {
        val col = square.file.ordinal
        val row = 7 - square.rank.ordinal
        return row * 8 + col
    }

    // Configurar botones de control del tablero
    private fun setupControls() {
        binding.btnReset.setOnClickListener {
            boardAdapter.resetBoard()
            historyAdapter.clear()
            chessBoard.loadFromFen(com.github.bhlangonijr.chesslib.Constants.startStandardFENPosition)
        }
    }
}