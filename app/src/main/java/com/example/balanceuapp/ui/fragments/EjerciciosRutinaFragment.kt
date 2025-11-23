package com.example.balanceuapp.ui.fragments

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.example.balanceuapp.R

data class Ejercicio(
    val nombre: String,
    val duracion: String,
    val descripcion: String,
    val videoUrl: String = "", // URL del video o path local
    val duracionSegundos: Int = 30, // Duración en segundos para el cronómetro
    val esPorRepeticiones: Boolean = false, // Si es true, se muestra contador de repeticiones
    val repeticiones: Int = 0 // Número de repeticiones
)

class EjerciciosRutinaFragment : Fragment() {
    
    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_INFO = "info"
        private const val ARG_DESCRIPTION = "description"
        
        fun newInstance(title: String, info: String, description: String): EjerciciosRutinaFragment {
            val fragment = EjerciciosRutinaFragment()
            val args = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_INFO, info)
                putString(ARG_DESCRIPTION, description)
            }
            fragment.arguments = args
            return fragment
        }
    }
    
    private var title: String? = null
    private var info: String? = null
    private var description: String? = null
    private var currentExerciseIndex = 0
    private var ejercicios: List<Ejercicio> = emptyList()
    
    private var videoView: VideoView? = null
    private var textExerciseName: TextView? = null
    private var textTimer: TextView? = null
    private var textRepeticiones: TextView? = null
    private var buttonAnterior: TextView? = null
    private var buttonSiguiente: TextView? = null
    private var buttonPausa: Button? = null
    private var buttonDone: Button? = null
    private var buttonBackTop: ImageButton? = null
    private var containerTimer: ViewGroup? = null
    private var containerRepeticiones: ViewGroup? = null
    
    // Vista del siguiente ejercicio
    private var containerSiguienteEjercicio: ViewGroup? = null
    private var textSiguienteEjercicioNombre: TextView? = null
    private var textSiguienteEjercicioDuracion: TextView? = null
    
    private var countDownTimer: CountDownTimer? = null
    private var tiempoRestante: Long = 0
    private var isPaused: Boolean = false
    private var isRunning: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString(ARG_TITLE)
            info = it.getString(ARG_INFO)
            description = it.getString(ARG_DESCRIPTION)
        }
        
        // Generar ejercicios de ejemplo basados en el tipo de rutina
        ejercicios = generarEjercicios(title ?: "")
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ejercicios_rutina, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener referencias
        videoView = view.findViewById(R.id.videoView)
        textExerciseName = view.findViewById(R.id.textExerciseName)
        textTimer = view.findViewById(R.id.textTimer)
        textRepeticiones = view.findViewById(R.id.textRepeticiones)
        buttonAnterior = view.findViewById(R.id.buttonAnterior)
        buttonSiguiente = view.findViewById(R.id.buttonSiguiente)
        buttonPausa = view.findViewById(R.id.buttonPausa)
        buttonDone = view.findViewById(R.id.buttonDone)
        buttonBackTop = view.findViewById(R.id.buttonBackTop)
        containerTimer = view.findViewById(R.id.containerTimer)
        containerRepeticiones = view.findViewById(R.id.containerRepeticiones)
        
        // Configurar botones
        buttonAnterior?.setOnClickListener { mostrarEjercicioAnterior() }
        buttonSiguiente?.setOnClickListener { mostrarEjercicioSiguiente() }
        buttonPausa?.setOnClickListener { mostrarDialogoPausa() }
        buttonDone?.setOnClickListener { ejercicioCompletado() }
        buttonBackTop?.setOnClickListener { completarRutina() }
        
        // Restaurar estado si existe
        if (savedInstanceState != null) {
            currentExerciseIndex = savedInstanceState.getInt("currentExerciseIndex", 0)
            tiempoRestante = savedInstanceState.getLong("tiempoRestante", 0)
            isPaused = savedInstanceState.getBoolean("isPaused", false)
            isRunning = savedInstanceState.getBoolean("isRunning", false)
        }
        
        // Mostrar el primer ejercicio
        mostrarEjercicio(currentExerciseIndex)
        
        // Iniciar cronómetro automáticamente
        if (!isPaused && !isRunning) {
            iniciarCronometro()
        } else if (isRunning && tiempoRestante > 0) {
            reanudarCronometro()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentExerciseIndex", currentExerciseIndex)
        outState.putLong("tiempoRestante", tiempoRestante)
        outState.putBoolean("isPaused", isPaused)
        outState.putBoolean("isRunning", isRunning)
    }
    
    private fun mostrarEjercicio(index: Int) {
        if (index < 0 || index >= ejercicios.size) return
        
        // Detener cronómetro anterior
        detenerCronometro()
        
        val ejercicio = ejercicios[index]
        
        // Mostrar nombre en mayúsculas
        textExerciseName?.text = ejercicio.nombre.uppercase()
        
        // Configurar video
        videoView?.let { video ->
            val mediaController = MediaController(requireContext())
            mediaController.setAnchorView(video)
            video.setMediaController(mediaController)
            
            // Si hay una URL de video, cargarla
            if (ejercicio.videoUrl.isNotEmpty()) {
                try {
                    video.setVideoURI(Uri.parse(ejercicio.videoUrl))
                    video.visibility = View.VISIBLE
                } catch (e: Exception) {
                    video.setVideoPath(ejercicio.videoUrl)
                    video.visibility = View.VISIBLE
                }
            } else {
                video.visibility = View.GONE
            }
        }
        
        // Actualizar estado de botones
        buttonAnterior?.isEnabled = index > 0
        buttonAnterior?.alpha = if (index > 0) 1.0f else 0.5f
        buttonSiguiente?.isEnabled = index < ejercicios.size - 1
        
        // Configurar según tipo de ejercicio (repeticiones o tiempo)
        if (ejercicio.esPorRepeticiones) {
            // Mostrar contador de repeticiones
            containerTimer?.visibility = View.GONE
            containerRepeticiones?.visibility = View.VISIBLE
            buttonPausa?.visibility = View.GONE
            buttonDone?.visibility = View.VISIBLE
            textRepeticiones?.text = "x${ejercicio.repeticiones}"
        } else {
            // Mostrar cronómetro
            containerTimer?.visibility = View.VISIBLE
            containerRepeticiones?.visibility = View.GONE
            buttonPausa?.visibility = View.VISIBLE
            buttonDone?.visibility = View.GONE
            tiempoRestante = (ejercicio.duracionSegundos * 1000).toLong()
            actualizarTimer()
            iniciarCronometro()
        }
    }
    
    private fun ejercicioCompletado() {
        // Avanzar al siguiente ejercicio
        mostrarEjercicioSiguiente()
    }
    
    private fun iniciarCronometro() {
        if (tiempoRestante <= 0) {
            val ejercicio = ejercicios[currentExerciseIndex]
            tiempoRestante = (ejercicio.duracionSegundos * 1000).toLong()
        }
        
        detenerCronometro()
        isPaused = false
        isRunning = true
        
        countDownTimer = object : CountDownTimer(tiempoRestante, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tiempoRestante = millisUntilFinished
                actualizarTimer()
            }
            
            override fun onFinish() {
                tiempoRestante = 0
                actualizarTimer()
                // Cambiar automáticamente al siguiente ejercicio
                if (currentExerciseIndex < ejercicios.size - 1) {
                    mostrarEjercicioSiguiente()
                } else {
                    // Rutina completada
                    completarRutina()
                }
            }
        }.start()
        
        buttonPausa?.text = "PAUSAR"
    }
    
    private fun reanudarCronometro() {
        if (tiempoRestante > 0 && isPaused) {
            iniciarCronometro()
        }
    }
    
    private fun pausarCronometro() {
        countDownTimer?.cancel()
        isPaused = true
        isRunning = false
        buttonPausa?.text = "CONTINUAR"
    }
    
    private fun detenerCronometro() {
        countDownTimer?.cancel()
        countDownTimer = null
        isRunning = false
    }
    
    private fun reiniciarCronometro() {
        detenerCronometro()
        val ejercicio = ejercicios[currentExerciseIndex]
        tiempoRestante = (ejercicio.duracionSegundos * 1000).toLong()
        actualizarTimer()
        iniciarCronometro()
    }
    
    private fun actualizarTimer() {
        val minutos = (tiempoRestante / 1000) / 60
        val segundos = (tiempoRestante / 1000) % 60
        textTimer?.text = String.format("%02d:%02d", minutos, segundos)
    }
    
    private fun mostrarDialogoPausa() {
        if (isRunning && !isPaused) {
            // Pausar el cronómetro
            pausarCronometro()
        }
        
        val opciones = arrayOf("Reiniciar", "Continuar", "Salir de la rutina")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Rutina en pausa")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> { // Reiniciar
                        reiniciarCronometro()
                    }
                    1 -> { // Continuar
                        if (isPaused) {
                            reanudarCronometro()
                        }
                    }
                    2 -> { // Salir
                        detenerCronometro()
                        completarRutina()
                    }
                }
            }
            .setCancelable(true)
            .setOnCancelListener {
                // Si se cancela el diálogo, reanudar si estaba pausado
                if (isPaused) {
                    reanudarCronometro()
                }
            }
            .show()
    }
    
    private fun mostrarEjercicioAnterior() {
        if (currentExerciseIndex > 0) {
            detenerCronometro()
            currentExerciseIndex--
            mostrarEjercicio(currentExerciseIndex)
        }
    }
    
    private fun mostrarEjercicioSiguiente() {
        if (currentExerciseIndex < ejercicios.size - 1) {
            detenerCronometro()
            currentExerciseIndex++
            mostrarEjercicio(currentExerciseIndex)
        } else {
            // Rutina completada
            completarRutina()
        }
    }
    
    private fun completarRutina() {
        // Regresar al fragment anterior (EjerciciosDetalleFragment)
        // Usar el childFragmentManager del padre si existe, sino el parentFragmentManager
        val fragmentManager = parentFragment?.childFragmentManager ?: parentFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        }
    }
    
    private fun crearEjercicio(nombre: String, duracion: String, descripcion: String, duracionSegundos: Int = 30): Ejercicio {
        // Detectar si es por repeticiones (contiene "x" o "repeticiones")
        val esRepeticiones = duracion.contains("x", ignoreCase = true) || 
                             duracion.contains("repeticiones", ignoreCase = true)
        
        if (esRepeticiones) {
            // Extraer número de repeticiones
            val regex = Regex("(\\d+)")
            val match = regex.find(duracion)
            val repeticiones = match?.value?.toIntOrNull() ?: 0
            return Ejercicio(nombre, duracion, descripcion, "", 0, true, repeticiones)
        }
        return Ejercicio(nombre, duracion, descripcion, "", duracionSegundos, false, 0)
    }
    
    private fun generarEjercicios(tipoRutina: String): List<Ejercicio> {
        return when (tipoRutina.lowercase()) {
            "torso" -> listOf(
                Ejercicio("Flexiones", "30 seg", "Realiza flexiones manteniendo el cuerpo recto", "", 30),
                Ejercicio("Plancha", "45 seg", "Mantén la posición de plancha con el cuerpo recto", "", 45),
                Ejercicio("Fondos de tríceps", "30 seg", "Realiza fondos usando una silla o banco", "", 30),
                Ejercicio("Remo con peso corporal", "30 seg", "Tira hacia atrás manteniendo la espalda recta", "", 30),
                Ejercicio("Abdominales", "30 seg", "Contrae el abdomen levantando el torso", "", 30),
                Ejercicio("Flexiones inclinadas", "30 seg", "Flexiones con pies elevados para mayor intensidad", "", 30),
                Ejercicio("Plancha lateral", "30 seg cada lado", "Mantén la plancha de lado para trabajar oblicuos", "", 30),
                Ejercicio("Superman", "30 seg", "Acostado boca abajo, levanta brazos y piernas", "", 30),
                Ejercicio("Mountain climbers", "30 seg", "Alterna las piernas como corriendo en posición de plancha", "", 30),
                Ejercicio("Burpees", "30 seg", "Salto, flexión y vuelta a posición inicial", "", 30),
                Ejercicio("Flexiones diamante", "30 seg", "Flexiones con manos en forma de diamante", "", 30),
                Ejercicio("Plancha con elevación de pierna", "30 seg", "Plancha alternando elevación de piernas", "", 30),
                Ejercicio("Abdominales bicicleta", "30 seg", "Simula pedaleo acostado para trabajar el core", "", 30),
                Ejercicio("Flexiones con palmada", "30 seg", "Flexiones explosivas con palmada en el aire", "", 30),
                Ejercicio("Plancha con toque de hombro", "30 seg", "Plancha alternando toque de hombros", "", 30)
            )
            "piernas" -> listOf(
                Ejercicio("Sentadillas", "30 seg", "Baja como si te sentaras en una silla", "", 30),
                Ejercicio("Zancadas", "30 seg cada pierna", "Da un paso largo y baja la rodilla trasera", "", 30),
                Ejercicio("Elevaciones de talón", "30 seg", "Levántate sobre las puntas de los pies", "", 30),
                Ejercicio("Sentadilla sumo", "30 seg", "Sentadilla con piernas abiertas", "", 30),
                Ejercicio("Estocadas laterales", "30 seg", "Da un paso lateral y flexiona la rodilla", "", 30),
                Ejercicio("Sentadillas con salto", "30 seg", "Sentadillas explosivas con salto", "", 30),
                Ejercicio("Zancadas alternas", "30 seg", "Alterna zancadas hacia adelante", "", 30),
                Ejercicio("Elevaciones de pierna lateral", "30 seg", "Eleva la pierna lateralmente de pie", "", 30),
                Ejercicio("Pistol squat", "30 seg", "Sentadilla a una pierna (avanzado)", "", 30),
                Ejercicio("Wall sit", "45 seg", "Mantén posición de sentadilla contra la pared", "", 45),
                Ejercicio("Step ups", "30 seg", "Sube y baja un escalón o banco", "", 30),
                Ejercicio("Glute bridge", "30 seg", "Eleva la cadera acostado boca arriba", "", 30)
            )
            "fuerza" -> listOf(
                crearEjercicio("Press de banca", "x12", "Empuja el peso alejándolo del pecho"),
                crearEjercicio("Peso muerto", "x10", "Levanta el peso manteniendo la espalda recta"),
                crearEjercicio("Sentadillas con peso", "x12", "Sentadillas con barra o mancuernas"),
                crearEjercicio("Press militar", "x10", "Empuja el peso por encima de la cabeza"),
                crearEjercicio("Remo con barra", "x12", "Tira la barra hacia el pecho"),
                crearEjercicio("Press inclinado", "x12", "Press de banca en banco inclinado"),
                crearEjercicio("Peso muerto rumano", "x10", "Variación de peso muerto con énfasis en isquiotibiales"),
                crearEjercicio("Sentadillas frontales", "x12", "Sentadillas con barra al frente"),
                crearEjercicio("Press de hombros con barra", "x10", "Press militar con barra"),
                crearEjercicio("Remo T", "x12", "Remo con barra en posición T"),
                crearEjercicio("Curl de bíceps con barra", "x12", "Flexión de brazos con barra"),
                crearEjercicio("Extensiones de tríceps", "x12", "Extiende los brazos trabajando tríceps"),
                crearEjercicio("Press de piernas", "x12", "Press en máquina para piernas"),
                crearEjercicio("Prensa de pecho", "x12", "Press en máquina para pecho"),
                crearEjercicio("Jalones al pecho", "x12", "Jala el peso hacia el pecho"),
                crearEjercicio("Elevaciones laterales", "x12", "Eleva mancuernas lateralmente"),
                crearEjercicio("Curl martillo", "x12", "Curl de bíceps con agarre neutro"),
                crearEjercicio("Fondos en paralelas", "x10", "Fondos para tríceps y pecho")
            )
            "cardio" -> listOf(
                Ejercicio("Burpees", "30 seg", "Salto, flexión y vuelta a posición inicial", "", 30),
                Ejercicio("Mountain climbers", "30 seg", "Alterna las piernas como corriendo en posición de plancha", "", 30),
                Ejercicio("Jumping jacks", "30 seg", "Salta abriendo piernas y brazos", "", 30),
                Ejercicio("High knees", "30 seg", "Corre en el lugar levantando las rodillas", "", 30),
                Ejercicio("Jump squats", "30 seg", "Salta desde posición de sentadilla", "", 30),
                Ejercicio("Correr en el lugar", "30 seg", "Corre en el lugar a buen ritmo", "", 30),
                Ejercicio("Escaladores", "30 seg", "Mountain climbers a alta velocidad", "", 30),
                Ejercicio("Saltos de tijera", "30 seg", "Salta alternando piernas adelante y atrás", "", 30),
                Ejercicio("Burpees sin flexión", "30 seg", "Burpees sin hacer flexión completa", "", 30),
                Ejercicio("Skipping", "30 seg", "Corre en el lugar con rodillas altas", "", 30)
            )
            "sin equipamiento" -> listOf(
                Ejercicio("Flexiones", "30 seg", "Ejercicio básico para pecho y brazos", "", 30),
                Ejercicio("Sentadillas", "30 seg", "Ejercicio fundamental para piernas", "", 30),
                Ejercicio("Plancha", "45 seg", "Fortalece el core", "", 45),
                Ejercicio("Abdominales", "30 seg", "Fortalece el abdomen", "", 30),
                Ejercicio("Burpees", "30 seg", "Ejercicio completo de cuerpo", "", 30),
                Ejercicio("Mountain climbers", "30 seg", "Alterna piernas en posición de plancha", "", 30),
                Ejercicio("Jumping jacks", "30 seg", "Salta abriendo piernas y brazos", "", 30),
                Ejercicio("Zancadas", "30 seg", "Da un paso largo y baja la rodilla", "", 30),
                Ejercicio("Plancha lateral", "30 seg cada lado", "Plancha de lado para oblicuos", "", 30),
                Ejercicio("Flexiones diamante", "30 seg", "Flexiones con manos juntas", "", 30),
                Ejercicio("Sentadillas con salto", "30 seg", "Sentadillas explosivas", "", 30),
                Ejercicio("Abdominales bicicleta", "30 seg", "Simula pedaleo acostado", "", 30),
                Ejercicio("Superman", "30 seg", "Levanta brazos y piernas acostado boca abajo", "", 30),
                Ejercicio("Wall sit", "45 seg", "Sentadilla estática contra la pared", "", 45),
                Ejercicio("High knees", "30 seg", "Corre en el lugar con rodillas altas", "", 30),
                Ejercicio("Glute bridge", "30 seg", "Eleva la cadera acostado", "", 30),
                Ejercicio("Estocadas laterales", "30 seg", "Zancadas hacia los lados", "", 30),
                Ejercicio("Flexiones inclinadas", "30 seg", "Flexiones con pies elevados", "", 30),
                Ejercicio("Jump squats", "30 seg", "Salta desde posición de sentadilla", "", 30),
                Ejercicio("Plancha con toque de hombro", "30 seg", "Plancha alternando toque de hombros", "", 30)
            )
            "con pesas" -> listOf(
                crearEjercicio("Curl de bíceps", "x12", "Flexiona los brazos levantando las pesas"),
                crearEjercicio("Press de hombros", "x12", "Empuja las pesas por encima de la cabeza"),
                crearEjercicio("Sentadillas con pesas", "x12", "Sentadillas sosteniendo pesas"),
                crearEjercicio("Remo con pesas", "x12", "Tira las pesas hacia el torso"),
                crearEjercicio("Elevaciones laterales", "x12", "Levanta las pesas a los lados"),
                crearEjercicio("Press de banca con mancuernas", "x12", "Press de pecho con mancuernas"),
                crearEjercicio("Zancadas con pesas", "x12 cada pierna", "Zancadas sosteniendo pesas"),
                crearEjercicio("Curl martillo", "x12", "Curl con agarre neutro"),
                crearEjercicio("Extensiones de tríceps", "x12", "Extiende los brazos con pesa"),
                crearEjercicio("Peso muerto con mancuernas", "x10", "Peso muerto con mancuernas"),
                crearEjercicio("Elevaciones frontales", "x12", "Eleva las pesas al frente"),
                crearEjercicio("Remo inclinado", "x12", "Remo inclinado con mancuernas"),
                crearEjercicio("Press francés", "x12", "Extensión de tríceps acostado"),
                crearEjercicio("Sentadillas goblet", "x12", "Sentadillas con una pesa al pecho")
            )
            "yoga" -> listOf(
                Ejercicio("Postura de la montaña", "60 seg", "Postura básica de pie, alineando todo el cuerpo", "", 60),
                Ejercicio("Saludo al sol", "90 seg", "Secuencia completa de saludo al sol", "", 90),
                Ejercicio("Postura del guerrero I", "45 seg cada lado", "Fortalece piernas y abre caderas", "", 45),
                Ejercicio("Postura del guerrero II", "45 seg cada lado", "Mejora equilibrio y fuerza", "", 45),
                Ejercicio("Postura del árbol", "60 seg cada lado", "Mejora equilibrio y concentración", "", 60),
                Ejercicio("Postura del perro boca abajo", "60 seg", "Estira toda la parte posterior del cuerpo", "", 60),
                Ejercicio("Postura del niño", "60 seg", "Relaja y estira la espalda", "", 60),
                Ejercicio("Postura de la cobra", "45 seg", "Fortalece la espalda y abre el pecho", "", 45),
                Ejercicio("Postura del puente", "60 seg", "Fortalece glúteos y abre caderas", "", 60),
                Ejercicio("Postura de la torsión sentada", "45 seg cada lado", "Mejora flexibilidad de columna", "", 45),
                Ejercicio("Postura del triángulo", "45 seg cada lado", "Estira laterales y fortalece piernas", "", 45),
                Ejercicio("Postura del cadáver", "120 seg", "Relajación final y meditación", "", 120)
            )
            else -> listOf(
                Ejercicio("Calentamiento", "5 min", "Calienta tus músculos antes de comenzar", "", 300),
                Ejercicio("Ejercicio principal", "30 seg", "Realiza el ejercicio principal de la rutina", "", 30),
                Ejercicio("Estiramiento", "5 min", "Estira los músculos trabajados", "", 300)
            )
        }
    }
    
    override fun onPause() {
        super.onPause()
        videoView?.pause()
        pausarCronometro()
    }
    
    override fun onResume() {
        super.onResume()
        // Reanudar cronómetro si estaba corriendo
        if (isPaused && tiempoRestante > 0) {
            reanudarCronometro()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        detenerCronometro()
        videoView?.stopPlayback()
        videoView = null
    }
}

