package com.example.proyectoajedrez.network

import com.example.proyectoajedrez.model.NewsResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. La Interfaz que define la petición
interface NewsApiService {
    @GET("v2/everything")
    suspend fun getChessNews(
        @Query("q") query: String,       // Buscaremos "ajedrez" o "chess"
        @Query("apiKey") apiKey: String, // Tu clave
        @Query("language") language: String = "es", // Noticias en español
        @Query("sortBy") sortBy: String = "publishedAt" // Las más nuevas primero
    ): NewsResponse
}

// 2. El objeto Singleton para usarlo desde cualquier lado
object RetrofitClient {
    private const val BASE_URL = "https://newsapi.org/"

    val instance: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }
}