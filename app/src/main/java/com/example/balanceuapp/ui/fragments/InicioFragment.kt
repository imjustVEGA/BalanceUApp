package com.example.balanceuapp.ui.fragments

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.balanceuapp.R
import com.example.balanceuapp.data.model.EstadoAnimo
import com.example.balanceuapp.data.model.TipoEstadoAnimo
import com.example.balanceuapp.databinding.FragmentInicioBinding
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.ui.viewmodel.EstadoAnimoViewModel
import com.example.balanceuapp.ui.viewmodel.InicioViewModel

class InicioFragment : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    private lateinit var inicioViewModel: InicioViewModel
    private lateinit var estadoAnimoViewModel: EstadoAnimoViewModel
    private lateinit var authViewModel: AuthViewModel
    private var selectedMood: TipoEstadoAnimo? = null

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
        estadoAnimoViewModel = ViewModelProvider(this)[EstadoAnimoViewModel::class.java]
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        val userId = authViewModel.obtenerUsuarioActual()
        if (userId != null) {
            inicioViewModel.cargarResumenDelDia(userId)
            val fechaActual = System.currentTimeMillis()
            estadoAnimoViewModel.obtenerEstadoAnimoDelDia(userId, fechaActual)
        }

        setupMoodSelector()
        setupListeners()
        setupObservers()
    }

    private fun setupMoodSelector() {
        val moodViews = mapOf(
            TipoEstadoAnimo.MUY_FELIZ to binding.moodMuyFeliz,
            TipoEstadoAnimo.FELIZ to binding.moodFeliz,
            TipoEstadoAnimo.NEUTRAL to binding.moodNeutral,
            TipoEstadoAnimo.TRISTE to binding.moodTriste,
            TipoEstadoAnimo.MUY_TRISTE to binding.moodMuyTriste
        )

        moodViews.forEach { (tipo, view) ->
            view.setOnClickListener {
                selectedMood = tipo
                updateMoodSelection(moodViews)
            }
        }
    }

    private fun updateMoodSelection(moodViews: Map<TipoEstadoAnimo, View>) {
        moodViews.forEach { (tipo, view) ->
            val background = if (tipo == selectedMood) {
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.cornerRadius = 16f
                drawable.setColor(ContextCompat.getColor(requireContext(), getMoodColor(tipo)))
                drawable
            } else {
                null
            }
            view.background = background
        }
    }

    private fun getMoodColor(tipo: TipoEstadoAnimo): Int {
        return when (tipo) {
            TipoEstadoAnimo.MUY_FELIZ -> R.color.mood_very_happy
            TipoEstadoAnimo.FELIZ -> R.color.mood_happy
            TipoEstadoAnimo.NEUTRAL -> R.color.mood_neutral
            TipoEstadoAnimo.TRISTE -> R.color.mood_sad
            TipoEstadoAnimo.MUY_TRISTE -> R.color.mood_very_sad
        }
    }

    private fun setupListeners() {
        binding.btnGuardarEstadoAnimo.setOnClickListener {
            val tipo = selectedMood
            if (tipo == null) {
                Toast.makeText(requireContext(), "Selecciona un estado de ánimo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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

        // Navegación a accesos rápidos
        binding.cardSaludFisica.setOnClickListener {
            findNavController().navigate(R.id.nav_salud_fisica)
        }

        binding.cardSaludMental.setOnClickListener {
            findNavController().navigate(R.id.nav_salud_mental)
        }

        binding.cardHabitos.setOnClickListener {
            findNavController().navigate(R.id.nav_habitos_estadisticas)
        }

        binding.cardPerfil.setOnClickListener {
            findNavController().navigate(R.id.nav_perfil)
        }
    }

    private fun setupObservers() {
        inicioViewModel.habitosDelDia.observe(viewLifecycleOwner) { habitos ->
            val completados = inicioViewModel.obtenerHabitossCompletados()
            val total = inicioViewModel.obtenerTotalHabitoss()
            binding.tvHabitossCompletados.text = "Hábitos: $completados/$total"
        }

        inicioViewModel.estadoAnimoDelDia.observe(viewLifecycleOwner) { estado ->
            if (estado != null) {
                binding.tvEstadoAnimo.text = "Estado: ${estado.tipo.name}"
                selectedMood = estado.tipo
                updateMoodSelection(mapOf(
                    TipoEstadoAnimo.MUY_FELIZ to binding.moodMuyFeliz,
                    TipoEstadoAnimo.FELIZ to binding.moodFeliz,
                    TipoEstadoAnimo.NEUTRAL to binding.moodNeutral,
                    TipoEstadoAnimo.TRISTE to binding.moodTriste,
                    TipoEstadoAnimo.MUY_TRISTE to binding.moodMuyTriste
                ))
                binding.etNota.setText(estado.nota)
            } else {
                binding.tvEstadoAnimo.text = "Estado: -"
            }
        }

        inicioViewModel.fraseMotivacional.observe(viewLifecycleOwner) { frase ->
            if (frase != null) {
                binding.tvRecomendacion.text = "\"${frase.frase}\" - ${frase.autor}"
            } else {
                binding.tvRecomendacion.text = "Tómate un momento para respirar y estar presente."
            }
        }

        estadoAnimoViewModel.operacionExitosa.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(requireContext(), "Estado de ánimo guardado", Toast.LENGTH_SHORT).show()
                binding.etNota.text?.clear()
                selectedMood = null
                updateMoodSelection(mapOf(
                    TipoEstadoAnimo.MUY_FELIZ to binding.moodMuyFeliz,
                    TipoEstadoAnimo.FELIZ to binding.moodFeliz,
                    TipoEstadoAnimo.NEUTRAL to binding.moodNeutral,
                    TipoEstadoAnimo.TRISTE to binding.moodTriste,
                    TipoEstadoAnimo.MUY_TRISTE to binding.moodMuyTriste
                ))
            }
        }

        estadoAnimoViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
