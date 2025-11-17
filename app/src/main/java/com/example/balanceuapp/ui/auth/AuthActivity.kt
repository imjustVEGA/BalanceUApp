package com.example.balanceuapp.ui.auth

import android.content.Intent
import android.os.Bundle
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
import com.google.android.material.tabs.TabLayoutMediator

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
            android.util.Log.e("AuthActivity", "Error en onCreate: ${e.message}", e)
            Toast.makeText(
                this,
                "Error al inicializar: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

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

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { userId ->
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.registroResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { userId ->
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

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

