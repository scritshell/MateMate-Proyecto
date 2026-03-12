package com.example.proyectoajedrez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.proyectoajedrez.ui.chat.ChatScreen
import com.google.android.material.color.DynamicColors

class SocialFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // En lugar de inflar un XML, creamos una vista de Compose directamente
        return ComposeView(requireContext()).apply {
            setContent {
                // Llamamos a la pantalla que acabamos de crear
                ChatScreen()
            }
        }
    }
}

/*
* TODO: La seccion social la dejare para el final, FEEDBACK:
*  Crear un FORO en lugar de un Tiktok Chess. Esto será para
*  tener 2 tipos de usuarios. Usuarios normales y corrientes
*  Y ADMINISTRADORES.
*  Intentar usar JetpackCompose.
* */