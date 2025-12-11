package com.example.proyectoajedrez.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.activities.MainActivity
import com.example.proyectoajedrez.activities.SessionManager
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginDialogFragment : DialogFragment() {

    private var esModoRegistro = false
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_login, null)

        // Referencias
        val tvTitulo = view.findViewById<TextView>(R.id.tvTitulo)
        val layoutUsername = view.findViewById<TextInputLayout>(R.id.layoutUsername)
        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        val layoutEmail = view.findViewById<TextInputLayout>(R.id.layoutEmail)
        val etEmail = view.findViewById<EditText>(R.id.etEmail) // Este campo sirve para Email O Usuario en login
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnAccion = view.findViewById<Button>(R.id.btnLogin)
        val tvCambiarModo = view.findViewById<TextView>(R.id.tvCambiarModo)

        isCancelable = false

        // 1. CAMBIAR MODO (Login <-> Registro)
        tvCambiarModo.setOnClickListener {
            esModoRegistro = !esModoRegistro

            if (esModoRegistro) {
                // MODO REGISTRO
                tvTitulo.text = "Crear Cuenta"
                btnAccion.text = "Registrarse"
                tvCambiarModo.text = "¿Ya tienes cuenta? Inicia Sesión"
                layoutUsername.visibility = View.VISIBLE // Mostramos campo Username
                layoutEmail.hint = "Email" // Aquí solo pedimos Email estricto
            } else {
                // MODO LOGIN
                tvTitulo.text = "Iniciar Sesión"
                btnAccion.text = "Entrar"
                tvCambiarModo.text = "¿No tienes cuenta? Regístrate AQUÍ"
                layoutUsername.visibility = View.GONE // Ocultamos campo Username
                layoutEmail.hint = "Email o Usuario" // Aquí vale todo
            }
        }

        // 2. BOTÓN ACCIÓN
        btnAccion.setOnClickListener {
            val inputEmailUser = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val username = etUsername.text.toString().trim()

            if (inputEmailUser.isNotEmpty() && password.isNotEmpty()) {
                if (esModoRegistro) {
                    // Validar que haya puesto username
                    if (username.isEmpty()) {
                        Toast.makeText(context, "El nombre de usuario es obligatorio", Toast.LENGTH_SHORT).show()
                    } else {
                        // Antes de registrar, verificar si el username ya existe (Opcional pero recomendado)
                        verificarYRegistrar(inputEmailUser, password, username)
                    }
                } else {
                    // LOGIN INTELIGENTE
                    loginInteligente(inputEmailUser, password)
                }
            } else {
                Toast.makeText(context, "Por favor, rellena los campos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setView(view)
        return builder.create()
    }

    // --- REGISTRO ---
    private fun verificarYRegistrar(email: String, pass: String, username: String) {
        // 1. Comprobar si el username ya está cogido en Firestore
        db.collection("usuarios")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(context, "Ese nombre de usuario ya existe", Toast.LENGTH_SHORT).show()
                } else {
                    // 2. Crear cuenta en Auth
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                val datosUsuario = hashMapOf(
                                    "username" to username, // Guardamos el nick
                                    "email" to email,
                                    "elo" to 1200,
                                    "fechaRegistro" to System.currentTimeMillis()
                                )
                                if (userId != null) {
                                    db.collection("usuarios").document(userId).set(datosUsuario)
                                }
                                Toast.makeText(context, "¡Cuenta creada!", Toast.LENGTH_SHORT).show()
                                // Iniciamos sesión guardando el Username, no el email
                                iniciarSesionEnApp(username)
                            } else {
                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
    }

    // --- LOGIN INTELIGENTE ---
    private fun loginInteligente(input: String, pass: String) {
        if (input.contains("@")) {
            // Es un Email -> Login directo
            auth.signInWithEmailAndPassword(input, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Buscar el username real en la DB para la sesión local
                        buscarUsernameYEntrar(auth.currentUser?.uid)
                    } else {
                        Toast.makeText(context, "Login fallido: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // Es un Username -> Buscar email en Firestore
            db.collection("usuarios")
                .whereEqualTo("username", input)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    } else {
                        // Encontrado! Sacamos el email
                        val email = documents.documents[0].getString("email")
                        if (email != null) {
                            // Ahora sí hacemos login con el email que encontramos
                            auth.signInWithEmailAndPassword(email, pass)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        iniciarSesionEnApp(input) // Usamos el nick que escribió
                                    } else {
                                        Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun buscarUsernameYEntrar(uid: String?) {
        if (uid == null) return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: "Jugador"
                iniciarSesionEnApp(username)
            }
    }

    private fun iniciarSesionEnApp(nombre: String) {
        val session = SessionManager(requireContext())
        session.createLoginSession(nombre)
        (activity as? MainActivity)?.actualizarMenu()
        Toast.makeText(context, "Bienvenido, $nombre", Toast.LENGTH_SHORT).show()
        dismiss()
    }
}