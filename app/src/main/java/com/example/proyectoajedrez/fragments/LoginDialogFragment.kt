package com.example.proyectoajedrez.fragments

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
import com.example.proyectoajedrez.domain.model.UserRole
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginDialogFragment : DialogFragment() {

    private var esModoRegistro = false
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Referencias a vistas que necesitamos fuera de onCreateDialog
    private var tvTitulo: TextView? = null
    private var btnAccion: Button? = null
    private var tvCambiarModo: TextView? = null
    private var layoutUsername: TextInputLayout? = null
    private var layoutEmail: TextInputLayout? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_login, null)

        // Referencias a vistas
        tvTitulo = view.findViewById(R.id.tvTitulo)
        layoutUsername = view.findViewById(R.id.layoutUsername)
        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        layoutEmail = view.findViewById(R.id.layoutEmail)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        btnAccion = view.findViewById(R.id.btnLogin)
        val btnGoogleSignIn = view.findViewById<Button>(R.id.btnGoogleSignIn)
        tvCambiarModo = view.findViewById(R.id.tvCambiarModo)

        isCancelable = false

        // Alternar entre modo login y registro
        tvCambiarModo?.setOnClickListener {
            esModoRegistro = !esModoRegistro
            actualizarModoUI()
        }

        // Botón principal
        btnAccion?.setOnClickListener {
            val inputEmailUser = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val username = etUsername.text.toString().trim()

            if (inputEmailUser.isEmpty() || password.isEmpty()) {
                mostrarToast(getString(R.string.msg_rellena_campos))
                return@setOnClickListener
            }

            if (esModoRegistro) {
                if (username.isEmpty()) {
                    mostrarToast(getString(R.string.msg_username_obligatorio))
                } else {
                    verificarYRegistrar(inputEmailUser, password, username)
                }
            } else {
                loginInteligente(inputEmailUser, password)
            }
        }

        btnGoogleSignIn?.setOnClickListener {
            iniciarGoogleSignIn()
        }

        builder.setView(view)
        return builder.create()
    }

    // Centraliza el cambio de UI al alternar modos
    private fun actualizarModoUI() {
        if (esModoRegistro) {
            tvTitulo?.text = getString(R.string.login_crear_cuenta)
            btnAccion?.text = getString(R.string.btn_registrarse)
            tvCambiarModo?.text = getString(R.string.ir_a_login)
            layoutUsername?.visibility = View.VISIBLE
            layoutEmail?.hint = getString(R.string.login_hint_email)
        } else {
            tvTitulo?.text = getString(R.string.login_iniciar_sesion)
            btnAccion?.text = getString(R.string.btn_entrar)
            tvCambiarModo?.text = getString(R.string.ir_a_registro)
            layoutUsername?.visibility = View.GONE
            layoutEmail?.hint = getString(R.string.login_hint_email_usuario)
        }
    }

    // --- REGISTRO ---

    private fun verificarYRegistrar(email: String, pass: String, username: String) {
        db.collection("usuarios")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                // Verificamos que el fragment sigue activo antes de tocar la UI
                if (!isAdded) return@addOnSuccessListener

                if (!documents.isEmpty) {
                    mostrarToast(getString(R.string.msg_usuario_existe))
                } else {
                    crearCuentaFirebase(email, pass, username)
                }
            }
            .addOnFailureListener { exception ->
                // ESTE BLOQUE ERA EL QUE FALTABA — sin él, los errores de Firestore
                // (reglas de seguridad, red, etc.) se tragaban en silencio
                if (!isAdded) return@addOnFailureListener
                mostrarToast(getString(R.string.msg_error_conexion))
            }
    }

    private fun crearCuentaFirebase(email: String, pass: String, username: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(requireActivity()) { task ->
                // requireActivity() como executor garantiza que el callback
                // se ejecuta en el hilo principal y ligado al ciclo de vida
                if (!isAdded) return@addOnCompleteListener

                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        guardarUsuarioEnFirestore(userId, username, email)
                            .addOnSuccessListener {
                                if (!isAdded) return@addOnSuccessListener
                                mostrarToast(getString(R.string.msg_cuenta_creada))
                                iniciarSesionEnApp(username, userId, UserRole.USER)
                            }
                            .addOnFailureListener {
                                if (!isAdded) return@addOnFailureListener
                                mostrarToast(getString(R.string.msg_error_conexion))
                            }
                    } else {
                        mostrarToast(getString(R.string.msg_error_conexion))
                    }
                } else {
                    val mensaje = task.exception?.message ?: ""
                    mostrarToast(getString(R.string.msg_error_login, mensaje))
                }
            }
    }

    private fun guardarUsuarioEnFirestore(userId: String, username: String, email: String) =
        db.collection("usuarios").document(userId).set(
            hashMapOf(
                "uid" to userId,
                "username" to username,
                "email" to email,
                "elo" to 1200,
                "role" to UserRole.USER.name,
                "createdAt" to System.currentTimeMillis(),
                "fechaRegistro" to System.currentTimeMillis()
            )
        )

    private fun parseUserRole(role: String?): UserRole {
        return runCatching {
            UserRole.valueOf(role?.uppercase() ?: UserRole.USER.name)
        }.getOrDefault(UserRole.USER)
    }

    private fun getDocumentRole(document: com.google.firebase.firestore.DocumentSnapshot): UserRole {
        return parseUserRole(document.getString("role"))
    }

    private fun getDocumentUsername(document: com.google.firebase.firestore.DocumentSnapshot, fallback: String): String {
        return document.getString("username")?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun crearDatosUsuarioGoogle(user: com.google.firebase.auth.FirebaseUser): HashMap<String, Any> {
        val username = user.displayName ?: user.email?.substringBefore("@") ?: getString(R.string.default_player_name)
        return hashMapOf(
            "uid" to user.uid,
            "username" to username,
            "email" to (user.email ?: ""),
            "elo" to 1200,
            "role" to UserRole.USER.name,
            "createdAt" to System.currentTimeMillis(),
            "fechaRegistro" to System.currentTimeMillis()
        )
    }

    private fun entrarConUsuarioGoogle(user: com.google.firebase.auth.FirebaseUser?) {
        user ?: return
        val fallbackName = user.displayName ?: user.email?.substringBefore("@") ?: getString(R.string.default_player_name)
        val userDoc = db.collection("usuarios").document(user.uid)

        userDoc.get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                if (doc.exists()) {
                    iniciarSesionEnApp(
                        getDocumentUsername(doc, fallbackName),
                        user.uid,
                        getDocumentRole(doc)
                    )
                } else {
                    userDoc.set(crearDatosUsuarioGoogle(user))
                        .addOnSuccessListener {
                            if (!isAdded) return@addOnSuccessListener
                            iniciarSesionEnApp(fallbackName, user.uid, UserRole.USER)
                        }
                        .addOnFailureListener {
                            if (!isAdded) return@addOnFailureListener
                            mostrarToast(getString(R.string.msg_error_conexion))
                        }
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                mostrarToast(getString(R.string.msg_error_conexion))
            }
    }

    // --- LOGIN ---

    private fun loginInteligente(input: String, pass: String) {
        if (input.contains("@")) {
            auth.signInWithEmailAndPassword(input, pass)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (!isAdded) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        buscarUsernameYEntrar(auth.currentUser?.uid)
                    } else {
                        val mensaje = task.exception?.message ?: ""
                        mostrarToast(getString(R.string.msg_login_fallido, mensaje))
                    }
                }
        } else {
            db.collection("usuarios")
                .whereEqualTo("username", input)
                .get()
                .addOnSuccessListener { documents ->
                    if (!isAdded) return@addOnSuccessListener
                    if (documents.isEmpty) {
                        mostrarToast(getString(R.string.msg_usuario_no_encontrado))
                        return@addOnSuccessListener
                    }
                    val email = documents.documents[0].getString("email")
                    val document = documents.documents[0]
                    val username = getDocumentUsername(document, input)
                    val role = getDocumentRole(document)
                    if (email != null) {
                        auth.signInWithEmailAndPassword(email, pass)
                            .addOnCompleteListener(requireActivity()) { task ->
                                if (!isAdded) return@addOnCompleteListener
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid ?: document.id
                                    iniciarSesionEnApp(username, uid, role)
                                } else {
                                    mostrarToast(getString(R.string.msg_password_incorrecta))
                                }
                            }
                    } else {
                        mostrarToast(getString(R.string.msg_error_conexion))
                    }
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    mostrarToast(getString(R.string.msg_error_conexion))
                }
        }
    }

    private fun buscarUsernameYEntrar(uid: String?) {
        if (uid == null) return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                val username = getDocumentUsername(document, getString(R.string.default_player_name))
                iniciarSesionEnApp(username, uid, getDocumentRole(document))
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                // Entramos igual aunque no encontremos el username en Firestore
                iniciarSesionEnApp(
                    auth.currentUser?.email?.substringBefore("@") ?: getString(R.string.default_player_name),
                    uid,
                    UserRole.USER
                )
            }
    }

    // --- GOOGLE SIGN-IN ---

    private fun iniciarGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        @Suppress("DEPRECATION")
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != RC_SIGN_IN) return

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity()) { authTask ->
                    if (!isAdded) return@addOnCompleteListener
                    if (authTask.isSuccessful) {
                        val user = auth.currentUser
                        entrarConUsuarioGoogle(user)
                    } else {
                        val mensaje = authTask.exception?.message ?: ""
                        mostrarToast(getString(R.string.msg_error_google, mensaje))
                    }
                }
        } catch (e: ApiException) {
            mostrarToast(getString(R.string.msg_google_cancelado))
        }
    }

    // --- HELPERS ---

    private fun iniciarSesionEnApp(nombre: String, uid: String, role: UserRole) {
        val session = SessionManager(requireContext())
        session.createLoginSession(nombre, uid, role)
        (activity as? MainActivity)?.actualizarMenu()
        mostrarToast(getString(R.string.msg_bienvenida, nombre))
        dismiss()
    }

    // Centraliza los Toasts con comprobación de isAdded
    private fun mostrarToast(mensaje: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
