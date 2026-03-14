package com.example.proyectoajedrez.network

import com.example.proyectoajedrez.model.ImgBbResponse
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

// 1. La interfaz del servicio
interface ImgBbApiService {
    @Multipart
    @POST("1/upload")
    suspend fun uploadImage(
        @Query("key") apiKey: String,
        @Part image: MultipartBody.Part
    ): ImgBbResponse
}

// 2. El cliente Singleton
object ImgBbClient {
    private const val BASE_URL = "https://api.imgbb.com/"

    val apiService: ImgBbApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImgBbApiService::class.java)
    }
}