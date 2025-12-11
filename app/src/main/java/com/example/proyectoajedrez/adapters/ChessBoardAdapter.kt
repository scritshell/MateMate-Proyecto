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

// Adaptador para mostrar tablero de ajedrez en GridView
class ChessBoardAdapter(private val context: Context) : BaseAdapter() {

    // Array de 64 casillas (8x8) que representa el tablero
    private val squares = Array(64) {
        ChessSquare(col = it % 8, row = it / 8)
    }

    // Posición actualmente seleccionada (-1 = ninguna)
    private var selectedPosition: Int = -1

    // Lista de posiciones donde es legal mover la pieza seleccionada
    private var legalMovePositions: List<Int> = emptyList()

    init {
        setupInitialPosition() // Configurar posición inicial del tablero
    }

    override fun getCount(): Int = 64 // 64 casillas en tablero 8x8
    override fun getItem(position: Int): ChessSquare = squares[position]
    override fun getItemId(position: Int): Long = position.toLong()

    // Crear/reciclar vista para cada casilla del tablero
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // 1. CÁLCULO DE TAMAÑO DE CELDA
        // Divide el ancho del GridView entre 8 para tamaño uniforme
        val boardWidth = parent?.width ?: 0
        val cellSize = if (boardWidth > 0) boardWidth / 8 else 120 // Valor por defecto

        // 2. CREAR O REUTILIZAR CONTENEDOR
        val container = (convertView as? FrameLayout) ?: FrameLayout(context).apply {
            // LayoutParams específicos para GridView (AbsListView)
            layoutParams = android.widget.AbsListView.LayoutParams(cellSize, cellSize)
        }

        // Actualizar tamaño si cambió (rotación de pantalla)
        if (container.layoutParams.height != cellSize && cellSize > 0) {
            container.layoutParams = android.widget.AbsListView.LayoutParams(cellSize, cellSize)
        }

        container.removeAllViews() // Limpiar vistas anteriores

        val square = squares[position]

        // 3. FONDO DE CASILLA (colores del tablero)
        val bgView = View(context)
        if (position == selectedPosition) {
            bgView.setBackgroundColor(Color.parseColor("#829769")) // Verde para selección
        } else {
            bgView.setBackgroundColor(
                if (square.isLightSquare) Color.parseColor("#EEEED2") // Clara
                else Color.parseColor("#769656") // Oscura
            )
        }
        container.addView(bgView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // 4. PIEZA DE AJEDREZ (ImageView)
        val pieceView = ImageView(context)
        pieceView.scaleType = ImageView.ScaleType.FIT_CENTER
        pieceView.setPadding(8, 8, 8, 8)

        if (square.piece != ChessPiece.EMPTY) {
            pieceView.setImageResource(square.piece.drawableRes) // Asignar imagen de pieza
        }
        container.addView(pieceView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // 5. INDICADORES DE MOVIMIENTO LEGAL
        if (legalMovePositions.contains(position)) {
            val indicator = View(context)

            // Tamaño proporcional: pequeño para casilla vacía, grande para captura
            val indicatorSize = if (square.piece == ChessPiece.EMPTY)
                (cellSize * 0.3).toInt() else cellSize

            val params = FrameLayout.LayoutParams(indicatorSize, indicatorSize)
            params.gravity = android.view.Gravity.CENTER

            val indicatorDrawable = android.graphics.drawable.GradientDrawable()
            indicatorDrawable.shape = android.graphics.drawable.GradientDrawable.OVAL

            if (square.piece == ChessPiece.EMPTY) {
                // Punto gris para movimiento a casilla vacía
                indicatorDrawable.setColor(Color.parseColor("#80666666"))
            } else {
                // Círculo rojo para captura de pieza
                indicatorDrawable.setColor(Color.TRANSPARENT)
                indicatorDrawable.setStroke(10, Color.parseColor("#80FF0000"))
            }

            indicator.background = indicatorDrawable
            indicator.layoutParams = params
            container.addView(indicator)
        }

        return container
    }

    // Actualizar lista de movimientos legales y refrescar vista
    fun setLegalMoves(positions: List<Int>) {
        this.legalMovePositions = positions
        notifyDataSetChanged()
    }

    // Limpiar indicadores de movimiento legal
    fun clearLegalMoves() {
        this.legalMovePositions = emptyList()
        notifyDataSetChanged()
    }

    // Marcar posición como seleccionada
    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    // Obtener posición actualmente seleccionada
    fun getSelectedPosition(): Int = selectedPosition

    // Mover pieza de una posición a otra
    fun movePiece(fromPosition: Int, toPosition: Int) {
        val pieceToMove = squares[fromPosition].piece
        squares[toPosition].piece = pieceToMove
        squares[fromPosition].piece = ChessPiece.EMPTY

        // Limpiar selección y movimientos legales después del movimiento
        selectedPosition = -1
        clearLegalMoves()

        notifyDataSetChanged()
    }

    // Reiniciar tablero a posición inicial
    fun resetBoard() {
        squares.forEach { it.piece = ChessPiece.EMPTY }
        setupInitialPosition()
        selectedPosition = -1
        clearLegalMoves()
        notifyDataSetChanged()
    }

    // Configurar la posición inicial estándar del ajedrez
    private fun setupInitialPosition() {
        // NEGRAS (fila 0 y 1)
        squares[0].piece = ChessPiece.BLACK_ROOK
        squares[1].piece = ChessPiece.BLACK_KNIGHT
        squares[2].piece = ChessPiece.BLACK_BISHOP
        squares[3].piece = ChessPiece.BLACK_QUEEN
        squares[4].piece = ChessPiece.BLACK_KING
        squares[5].piece = ChessPiece.BLACK_BISHOP
        squares[6].piece = ChessPiece.BLACK_KNIGHT
        squares[7].piece = ChessPiece.BLACK_ROOK
        for (i in 8..15) squares[i].piece = ChessPiece.BLACK_PAWN

        // BLANCAS (fila 6 y 7)
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