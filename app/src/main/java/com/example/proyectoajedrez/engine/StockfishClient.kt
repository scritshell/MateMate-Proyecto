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

    // 1. Inicialización: Copia el binario y prepara el proceso
    suspend fun inicializar() = withContext(Dispatchers.IO) {
        val stockfishFile = File(context.filesDir, BINARY_NAME)

        // A. Copiar y dar permisos
        try {
            // Copia forzosa para evitar archivos corruptos
            context.assets.open(BINARY_NAME).use { inputStream ->
                FileOutputStream(stockfishFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Permisos de ejecución (Intento nativo Java)
            val success = stockfishFile.setExecutable(true, false)
            Log.d(TAG, "Permisos de ejecución otorgados: $success")

            // Permisos de ejecución (Fallback Linux por si falla el anterior)
            if (!success) {
                Runtime.getRuntime().exec("chmod 744 ${stockfishFile.absolutePath}").waitFor()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error copiando Stockfish", e)
        }

        // B. Arrancar el proceso (SOLO UNA VEZ)
        try {
            val processBuilder = ProcessBuilder(stockfishFile.absolutePath)
            processBuilder.redirectErrorStream(true) // Unificar errores y salida estándar

            // ¡ARRANCAMOS!
            process = processBuilder.start()

            // Configuramos los canales de comunicación
            process?.let { proc ->
                reader = BufferedReader(InputStreamReader(proc.inputStream))
                writer = OutputStreamWriter(proc.outputStream)

                Log.d(TAG, "¡MOTOR STOCKFISH ARRANCADO!")
                sendCommand("uci") // Saludo inicial al protocolo UCI
            } ?: run {
                Log.e(TAG, "Error: El objeto Process es nulo")
                throw IllegalStateException("No se pudo iniciar Stockfish")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando el proceso", e)
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
