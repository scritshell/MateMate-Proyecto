package com.example.proyectoajedrez.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.proyectoajedrez.R
import com.example.proyectoajedrez.databinding.ActivityMainBinding
import com.example.proyectoajedrez.fragments.LoginDialogFragment

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionManager // Nuestra sesión

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Inicializar Sesión
        session = SessionManager(this)

        // Configuración de Navegación
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.inicioFragment,
                R.id.aperturasFragment,
                R.id.tacticasFragment,
                R.id.blocNotasFragment,
                R.id.socialFragment
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // 1. Configuración estándar del menú lateral
        binding.navView.setupWithNavController(navController)

        // 2. CORRECCIÓN DEL BUG DEL MENÚ LATERAL (¡ESTO FALTABA!)
        binding.navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.inicioFragment) {
                // Si pulsamos Inicio, limpiamos la pila hasta el principio
                navController.popBackStack(R.id.inicioFragment, false)
                binding.drawerLayout.closeDrawers()
                true
            } else {
                // Para el resto, comportamiento normal
                val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                if (handled) {
                    binding.drawerLayout.closeDrawers()
                }
                handled
            }
        }

        // Configuración del botón hamburguesa
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // LÓGICA DE INICIO: ¿Está logueado?
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        if (!session.isLoggedIn()) {
            mostrarLoginDialog()
        }
    }

    private fun mostrarLoginDialog() {
        val loginDialog = LoginDialogFragment()
        // Evitamos que se pueda cancelar pulsando fuera (isCancelable = false en el fragment)
        loginDialog.isCancelable = false
        loginDialog.show(supportFragmentManager, "LoginDialog")
    }

    // Método público para que el Dialog nos avise de actualizar el menú
    fun actualizarMenu() {
        invalidateOptionsMenu() // Esto fuerza a Android a llamar a onPrepareOptionsMenu de nuevo
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    // AQUÍ ES DONDE OCULTAMOS/MOSTRAMOS OPCIONES
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val isLoggedIn = session.isLoggedIn()

        // Si está logueado: Ocultar Login, Mostrar Logout y Ajustes
        menu?.findItem(R.id.action_login)?.isVisible = !isLoggedIn
        menu?.findItem(R.id.action_logout)?.isVisible = isLoggedIn
        menu?.findItem(R.id.action_settings)?.isVisible = isLoggedIn

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_login -> {
                mostrarLoginDialog()
                true
            }
            R.id.action_logout -> {
                session.logoutUser() // Borramos sesión
                actualizarMenu()     // Actualizamos menú (ahora saldrá "Iniciar sesión")
                mostrarLoginDialog() // Pedimos login de nuevo
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                // Navegar al fragmento de ajustes
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}