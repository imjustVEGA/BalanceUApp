package com.example.balanceuapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.balanceuapp.data.model.EstadoAnimo
import com.example.balanceuapp.data.model.TipoEstadoAnimo
import com.example.balanceuapp.databinding.FragmentEstadoAnimoBinding
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.ui.viewmodel.EstadoAnimoViewModel

class EstadoAnimoFragment : Fragment() {
    private var _binding: FragmentEstadoAnimoBinding? = null
    private val binding get() = _binding!!
    private lateinit var estadoAnimoViewModel: EstadoAnimoViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstadoAnimoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        estadoAnimoViewModel = ViewModelProvider(this)[EstadoAnimoViewModel::class.java]
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        val userId = authViewModel.obtenerUsuarioActual()
        if (userId != null) {
            val fechaActual = System.currentTimeMillis()
            estadoAnimoViewModel.obtenerEstadoAnimoDelDia(userId, fechaActual)
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        estadoAnimoViewModel.estadoAnimoActual.observe(viewLifecycleOwner) { estado ->
            if (estado != null) {
                binding.cardEstadoAnimoActual.visibility = View.VISIBLE
                binding.tvEstadoAnimoActual.text = "Estado actual: ${estado.tipo.name}\n${if (estado.nota.isNotEmpty()) "Nota: ${estado.nota}" else ""}"
                
                // Seleccionar el radio button correspondiente
                when (estado.tipo) {
                    TipoEstadoAnimo.MUY_FELIZ -> binding.rgEstadoAnimo.check(binding.rbMuyFeliz.id)
                    TipoEstadoAnimo.FELIZ -> binding.rgEstadoAnimo.check(binding.rbFeliz.id)
                    TipoEstadoAnimo.NEUTRAL -> binding.rgEstadoAnimo.check(binding.rbNeutral.id)
                    TipoEstadoAnimo.TRISTE -> binding.rgEstadoAnimo.check(binding.rbTriste.id)
                    TipoEstadoAnimo.MUY_TRISTE -> binding.rgEstadoAnimo.check(binding.rbMuyTriste.id)
                }
                binding.etNota.setText(estado.nota)
            } else {
                binding.cardEstadoAnimoActual.visibility = View.GONE
            }
        }

        estadoAnimoViewModel.operacionExitosa.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(requireContext(), "Estado de ánimo guardado", Toast.LENGTH_SHORT).show()
            }
        }

        estadoAnimoViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnGuardarEstadoAnimo.setOnClickListener {
            val selectedId = binding.rgEstadoAnimo.checkedRadioButtonId
            val tipo = when (selectedId) {
                binding.rbMuyFeliz.id -> TipoEstadoAnimo.MUY_FELIZ
                binding.rbFeliz.id -> TipoEstadoAnimo.FELIZ
                binding.rbNeutral.id -> TipoEstadoAnimo.NEUTRAL
                binding.rbTriste.id -> TipoEstadoAnimo.TRISTE
                binding.rbMuyTriste.id -> TipoEstadoAnimo.MUY_TRISTE
                else -> {
                    Toast.makeText(requireContext(), "Selecciona un estado de ánimo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val nota = binding.etNota.text.toString().trim()
            val userId = authViewModel.obtenerUsuarioActual()

            if (userId != null) {
                val estadoAnimo = EstadoAnimo(
                    usuarioId = userId,
                    tipo = tipo,
                    nota = nota,
                    fecha = System.currentTimeMillis()
                )
                estadoAnimoViewModel.registrarEstadoAnimo(estadoAnimo)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

