package com.example.proyectoajedrez.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectoajedrez.databinding.ItemExplorerMoveBinding
import com.example.proyectoajedrez.model.ExplorerMove

class ExplorerAdapter : RecyclerView.Adapter<ExplorerAdapter.ViewHolder>() {

    private var moves: List<ExplorerMove> = emptyList()

    // Actualiza la lista cuando recibimos datos de la API
    fun submitList(newMoves: List<ExplorerMove>) {
        moves = newMoves
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemExplorerMoveBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExplorerMoveBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val move = moves[position]
        val total = move.getTotalGames().toFloat()

        holder.binding.tvMoveSan.text = move.san
        holder.binding.tvTotalGames.text = move.getTotalGames().toString()

        // LÓGICA DE LA BARRA DE PORCENTAJES
        val whiteParams = holder.binding.viewWhiteBar.layoutParams as LinearLayout.LayoutParams
        val drawParams = holder.binding.viewDrawBar.layoutParams as LinearLayout.LayoutParams
        val blackParams = holder.binding.viewBlackBar.layoutParams as LinearLayout.LayoutParams

        if (total > 0) {
            // Asignamos el "weight" basado en la proporción de victorias
            whiteParams.weight = (move.white / total)
            drawParams.weight = (move.draws / total)
            blackParams.weight = (move.black / total)
        } else {
            whiteParams.weight = 0f
            drawParams.weight = 1f
            blackParams.weight = 0f
        }

        holder.binding.viewWhiteBar.layoutParams = whiteParams
        holder.binding.viewDrawBar.layoutParams = drawParams
        holder.binding.viewBlackBar.layoutParams = blackParams
    }

    override fun getItemCount() = moves.size
}