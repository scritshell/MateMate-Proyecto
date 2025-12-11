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
        // Usamos un layout simple de Android para no complicarnos
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return MoveViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        holder.tvMove.text = moves[position]
    }

    override fun getItemCount() = moves.size

    fun addMove(moveText: String) {
        moves.add(moveText)
        notifyItemInserted(moves.size - 1)
    }

    fun clear() {
        moves.clear()
        notifyDataSetChanged()
    }
}