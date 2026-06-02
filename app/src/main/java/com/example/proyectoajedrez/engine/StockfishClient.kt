package com.example.proyectoajedrez.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class StockfishClient(private val context: Context) {

    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStreamWriter? = null
    private var isRunning = false

    companion object {
        private const val TAG = "StockfishClient"
        // El nombre DEBE coincidir con jniLibs/ABI/libstockfish.so
        private const val LIBRARY_NAME = "libstockfish.so"
    }

    private fun logNativeLibraryDir() {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        Log.d(TAG, "nativeLibraryDir=${nativeDir.absolutePath} exists=${nativeDir.exists()} canRead=${nativeDir.canRead()}")
        nativeDir.listFiles()?.forEach { file ->
            Log.d(TAG, "  ${file.name} (${file.length()} bytes, canExecute=${file.canExecute()})")
        }
    }

    /**
     * Inicializa Stockfish.
     * El binario DEBE estar en jniLibs/<ABI>/libstockfish.so para que
     * Android lo extraiga automáticamente a nativeLibraryDir, que es la
     * única partición con permisos de ejecución en Android 10+.
     */
    suspend fun inicializar() = withContext(Dispatchers.IO) {
        if (isRunning) {
            Log.d(TAG, "Stockfish ya está en ejecución")
            return@withContext
        }

        logNativeLibraryDir()

        val stockfishBinary = File(context.applicationInfo.nativeLibraryDir, LIBRARY_NAME)

        Log.d(TAG, "Buscando Stockfish en: ${stockfishBinary.absolutePath}")
        Log.d(TAG, "exists=${stockfishBinary.exists()} " +
                "canExecute=${stockfishBinary.canExecute()} " +
                "length=${stockfishBinary.length()}")

        if (!stockfishBinary.exists()) {
            Log.e(TAG, """
                Stockfish no encontrado en nativeLibraryDir.
                
                SOLUCIÓN: Coloca el binario en:
                app/src/main/jniLibs/arm64-v8a/libstockfish.so
                (y x86_64/libstockfish.so para el emulador)
                
                NO uses assets/ ni filesDir: SELinux bloquea la ejecución
                de archivos escritos en filesDir desde Android 10.
            """.trimIndent())
            return@withContext
        }

        try {
            process = ProcessBuilder(stockfishBinary.absolutePath)
                .redirectErrorStream(true)
                .start()

            process?.let { proc ->
                reader = BufferedReader(InputStreamReader(proc.inputStream))
                writer = OutputStreamWriter(proc.outputStream)
                isRunning = true
                Log.d(TAG, "Stockfish iniciado correctamente")
                sendCommand("uci")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando Stockfish: ${e.message}", e)
            isRunning = false
        }
    }

    fun sendCommand(command: String) {
        if (!isRunning) {
            Log.w(TAG, "Ignorando comando '$command': Stockfish no está activo")
            return
        }
        try {
            writer?.write("$command\n")
            writer?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando comando '$command': ${e.message}")
            isRunning = false
        }
    }

    fun readOutput(onLineReceived: (String) -> Unit) {
        Thread {
            try {
                var line: String?
                while (reader?.readLine().also { line = it } != null) {
                    line?.let { onLineReceived(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error leyendo output de Stockfish: ${e.message}")
            } finally {
                isRunning = false
            }
        }.apply {
            isDaemon = true  // No bloquear el cierre de la app
            start()
        }
    }

    val isAvailable: Boolean get() = isRunning

    fun close() {
        try {
            sendCommand("quit")
            writer?.close()
            reader?.close()
            process?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando Stockfish: ${e.message}")
        } finally {
            isRunning = false
        }
    }
}
