package com.example.balanceuapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.balanceuapp.databinding.FragmentRegisterBinding
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.util.Constants

/**
 * Fragment que maneja el registro de nuevos usuarios.
 */
class RegisterFragment : Fragment() {
    
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRegisterButton()
    }

    /**
     * Configura el botón de registro con validación de campos.
     */
    private fun setupRegisterButton() {
        binding.btnRegister.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validarCampos(nombre, email, password)) {
                return@setOnClickListener
            }

            (requireActivity() as? AuthActivity)?.showProgress()
            viewModel.registrarUsuario(email, password, nombre)
        }
    }

    /**
     * Valida que los campos de registro no estén vacíos y que la contraseña tenga la longitud mínima.
     * 
     * @param nombre Nombre ingresado
     * @param email Email ingresado
     * @param password Contraseña ingresada
     * @return true si los campos son válidos, false en caso contrario
     */
    private fun validarCampos(nombre: String, email: String, password: String): Boolean {
        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), Constants.UIMessages.COMPLETAR_CAMPOS, Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < Constants.Validation.PASSWORD_MIN_LENGTH) {
            Toast.makeText(requireContext(), Constants.UIMessages.PASSWORD_CORTA, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

