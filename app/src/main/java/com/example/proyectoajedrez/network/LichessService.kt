package com.example.proyectoajedrez.network

import com.example.proyectoajedrez.model.LichessPuzzleResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Define los métodos para comunicarse con la API de Lichess
interface LichessApiService {

    // Petición GET a la ruta específica del puzzle diario
    @GET("api/puzzle/daily")
    // Función suspendida para ejecutarse en segundo plano (Corrutinas)
    suspend fun getDailyPuzzle(): LichessPuzzleResponse
}

// Objeto Singleton para gestionar la conexión de red
object LichessClient {
    // URL base del servidor de Lichess
    private const val BASE_URL = "https://lichess.org/"

    // Inicializa Retrofit de forma perezosa (solo cuando se necesita por primera vez)
    val instance: LichessApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Convierte el JSON a objetos Kotlin automáticamente
            .build()
            .create(LichessApiService::class.java)
    }
}