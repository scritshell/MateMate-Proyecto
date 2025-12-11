package com.example.proyectoajedrez.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adaptador para mostrar lista de movimientos de ajedrez
class MovesAdapter : RecyclerView.Adapter<MovesAdapter.MoveViewHolder>() {

    // Lista que almacena los movimientos como strings
    private val moves = ArrayList<String>()

    // ViewHolder para cada item de movimiento
    class MoveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMove: TextView = view.findViewById(android.R.id.text1) // TextView del layout simple
    }

    // Crear nuevo ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false) // Layout simple de Android
        return MoveViewHolder(view)
    }

    // Vincular datos del movimiento a la vista
    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        holder.tvMove.text = moves[position] // Establecer texto del movimiento
        holder.tvMove.textSize = 16f // Tamaño de texto aumentado para mejor legibilidad
    }

    // Número total de movimientos
    override fun getItemCount() = moves.size

    // Agregar nuevo movimiento a la lista
    fun addMove(moveText: String) {
        moves.add(moveText) // Añadir al final
        notifyItemInserted(moves.size - 1) // Notificar inserción
    }

    // Obtener el último movimiento registrado
    fun getLastMove(): String {
        return if (moves.isNotEmpty()) moves.last() else ""
    }

    // Actualizar el último movimiento con información completa (blancas + negras)
    fun updateLastItem(fullText: String) {
        if (moves.isNotEmpty()) {
            moves[moves.size - 1] = fullText // Reemplazar último movimiento
            notifyItemChanged(moves.size - 1) // Notificar cambio
        }
    }

    // Limpiar toda la lista de movimientos
    fun clear() {
        moves.clear()
        notifyDataSetChanged() // Notificar que todos los datos cambiaron
    }
}