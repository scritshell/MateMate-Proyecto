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

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar funcional
        setSupportActionBar(binding.toolbar)

        // NavController
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Drawer + destinos de nivel superior
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

        // Menú lateral conectado al NavController
        binding.navView.setupWithNavController(navController)

        // Hamburguesa (abre/cierra menú lateral)
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    // --- AQUÍ ESTÁ EL CAMBIO PARA LOS 3 PUNTOS ---

    // 1. Inflamos el NUEVO menú (menu_toolbar) en lugar del menu_main
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    // 2. Gestionamos los clics en las opciones de los 3 puntos
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_login -> {
                // Aquí irá la lógica de abrir LoginActivity más adelante
                Toast.makeText(this, "Opción: Iniciar Sesión", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                // Aquí irá la lógica de abrir Ajustes/Preferencias
                Toast.makeText(this, "Opción: Ajustes", Toast.LENGTH_SHORT).show()
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