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

    private var selectedPosition: Int = -1

    init {
        setupInitialPosition()
    }

    override fun getCount(): Int = 64
    override fun getItem(position: Int): ChessSquare = squares[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Usamos ImageView en lugar de TextView
        val squareView = (convertView as? ImageView) ?: ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                120 // Altura fija aproximada
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(8, 8, 8, 8) // Un poco de margen para que la pieza no toque los bordes
        }

        val square = squares[position]

        // 1. Configurar color de fondo (Tablero + Selección)
        if (position == selectedPosition) {
            squareView.setBackgroundColor(Color.parseColor("#829769")) // Verde selección (tipo Chess.com)
        } else {
            squareView.setBackgroundColor(
                if (square.isLightSquare) Color.parseColor("#EEEED2")  // Casilla clara
                else Color.parseColor("#769656")   // Casilla oscura
            )
        }

        // 2. Pintar la pieza (Imagen)
        if (square.piece != ChessPiece.EMPTY) {
            squareView.setImageResource(square.piece.drawableRes)
            squareView.alpha = 1.0f
        } else {
            squareView.setImageDrawable(null) // Limpiar imagen si está vacía
        }

        return squareView
    }

    // --- Lógica del Juego ---

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun movePiece(fromPosition: Int, toPosition: Int) {
        val pieceToMove = squares[fromPosition].piece
        squares[toPosition].piece = pieceToMove
        squares[fromPosition].piece = ChessPiece.EMPTY
        notifyDataSetChanged()
    }

    fun resetBoard() {
        squares.forEach { it.piece = ChessPiece.EMPTY }
        setupInitialPosition()
        selectedPosition = -1
        notifyDataSetChanged()
    }

    private fun setupInitialPosition() {
        // Configuración estándar del tablero (Fila 0 = Arriba/Negras)

        // NEGRAS
        squares[0].piece = ChessPiece.BLACK_ROOK
        squares[1].piece = ChessPiece.BLACK_KNIGHT
        squares[2].piece = ChessPiece.BLACK_BISHOP
        squares[3].piece = ChessPiece.BLACK_QUEEN
        squares[4].piece = ChessPiece.BLACK_KING
        squares[5].piece = ChessPiece.BLACK_BISHOP
        squares[6].piece = ChessPiece.BLACK_KNIGHT
        squares[7].piece = ChessPiece.BLACK_ROOK
        for (i in 8..15) squares[i].piece = ChessPiece.BLACK_PAWN

        // BLANCAS
        for (i in 48..55) squares[i].piece = ChessPiece.WHITE_PAWN
        squares[56].piece = ChessPiece.WHITE_ROOK
        squares[57].piece = ChessPiece.WHITE_KNIGHT
        squares[58].piece = ChessPiece.WHITE_BISHOP
        squares[59].piece = ChessPiece.WHITE_QUEEN
        squares[60].piece = ChessPiece.WHITE_KING
        squares[61].piece = ChessPiece.WHITE_BISHOP
        squares[62].piece = ChessPiece.WHITE_KNIGHT
        squares[63].piece = ChessPiece.WHITE_ROOK
    }
}