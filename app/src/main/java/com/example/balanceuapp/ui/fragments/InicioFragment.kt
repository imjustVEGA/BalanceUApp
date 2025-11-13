package com.example.balanceuapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.balanceuapp.databinding.FragmentInicioBinding
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.ui.viewmodel.InicioViewModel

class InicioFragment : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    private lateinit var inicioViewModel: InicioViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicioViewModel = ViewModelProvider(this)[InicioViewModel::class.java]
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        val userId = authViewModel.obtenerUsuarioActual()
        if (userId != null) {
            inicioViewModel.cargarResumenDelDia(userId)
        }

        setupObservers()
    }

    private fun setupObservers() {
        inicioViewModel.habitosDelDia.observe(viewLifecycleOwner) { habitos ->
            val completados = inicioViewModel.obtenerHabitossCompletados()
            val total = inicioViewModel.obtenerTotalHabitoss()
            binding.tvHabitossCompletados.text = "Hábitos completados: $completados/$total"
        }

        inicioViewModel.estadoAnimoDelDia.observe(viewLifecycleOwner) { estado ->
            if (estado != null) {
                binding.tvEstadoAnimo.text = "Estado de ánimo: ${estado.tipo.name}"
            } else {
                binding.tvEstadoAnimo.text = "Estado de ánimo: No registrado"
            }
        }

        inicioViewModel.fraseMotivacional.observe(viewLifecycleOwner) { frase ->
            if (frase != null) {
                binding.tvFraseMotivacional.text = "\"${frase.frase}\""
                binding.tvAutorFrase.text = "- ${frase.autor}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

