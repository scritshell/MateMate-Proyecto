package com.example.proyectoajedrez.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectoajedrez.databinding.ItemAperturaBinding
import com.example.proyectoajedrez.model.Apertura

class AperturasAdapter(
    private val aperturas: List<Apertura>,
    private val onAperturaClick: (Apertura) -> Unit
) : RecyclerView.Adapter<AperturasAdapter.AperturaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AperturaViewHolder {
        val binding = ItemAperturaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AperturaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AperturaViewHolder, position: Int) {
        holder.bind(aperturas[position])
    }

    override fun getItemCount(): Int = aperturas.size

    inner class AperturaViewHolder(private val binding: ItemAperturaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(apertura: Apertura) {
            binding.tvNombreApertura.text = apertura.nombre
            binding.tvDescripcionApertura.text = apertura.descripcion
            binding.tvEmojiApertura.text = apertura.emoji

            binding.root.setOnClickListener {
                onAperturaClick(apertura)
            }
        }
    }
}