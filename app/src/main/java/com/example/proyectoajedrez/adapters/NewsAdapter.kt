package com.example.proyectoajedrez.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.model.Article

// Adaptador para mostrar noticias de ajedrez en RecyclerView
class NewsAdapter(private val noticias: List<Article>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    // ViewHolder que contiene las vistas de cada item de noticia
    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.txtTitulo)        // Título de la noticia
        val fuente: TextView = view.findViewById(R.id.txtFuente)        // Fuente/origen
        val imagen: ImageView = view.findViewById(R.id.imgNoticia)      // Imagen principal
    }

    // Inflar layout del item de noticia y crear ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_noticia, parent, false)
        return NewsViewHolder(view)
    }

    // Vincular datos de la noticia a las vistas del ViewHolder
    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val noticia = noticias[position]

        holder.titulo.text = noticia.title                          // Establecer título
        holder.fuente.text = noticia.source?.name ?: "Desconocido"  // Fuente o valor por defecto

        // Cargar imagen usando Glide con manejo de URL
        Glide.with(holder.itemView.context)
            .load(noticia.urlToImage)                               // URL de la imagen
            .centerCrop()                                                  // Recortar al centro
            .placeholder(android.R.drawable.ic_menu_gallery)    // Imagen mientras carga
            .into(holder.imagen)                                    // Asignar a ImageView
    }

    // Retornar cantidad total de noticias
    override fun getItemCount() = noticias.size
}