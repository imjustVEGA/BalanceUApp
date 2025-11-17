package com.example.balanceuapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.balanceuapp.R
import com.example.balanceuapp.databinding.FragmentPerfilBinding
import com.example.balanceuapp.ui.auth.AuthActivity
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        setupUserInfo()
        setupListeners()
    }

    private fun setupUserInfo() {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                binding.tvNombreUsuario.text = currentUser.displayName ?: "Usuario"
                binding.tvEmailUsuario.text = currentUser.email ?: "usuario@email.com"
            } else {
                binding.tvNombreUsuario.text = "Usuario"
                binding.tvEmailUsuario.text = "usuario@email.com"
            }
        } catch (e: Exception) {
            android.util.Log.e("PerfilFragment", "Error al cargar información del usuario: ${e.message}", e)
        }
    }

    private fun setupListeners() {
        binding.cardCerrarSesion.setOnClickListener {
            authViewModel.cerrarSesion()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

