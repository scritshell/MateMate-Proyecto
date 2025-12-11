package com.example.proyectoajedrez.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.model.Nota
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        // 1. Asignar Título
        holder.titulo.text = nota.titulo

        // 2. Asignar Contenido (Antes era preview)
        // Cortamos el texto si es muy largo para que no deforme la tarjeta
        val textoContenido = nota.contenido
        holder.preview.text = if (textoContenido.length > 50) {
            "${textoContenido.substring(0, 50)}..."
        } else {
            textoContenido
        }

        // 3. Formatear Fecha (RA 2.a - Transformación de datos)
        // Convertimos el Long (milisegundos) a un String legible
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val netDate = Date(nota.fecha)
            holder.fecha.text = sdf.format(netDate)
        } catch (e: Exception) {
            holder.fecha.text = "Fecha desconocida"
        }

        // 4. Click Listener
        holder.itemView.setOnClickListener {
            onItemClick(nota)
        }
    }

    override fun getItemCount() = notas.size
}