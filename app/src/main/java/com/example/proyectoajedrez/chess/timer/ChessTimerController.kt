package com.example.proyectoajedrez.chess.timer

import android.os.CountDownTimer
import com.github.bhlangonijr.chesslib.Side

class ChessTimerController(
    private val onTick: (side: Side, remainingMillis: Long) -> Unit = { _, _ -> },
    private val onTimeExpired: (side: Side) -> Unit = { _ -> }
) {

    private var timerWhite: CountDownTimer? = null
    private var timerBlack: CountDownTimer? = null
    private var timeLeftWhite = 300_000L
    private var timeLeftBlack = 300_000L
    var isUnlimited: Boolean = true
        private set

    fun configure(minutes: Int) {
        isUnlimited = (minutes <= 0)
        if (!isUnlimited) {
            timeLeftWhite = minutes * 60_000L
            timeLeftBlack = minutes * 60_000L
        }
    }

    fun startFor(side: Side) {
        if (isUnlimited) return
        cancelAll()

        val timeLeft = if (side == Side.WHITE) timeLeftWhite else timeLeftBlack

        val timer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (side == Side.WHITE) timeLeftWhite = millisUntilFinished
                else timeLeftBlack = millisUntilFinished
                onTick(side, millisUntilFinished)
            }
            override fun onFinish() {
                onTimeExpired(side)
            }
        }.start()

        if (side == Side.WHITE) timerWhite = timer else timerBlack = timer
    }

    fun cancelAll() {
        timerWhite?.cancel()
        timerBlack?.cancel()
    }

    fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    fun getTimeLeft(side: Side): Long =
        if (side == Side.WHITE) timeLeftWhite else timeLeftBlack
}