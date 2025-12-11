package com.example.proyectoajedrez.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MovesAdapter : RecyclerView.Adapter<MovesAdapter.MoveViewHolder>() {

    private val moves = ArrayList<String>()

    class MoveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMove: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return MoveViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        holder.tvMove.text = moves[position]
        // Opcional: Poner el texto un poco más grande o negrita
        holder.tvMove.textSize = 16f
    }

    override fun getItemCount() = moves.size

    fun addMove(moveText: String) {
        moves.add(moveText)
        notifyItemInserted(moves.size - 1)
    }

    // --- NUEVAS FUNCIONES PARA EL TURNO DE NEGRAS ---

    // Obtener el texto de la última jugada (ej: "1. e2e4")
    fun getLastMove(): String {
        return if (moves.isNotEmpty()) moves.last() else ""
    }

    // Actualizar la última jugada añadiendo el movimiento negro (ej: "1. e2e4" -> "1. e2e4  e7e5")
    fun updateLastItem(fullText: String) {
        if (moves.isNotEmpty()) {
            moves[moves.size - 1] = fullText
            notifyItemChanged(moves.size - 1)
        }
    }

    fun clear() {
        moves.clear()
        notifyDataSetChanged()
    }
}