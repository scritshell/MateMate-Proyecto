package com.example.proyectoajedrez.chess.engine

import android.content.Context
import com.example.proyectoajedrez.engine.StockfishClient
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope

class StockfishController(
    private val context: Context,
    private val scope: CoroutineScope? = null,
    private val onMoveReady: (uciMove: String) -> Unit = { _ -> },
    private val onThinkingChanged: (isThinking: Boolean) -> Unit = { _ -> }
) {
    private val client = StockfishClient(context)
    private var depth = 1
    private var activeScope: CoroutineScope = scope ?: GlobalScope

    // Delegar disponibilidad del cliente para que el ViewModel pueda consultarlo
    val isAvailable: Boolean
        get() = client.isAvailable

    fun setScope(newScope: CoroutineScope) {
        this.activeScope = newScope
    }

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
        if (!isAvailable) {
            onThinkingChanged(false)
            return
        }

        onThinkingChanged(true)
        activeScope.launch(Dispatchers.IO) {
            delay((500..1200).random().toLong())
            client.sendCommand("position fen ${board.fen}")
            client.sendCommand("go depth $depth")
        }
    }

    fun close() {
        client.close()
    }
}
