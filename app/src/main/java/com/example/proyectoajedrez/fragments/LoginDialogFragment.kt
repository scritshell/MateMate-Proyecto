package com.example.proyectoajedrez.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

// Fragmento de diálogo para login/registro de usuarios
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
        val btnGoogleSignIn = view.findViewById<Button>(R.id.btnGoogleSignIn) // <-- NUEVO BOTÓN DE GOOGLE
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

        // Botón principal de acción (Login o Registro Normal)
        btnAccion.setOnClickListener {
            val inputEmailUser = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val username = etUsername.text.toString().trim()

            if (inputEmailUser.isNotEmpty() && password.isNotEmpty()) {
                if (esModoRegistro) {
                    if (username.isEmpty()) {
                        Toast.makeText(context, "El nombre de usuario es obligatorio", Toast.LENGTH_SHORT).show()
                    } else {
                        verificarYRegistrar(inputEmailUser, password, username)
                    }
                } else {
                    loginInteligente(inputEmailUser, password)
                }
            } else {
                Toast.makeText(context, "Por favor, rellena los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // --- LÓGICA DEL BOTÓN DE GOOGLE ---
        btnGoogleSignIn.setOnClickListener {
            iniciarGoogleSignIn()
        }

        builder.setView(view)
        return builder.create()
    }

    // --- FUNCIONES DE GOOGLE SIGN-IN ---

    private fun iniciarGoogleSignIn() {
        // El Web Client ID se auto-genera gracias a google-services.json
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        // Lanzamos la ventana emergente de Google
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Recogemos el resultado de la ventana de Google
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                // Le pasamos las credenciales a Firebase
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(requireActivity()) { authTask ->
                        if (authTask.isSuccessful) {
                            val user = auth.currentUser
                            guardarUsuarioGoogleEnFirestore(user)

                            // Extraemos el nombre para mostrarlo en el Toast
                            val nombreParaMostrar = user?.displayName ?: user?.email?.substringBefore("@") ?: "Jugador"
                            iniciarSesionEnApp(nombreParaMostrar)

                        } else {
                            Toast.makeText(context, "Error con Google: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-In cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarUsuarioGoogleEnFirestore(user: com.google.firebase.auth.FirebaseUser?) {
        user ?: return

        // Solo creamos el documento si es su primera vez en la app
        db.collection("usuarios").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val datos = hashMapOf(
                        "username" to (user.displayName ?: user.email?.substringBefore("@") ?: "Jugador"),
                        "email" to (user.email ?: ""),
                        "elo" to 1200,
                        "fechaRegistro" to System.currentTimeMillis()
                    )
                    db.collection("usuarios").document(user.uid).set(datos)
                }
            }
    }


    // --- FUNCIONES DE LOGIN TRADICIONAL ---

    private fun verificarYRegistrar(email: String, pass: String, username: String) {
        db.collection("usuarios")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(context, "Ese nombre de usuario ya existe", Toast.LENGTH_SHORT).show()
                } else {
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                val datosUsuario = hashMapOf(
                                    "username" to username,
                                    "email" to email,
                                    "elo" to 1200,
                                    "fechaRegistro" to System.currentTimeMillis()
                                )
                                if (userId != null) {
                                    db.collection("usuarios").document(userId).set(datosUsuario)
                                }
                                Toast.makeText(context, "¡Cuenta creada!", Toast.LENGTH_SHORT).show()
                                iniciarSesionEnApp(username)
                            } else {
                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
    }

    private fun loginInteligente(input: String, pass: String) {
        if (input.contains("@")) {
            auth.signInWithEmailAndPassword(input, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        buscarUsernameYEntrar(auth.currentUser?.uid)
                    } else {
                        Toast.makeText(context, "Login fallido: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            db.collection("usuarios")
                .whereEqualTo("username", input)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    } else {
                        val email = documents.documents[0].getString("email")
                        if (email != null) {
                            auth.signInWithEmailAndPassword(email, pass)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        iniciarSesionEnApp(input)
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

    companion object {
        // Código de respuesta constante para saber que volvemos de la ventana de Google
        private const val RC_SIGN_IN = 9001
    }
}