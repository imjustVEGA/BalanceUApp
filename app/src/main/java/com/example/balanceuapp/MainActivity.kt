package com.example.balanceuapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.balanceuapp.databinding.ActivityMainBinding
import com.example.balanceuapp.ui.auth.AuthActivity
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.util.Constants

/**
 * Activity principal de la aplicación.
 * Gestiona la navegación entre fragments y verifica la autenticación del usuario.
 */
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
            if (!verificarAutenticacion()) {
                return
            }

            setupNavigation()
        } catch (e: Exception) {
            Log.e(Constants.LogTags.MAIN_ACTIVITY, "Error en onCreate: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(
                this,
                Constants.ErrorMessages.ERROR_INICIALIZAR_APLICACION,
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    /**
     * Verifica si el usuario está autenticado.
     * Si no lo está, redirige a AuthActivity.
     * 
     * @return true si el usuario está autenticado, false en caso contrario
     */
    private fun verificarAutenticacion(): Boolean {
        return try {
            if (!authViewModel.verificarSesion()) {
                redirigirALogin()
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(Constants.LogTags.MAIN_ACTIVITY, "Error al verificar sesión: ${e.message}", e)
            redirigirALogin()
            false
        }
    }

    /**
     * Redirige al usuario a la pantalla de autenticación.
     */
    private fun redirigirALogin() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Configura la navegación entre fragments usando Navigation Component.
     */
    private fun setupNavigation() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as? NavHostFragment
                ?: throw IllegalStateException("NavHostFragment no encontrado")
            
            val navController = navHostFragment.navController
            binding.bottomNavigationView.setupWithNavController(navController)
        } catch (e: Exception) {
            Log.e(Constants.LogTags.MAIN_ACTIVITY, "Error en setupNavigation: ${e.message}", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                cerrarSesion()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Cierra la sesión del usuario y redirige a la pantalla de autenticación.
     */
    private fun cerrarSesion() {
        authViewModel.cerrarSesion()
        redirigirALogin()
    }
}
