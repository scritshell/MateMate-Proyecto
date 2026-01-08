package com.example.proyectoajedrez.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.proyectoajedrez.model.ChessPiece
import com.example.proyectoajedrez.model.ChessSquare
import com.github.bhlangonijr.chesslib.File
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import android.widget.AbsListView

class ChessBoardAdapter(private val context: Context) : BaseAdapter() {

    // Tablero lógico interno (Siempre de 0=A8 a 63=H1)
    private val squares = Array(64) {
        ChessSquare(col = it % 8, row = it / 8)
    }

    private var selectedPosition: Int = -1
    private var legalMovePositions: List<Int> = emptyList()
    private var errorPosition: Int = -1

    // Control de orientación
    private var isFlipped: Boolean = false // false = Blancas abajo, true = Negras abajo

    init {
        setupInitialPosition()
    }

    // Cambiar la orientación y refrescar
    fun setFlipped(flipped: Boolean) {
        if (this.isFlipped != flipped) {
            this.isFlipped = flipped
            notifyDataSetChanged()
        }
    }

    override fun getCount(): Int = 64

    // Obtiene el objeto visual basado en la posición del GridView
    override fun getItem(position: Int): ChessSquare {
        val logicalIndex = getLogicalIndex(position)
        return squares[logicalIndex]
    }
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // 1. Calcular tamaño: divide el ancho del GridView entre 8 casillas
        val boardWidth = parent?.width ?: 0
        val cellSize = if (boardWidth > 0) boardWidth / 8 else 120

        // Reutilizar vista
        val container = (convertView as? FrameLayout) ?: FrameLayout(context).apply {
            layoutParams = AbsListView.LayoutParams(cellSize, cellSize)
        }

        // Ajustar tamaño si hubo cambio, asegura que todas las celdas tengan el mismo tamaño
        if (container.layoutParams.height != cellSize && cellSize > 0) {
            container.layoutParams = AbsListView.LayoutParams(cellSize, cellSize)
        }
        // Limpiar vistas anteriores
        container.removeAllViews()



        // TRUCO MATEMÁTICO:
        // Si está flipped, la posición visual 0 corresponde a la lógica 63 (H1).
        val logicalIndex = getLogicalIndex(position)
        val square = squares[logicalIndex]

        // 1. FONDO
        val bgView = View(context)
        when {
            logicalIndex == errorPosition -> bgView.setBackgroundColor(Color.parseColor("#FF5252"))
            logicalIndex == selectedPosition -> bgView.setBackgroundColor(Color.parseColor("#829769"))
            else -> bgView.setBackgroundColor(
                if (square.isLightSquare) Color.parseColor("#EEEED2") else Color.parseColor("#769656")
            )
        }
        container.addView(bgView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // 2. PIEZA
        val pieceView = ImageView(context)
        pieceView.scaleType = ImageView.ScaleType.FIT_CENTER
        pieceView.setPadding(8, 8, 8, 8)
        if (square.piece != ChessPiece.EMPTY) {
            pieceView.setImageResource(square.piece.drawableRes)
        }
        container.addView(pieceView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // 3. INDICADORES
        if (legalMovePositions.contains(logicalIndex)) {
            val indicator = View(context)
            val indicatorSize = if (square.piece == ChessPiece.EMPTY) (cellSize * 0.3).toInt() else cellSize
            val params = FrameLayout.LayoutParams(indicatorSize, indicatorSize).apply {
                gravity = android.view.Gravity.CENTER
            }
            val indicatorDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (square.piece == ChessPiece.EMPTY) {
                    setColor(Color.parseColor("#80666666"))
                } else {
                    setColor(Color.TRANSPARENT)
                    setStroke(10, Color.parseColor("#80FF0000"))
                }
            }
            indicator.background = indicatorDrawable
            indicator.layoutParams = params
            container.addView(indicator)
        }

        return container
    }

    // FUNCIONES PARA LA LÓGICA INVERTIDA //

    // Convierte el índice visual (click en pantalla) a un índice lógico (array interno)
    fun getLogicalIndex(visualPosition: Int): Int {
        return if (isFlipped) 63 - visualPosition else visualPosition
    }

    // Convierte índice lógico a visual
    fun getVisualIndex(logicalIndex: Int): Int {
        return if (isFlipped) 63 - logicalIndex else logicalIndex
    }

