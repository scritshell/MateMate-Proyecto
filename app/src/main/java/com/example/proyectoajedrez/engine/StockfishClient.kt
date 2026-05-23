package com.example.proyectoajedrez.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class StockfishClient(private val context: Context) {

    private var process: Process? = null           // Proceso nativo del motor Stockfish
    private var reader: BufferedReader? = null     // Lector de salida del proceso
    private var writer: OutputStreamWriter? = null // Escritor de entrada del proceso

    companion object {
        private const val TAG = "StockfishClient"
        private const val BINARY_NAME = "stockfish_binary"
    }

    // 1. Inicialización: usar nativeLibraryDir si el binario ya está empaquetado allí; si no, copiar desde assets a filesDir.
    suspend fun inicializar() = withContext(Dispatchers.IO) {
        val libDirCandidate = File(context.applicationInfo.nativeLibraryDir, BINARY_NAME)
        val filesDirTarget = File(context.filesDir, BINARY_NAME)

        fun startBinary(execFile: File): Boolean {
            Log.d(TAG, "Intentando ejecutar Stockfish desde: ${execFile.absolutePath}")
            Log.d(TAG, "Path info: exists=${execFile.exists()} canExecute=${execFile.canExecute()} length=${execFile.length()}")
            if (!execFile.exists()) return false
            try {
                if (!execFile.canExecute()) {
                    execFile.setExecutable(true, false)
                    Log.d(TAG, "setExecutable(true,false) aplicado a ${execFile.absolutePath}")
                }
                val processBuilder = ProcessBuilder(execFile.absolutePath)
                processBuilder.redirectErrorStream(true)
                process = processBuilder.start()
                process?.let { proc ->
                    reader = BufferedReader(InputStreamReader(proc.inputStream))
                    writer = OutputStreamWriter(proc.outputStream)
                    Log.d(TAG, "Stockfish arrancado desde ${execFile.absolutePath}")
                    sendCommand("uci")
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando Stockfish desde ${execFile.absolutePath}", e)
                Log.e(TAG, "Stockfish path info: exists=${execFile.exists()} canExecute=${execFile.canExecute()} length=${execFile.length()}")
            }
            return false
        }

        // Primero probar si el binario ya está disponible en nativeLibraryDir.
        if (libDirCandidate.exists()) {
            if (startBinary(libDirCandidate)) return@withContext
        }

        // Si no, copiar desde assets al filesDir y ejecutar.
        try {
            if (filesDirTarget.exists()) {
                filesDirTarget.delete()
            }
            context.assets.open(BINARY_NAME).use { inputStream ->
                FileOutputStream(filesDirTarget).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            filesDirTarget.setReadable(true, false)
            filesDirTarget.setWritable(true, false)
            filesDirTarget.setExecutable(true, false)
            Runtime.getRuntime().exec("chmod 755 ${filesDirTarget.absolutePath}").waitFor()
            Log.d(TAG, "Stockfish copiado a filesDir: ${filesDirTarget.absolutePath} length=${filesDirTarget.length()} exists=${filesDirTarget.exists()} canExecute=${filesDirTarget.canExecute()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error copiando Stockfish a filesDir", e)
        }

        if (!startBinary(filesDirTarget)) {
            Log.e(TAG, "No se ha podido arrancar Stockfish desde ninguna ubicación. Si el emulador usa SELinux, coloque el binario en nativeLibraryDir o revise la política de ejecución.")
        }
    }

    // 2. Enviar comandos al motor
    fun sendCommand(command: String) {
        try {
            writer?.write("$command\n")
            writer?.flush()
            Log.d(TAG, "Comando enviado: $command")
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando comando", e)
        }
    }

    // 3. Leer respuesta del motor
    fun readOutput(onLineReceived: (String) -> Unit) {
        Thread {
            try {
                var line: String?
                while (reader?.readLine().also { line = it } != null) {
                    Log.d("STOCKFISH_LOG", "Recibido: $line")
                    line?.let { onLineReceived(it) }
                }
            } catch (e: Exception) {
                Log.e("STOCKFISH_LOG", "Error leyendo output", e)
            }
        }.start()
    }

    // 4. Limpieza
    fun close() {
        try {
            sendCommand("quit")
            writer?.close()
            reader?.close()
            process?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando Stockfish", e)
        }
    }
}

/*
* TODO: Quitar todos esos logs cuando termine de testear a Stockfish
*
* */
