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

// Fragmento de diálogo para login/registro de usuarios
// FINALIZADO
class LoginDialogFragment : DialogFragment() {

    private var esModoRegistro = false                // Controla si estamos en registro o login
    private val auth = FirebaseAuth.getInstance()     // Autenticación Firebase
    private val db = FirebaseFirestore.getInstance()  // Base de datos Firestore

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_login, null)

        // Referencias a elementos de la vista
        val tvTitulo = view.findViewById<TextView>(R.id.tvTitulo)
        val layoutUsername = view.findViewById<TextInputLayout>(R.id.layoutUsername)
        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        val layoutEmail = view.findViewById<TextInputLayout>(R.id.layoutEmail)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)  // Email en registro, Email/Usuario en login
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnAccion = view.findViewById<Button>(R.id.btnLogin)
        val tvCambiarModo = view.findViewById<TextView>(R.id.tvCambiarModo)

        isCancelable = false  // Diálogo no cancelable (obligatorio login)

        // Cambiar entre modos Login y Registro
        tvCambiarModo.setOnClickListener {
            esModoRegistro = !esModoRegistro

            if (esModoRegistro) {
                // Configurar interfaz para registro
                tvTitulo.text = "Crear Cuenta"
                btnAccion.text = "Registrarse"
                tvCambiarModo.text = "¿Ya tienes cuenta? Inicia Sesión"
                layoutUsername.visibility = View.VISIBLE  // Mostrar campo username
                layoutEmail.hint = "Email"                // Solo email en registro
            } else {
                // Configurar interfaz para login
                tvTitulo.text = "Iniciar Sesión"
                btnAccion.text = "Entrar"
                tvCambiarModo.text = "¿No tienes cuenta? Regístrate AQUÍ"
                layoutUsername.visibility = View.GONE     // Ocultar campo username
                layoutEmail.hint = "Email o Usuario"      // Acepta email o username
            }
        }

        // Botón principal de acción (Login o Registro)
        btnAccion.setOnClickListener {
            val inputEmailUser = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val username = etUsername.text.toString().trim()

            if (inputEmailUser.isNotEmpty() && password.isNotEmpty()) {
                if (esModoRegistro) {
                    // Validar que el username no esté vacío en registro
                    if (username.isEmpty()) {
                        Toast.makeText(context, "El nombre de usuario es obligatorio", Toast.LENGTH_SHORT).show()
                    } else {
                        verificarYRegistrar(inputEmailUser, password, username)
                    }
                } else {
                    // Realizar login inteligente (email o username)
                    loginInteligente(inputEmailUser, password)
                }
            } else {
                Toast.makeText(context, "Por favor, rellena los campos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setView(view)
        return builder.create()
    }

    // Registrar nuevo usuario verificando disponibilidad de username
    private fun verificarYRegistrar(email: String, pass: String, username: String) {
        // Verificar si el username ya existe en Firestore
        db.collection("usuarios")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(context, "Ese nombre de usuario ya existe", Toast.LENGTH_SHORT).show()
                } else {
                    // Crear cuenta en Firebase Auth
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                val datosUsuario = hashMapOf(
                                    "username" to username,  // Nick personalizado
                                    "email" to email,
                                    "elo" to 1200,          // Puntuación inicial
                                    "fechaRegistro" to System.currentTimeMillis()
                                )
                                if (userId != null) {
                                    // Guardar datos adicionales en Firestore
                                    db.collection("usuarios").document(userId).set(datosUsuario)
                                }
                                Toast.makeText(context, "¡Cuenta creada!", Toast.LENGTH_SHORT).show()
                                iniciarSesionEnApp(username)  // Iniciar sesión con el username
                            } else {
                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
    }

    // Login inteligente que acepta email o username
    private fun loginInteligente(input: String, pass: String) {
        if (input.contains("@")) {
            // Si contiene @, asumimos que es email
            auth.signInWithEmailAndPassword(input, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        buscarUsernameYEntrar(auth.currentUser?.uid)
                    } else {
                        Toast.makeText(context, "Login fallido: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // Si no contiene @, buscar email asociado al username
            db.collection("usuarios")
                .whereEqualTo("username", input)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    } else {
                        // Obtener email asociado al username
                        val email = documents.documents[0].getString("email")
                        if (email != null) {
                            auth.signInWithEmailAndPassword(email, pass)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        iniciarSesionEnApp(input)  // Usar el username ingresado
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

    // Obtener username desde Firestore para usuarios que iniciaron con email
    private fun buscarUsernameYEntrar(uid: String?) {
        if (uid == null) return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: "Jugador"
                iniciarSesionEnApp(username)
            }
    }

    // Iniciar sesión en la app local (SharedPreferences)
    private fun iniciarSesionEnApp(nombre: String) {
        val session = SessionManager(requireContext())
        session.createLoginSession(nombre)          // Guardar sesión local
        (activity as? MainActivity)?.actualizarMenu() // Actualizar menú de la actividad principal
        Toast.makeText(context, "Bienvenido, $nombre", Toast.LENGTH_SHORT).show()
        dismiss()  // Cerrar diálogo
    }
}