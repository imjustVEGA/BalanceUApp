package com.example.balanceuapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.balanceuapp.databinding.ActivityMainBinding
import com.example.balanceuapp.ui.auth.AuthActivity
import com.example.balanceuapp.ui.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

            // Verificar si el usuario está autenticado
            try {
                if (!authViewModel.verificarSesion()) {
                    val intent = Intent(this, AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error al verificar sesión: ${e.message}", e)
                // Si hay error verificando sesión, redirigir a login
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return
            }

            setupNavigation()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error en onCreate: ${e.message}", e)
            e.printStackTrace()
            // Mostrar mensaje de error al usuario
            android.widget.Toast.makeText(
                this,
                "Error al inicializar la aplicación. Verifica la configuración de Firebase.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun setupNavigation() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as? NavHostFragment
                ?: throw IllegalStateException("NavHostFragment no encontrado")
            
            val navController = navHostFragment.navController
            binding.bottomNavigationView.setupWithNavController(navController)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error en setupNavigation: ${e.message}", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                authViewModel.cerrarSesion()
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
