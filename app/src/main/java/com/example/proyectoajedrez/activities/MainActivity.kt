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

    // Configuración para la barra de la app con Navigation Component
    private lateinit var appBarConfiguration: AppBarConfiguration

    // ViewBinding para la actividad principal
    private lateinit var binding: ActivityMainBinding

    // Gestor de sesión de usuario
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar layout usando ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar personalizada como ActionBar
        setSupportActionBar(binding.toolbar)

        // Inicializar gestor de sesión
        session = SessionManager(this)

        // Configurar Navigation Component
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Definir fragmentos de nivel superior y drawer layout
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

        // Configurar ActionBar con Navigation Controller
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Configurar NavigationView con Navigation Component
        binding.navView.setupWithNavController(navController)

        // Listener personalizado para items del menú lateral
        binding.navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.inicioFragment) {
                // Navegar a inicio limpiando la pila
                navController.popBackStack(R.id.inicioFragment, false)
                binding.drawerLayout.closeDrawers()
                true
            } else {
                // Navegación normal para otros fragments
                val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                if (handled) {
                    binding.drawerLayout.closeDrawers()
                }
                handled
            }
        }

        // Configurar menu lateral
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Verificar estado de login al iniciar
        checkLoginStatus()
    }

    // Verificar si el usuario está logueado
    private fun checkLoginStatus() {
        if (!session.isLoggedIn()) {
            mostrarLoginDialog()
        }
    }

    // Mostrar diálogo de login
    private fun mostrarLoginDialog() {
        val loginDialog = LoginDialogFragment()
        // Diálogo no cancelable (obligatorio login)
        loginDialog.isCancelable = false
        loginDialog.show(supportFragmentManager, "LoginDialog")
    }

    // Actualizar menú de toolbar (llamado desde LoginDialogFragment)
    fun actualizarMenu() {
        invalidateOptionsMenu() // Forzar recreación del menú
    }

    // Inflar menú de la toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    // Configurar visibilidad de items según estado de login
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val isLoggedIn = session.isLoggedIn()

        // Mostrar/ocultar items basado en login
        menu?.findItem(R.id.action_login)?.isVisible = !isLoggedIn
        menu?.findItem(R.id.action_logout)?.isVisible = isLoggedIn
        menu?.findItem(R.id.action_settings)?.isVisible = isLoggedIn

        return super.onPrepareOptionsMenu(menu)
    }

    // Manejar clicks en items del menú de toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_login -> {
                mostrarLoginDialog()
                true
            }
            R.id.action_logout -> {
                session.logoutUser() // Cerrar sesión
                actualizarMenu()     // Actualizar toolbar
                mostrarLoginDialog() // Pedir login nuevamente
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                // Navegar a fragmento de ajustes
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Manejar botón de navegación hacia arriba
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}