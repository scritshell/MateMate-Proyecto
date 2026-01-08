package com.example.proyectoajedrez.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectoajedrez.databinding.ItemAperturaBinding
import com.example.proyectoajedrez.model.Apertura

// Adaptador para mostrar la lista de aperturas de ajedrez en RecyclerView
class AperturasAdapter(
    private val aperturas: List<Apertura>,           // Lista de aperturas a mostrar
    private val onAperturaClick: (Apertura) -> Unit  // Callback al hacer click en una apertura
) : RecyclerView.Adapter<AperturasAdapter.AperturaViewHolder>() {

    // Crear nuevo ViewHolder inflando el layout del item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AperturaViewHolder {
        val binding = ItemAperturaBinding.inflate(
            LayoutInflater.from(parent.context),  // Inflador del layout
            parent,                               // ViewGroup padre
            false                                 // No adjuntar al padre aún
        )
        return AperturaViewHolder(binding)
    }

    // Vincular datos de una apertura a un ViewHolder en posición específica
    override fun onBindViewHolder(holder: AperturaViewHolder, position: Int) {
        holder.bind(aperturas[position])
    }

    // Retornar cantidad total de items en la lista
    override fun getItemCount(): Int = aperturas.size

    // Usamos clase inner para tener acceso a los miembros de la clase externa
    // ViewHolder que representa cada item de apertura en el RecyclerView
    inner class AperturaViewHolder(private val binding: ItemAperturaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Asignar datos de la apertura a las vistas del layout
        fun bind(apertura: Apertura) {
            binding.tvNombreApertura.text = apertura.nombre         // Nombre de la apertura
            binding.tvDescripcionApertura.text = apertura.descripcion // Descripción breve
            binding.tvEmojiApertura.text = apertura.emoji           // Emoji representativo

            // Configurar click listener en el item completo
            binding.root.setOnClickListener {
                onAperturaClick(apertura)  // Ejecutar callback con la apertura clickeada
            }
        }
    }
}