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
import com.example.balanceuapp.R
import com.example.balanceuapp.data.model.EstadoAnimo
import com.example.balanceuapp.data.model.TipoEstadoAnimo
import com.example.balanceuapp.databinding.FragmentInicioBinding
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.ui.viewmodel.EstadoAnimoViewModel
import com.example.balanceuapp.ui.viewmodel.InicioViewModel
import com.google.android.material.snackbar.Snackbar

class InicioFragment : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    private lateinit var inicioViewModel: InicioViewModel
    private lateinit var estadoAnimoViewModel: EstadoAnimoViewModel
    private lateinit var authViewModel: AuthViewModel
    private var selectedMood: TipoEstadoAnimo? = null
    private var savedUserId: String? = null

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

        try {
            inicioViewModel = ViewModelProvider(this)[InicioViewModel::class.java]
            estadoAnimoViewModel = ViewModelProvider(this)[EstadoAnimoViewModel::class.java]
            authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

            setupMoodSelector()
            setupListeners()
            setupObservers()

            val userId = authViewModel.obtenerUsuarioActual()
            if (userId != null) {
                setupSaludoPersonalizado()
                inicioViewModel.startObservandoHabitosDelDia(userId)
                inicioViewModel.cargarResumenDelDia(userId)
                savedUserId = userId
            } else {
                Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("InicioFragment", "Error en onViewCreated: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupSaludoPersonalizado() {
        val hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val saludo = when (hora) {
            in 5..11 -> "¡Buenos días!"
            in 12..17 -> "¡Buenas tardes!"
            in 18..23 -> "¡Buenas noches!"
            else -> "¡Hola!"
        }
        binding.tvSaludo.text = "$saludo 👋"
    }

    private fun setupMoodSelector() {
        val moodViews = mapOf(
            TipoEstadoAnimo.ALEGRE to binding.moodMuyFeliz,
            TipoEstadoAnimo.FELIZ to binding.moodFeliz,
            TipoEstadoAnimo.NEUTRAL to binding.moodNeutral,
            TipoEstadoAnimo.TRISTE to binding.moodTriste,
            TipoEstadoAnimo.TERRIBLE to binding.moodMuyTriste
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
            TipoEstadoAnimo.ALEGRE -> R.color.mood_very_happy
            TipoEstadoAnimo.FELIZ -> R.color.mood_happy
            TipoEstadoAnimo.NEUTRAL -> R.color.mood_neutral
            TipoEstadoAnimo.TRISTE -> R.color.mood_sad
            TipoEstadoAnimo.TERRIBLE -> R.color.mood_very_sad
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
                binding.btnGuardarEstadoAnimo.isEnabled = false
                animateSaveButton()
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

    private fun animateSaveButton() {
        binding.btnGuardarEstadoAnimo.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(80)
            .withEndAction {
                binding.btnGuardarEstadoAnimo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .start()
            }
            .start()
    }

    private fun setupObservers() {
        inicioViewModel.habitosDelDia.observe(viewLifecycleOwner) { habitos ->
            val completados = inicioViewModel.obtenerHabitossCompletados()
            val total = inicioViewModel.obtenerTotalHabitoss()
            binding.tvHabitossCompletados.text = "$completados/$total"
        }

        inicioViewModel.estadoAnimoDelDia.observe(viewLifecycleOwner) { estado ->
            if (estado != null) {
                val estadoTexto = when (estado.tipo) {
                    TipoEstadoAnimo.ALEGRE -> "Muy Feliz"
                    TipoEstadoAnimo.FELIZ -> "Feliz"
                    TipoEstadoAnimo.NEUTRAL -> "Neutral"
                    TipoEstadoAnimo.TRISTE -> "Triste"
                    TipoEstadoAnimo.TERRIBLE -> "Muy Triste"
                }
                val estadoEmoji = when (estado.tipo) {
                    TipoEstadoAnimo.ALEGRE -> "😄"
                    TipoEstadoAnimo.FELIZ -> "🙂"
                    TipoEstadoAnimo.NEUTRAL -> "😐"
                    TipoEstadoAnimo.TRISTE -> "😔"
                    TipoEstadoAnimo.TERRIBLE -> "😢"
                }
                binding.tvEstadoAnimo.text = estadoTexto
                binding.tvEstadoAnimoIcon.text = estadoEmoji
                selectedMood = estado.tipo
                updateMoodSelection(mapOf(
                    TipoEstadoAnimo.ALEGRE to binding.moodMuyFeliz,
                    TipoEstadoAnimo.FELIZ to binding.moodFeliz,
                    TipoEstadoAnimo.NEUTRAL to binding.moodNeutral,
                    TipoEstadoAnimo.TRISTE to binding.moodTriste,
                    TipoEstadoAnimo.TERRIBLE to binding.moodMuyTriste
                ))
                binding.etNota.setText(estado.nota)
            } else {
                binding.tvEstadoAnimo.text = "Sin registrar"
                binding.tvEstadoAnimoIcon.text = "😐"
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
            if (exitoso == true) {
                Snackbar.make(binding.root, "Nota guardada", Snackbar.LENGTH_SHORT).show()
                binding.etNota.text?.clear()
                selectedMood = null
                updateMoodSelection(mapOf(
                    TipoEstadoAnimo.ALEGRE to binding.moodMuyFeliz,
                    TipoEstadoAnimo.FELIZ to binding.moodFeliz,
                    TipoEstadoAnimo.NEUTRAL to binding.moodNeutral,
                    TipoEstadoAnimo.TRISTE to binding.moodTriste,
                    TipoEstadoAnimo.TERRIBLE to binding.moodMuyTriste
                ))
            }
            if (exitoso != null) {
                binding.btnGuardarEstadoAnimo.isEnabled = true
                savedUserId?.let { inicioViewModel.cargarResumenDelDia(it) }
            }
        }

        estadoAnimoViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.btnGuardarEstadoAnimo.isEnabled = true
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }
        }

        inicioViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.util.Log.e("InicioFragment", "Error del ViewModel: $it")
                // No mostrar toast para errores menores, solo loguear
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        inicioViewModel.stopObservandoHabitosDelDia()
        _binding = null
    }
}
