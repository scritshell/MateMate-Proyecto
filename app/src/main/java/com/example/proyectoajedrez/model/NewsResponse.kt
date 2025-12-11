package com.example.proyectoajedrez.model
// La respuesta general de la API
data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

// Cada noticia individual
data class Article(
    val title: String?,
    val description: String?,
    val urlToImage: String?, // La foto de la noticia
    val url: String?,        // El enlace para leerla completa
    val source: Source?
)

data class Source(
    val name: String?
)