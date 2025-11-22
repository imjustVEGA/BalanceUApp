package com.example.balanceuapp.ui.fragments

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
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
import com.example.balanceuapp.util.Constants
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

/**
 * Fragment principal que muestra el resumen del día:
 * - Saludo personalizado según la hora
 * - Selector de estado de ánimo
 * - Resumen de hábitos completados
 * - Frase motivacional
 */
class InicioFragment : Fragment() {
    
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var inicioViewModel: InicioViewModel
    private lateinit var estadoAnimoViewModel: EstadoAnimoViewModel
    private lateinit var authViewModel: AuthViewModel
    
    private var selectedMood: TipoEstadoAnimo? = null
    private lateinit var moodViews: Map<TipoEstadoAnimo, View>
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
            inicializarViewModels()
            configurarUI()
            cargarDatosDelUsuario()
        } catch (e: Exception) {
            Log.e(Constants.LogTags.INICIO_FRAGMENT, "Error en onViewCreated: ${e.message}", e)
            Toast.makeText(requireContext(), "${Constants.ErrorMessages.ERROR_CARGAR_DATOS}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Inicializa los ViewModels necesarios.
     */
    private fun inicializarViewModels() {
        inicioViewModel = ViewModelProvider(this)[InicioViewModel::class.java]
        estadoAnimoViewModel = ViewModelProvider(this)[EstadoAnimoViewModel::class.java]
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
    }

    /**
     * Configura la UI: selector de estado de ánimo, listeners y observadores.
     */
    private fun configurarUI() {
        setupMoodSelector()
        setupListeners()
        setupObservers()
    }

    /**
     * Carga los datos del usuario autenticado.
     */
    private fun cargarDatosDelUsuario() {
        val userId = authViewModel.obtenerUsuarioActual()
        if (userId != null) {
            setupSaludoPersonalizado()
            inicioViewModel.startObservandoHabitosDelDia(userId)
            inicioViewModel.cargarResumenDelDia(userId)
            savedUserId = userId
        } else {
            Toast.makeText(requireContext(), Constants.ErrorMessages.ERROR_USUARIO_NO_AUTENTICADO, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Configura el saludo personalizado según la hora del día.
     */
    private fun setupSaludoPersonalizado() {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saludo = when (hora) {
            in 5..11 -> "¡Buenos días!"
            in 12..17 -> "¡Buenas tardes!"
            in 18..23 -> "¡Buenas noches!"
            else -> "¡Hola!"
        }
        binding.tvSaludo.text = "$saludo 👋"
    }

    private fun setupMoodSelector() {
        moodViews = mapOf(
            TipoEstadoAnimo.ALEGRE to binding.moodMuyFeliz,
            TipoEstadoAnimo.FELIZ to binding.moodFeliz,
            TipoEstadoAnimo.NEUTRAL to binding.moodNeutral,
            TipoEstadoAnimo.TRISTE to binding.moodTriste,
            TipoEstadoAnimo.TERRIBLE to binding.moodMuyTriste
        )

        moodViews.forEach { (tipo, view) ->
            view.setOnClickListener {
                selectedMood = tipo
                updateMoodSelection()
            }
        }
    }

    private fun updateMoodSelection() {
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

    /**
     * Configura los listeners de los botones y campos de entrada.
     */
    private fun setupListeners() {
        binding.btnGuardarEstadoAnimo.setOnClickListener {
            guardarEstadoAnimo()
        }
    }

    /**
     * Guarda el estado de ánimo seleccionado por el usuario.
     */
    private fun guardarEstadoAnimo() {
        val tipo = selectedMood
        if (tipo == null) {
            Toast.makeText(requireContext(), Constants.UIMessages.SELECCIONAR_ESTADO_ANIMO, Toast.LENGTH_SHORT).show()
            return
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
                updateMoodSelection()
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
                Snackbar.make(binding.root, Constants.SuccessMessages.REGISTRO_GUARDADO, Snackbar.LENGTH_SHORT).show()
                limpiarFormularioEstadoAnimo()
                savedUserId?.let { inicioViewModel.cargarResumenDelDia(it) }
            }
            if (exitoso != null) {
                binding.btnGuardarEstadoAnimo.isEnabled = true
                estadoAnimoViewModel.limpiarOperacionExitosa()
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
                Log.e(Constants.LogTags.INICIO_FRAGMENT, "Error del ViewModel: $it")
                // No mostrar toast para errores menores, solo loguear
            }
        }
    }

    /**
     * Limpia el formulario de estado de ánimo después de guardar.
     */
    private fun limpiarFormularioEstadoAnimo() {
        binding.etNota.text?.clear()
        selectedMood = null
        updateMoodSelection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        inicioViewModel.stopObservandoHabitosDelDia()
        _binding = null
    }
}
