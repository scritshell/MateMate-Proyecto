package com.example.proyectoajedrez.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.model.Nota

class NotasAdapter(
    private val notas: List<Nota>,
    private val onItemClick: (Nota) -> Unit
) : RecyclerView.Adapter<NotasAdapter.NotaViewHolder>() {

    class NotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titulo: TextView = itemView.findViewById(R.id.tituloNota)
        val fecha: TextView = itemView.findViewById(R.id.fechaNota)
        val preview: TextView = itemView.findViewById(R.id.previewNota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = notas[position]

        holder.titulo.text = nota.titulo
        holder.fecha.text = nota.fecha
        holder.preview.text = nota.preview

        holder.itemView.setOnClickListener {
            onItemClick(nota)
        }
    }

    override fun getItemCount() = notas.size
}