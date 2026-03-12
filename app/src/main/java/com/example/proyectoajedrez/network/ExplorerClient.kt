package com.example.proyectoajedrez.network

import com.example.proyectoajedrez.model.ExplorerResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Definición de la interfaz de la API
interface ExplorerApiService {
    // Consultamos la base de datos de maestros pasando el FEN actual
    @GET("masters")
    suspend fun getOpeningMoves(
        @Query("fen") fen: String,
        @Query("moves") moves: Int = 12 // Pedimos las 12 jugadas más comunes
    ): ExplorerResponse
}

// Objeto para acceder a la API desde cualquier parte
object ExplorerClient {
    private const val BASE_URL = "https://explorer.lichess.ovh/"

    val instance: ExplorerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExplorerApiService::class.java)
    }
}