package com.example.proyectoajedrez.fragments

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
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.adapters.NotasAdapter
import com.example.proyectoajedrez.databinding.FragmentBlocNotasBinding
import com.example.proyectoajedrez.model.Nota
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Fragmento para gestionar notas personales de ajedrez
// Finalizado
class BlocNotasFragment : Fragment() {

    private var _binding: FragmentBlocNotasBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()     // Instancia de Firestore
    private val auth = FirebaseAuth.getInstance()        // Instancia de Firebase Auth
    private val listaNotas = mutableListOf<Nota>()       // Lista mutable de notas
    private lateinit var adapter: NotasAdapter           // Adaptador para RecyclerView
    private var firestoreListener: ListenerRegistration? = null  // Listener para evitar memory leaks

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBlocNotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()           // Configurar RecyclerView
        cargarNotasEnTiempoReal()     // Cargar notas desde Firebase

        // Click en botón/card para crear nueva nota
        binding.cardNuevaNota.setOnClickListener { mostrarDialogoNota(null) }
    }

    private fun setupRecyclerView() {
        // Crear adaptador con callbacks para editar y borrar notas
        adapter = NotasAdapter(
            listaNotas,
            { nota -> mostrarDialogoNota(nota) },    // Click en nota: editar
            { nota -> confirmarBorrado(nota.id) }            // Click en papelera: borrar
        )

        binding.recyclerViewNotas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotas.adapter = adapter
    }

    // Escuchar cambios en tiempo real en la colección de notas
    private fun cargarNotasEnTiempoReal() {
        val userId = auth.currentUser?.uid ?: return  // ID del usuario actual

        firestoreListener = db.collection("notas").whereEqualTo("userId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING)  // Ordenar por fecha descendente
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    listaNotas.clear()
                    for (document in snapshots) {
                        val nota = document.toObject(Nota::class.java)
                        nota.id = document.id  // Asignar ID del documento Firestore
                        listaNotas.add(nota)
                    }
                    adapter.notifyDataSetChanged()  // Actualizar RecyclerView
                }
            }
    }


    // Mostrar diálogo para crear o editar una nota
    private fun mostrarDialogoNota(notaExistente: Nota?) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(
            if (notaExistente == null) 
                getString(R.string.dialog_nueva_nota) 
            else 
                getString(R.string.dialog_editar_nota)
        )

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Campo para título de la nota
        val inputTitulo = EditText(requireContext())
        inputTitulo.hint = getString(R.string.note_hint_title)
        inputTitulo.setText(notaExistente?.titulo ?: "")
        layout.addView(inputTitulo)

        // Spinner para seleccionar categoría
        val tvCategoria = TextView(requireContext())
        tvCategoria.text = getString(R.string.note_label_category)
        tvCategoria.setPadding(0, 20, 0, 5)
        layout.addView(tvCategoria)

        val spinner = Spinner(requireContext())
        val opcionesBase = resources.getStringArray(R.array.note_category_options).toList()
        val opciones = if (notaExistente?.categoria != null && notaExistente.categoria !in opcionesBase) {
            listOf(notaExistente.categoria) + opcionesBase
        } else {
            opcionesBase
        }
        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opciones)
        spinner.adapter = adapterSpinner

        // Seleccionar categoría existente si se está editando
        if (notaExistente != null) {
            val position = opciones.indexOf(notaExistente.categoria)
            if (position >= 0) spinner.setSelection(position)
        }
        layout.addView(spinner)

        // Campo para contenido de la nota
        val inputContenido = EditText(requireContext())
        inputContenido.hint = getString(R.string.note_hint_content)
        inputContenido.minLines = 4
        inputContenido.setText(notaExistente?.contenido ?: "")
        layout.addView(inputContenido)

        // Botón para compartir nota (solo disponible en edición)
        if (notaExistente != null) {
            val btnShare = Button(requireContext())
            btnShare.text = getString(R.string.note_btn_share)
            btnShare.setOnClickListener {
                val textoACompartir = getString(
                    R.string.note_share_text,
                    inputTitulo.text.toString(),
                    inputContenido.text.toString()
                )
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, textoACompartir)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, getString(R.string.note_share_chooser)))
            }
            layout.addView(btnShare)
        }

        builder.setView(layout)

        // Botón para guardar cambios
        builder.setPositiveButton(getString(R.string.btn_guardar)) { _, _ ->
            val titulo = inputTitulo.text.toString()
            val contenido = inputContenido.text.toString()
            val categoria = spinner.selectedItem.toString()

            if (titulo.isNotEmpty()) {
                guardarEnFirebase(notaExistente?.id, titulo, contenido, categoria)
            }
        }
        builder.setNegativeButton(getString(R.string.dialog_btn_cancelar), null)

        builder.show()
    }

    // Guardar nota en Firestore (crear o actualizar)
    private fun guardarEnFirebase(id: String?, titulo: String, contenido: String, categoria: String) {
        val userId = auth.currentUser?.uid ?: return

        val datos = hashMapOf(
            "userId" to userId,
            "titulo" to titulo,
            "contenido" to contenido,
            "categoria" to categoria,
            "fecha" to System.currentTimeMillis()  // Timestamp actual
        )

        if (id == null) {
            // Crear nueva nota
            db.collection("notas").add(datos)
        } else {
            // Actualizar nota existente
            db.collection("notas").document(id).update(datos as Map<String, Any>)
        }
    }

    // Confirmar eliminación de una nota
    private fun confirmarBorrado(id: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_borrar_nota_titulo))
            .setMessage(getString(R.string.dialog_borrar_nota_mensaje))
            .setPositiveButton(getString(R.string.dialog_btn_si_borrar)) { _, _ ->
                db.collection("notas").document(id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, getString(R.string.msg_nota_eliminada), Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton(getString(R.string.dialog_btn_cancelar), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Liberar listener de Firestore para evitar memory leaks
        firestoreListener?.remove()
        firestoreListener = null
        _binding = null  // Limpiar binding para evitar memory leaks
    }
}
