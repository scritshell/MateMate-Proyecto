package com.example.proyectoajedrez.adapters

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.example.proyectoajedrez.model.ChessPiece
import com.example.proyectoajedrez.models.ChessSquare

class ChessBoardAdapter(private val context: Context) : BaseAdapter() {

    private val squares = Array(64) {
        ChessSquare(col = it % 8, row = it / 8)
    }

    init {
        setupInitialPosition()
    }

    override fun getCount(): Int = 64

    override fun getItem(position: Int): ChessSquare = squares[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val squareView = (convertView as? ImageView) ?: ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val square = squares[position]

        // Configurar color de fondo
        squareView.setBackgroundColor(
            if (square.isLightSquare)
                Color.parseColor("#F0D9B5")  // Casilla clara
            else
                Color.parseColor("#B58863")   // Casilla oscura
        )

        // TODO: Configurar imagen de la pieza cuando tengamos los drawables
        // Por ahora podemos usar el símbolo como contenido descriptivo
        squareView.contentDescription = "${square.position} - ${square.piece.pieceType}"

        return squareView
    }

    fun resetBoard() {
        // Limpiar todas las piezas
        squares.forEach { it.piece = ChessPiece.EMPTY }
        setupInitialPosition()
        notifyDataSetChanged()
    }

    private fun setupInitialPosition() {
        // Posición inicial del ajedrez
        for (col in 0 until 8) {
            // Peones negros (fila 1)
            squares[1 * 8 + col].piece = ChessPiece.BLACK_PAWN
            // Peones blancos (fila 6)
            squares[6 * 8 + col].piece = ChessPiece.WHITE_PAWN
        }

        // Torres
        squares[0 * 8 + 0].piece = ChessPiece.BLACK_ROOK  // a8
        squares[0 * 8 + 7].piece = ChessPiece.BLACK_ROOK  // h8
        squares[7 * 8 + 0].piece = ChessPiece.WHITE_ROOK  // a1
        squares[7 * 8 + 7].piece = ChessPiece.WHITE_ROOK  // h1

        // Caballos
        squares[0 * 8 + 1].piece = ChessPiece.BLACK_KNIGHT  // b8
        squares[0 * 8 + 6].piece = ChessPiece.BLACK_KNIGHT  // g8
        squares[7 * 8 + 1].piece = ChessPiece.WHITE_KNIGHT  // b1
        squares[7 * 8 + 6].piece = ChessPiece.WHITE_KNIGHT  // g1

        // Alfiles
        squares[0 * 8 + 2].piece = ChessPiece.BLACK_BISHOP  // c8
        squares[0 * 8 + 5].piece = ChessPiece.BLACK_BISHOP  // f8
        squares[7 * 8 + 2].piece = ChessPiece.WHITE_BISHOP  // c1
        squares[7 * 8 + 5].piece = ChessPiece.WHITE_BISHOP  // f1

        // Reinas y Reyes
        squares[0 * 8 + 3].piece = ChessPiece.BLACK_QUEEN   // d8
        squares[0 * 8 + 4].piece = ChessPiece.BLACK_KING    // e8
        squares[7 * 8 + 3].piece = ChessPiece.WHITE_QUEEN   // d1
        squares[7 * 8 + 4].piece = ChessPiece.WHITE_KING    // e1
    }

    fun getSquareAt(row: Int, col: Int): ChessSquare? {
        return squares.getOrNull(row * 8 + col)
    }

    fun updateSquare(row: Int, col: Int, piece: ChessPiece) {
        getSquareAt(row, col)?.let { square ->
            square.piece = piece
            notifyDataSetChanged()
        }
    }

    fun getSquareByPosition(position: String): ChessSquare? {
        return squares.find { it.position == position }
    }
}