package com.example.proyectoajedrez.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.model.Nota
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Adaptador para mostrar lista de notas en RecyclerView
class NotasAdapter(
    private val notas: List<Nota>,                   // Lista de notas a mostrar
    private val onItemClick: (Nota) -> Unit,         // Callback al hacer click en nota (editar)
    private val onDeleteClick: (Nota) -> Unit        // Callback al hacer click en botón borrar
) : RecyclerView.Adapter<NotasAdapter.NotaViewHolder>() {

    // ViewHolder que contiene las vistas de cada item de nota
    class NotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titulo: TextView = itemView.findViewById(R.id.tituloNota)      // Título de la nota
        val fecha: TextView = itemView.findViewById(R.id.fechaNota)        // Fecha de creación
        val preview: TextView = itemView.findViewById(R.id.previewNota)    // Vista previa del contenido
        val btnBorrar: ImageButton = itemView.findViewById(R.id.btnBorrar) // Botón para eliminar nota
    }

    // Crear nuevo ViewHolder inflando el layout del item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    // Vincular datos de la nota a las vistas del ViewHolder
    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = notas[position]

        // Mostrar título de la nota
        holder.titulo.text = nota.titulo

        // Mostrar vista previa del contenido (máximo 50 caracteres)
        val textoContenido = nota.contenido
        holder.preview.text = if (textoContenido.length > 50) {
            "${textoContenido.substring(0, 50)}..." // Recortar y agregar puntos suspensivos
        } else {
            textoContenido
        }

        // Formatear y mostrar fecha de creación
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val netDate = Date(nota.fecha)
            holder.fecha.text = sdf.format(netDate)
        } catch (e: Exception) {
            holder.fecha.text = "-" // Texto alternativo si hay error
        }

        // Click en el item completo: editar nota
        holder.itemView.setOnClickListener {
            onItemClick(nota)
        }

        // Click en botón borrar: eliminar nota
        holder.btnBorrar.setOnClickListener {
            onDeleteClick(nota)
        }
    }

    // Retornar cantidad total de notas
    override fun getItemCount() = notas.size
}