    // Sincroniza tablero
    fun updateBoard(board: Board) {
        for (i in 0 until 64) {
            val squareLib = getSquareFromIndex(i)
            val pieceLib = board.getPiece(squareLib)
            squares[i].piece = mapLibPieceToLocal(pieceLib)
        }
        notifyDataSetChanged()
    }

    fun highlightError(fromSquare: Square, toSquare: Square) {
        errorPosition = getIndexFromSquare(toSquare)
        notifyDataSetChanged()
        Handler(Looper.getMainLooper()).postDelayed({
            errorPosition = -1
            notifyDataSetChanged()
        }, 500)
    }

    // SETTERS Y GETTERS //

    fun setLegalMoves(logicalPositions: List<Int>) {
        this.legalMovePositions = logicalPositions
        notifyDataSetChanged()
    }

    fun clearLegalMoves() {
        this.legalMovePositions = emptyList()
        notifyDataSetChanged()
    }

    fun setSelectedPosition(logicalPosition: Int) {
        selectedPosition = logicalPosition
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun movePiece(fromLogical: Int, toLogical: Int) {
        val pieceToMove = squares[fromLogical].piece
        squares[toLogical].piece = pieceToMove
        squares[fromLogical].piece = ChessPiece.EMPTY
        selectedPosition = -1
        clearLegalMoves()
        notifyDataSetChanged()
    }

    fun resetBoard() {
        setupInitialPosition()
        selectedPosition = -1
        clearLegalMoves()
        notifyDataSetChanged()
    }

    // LÓGICA INTERNA PRIVADA //

    private fun setupInitialPosition() {
        squares.forEach { it.piece = ChessPiece.EMPTY }
        squares[0].piece = ChessPiece.BLACK_ROOK
        squares[1].piece = ChessPiece.BLACK_KNIGHT
        squares[2].piece = ChessPiece.BLACK_BISHOP
        squares[3].piece = ChessPiece.BLACK_QUEEN
        squares[4].piece = ChessPiece.BLACK_KING
        squares[5].piece = ChessPiece.BLACK_BISHOP
        squares[6].piece = ChessPiece.BLACK_KNIGHT
        squares[7].piece = ChessPiece.BLACK_ROOK
        for (i in 8..15) squares[i].piece = ChessPiece.BLACK_PAWN
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

    private fun mapLibPieceToLocal(piece: Piece): ChessPiece {
        return when (piece) {
            Piece.WHITE_PAWN -> ChessPiece.WHITE_PAWN
            Piece.WHITE_KNIGHT -> ChessPiece.WHITE_KNIGHT
            Piece.WHITE_BISHOP -> ChessPiece.WHITE_BISHOP
            Piece.WHITE_ROOK -> ChessPiece.WHITE_ROOK
            Piece.WHITE_QUEEN -> ChessPiece.WHITE_QUEEN
            Piece.WHITE_KING -> ChessPiece.WHITE_KING
            Piece.BLACK_PAWN -> ChessPiece.BLACK_PAWN
            Piece.BLACK_KNIGHT -> ChessPiece.BLACK_KNIGHT
            Piece.BLACK_BISHOP -> ChessPiece.BLACK_BISHOP
            Piece.BLACK_ROOK -> ChessPiece.BLACK_ROOK
            Piece.BLACK_QUEEN -> ChessPiece.BLACK_QUEEN
            Piece.BLACK_KING -> ChessPiece.BLACK_KING
            else -> ChessPiece.EMPTY
        }
    }

    private fun getSquareFromIndex(index: Int): Square {
        val col = index % 8        // Columna (0=A, 1=B, ..., 7=H)
        val row = index / 8        // Fila (0=fila 8, 1=fila 7, ..., 7=fila 1)
        val rankIndex = 7 - row    // Invertir fila para ajedrez (A8=0, A1=7)

        val file = File.values()[col]
        val rank = Rank.values()[rankIndex]

        return Square.encode(rank, file)
    }

    private fun getIndexFromSquare(square: Square): Int {
        val col = square.file.ordinal
        val row = 7 - square.rank.ordinal
        return row * 8 + col
    }
}

/*
* TODO: Metodos aun no implementados, para futuro :)
* */