package com.example.proyectoajedrez.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectoajedrez.adapters.NotasAdapter
import com.example.proyectoajedrez.databinding.FragmentBlocNotasBinding
import com.example.proyectoajedrez.model.Nota
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlocNotasFragment : Fragment() {

    private var _binding: FragmentBlocNotasBinding? = null
    private val binding get() = _binding!!

    // Referencias a Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val listaNotas = mutableListOf<Nota>() // Lista dinámica
    private lateinit var adapter: NotasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlocNotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Configurar RecyclerView
        setupRecyclerView()

        // 2. Cargar notas desde Firebase (READ)
        cargarNotasEnTiempoReal()

        // 3. Botón para crear nueva nota (CREATE)
        binding.cardNuevaNota.setOnClickListener {
            mostrarDialogoNota(null) // null significa "Nueva Nota"
        }
    }

    private fun setupRecyclerView() {
        // Inicializamos el adapter con la lista vacía y la función de click para EDITAR
        adapter = NotasAdapter(listaNotas) { notaSeleccionada ->
            mostrarDialogoNota(notaSeleccionada) // Pasamos la nota para Editarla
        }
        binding.recyclerViewNotas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotas.adapter = adapter
    }

    // --- LECTURA DE DATOS (READ) ---
    private fun cargarNotasEnTiempoReal() {
        val userId = auth.currentUser?.uid ?: return // Si no hay usuario, no cargamos nada

        // Escuchamos la colección "notas" donde el userId coincida con el actual
        db.collection("notas")
            .whereEqualTo("userId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING) // Ordenar por fecha (más nueva arriba)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error al cargar notas", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    listaNotas.clear() // Limpiamos la lista vieja
                    for (document in snapshots) {
                        // Convertimos el documento a objeto Nota
                        val nota = document.toObject(Nota::class.java)
                        nota.id = document.id // Guardamos el ID del documento
                        listaNotas.add(nota)
                    }
                    adapter.notifyDataSetChanged() // Avisamos al adaptador que hay datos nuevos
                    actualizarUI()
                }
            }
    }

    // --- DIÁLOGO PARA CREAR Y EDITAR ---
    private fun mostrarDialogoNota(notaExistente: Nota?) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (notaExistente == null) "Nueva Nota" else "Editar Nota")

        // Layout simple dentro del código para no crear XMLs extra
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 10)

        val inputTitulo = EditText(requireContext())
        inputTitulo.hint = "Título"
        inputTitulo.setText(notaExistente?.titulo ?: "") // Si existe, ponemos el texto
        layout.addView(inputTitulo)

        val inputContenido = EditText(requireContext())
        inputContenido.hint = "Escribe tu análisis..."
        inputContenido.minLines = 3
        inputContenido.setText(notaExistente?.contenido ?: "")
        layout.addView(inputContenido)

        builder.setView(layout)

        // Botón GUARDAR
        builder.setPositiveButton("Guardar") { _, _ ->
            val titulo = inputTitulo.text.toString()
            val contenido = inputContenido.text.toString()

            if (titulo.isNotEmpty()) {
                guardarEnFirebase(notaExistente?.id, titulo, contenido)
            } else {
                Toast.makeText(context, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón CANCELAR
        builder.setNegativeButton("Cancelar", null)

        // Botón BORRAR (Solo si estamos editando una nota existente)
        if (notaExistente != null) {
            builder.setNeutralButton("Borrar") { _, _ ->
                confirmarBorrado(notaExistente.id)
            }
        }

        builder.show()
    }

    // --- GUARDADO (CREATE / UPDATE) ---
    private fun guardarEnFirebase(idNota: String?, titulo: String, contenido: String) {
        val userId = auth.currentUser?.uid ?: return
        val fechaActual = System.currentTimeMillis()

        val datosNota = hashMapOf(
            "userId" to userId,
            "titulo" to titulo,
            "contenido" to contenido,
            "fecha" to fechaActual
        )

        if (idNota == null) {
            // CREAR (Create)
            db.collection("notas")
                .add(datosNota)
                .addOnSuccessListener {
                    Toast.makeText(context, "Nota guardada", Toast.LENGTH_SHORT).show()
                }
        } else {
            // ACTUALIZAR (Update)
            db.collection("notas").document(idNota)
                .update(datosNota as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(context, "Nota actualizada", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- BORRADO (DELETE) ---
    private fun confirmarBorrado(idNota: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("¿Eliminar nota?")
            .setMessage("Esta acción no se puede deshacer.")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                db.collection("notas").document(idNota).delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Nota eliminada", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun actualizarUI() {
        if (listaNotas.isEmpty()) {
            // Podrías mostrar un texto de "No hay notas" si quieres
            // binding.tvSinNotas.visibility = View.VISIBLE
        } else {
            // binding.tvSinNotas.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}