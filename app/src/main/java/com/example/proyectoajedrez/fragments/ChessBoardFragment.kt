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

    // EL CEREBRO DE LA PARTIDA (Librería chesslib)
    private val chessBoard = Board()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentChessBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Configurar Tablero Visual
        boardAdapter = ChessBoardAdapter(requireContext())
        binding.chessBoard.adapter = boardAdapter

        // 2. Configurar Historial (Lista de jugadas)
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
                // PRIMER CLIC: Seleccionar pieza
                // Solo dejamos seleccionar si hay pieza y es del turno correcto
                val piece = boardAdapter.getItem(position).piece
                if (piece != ChessPiece.EMPTY) {
                    // (Opcional: Verificar si es turno blancas/negras usando chessBoard.sideToMove)
                    boardAdapter.setSelectedPosition(position)
                }
            } else {
                // SEGUNDO CLIC: Intentar mover
                if (selectedPos == position) {
                    boardAdapter.setSelectedPosition(-1) // Deseleccionar
                } else {
                    intentarMover(selectedPos, position)
                }
            }
        }
    }

    private fun intentarMover(fromPos: Int, toPos: Int) {
        try {
            // 1. Convertir índices (0..63) a Casillas de la librería (Square.A1...)
            val fromSquare = getSquareFromIndex(fromPos)
            val toSquare = getSquareFromIndex(toPos)

            // 2. Crear el movimiento
            val move = Move(fromSquare, toSquare)

            // 3. Validar con la librería: ¿Es legal?
            if (chessBoard.isMoveLegal(move, true)) {

                // Generar notación ANTES de mover (para tener el contexto)
                // Nota: chesslib requiere configuración extra para SAN perfecto,
                // pero move.toString() nos da algo básico tipo "e2e4" por ahora.
                val moveText = "${chessBoard.moveCounter}. $move"

                // A. Mover en el CEREBRO (Lógica)
                chessBoard.doMove(move)

                // B. Mover en la VISTA (Visual)
                boardAdapter.movePiece(fromPos, toPos)
                boardAdapter.setSelectedPosition(-1)

                // C. Actualizar Historial
                historyAdapter.addMove(moveText)
                binding.recyclerHistory.scrollToPosition(historyAdapter.itemCount - 1)

            } else {
                Toast.makeText(context, "Movimiento ilegal", Toast.LENGTH_SHORT).show()
                boardAdapter.setSelectedPosition(-1)
            }

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Función auxiliar para mapear tus índices (0=A8) a la librería
    private fun getSquareFromIndex(index: Int): Square {
        // Tu tablero empieza en 0 (A8) y termina en 63 (H1)
        // chesslib usa Square.values() pero hay que asegurar el orden.
        // Fila 0 de tu Grid = Rank 8 del Ajedrez

        val col = index % 8
        val row = index / 8

        // Convertir a notación de archivo (File) y rango (Rank)
        val file = com.github.bhlangonijr.chesslib.File.values()[col] // File.FILE_A es 0
        val rank = com.github.bhlangonijr.chesslib.Rank.values()[7 - row] // Rank.RANK_8 es 7

        return Square.encode(rank, file)
    }

    private fun setupControls() {
        binding.btnReset.setOnClickListener {
            boardAdapter.resetBoard()
            historyAdapter.clear()
            chessBoard.loadFromFen(com.github.bhlangonijr.chesslib.Constants.startStandardFENPosition)
            Toast.makeText(context, "Partida Reiniciada", Toast.LENGTH_SHORT).show()
        }
        // ... otros botones ...
    }
}