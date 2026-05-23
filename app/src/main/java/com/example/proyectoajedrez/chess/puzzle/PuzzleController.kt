package com.example.proyectoajedrez.chess.puzzle

import android.content.Context
import com.example.proyectoajedrez.data.local.MateMateDataBase
import com.example.proyectoajedrez.data.local.PuzzleProgressEntity
import com.example.proyectoajedrez.network.LichessClient
import com.example.proyectoajedrez.utils.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Constants
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class PuzzleController(private val context: Context) {

    private val solution = mutableListOf<String>()
    val hasSolution: Boolean get() = solution.isNotEmpty()

    // Carga el puzzle en el board y retorna el rating, o null si falla
    suspend fun loadDailyPuzzle(board: Board): PuzzleLoadResult {
        return try {
            val response = LichessClient.instance.getDailyPuzzle()

            board.loadFromFen(Constants.startStandardFENPosition)

            val cleanPgn = response.game.pgn
                .replace(Regex("\\[.*?\\]"), " ")
                .replace(Regex("\\{.*?\\}"), " ")
                .replace(Regex("\\d+\\.+"), " ")
                .replace("0-0-0", "O-O-O")
                .replace("0-0", "O-O")
                .trim()

            cleanPgn.split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { san ->
                ChessUtils.sanToMove(san, board)?.let { board.doMove(it) }
            }

            solution.clear()
            solution.addAll(response.puzzle.solution)

            PuzzleLoadResult.Success(
                rating = response.puzzle.rating,
                playerSide = board.sideToMove
            )
        } catch (e: Exception) {
            PuzzleLoadResult.Error(e.message ?: "Error desconocido")
        }
    }

    // Retorna true si el movimiento es correcto
    fun validateMove(move: Move): PuzzleMoveResult {
        if (solution.isEmpty()) return PuzzleMoveResult.Incorrect

        return if (move.toString().lowercase() == solution[0]) {
            solution.removeAt(0)
            if (solution.isEmpty()) PuzzleMoveResult.Solved
            else PuzzleMoveResult.CorrectContinue(solution[0])
        } else {
            PuzzleMoveResult.Incorrect
        }
    }

    fun getNextEngineMove(): String? = solution.firstOrNull()

    fun consumeEngineMove() { if (solution.isNotEmpty()) solution.removeAt(0) }

    suspend fun saveProgress() {
        val dao = MateMateDataBase.getInstance(context).puzzleProgressDao()
        val current = dao.getProgress().firstOrNull() ?: PuzzleProgressEntity()
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        val newStreak = when {
            current.lastSolvedDate == today -> current.currentStreak
            isYesterday(current.lastSolvedDate) -> current.currentStreak + 1
            else -> 1
        }

        // Guardar en SharedPreferences para que PuzzleDiarioFragment lea métricas rápidamente
        val prefs = context.getSharedPreferences(com.example.proyectoajedrez.utils.PrefKeys.AJEDREZ_PREFS, Context.MODE_PRIVATE)
        val totalSolved = prefs.getInt(com.example.proyectoajedrez.utils.PrefKeys.KEY_LOCAL_PUZZLES_SOLVED, current.totalSolved)

        prefs.edit()
            .putInt(com.example.proyectoajedrez.utils.PrefKeys.KEY_PUZZLE_STREAK_DAYS, newStreak)
            .putInt(com.example.proyectoajedrez.utils.PrefKeys.KEY_LOCAL_PUZZLES_SOLVED, totalSolved + 1)
            .putString("last_puzzle_date", today)
            .apply()

        // También actualizar Room (fuente canónica)
        dao.saveProgress(current.copy(
            currentStreak = newStreak,
            totalSolved = totalSolved + 1,
            lastSolvedDate = today
        ))
    }

    private fun isYesterday(date: String?): Boolean {
        if (date.isNullOrEmpty()) return false
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val cal = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        return date == sdf.format(cal.time)
    }
}

sealed class PuzzleLoadResult {
    data class Success(val rating: Int, val playerSide: com.github.bhlangonijr.chesslib.Side) : PuzzleLoadResult()
    data class Error(val message: String) : PuzzleLoadResult()
}

sealed class PuzzleMoveResult {
    object Incorrect : PuzzleMoveResult()
    data class CorrectContinue(val nextEngineMove: String) : PuzzleMoveResult()
    object Solved : PuzzleMoveResult()
}
