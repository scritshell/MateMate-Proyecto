package com.example.proyectoajedrez.network

import com.example.proyectoajedrez.model.NewsResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Interfaz para definir las peticiones a la API de noticias
interface NewsApiService {
    @GET("v2/everything")  // Endpoint para obtener noticias
    suspend fun getChessNews(
        @Query("q") query: String,        // Término de búsqueda (ej: "ajedrez", "chess")
        @Query("apiKey") apiKey: String,  // Clave de API para autenticación
        @Query("language") language: String = "es",  // Idioma de las noticias
        @Query("sortBy") sortBy: String = "publishedAt"  // Ordenar por fecha de publicación
    ): NewsResponse  // Retorna objeto NewsResponse con los resultados
}

// Objeto singleton para manejar la instancia de Retrofit
object RetrofitClient {
    private const val BASE_URL = "https://newsapi.org/"  // URL base de la API

    // Instancia lazy de NewsApiService (se crea solo cuando se necesita)
    val instance: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)                    // Establecer URL base
            .addConverterFactory(GsonConverterFactory.create())  // Convertidor JSON
            .build()
            .create(NewsApiService::class.java)   // Crear implementación de la interfaz
    }
}