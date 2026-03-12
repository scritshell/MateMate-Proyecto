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
import com.example.proyectoajedrez.fragments.GameSetupDialogFragment
import com.example.proyectoajedrez.fragments.LoginDialogFragment


// El contenedor principal del proyecto. Patrón utilizado: Single Activity Architecture.

class   MainActivity : AppCompatActivity() {

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

        // Definir los fragmentos
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.inicioFragment,
                R.id.aperturasFragment,
                R.id.puzzleDiarioFragment,
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
            // Cerrar el drawer primero para mejor UX
            binding.drawerLayout.closeDrawers()

            when (item.itemId) {
                R.id.inicioFragment -> {
                    navController.popBackStack(R.id.inicioFragment, false)
                    true
                }

                // INTERCEPTAMOS LA NAVEGACIÓN A PRÁCTICA LIBRE.
                R.id.chessBoardFragment -> {
                    // En lugar de navegar directo, mostramos diálogo
                    // Con eso no se navega directamente al tablero.
                    val dialog = GameSetupDialogFragment { modo, side, dif, tiempo ->

                        // El Callback
                        // Teniendo los datos ya elegidos por parte del usuario, se envian
                        // usanod Bundle.
                        val bundle = Bundle().apply {
                            putString("modo", modo)          // "libre" o "local_2p"
                            putString("side", side)          // "WHITE", "BLACK" o "BOTH"
                            putInt("difficulty", dif)        // 1-20 STOCKFISH!
                            putInt("timeIndex", tiempo)      // Indice del array de tiempos
                        }

                        // Limpiamos la pila hasta inicio y navegamos con los argumentos
                        navController.popBackStack(R.id.inicioFragment, false)
                        navController.navigate(R.id.chessBoardFragment, bundle)
                    }
                    dialog.show(supportFragmentManager, "GameSetup")

                    // Devolvemos un false para no marcar el item como seleccionado visualmente hasta navegar
                    false
                }

                else -> {
                    // Navegación normal para otros fragments (Aperturas, Tácticas...)
                    navController.popBackStack(R.id.inicioFragment, false)
                    navController.navigate(item.itemId)
                    true
                }
            }
        }

        // Configurar el menu lateral.
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Verificar el estado de login al iniciar
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
        // Diálogo no cancelable (el login es OBLIGATORIO.)
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

    // Manejar clicks en los items del menú de toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_login -> {
                mostrarLoginDialog()
                true
            }
            R.id.action_logout -> {
                session.logoutUser() // Cerrar sesión
                actualizarMenu()     // Actualizar toolbar
                mostrarLoginDialog() // Pedire el login nuevamente.
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

/*
* TODO: GLOBAL
*  IDEAS: Paso 1: El Puzzle Diario : COMPLETADO <=================================
*  Sustituir los puzzles estáticos por el endpoint /api/puzzle/daily.
*  Implementación de Retrofit para manejar el JSON dinámico.
*  Objetivo: Que el tablero se inicialice cada día con un reto nuevo y real.
*  -------------------------------------------------------------------------
*  Paso 2: Explorador de Aperturas (Análisis Real) COMPLETADO <=================================
*  Consumo de la API /masters enviando el FEN actual tras cada movimiento.
*  Añadir un RecyclerView que muestre jugadas probables
*  y porcentajes de victoria de Grandes Maestros.
*  Objetivo: Convertir el modo libre en una herramienta de estudio de alto nivel.
*  --------------------------------------------------------------------------
*  Paso 3: Sincronización de Perfil
*  Mostrar ELO real y estadísticas en el InicioFragment.
*  Objetivo: Gamificación y personalización real de la cuenta del usuario, que no sea estatico
*
*  TODO EXTRA: Meter personalizacion de skin de las piezas.
* */
