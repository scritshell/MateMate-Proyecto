package com.example.proyectoajedrez.model

import com.google.gson.annotations.SerializedName

// Mapeamos la respuesta JSON
data class ImgBbResponse(
    val data: ImgBbData,
    val success: Boolean,
    val status: Int
)

data class ImgBbData(
    val id: String,
    val title: String?,
    @SerializedName("url") val url: String,
    @SerializedName("display_url") val displayUrl: String
)