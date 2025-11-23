package com.example.balanceuapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.balanceuapp.databinding.FragmentLoginBinding
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.util.Constants

/**
 * Fragment que maneja el inicio de sesión de usuarios.
 */
class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLoginButton()
    }

    /**
     * Configura el botón de login con validación de campos.
     */
    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validarCampos(email, password)) {
                return@setOnClickListener
            }

            (requireActivity() as? AuthActivity)?.showProgress()
            viewModel.iniciarSesion(email, password)
        }
    }

    /**
     * Valida que los campos de email y contraseña no estén vacíos.
     * 
     * @param email Email ingresado
     * @param password Contraseña ingresada
     * @return true si los campos son válidos, false en caso contrario
     */
    private fun validarCampos(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), Constants.UIMessages.COMPLETAR_CAMPOS, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

