package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.adapters.ChessBoardAdapter
import com.example.proyectoajedrez.adapters.MovesAdapter
import com.example.proyectoajedrez.databinding.FragmentChessBoardBinding
import com.example.proyectoajedrez.model.ChessPiece
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

class ChessBoardFragment : Fragment() {

    private lateinit var binding: FragmentChessBoardBinding
    private lateinit var boardAdapter: ChessBoardAdapter
    private lateinit var historyAdapter: MovesAdapter
    private val chessBoard = Board() // La librería lógica

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChessBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        boardAdapter = ChessBoardAdapter(requireContext())
        binding.chessBoard.adapter = boardAdapter

        historyAdapter = MovesAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(context)
        binding.recyclerHistory.adapter = historyAdapter

        setupBoardInteraction()
        setupControls()
    }

    private fun setupBoardInteraction() {
        binding.chessBoard.setOnItemClickListener { _, _, position, _ ->

            val selectedPos = boardAdapter.getSelectedPosition()

            if (selectedPos == -1) {
                // --- PRIMER CLIC: SELECCIÓN ---
                val piece = boardAdapter.getItem(position).piece

                // 1. Validar que no sea una casilla vacía
                if (piece == ChessPiece.EMPTY) return@setOnItemClickListener

                // 2. VALIDACIÓN DE TURNO (¡NUEVO!)
                // Preguntamos a la librería de quién es el turno
                val sideToMove = chessBoard.sideToMove // Side.WHITE o Side.BLACK

                // Comprobamos si la pieza que toco es de mi color
                val isMyTurn = if (sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE) {
                    piece.isWhite
                } else {
                    piece.isBlack
                }

                if (isMyTurn) {
                    // Es mi pieza y es mi turno -> Selecciono y muestro ayudas
                    boardAdapter.setSelectedPosition(position)
                    mostrarMovimientosPosibles(position)
                } else {
                    // No es mi turno -> Feedback visual (opcional)
                    Toast.makeText(context, "No es tu turno", Toast.LENGTH_SHORT).show()
                }

            } else {
                // --- SEGUNDO CLIC: MOVER O CANCELAR ---
                if (selectedPos == position) {
                    // Clic en la misma pieza -> Cancelar
                    boardAdapter.setSelectedPosition(-1)
                    boardAdapter.clearLegalMoves()
                } else {
                    // Clic en otra casilla -> Intentar Mover
                    // AQUÍ ES CLAVE: Solo intentamos mover si la casilla destino es válida
                    // (Ya lo valida intentarMover con chessBoard.isMoveLegal, pero no está de más)
                    intentarMover(selectedPos, position)
                }
            }
        }
    }

    // NUEVO: Función para calcular y dibujar los puntos
    private fun mostrarMovimientosPosibles(fromIndex: Int) {
        try {
            val fromSquare = getSquareFromIndex(fromIndex)
            // chesslib nos da TODOS los movimientos legales del tablero
            val legalMoves = chessBoard.legalMoves()

            // Filtramos solo los que salen de nuestra casilla seleccionada
            val validDestinations = legalMoves
                .filter { it.from == fromSquare }
                .map { getIndexFromSquare(it.to) } // Convertimos Square -> Int (0..63)

            // Pasamos la lista al adaptador para que pinte
            boardAdapter.setLegalMoves(validDestinations)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun intentarMover(fromPos: Int, toPos: Int) {
        try {
            val fromSquare = getSquareFromIndex(fromPos)
            val toSquare = getSquareFromIndex(toPos)
            val move = Move(fromSquare, toSquare)

            // Validamos legalidad estricta
            if (chessBoard.isMoveLegal(move, true)) {

                // --- 1. GENERAR NOTACIÓN (SOLUCIÓN SEGURA) ---
                // Usamos la notación estándar UCI (ej: "e2e4", "g1f3")
                // Es infalible y no da errores de compilación.
                val san = move.toString()

                // --- 2. DETECTAR TURNO ---
                val esTurnoBlancas = chessBoard.sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE
                val moveCounter = chessBoard.moveCounter

                // --- 3. MOVER EN EL CEREBRO ---
                chessBoard.doMove(move)

                // --- 4. MOVER EN LA VISTA ---
                boardAdapter.movePiece(fromPos, toPos)
                boardAdapter.setSelectedPosition(-1)
                boardAdapter.clearLegalMoves()

                // --- 5. ACTUALIZAR HISTORIAL ---
                if (esTurnoBlancas) {
                    // Turno Blancas: Nueva línea
                    historyAdapter.addMove("$moveCounter. $san")
                } else {
                    // Turno Negras: Completar línea anterior
                    val prevText = historyAdapter.getLastMove()
                    // Añadimos espacios para separar bien
                    historyAdapter.updateLastItem("$prevText      $san")
                }

                binding.recyclerHistory.scrollToPosition(historyAdapter.itemCount - 1)

            } else {
                // Movimiento ilegal
                boardAdapter.setSelectedPosition(-1)
                boardAdapter.clearLegalMoves()
            }
        } catch (e: Exception) {
            boardAdapter.setSelectedPosition(-1)
            boardAdapter.clearLegalMoves()
            e.printStackTrace()
        }
    }

    // --- CONVERSORES DE COORDENADAS ---

    // De Índice (0..63) a Square de Chesslib
    private fun getSquareFromIndex(index: Int): Square {
        val col = index % 8 // Columna 0..7 (A..H)
        val row = index / 8 // Fila visual 0..7

        // Fila 0 visual es Rank 8 lógico (Piezas Negras)
        // Fila 7 visual es Rank 1 lógico (Piezas Blancas)
        val rankIndex = 7 - row

        val file = com.github.bhlangonijr.chesslib.File.values()[col]
        val rank = com.github.bhlangonijr.chesslib.Rank.values()[rankIndex]

        return Square.encode(rank, file)
    }

    // De Square de Chesslib a Índice (0..63)
    private fun getIndexFromSquare(square: Square): Int {
        val col = square.file.ordinal // File A = 0
        val row = 7 - square.rank.ordinal // Rank 8 = 7 (porque index 0 es arriba)
        return row * 8 + col
    }

    private fun setupControls() {
        binding.btnReset.setOnClickListener {
            boardAdapter.resetBoard()
            historyAdapter.clear()
            chessBoard.loadFromFen(com.github.bhlangonijr.chesslib.Constants.startStandardFENPosition)
        }
        // ...
    }
}