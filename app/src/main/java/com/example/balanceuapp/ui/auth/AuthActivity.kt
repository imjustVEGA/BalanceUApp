package com.example.balanceuapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.balanceuapp.MainActivity
import com.example.balanceuapp.R
import com.example.balanceuapp.databinding.ActivityAuthBinding
import com.example.balanceuapp.ui.auth.LoginFragment
import com.example.balanceuapp.ui.auth.RegisterFragment
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.util.Constants
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Activity que maneja la autenticación de usuarios (login y registro).
 * Utiliza un ViewPager2 con tabs para alternar entre login y registro.
 */
class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityAuthBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupViewPager()
            setupObservers()
        } catch (e: Exception) {
            Log.e(Constants.LogTags.AUTH_ACTIVITY, "Error en onCreate: ${e.message}", e)
            Toast.makeText(
                this,
                "Error al inicializar: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    /**
     * Configura el ViewPager2 con los fragments de login y registro.
     */
    private fun setupViewPager() {
        val adapter = AuthPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Iniciar sesión"
                1 -> "Registrarse"
                else -> ""
            }
        }.attach()
    }

    /**
     * Configura los observadores de LiveData del ViewModel.
     */
    private fun setupObservers() {
        viewModel.loginResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { userId ->
                Toast.makeText(this, Constants.SuccessMessages.LOGIN_EXITOSO, Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.registroResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { userId ->
                Toast.makeText(this, Constants.SuccessMessages.REGISTRO_EXITOSO, Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Muestra el indicador de progreso.
     */
    fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
    }

    /**
     * Navega a la MainActivity y limpia el stack de actividades.
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Adapter para el ViewPager2 que maneja los fragments de autenticación.
     */
    private class AuthPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LoginFragment()
                1 -> RegisterFragment()
                else -> LoginFragment()
            }
        }
    }
}

