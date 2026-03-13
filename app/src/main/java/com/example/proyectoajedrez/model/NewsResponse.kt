package com.example.proyectoajedrez.model

// Respuesta general de la API de noticias
data class NewsResponse(
    val status: String,          // Estado de la petición
    val totalResults: Int,       // Número total de resultados encontrados
    val articles: List<Article>  // Lista de artículos de noticias
)

// Representa un artículo individual de noticias
data class Article(
    val title: String?,         // Título de la noticia
    val description: String?,   // Descripción breve
    val urlToImage: String?,    // URL de la imagen principal
    val url: String?,           // URL completa de la noticia
    val source: Source?         // Fuente/origen de la noticia
)

// Representa la fuente/origen de una noticia
data class Source(
    val name: String?   // Nombre de la fuente
)