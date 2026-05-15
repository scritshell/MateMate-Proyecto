package com.example.proyectoajedrez.chess.engine

import android.content.Context
import com.example.proyectoajedrez.engine.StockfishClient
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StockfishController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMoveReady: (uciMove: String) -> Unit,
    private val onThinkingChanged: (isThinking: Boolean) -> Unit
) {
    private val client = StockfishClient(context)
    private var depth = 1

    suspend fun initialize() {
        client.inicializar()
        client.readOutput { line ->
            if (line.startsWith("bestmove")) {
                val move = line.split(" ").getOrNull(1) ?: return@readOutput
                onThinkingChanged(false)
                onMoveReady(move)
            }
        }
    }

    fun setDifficulty(depth: Int) {
        this.depth = depth.coerceIn(1, 20)
    }

    fun requestMove(board: Board) {
        onThinkingChanged(true)
        scope.launch(Dispatchers.IO) {
            delay((500..1200).random().toLong())
            client.sendCommand("position fen ${board.fen}")
            client.sendCommand("go depth $depth")
        }
    }

    fun close() {
        client.close()
    }
}
