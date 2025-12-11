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

class NewsAdapter(private val noticias: List<Article>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.txtTitulo)
        val fuente: TextView = view.findViewById(R.id.txtFuente)
        val imagen: ImageView = view.findViewById(R.id.imgNoticia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_noticia, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val noticia = noticias[position]
        holder.titulo.text = noticia.title
        holder.fuente.text = noticia.source?.name ?: "Desconocido"

        // Usamos Glide para cargar la imagen desde la URL
        Glide.with(holder.itemView.context)
            .load(noticia.urlToImage)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery) // Imagen mientras carga
            .into(holder.imagen)
    }

    override fun getItemCount() = noticias.size
}