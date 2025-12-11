package com.example.proyectoajedrez.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.adapters.NotasAdapter
import com.example.proyectoajedrez.databinding.FragmentBlocNotasBinding
import com.example.proyectoajedrez.model.Nota
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class BlocNotasFragment : Fragment() {

    private var _binding: FragmentBlocNotasBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val listaNotas = mutableListOf<Nota>()
    private lateinit var adapter: NotasAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBlocNotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        cargarNotasEnTiempoReal()
        binding.cardNuevaNota.setOnClickListener { mostrarDialogoNota(null) }
    }

    private fun setupRecyclerView() {
        adapter = NotasAdapter(listaNotas) { nota -> mostrarDialogoNota(nota) }
        binding.recyclerViewNotas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotas.adapter = adapter
    }

    private fun cargarNotasEnTiempoReal() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("notas").whereEqualTo("userId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    listaNotas.clear()
                    for (document in snapshots) {
                        val nota = document.toObject(Nota::class.java)
                        nota.id = document.id
                        listaNotas.add(nota)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    // EL DIALOGO
    private fun mostrarDialogoNota(notaExistente: Nota?) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (notaExistente == null) "Nueva Nota" else "Editar Nota")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // 1. TÍTULO
        val inputTitulo = EditText(requireContext())
        inputTitulo.hint = "Título"
        inputTitulo.setText(notaExistente?.titulo ?: "")
        layout.addView(inputTitulo)

        // 2. SPINNER
        val tvCategoria = TextView(requireContext())
        tvCategoria.text = "Categoría:"
        tvCategoria.setPadding(0, 20, 0, 5)
        layout.addView(tvCategoria)

        val spinner = Spinner(requireContext())
        val opciones = listOf("General", "Apertura", "Partida", "Análisis", "Torneo")
        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opciones)
        spinner.adapter = adapterSpinner

        // Seleccionar la categoría guardada si existe
        if (notaExistente != null) {
            val position = opciones.indexOf(notaExistente.categoria)
            if (position >= 0) spinner.setSelection(position)
        }
        layout.addView(spinner)

        // 3. INPUT CONTENIDO
        val inputContenido = EditText(requireContext())
        inputContenido.hint = "Escribe aquí..."
        inputContenido.minLines = 4
        inputContenido.setText(notaExistente?.contenido ?: "")
        layout.addView(inputContenido)

        // 4. BOTÓN COMPARTIR
        if (notaExistente != null) {
            val btnShare = Button(requireContext())
            btnShare.text = "📤 Compartir Nota"
            btnShare.setOnClickListener {
                val textoACompartir = "📌 ${inputTitulo.text}\n\n${inputContenido.text}\n\n- Vía MateMate App"
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, textoACompartir)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, "Compartir con:"))
            }
            layout.addView(btnShare)
        }

        builder.setView(layout)

        // BOTONES DEL DIÁLOGO
        builder.setPositiveButton("Guardar") { _, _ ->
            val titulo = inputTitulo.text.toString()
            val contenido = inputContenido.text.toString()
            val categoria = spinner.selectedItem.toString() // Cogemos el valor del Spinner

            if (titulo.isNotEmpty()) {
                guardarEnFirebase(notaExistente?.id, titulo, contenido, categoria)
            }
        }
        builder.setNegativeButton("Cancelar", null)

        if (notaExistente != null) {
            builder.setNeutralButton("🗑️ Borrar") { _, _ ->
                confirmarBorrado(notaExistente.id)
            }
        }

        builder.show()
    }

    private fun guardarEnFirebase(id: String?, titulo: String, contenido: String, categoria: String) {
        val userId = auth.currentUser?.uid ?: return
        val datos = hashMapOf(
            "userId" to userId,
            "titulo" to titulo,
            "contenido" to contenido,
            "categoria" to categoria,
            "fecha" to System.currentTimeMillis()
        )

        if (id == null) {
            db.collection("notas").add(datos)
        } else {
            db.collection("notas").document(id).update(datos as Map<String, Any>)
        }
    }

    private fun confirmarBorrado(id: String) {
        db.collection("notas").document(id).delete()
            .addOnSuccessListener { Toast.makeText(context, "Nota eliminada", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}