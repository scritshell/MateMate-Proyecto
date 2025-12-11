package com.example.proyectoajedrez.adapters

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.model.ChessPiece
import com.example.proyectoajedrez.models.ChessSquare

class ChessBoardAdapter(private val context: Context) : BaseAdapter() {

    private val squares = Array(64) {
        ChessSquare(col = it % 8, row = it / 8)
    }

    private var selectedPosition: Int = -1

    // NUEVO: Lista de índices donde es legal moverse
    private var legalMovePositions: List<Int> = emptyList()

    init {
        setupInitialPosition()
    }

    override fun getCount(): Int = 64
    override fun getItem(position: Int): ChessSquare = squares[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Usamos un FrameLayout para poder apilar capas (Fondo + Pieza + Punto de ayuda)
        val container = (convertView as? FrameLayout) ?: FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120)
        }

        // Limpiamos vistas anteriores si se recicla
        container.removeAllViews()

        val square = squares[position]

        // 1. FONDO (Tablero)
        val bgView = View(context)
        if (position == selectedPosition) {
            bgView.setBackgroundColor(Color.parseColor("#829769")) // Verde Selección
        } else {
            bgView.setBackgroundColor(
                if (square.isLightSquare) Color.parseColor("#EEEED2")
                else Color.parseColor("#769656")
            )
        }
        container.addView(bgView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // 2. PIEZA (ImageView)
        val pieceView = ImageView(context)
        pieceView.scaleType = ImageView.ScaleType.FIT_CENTER
        pieceView.setPadding(8, 8, 8, 8)

        if (square.piece != ChessPiece.EMPTY) {
            pieceView.setImageResource(square.piece.drawableRes)
        }
        container.addView(pieceView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // 3. NUEVO: INDICADOR DE MOVIMIENTO LEGAL (Círculo)
        if (legalMovePositions.contains(position)) {
            val indicator = View(context)

            // Lógica visual:
            // - Si está vacío: Un punto pequeño gris.
            // - Si hay una pieza (Captura): Un marco o círculo rojo/transparente grande.

            val size = if (square.piece == ChessPiece.EMPTY) 40 else 100 // Tamaño
            val params = FrameLayout.LayoutParams(size, size)
            params.gravity = android.view.Gravity.CENTER

            val indicatorDrawable = android.graphics.drawable.GradientDrawable()
            indicatorDrawable.shape = android.graphics.drawable.GradientDrawable.OVAL

            if (square.piece == ChessPiece.EMPTY) {
                // Casilla vacía: Punto gris semitransparente
                indicatorDrawable.setColor(Color.parseColor("#80666666"))
            } else {
                // Captura: Anillo rojo semitransparente
                indicatorDrawable.setColor(Color.TRANSPARENT)
                indicatorDrawable.setStroke(10, Color.parseColor("#80FF0000"))
            }

            indicator.background = indicatorDrawable
            indicator.layoutParams = params
            container.addView(indicator)
        }

        return container
    }

    // NUEVO: Método para actualizar los movimientos legales desde el Fragment
    fun setLegalMoves(positions: List<Int>) {
        this.legalMovePositions = positions
        notifyDataSetChanged()
    }

    // Al mover o cancelar, limpiamos los puntos
    fun clearLegalMoves() {
        this.legalMovePositions = emptyList()
        notifyDataSetChanged()
    }

    // ... (El resto de métodos: setSelectedPosition, movePiece, resetBoard... se quedan igual) ...
    // Solo recuerda llamar a clearLegalMoves() dentro de movePiece y setSelectedPosition(-1)

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun movePiece(fromPosition: Int, toPosition: Int) {
        val pieceToMove = squares[fromPosition].piece
        squares[toPosition].piece = pieceToMove
        squares[fromPosition].piece = ChessPiece.EMPTY

        // Limpiamos selección y sugerencias tras mover
        selectedPosition = -1
        clearLegalMoves()

        notifyDataSetChanged()
    }

    fun resetBoard() {
        squares.forEach { it.piece = ChessPiece.EMPTY }
        setupInitialPosition()
        selectedPosition = -1
        clearLegalMoves()
        notifyDataSetChanged()
    }

    // ... (setupInitialPosition igual que antes) ...

    private fun setupInitialPosition() {
        // ... (Tu código existente de setupInitialPosition) ...
        // Configuración estándar del tablero (Fila 0 = Arriba/Negras)
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