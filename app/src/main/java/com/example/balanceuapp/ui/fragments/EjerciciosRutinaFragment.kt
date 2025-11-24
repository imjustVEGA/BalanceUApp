package com.example.balanceuapp.ui.fragments

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.balanceuapp.R
import java.io.File

data class Ejercicio(
    val nombre: String,
    val duracion: String,
    val descripcion: String,
    val videoUrl: String = "", // URL del video o path local
    val duracionSegundos: Int = 30, // Duración en segundos para el cronómetro
    val esPorRepeticiones: Boolean = false, // Si es true, se muestra contador de repeticiones
    val repeticiones: Int = 0, // Número de repeticiones
    val instrucciones: String = "" // Instrucciones detalladas de cómo hacer el ejercicio
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
    private var imageViewGif: ImageView? = null
    private var imageViewGifBottom: ImageView? = null
    private var textExerciseName: TextView? = null
    private var textExerciseNameTop: TextView? = null
    private var textInstrucciones: TextView? = null
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
    
    // Variables para descanso
    private var isEnDescanso: Boolean = false
    private var descansoTimer: CountDownTimer? = null
    private var tiempoDescansoRestante: Long = 60000 // 60 segundos por defecto
    private var containerDescanso: ViewGroup? = null
    private var textCronometroDescanso: TextView? = null
    private var buttonAumentarTiempo: Button? = null
    private var buttonSaltarDescanso: TextView? = null
    private var ejercicioView: View? = null
    private var descansoView: View? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString(ARG_TITLE)
            info = it.getString(ARG_INFO)
            description = it.getString(ARG_DESCRIPTION)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Crear un contenedor principal que incluya tanto el ejercicio como el descanso
        val rootView = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Inflar el layout del ejercicio
        val ejercicioView = inflater.inflate(R.layout.fragment_ejercicios_rutina, rootView, false)
        rootView.addView(ejercicioView)
        
        // Inflar el layout del descanso (inicialmente oculto)
        val descansoView = inflater.inflate(R.layout.fragment_descanso, rootView, false)
        descansoView.visibility = View.GONE
        rootView.addView(descansoView)
        
        return rootView
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Generar ejercicios de ejemplo basados en el tipo de rutina (después de que el contexto esté disponible)
        ejercicios = generarEjercicios(title ?: "")
        
        // Obtener referencias del ejercicio
        ejercicioView = (view as? ViewGroup)?.getChildAt(0)
        videoView = ejercicioView?.findViewById(R.id.videoView)
        imageViewGif = ejercicioView?.findViewById(R.id.imageViewGif)
        imageViewGifBottom = ejercicioView?.findViewById(R.id.imageViewGifBottom)
        textExerciseName = ejercicioView?.findViewById(R.id.textExerciseName)
        textExerciseNameTop = ejercicioView?.findViewById(R.id.textExerciseNameTop)
        textInstrucciones = ejercicioView?.findViewById(R.id.textInstrucciones)
        textTimer = ejercicioView?.findViewById(R.id.textTimer)
        textRepeticiones = ejercicioView?.findViewById(R.id.textRepeticiones)
        buttonAnterior = ejercicioView?.findViewById(R.id.buttonAnterior)
        buttonSiguiente = ejercicioView?.findViewById(R.id.buttonSiguiente)
        buttonPausa = ejercicioView?.findViewById(R.id.buttonPausa)
        buttonDone = ejercicioView?.findViewById(R.id.buttonDone)
        buttonBackTop = ejercicioView?.findViewById(R.id.buttonBackTop)
        containerTimer = ejercicioView?.findViewById(R.id.containerTimer)
        containerRepeticiones = ejercicioView?.findViewById(R.id.containerRepeticiones)
        
        // Obtener referencias del descanso
        descansoView = (view as? ViewGroup)?.getChildAt(1)
        containerDescanso = descansoView as? ViewGroup
        textCronometroDescanso = descansoView?.findViewById(R.id.textCronometroDescanso)
        buttonAumentarTiempo = descansoView?.findViewById(R.id.buttonAumentarTiempo)
        buttonSaltarDescanso = descansoView?.findViewById(R.id.buttonSaltarDescanso)
        
        // Configurar botones del descanso
        buttonAumentarTiempo?.setOnClickListener { aumentarTiempoDescanso() }
        buttonSaltarDescanso?.setOnClickListener { saltarDescanso() }
        
        // Asegurar que el ImageView esté visible desde el inicio
        imageViewGif?.let { imageView ->
            imageView.visibility = View.VISIBLE
            imageView.bringToFront()
        }
        // Ocultar placeholder y video
        ejercicioView?.findViewById<View>(R.id.illustrationPlaceholder)?.visibility = View.GONE
        videoView?.visibility = View.GONE
        
        // Configurar botones
        buttonAnterior?.setOnClickListener { mostrarEjercicioAnterior() }
        buttonSiguiente?.setOnClickListener { mostrarEjercicioSiguiente() }
        buttonPausa?.setOnClickListener { mostrarDialogoPausa() }
        buttonDone?.setOnClickListener { ejercicioCompletado() }
        buttonBackTop?.setOnClickListener { completarRutina() }
        
        // Asegurar que las vistas estén en el estado correcto al inicio
        ejercicioView?.visibility = View.VISIBLE
        descansoView?.visibility = View.GONE
        
        // Restaurar estado si existe
        if (savedInstanceState != null) {
            currentExerciseIndex = savedInstanceState.getInt("currentExerciseIndex", 0)
            tiempoRestante = savedInstanceState.getLong("tiempoRestante", 0)
            isPaused = savedInstanceState.getBoolean("isPaused", false)
            isRunning = savedInstanceState.getBoolean("isRunning", false)
            isEnDescanso = savedInstanceState.getBoolean("isEnDescanso", false)
            tiempoDescansoRestante = savedInstanceState.getLong("tiempoDescansoRestante", 60000)
        } else {
            // Si no hay estado guardado, asegurar que todo esté en el estado inicial
            currentExerciseIndex = 0
            isEnDescanso = false
            tiempoDescansoRestante = 60000
            isPaused = false
            isRunning = false
        }
        
        // NUNCA mostrar descanso al inicio - solo después de completar un ejercicio
        // Si el estado guardado dice que estamos en descanso, ignorarlo y mostrar el ejercicio
        isEnDescanso = false
        ejercicioView?.visibility = View.VISIBLE
        descansoView?.visibility = View.GONE
        
        // Asegurar que el índice sea válido
        if (currentExerciseIndex >= ejercicios.size || currentExerciseIndex < 0) {
            currentExerciseIndex = 0
        }
        
        // Mostrar el primer ejercicio
        Log.d("EjerciciosRutina", "=== INICIALIZACIÓN ===")
        Log.d("EjerciciosRutina", "currentExerciseIndex inicial: $currentExerciseIndex")
        Log.d("EjerciciosRutina", "Total ejercicios disponibles: ${ejercicios.size}")
        if (ejercicios.isNotEmpty()) {
            Log.d("EjerciciosRutina", "Primer ejercicio en la lista: ${ejercicios[0].nombre}")
            if (currentExerciseIndex < ejercicios.size) {
                Log.d("EjerciciosRutina", "Ejercicio que se va a mostrar (índice $currentExerciseIndex): ${ejercicios[currentExerciseIndex].nombre}")
            }
        }
        
        mostrarEjercicio(currentExerciseIndex)
        
        // Iniciar cronómetro automáticamente solo si no está pausado
        if (!isPaused && !isRunning && tiempoRestante > 0) {
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
        outState.putBoolean("isEnDescanso", isEnDescanso)
        outState.putLong("tiempoDescansoRestante", tiempoDescansoRestante)
    }
    
    private fun mostrarEjercicio(index: Int) {
        if (ejercicios.isEmpty()) {
            Log.e("EjerciciosRutina", "La lista de ejercicios está vacía")
            return
        }
        
        if (index < 0 || index >= ejercicios.size) {
            Log.e("EjerciciosRutina", "Índice inválido: $index (tamaño de lista: ${ejercicios.size})")
            // Corregir el índice si es inválido
            currentExerciseIndex = 0
            mostrarEjercicio(0)
            return
        }
        
        Log.d("EjerciciosRutina", "=== MOSTRANDO EJERCICIO ===")
        Log.d("EjerciciosRutina", "Índice: $index")
        Log.d("EjerciciosRutina", "Nombre del ejercicio: ${ejercicios[index].nombre}")
        Log.d("EjerciciosRutina", "Total de ejercicios: ${ejercicios.size}")
        
        // Asegurar que el ejercicio esté visible y el descanso oculto
        ejercicioView?.visibility = View.VISIBLE
        descansoView?.visibility = View.GONE
        isEnDescanso = false
        
        // Detener cronómetro anterior
        detenerCronometro()
        
        // Limpiar GIF anterior para liberar memoria
        imageViewGif?.let { imageView ->
            Glide.with(this).clear(imageView)
        }
        
        val ejercicio = ejercicios[index]
        
        // Mostrar nombre en mayúsculas (tanto en la parte superior como en la inferior)
        val nombreEjercicio = ejercicio.nombre.uppercase()
        textExerciseName?.text = nombreEjercicio
        textExerciseNameTop?.text = nombreEjercicio
        
        // Mostrar instrucciones
        textInstrucciones?.text = ejercicio.instrucciones.ifEmpty { ejercicio.descripcion }
        
        // Ocultar video
        videoView?.visibility = View.GONE
        
        // FORZAR visibilidad del ImageView y ocultar placeholder
        val placeholderView = view?.findViewById<View>(R.id.illustrationPlaceholder)
        placeholderView?.visibility = View.GONE
        
        imageViewGif?.let { imageView ->
            imageView.visibility = View.VISIBLE
            imageView.bringToFront() // Traer al frente
            // Asegurar que tenga un tamaño mínimo
            if (imageView.width == 0 || imageView.height == 0) {
                imageView.layoutParams = imageView.layoutParams.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }
        
        // Cargar GIF del ejercicio en ambos ImageViews
        cargarGifEjercicio(ejercicio.nombre)
        
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
        // Mostrar pantalla de descanso antes del siguiente ejercicio
        mostrarDescanso()
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
                // Mostrar pantalla de descanso antes del siguiente ejercicio
                if (currentExerciseIndex < ejercicios.size - 1) {
                    mostrarDescanso()
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
    
    private fun mostrarDescanso() {
        isEnDescanso = true
        tiempoDescansoRestante = 60000 // Reiniciar a 60 segundos
        
        // Ocultar vista del ejercicio y mostrar vista de descanso
        ejercicioView?.visibility = View.GONE
        descansoView?.visibility = View.VISIBLE
        
        // Actualizar cronómetro de descanso
        actualizarCronometroDescanso()
        
        // Iniciar cronómetro de descanso
        iniciarCronometroDescanso()
    }
    
    private fun ocultarDescanso() {
        isEnDescanso = false
        detenerCronometroDescanso()
        
        // Ocultar vista de descanso y mostrar vista del ejercicio
        descansoView?.visibility = View.GONE
        ejercicioView?.visibility = View.VISIBLE
        
        // Avanzar al siguiente ejercicio
        mostrarEjercicioSiguiente()
    }
    
    private fun iniciarCronometroDescanso() {
        detenerCronometroDescanso()
        
        descansoTimer = object : CountDownTimer(tiempoDescansoRestante, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tiempoDescansoRestante = millisUntilFinished
                actualizarCronometroDescanso()
            }
            
            override fun onFinish() {
                tiempoDescansoRestante = 0
                actualizarCronometroDescanso()
                // Terminar descanso y continuar con el siguiente ejercicio
                ocultarDescanso()
            }
        }.start()
    }
    
    private fun detenerCronometroDescanso() {
        descansoTimer?.cancel()
        descansoTimer = null
    }
    
    private fun actualizarCronometroDescanso() {
        val segundos = (tiempoDescansoRestante / 1000).toInt()
        val minutos = segundos / 60
        val segundosRestantes = segundos % 60
        textCronometroDescanso?.text = String.format("%02d:%02d", minutos, segundosRestantes)
    }
    
    private fun aumentarTiempoDescanso() {
        tiempoDescansoRestante += 10000 // Aumentar 10 segundos
        actualizarCronometroDescanso()
        
        // Reiniciar el cronómetro con el nuevo tiempo
        if (descansoTimer != null) {
            iniciarCronometroDescanso()
        }
    }
    
    private fun saltarDescanso() {
        ocultarDescanso()
    }
    
    private fun completarRutina() {
        // Regresar al fragment anterior (EjerciciosDetalleFragment)
        // Usar el childFragmentManager del padre si existe, sino el parentFragmentManager
        val fragmentManager = parentFragment?.childFragmentManager ?: parentFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        }
    }
    
    private fun cargarGifEjercicio(nombreEjercicio: String) {
        val placeholderView = view?.findViewById<View>(R.id.illustrationPlaceholder)
        
        // Cargar GIF en el ImageView superior
        imageViewGif?.let { imageView ->
            // Asegurar que el ImageView esté visible desde el inicio
            imageView.visibility = View.VISIBLE
            placeholderView?.visibility = View.GONE
            
            // Usar post para asegurar que el ImageView tenga dimensiones
            imageView.post {
                cargarGifEnImageView(imageView, placeholderView, nombreEjercicio)
            }
        } ?: run {
            // Si imageViewGif es null, loguear el error
            Log.e("GIF", "imageViewGif es null!")
        }
        
        // Cargar GIF en el ImageView inferior
        imageViewGifBottom?.let { imageView ->
            imageView.visibility = View.VISIBLE
            imageView.post {
                cargarGifEnImageView(imageView, null, nombreEjercicio)
            }
        }
    }
    
    private fun cargarGifEnImageView(imageView: ImageView, placeholderView: View?, nombreEjercicio: String) {
            
            // Normalizar nombre del ejercicio para buscar el GIF
            // Primero, normalizar caracteres especiales y espacios
            var nombreNormalizado = nombreEjercicio.lowercase()
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("ñ", "n").replace("ü", "u")
                .replace("(", "").replace(")", "")
                .trim()
            
            // Reemplazar palabras comunes que no están en los nombres de archivos
            nombreNormalizado = nombreNormalizado
                .replace(" cada lado", "")
                .replace(" cada", "")
                .replace(" lado", "")
                .replace(" por encima", "")
                .replace(" hacia", "")
                .replace(" con ", "_con_")
                .replace(" de ", "_de_")
                .replace(" en ", "_en_")
                .replace(" ", "_")
            
            // Limpiar guiones bajos múltiples
            while (nombreNormalizado.contains("__")) {
                nombreNormalizado = nombreNormalizado.replace("__", "_")
            }
            nombreNormalizado = nombreNormalizado.trim('_')
            
            // Lista de variaciones posibles del nombre para intentar
            val variaciones = mutableListOf(nombreNormalizado)
            
            // Agregar variaciones comunes
            if (nombreNormalizado.contains("_")) {
                variaciones.add(nombreNormalizado.replace("_", ""))
            }
            if (nombreNormalizado.endsWith("s")) {
                variaciones.add(nombreNormalizado.dropLast(1)) // Sin la 's' final
            }
            
            // Mapeo específico para ejercicios que tienen nombres diferentes en los archivos
            val mapeoNombres = mapOf(
                "sentadillas" to "sentadillas_con_peso",
                "sentadillas_con_peso" to "sentadillas_con_peso",
                "zancadas" to "zancadas",
                "zancadas_con_pesas" to "zancadas_con_pesas",
                "flexiones" to "flexiones",
                "flexiones_diamante" to "flexiones_diamante",
                "flexiones_inclinadas" to "flexiones_inclinadas",
                "abdominales" to "abdominales",
                "abdominales_bicicleta" to "abdominales_bicicleta",
                "burpees" to "burpees",
                "mountain_climbers" to "mountain_climbers",
                "escaladores" to "escaladores",
                "jumping_jacks" to "jumping_jacks",
                "jump_squats" to "jump_squats",
                "sentadillas_con_salto" to "jump_squats",
                "plancha" to "plancha",
                "plancha_lateral" to "plancha_lateral",
                "curl_de_biceps" to "curl_de_biceps",
                "curl_de_biceps_con_barra" to "curl_de_biceps_con_barra",
                "curl_martillo" to "curl_martillo",
                "extensiones_de_triceps" to "extensiones de triceps",
                "extensiones" to "extensiones de triceps",
                "fondos_de_triceps" to "fondos_de_triceps",
                "press_de_banca" to "press_de_banca",
                "press_de_banca_con_mancuernas" to "press_de_banca_con_mancuernas",
                "press_de_hombros" to "press_de_hombros",
                "press_militar" to "press_militar",
                "press_inclinado" to "press_inclinado",
                "press_frances" to "press_frances",
                "remo_con_barra" to "remo_con_barra",
                "remo_con_pesas" to "remo_con_pesas",
                "remo_inclinado" to "remo_inclinado",
                "remo_t" to "remo_t",
                "peso_muerto" to "peso_muerto",
                "peso_muerto_con_mancuernas" to "peso_muerto_con_mancuernas",
                "peso_muerto_rumano" to "peso_muerto_rumano",
                "sentadillas_frontales" to "sentadillas_frontales",
                "sentadillas_goblet" to "sentadillas_goblet",
                "sentadilla_sumo" to "sentadilla_sumo",
                "elevaciones_de_talon" to "elevaciones_de_talon",
                "elevaciones_frontales" to "elevaciones_frontales",
                "elevaciones_laterales" to "elevaciones_laterales",
                "elevaciones_de_pierna_lateral" to "elevaciones_de_pierna_lateral",
                "estocadas_laterales" to "estocadas_laterales",
                "glute_bridge" to "glute_bridge",
                "high_knees" to "high_knees",
                "skipping" to "skipping",
                "superman" to "superman",
                "wall_sit" to "wall_sit"
            )
            
            // Si hay un mapeo específico, agregarlo primero
            mapeoNombres[nombreNormalizado]?.let { nombreMapeado ->
                variaciones.add(0, nombreMapeado) // Agregar al inicio para prioridad
            }
            
            // Buscar GIF en assets/gifs_ejercicios/
            val extensiones = listOf(".gif", ".GIF")
            var gifEncontrado = false
            
            // Obtener dimensiones del ImageView o usar valores por defecto
            val displayMetrics = resources.displayMetrics
            val maxWidth = if (imageView.width > 0) imageView.width else displayMetrics.widthPixels
            val maxHeight = if (imageView.height > 0) imageView.height else (displayMetrics.heightPixels / 2)
            
            // Configurar opciones para reducir el uso de memoria
            val requestOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE) // No cachear en disco para assets
                .skipMemoryCache(false) // Permitir cache en memoria para mejor rendimiento
                .override(maxWidth, maxHeight) // Redimensionar al tamaño máximo disponible
                .fitCenter() // Ajustar al centro
            
            // Intentar cargar con cada variación del nombre
            for (variacion in variaciones) {
                if (gifEncontrado) break
                
                for (ext in extensiones) {
                    try {
                        // El nombre puede tener espacios si viene del mapeo
                        val ruta = "gifs_ejercicios/$variacion$ext"
                        // Verificar que el archivo existe
                        val inputStream = requireContext().assets.open(ruta)
                        inputStream.close()
                        
                        // Si llegamos aquí, el archivo existe, cargarlo con Glide
                        // Glide necesita el formato "file:///android_asset/" para assets
                        val assetPath = "file:///android_asset/$ruta"
                        
                        // Asegurar visibilidad
                        placeholderView?.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        
                        // Cargar el GIF usando Glide (sin .asGif() para evitar problemas)
                        Glide.with(this)
                            .load(assetPath)
                            .apply(requestOptions)
                            .into(imageView)
                        
                        // Si llegamos aquí sin excepción, el GIF se está cargando
                        gifEncontrado = true
                        break
                    } catch (e: Exception) {
                        // Continuar con el siguiente intento
                        Log.d("GIF", "No se encontró: gifs_ejercicios/$variacion$ext")
                    }
                }
            }
            
        if (!gifEncontrado) {
            // Si no se encuentra el GIF, mostrar placeholder
            imageView.visibility = View.GONE
            placeholderView?.visibility = View.VISIBLE
            Log.d("GIF", "No se encontró GIF para: $nombreEjercicio (normalizado: $nombreNormalizado)")
        }
    }
    
    private fun crearEjercicio(nombre: String, duracion: String, descripcion: String, duracionSegundos: Int = 30, instrucciones: String = ""): Ejercicio {
        // Detectar si es por repeticiones (contiene "x" o "repeticiones")
        val esRepeticiones = duracion.contains("x", ignoreCase = true) || 
                             duracion.contains("repeticiones", ignoreCase = true)
        
        val instruccionesFinales = if (instrucciones.isNotEmpty()) instrucciones else descripcion
        
        if (esRepeticiones) {
            // Extraer número de repeticiones
            val regex = Regex("(\\d+)")
            val match = regex.find(duracion)
            val repeticiones = match?.value?.toIntOrNull() ?: 0
            return Ejercicio(nombre, duracion, descripcion, "", 0, true, repeticiones, instruccionesFinales)
        }
        return Ejercicio(nombre, duracion, descripcion, "", duracionSegundos, false, 0, instruccionesFinales)
    }
    
    private fun ejercicioTieneImagen(nombreEjercicio: String, tipoRutina: String): Boolean {
        val context = context ?: return false
        
        val carpetaCategoria = when (tipoRutina.lowercase()) {
            "torso" -> "torso"
            "piernas" -> "piernas"
            "fuerza" -> "fuerza"
            "cardio" -> "cardio"
            "sin equipamiento" -> "sin_equipamiento"
            "con pesas" -> "con_pesas"
            "yoga" -> "yoga"
            else -> {
                when {
                    tipoRutina.lowercase().contains("torso") -> "torso"
                    tipoRutina.lowercase().contains("pierna") -> "piernas"
                    tipoRutina.lowercase().contains("fuerza") -> "fuerza"
                    tipoRutina.lowercase().contains("cardio") -> "cardio"
                    tipoRutina.lowercase().contains("equipamiento") -> "sin_equipamiento"
                    tipoRutina.lowercase().contains("pesa") -> "con_pesas"
                    tipoRutina.lowercase().contains("yoga") || tipoRutina.lowercase().contains("postura") -> "yoga"
                    else -> null
                }
            }
        }
        
        if (carpetaCategoria == null) {
            return false
        }
        
        val nombreArchivo = nombreEjercicio.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("ñ", "n").replace("ü", "u")
            .replace(" ", "_")
            .replace("cada lado", "")
            .replace("(", "").replace(")", "")
            .replace("__", "_")
            .trim()
        
        val extensiones = listOf(".png", ".jpg", ".jpeg", ".webp")
        val nombresArchivo = listOf(nombreArchivo, "imagen", "image", "foto", "photo")
        
        for (nombreArch in nombresArchivo) {
            for (ext in extensiones) {
                try {
                    val ruta = "ejercicios/$carpetaCategoria/$nombreArch$ext"
                    context.assets.open(ruta).use { 
                        Log.d("EjerciciosRutina", "✓ Imagen encontrada para '$nombreEjercicio': $ruta (normalizado: $nombreArchivo)")
                        return true 
                    }
                } catch (e: java.io.IOException) {
                    // Continuar con el siguiente nombre o extensión
                }
            }
        }
        Log.d("EjerciciosRutina", "✗ NO se encontró imagen para '$nombreEjercicio' (normalizado: $nombreArchivo, carpeta: $carpetaCategoria)")
        return false
    }
    
    private fun generarEjercicios(tipoRutina: String): List<Ejercicio> {
        val todosEjercicios = when (tipoRutina.lowercase()) {
            "torso" -> listOf(
                crearEjercicio("Flexiones", "30 seg", "Realiza flexiones manteniendo el cuerpo recto", 30, 
                    "1. Colócate en posición de plancha con las manos separadas al ancho de los hombros.\n2. Mantén el cuerpo recto desde la cabeza hasta los talones.\n3. Baja el cuerpo flexionando los codos hasta casi tocar el suelo.\n4. Empuja hacia arriba hasta extender completamente los brazos.\n5. Repite manteniendo el ritmo constante."),
                crearEjercicio("Plancha", "45 seg", "Mantén la posición de plancha con el cuerpo recto", 45,
                    "1. Colócate boca abajo apoyándote en los antebrazos y las puntas de los pies.\n2. Mantén el cuerpo recto como una tabla, sin arquear la espalda.\n3. Contrae el abdomen y los glúteos.\n4. Respira de forma constante.\n5. Mantén la posición sin moverte."),
                crearEjercicio("Fondos de tríceps", "30 seg", "Realiza fondos usando una silla o banco", 30,
                    "1. Siéntate en el borde de una silla o banco con las manos agarrando el borde.\n2. Desliza el cuerpo hacia adelante hasta que los glúteos queden fuera del asiento.\n3. Flexiona los codos bajando el cuerpo.\n4. Extiende los brazos para volver a la posición inicial.\n5. Mantén los pies firmes en el suelo."),
                crearEjercicio("Abdominales", "30 seg", "Contrae el abdomen levantando el torso", 30,
                    "1. Acuéstate boca arriba con las rodillas flexionadas y los pies en el suelo.\n2. Coloca las manos detrás de la cabeza o cruzadas en el pecho.\n3. Levanta el torso contrayendo el abdomen.\n4. Lleva los codos hacia las rodillas sin forzar el cuello.\n5. Baja controladamente y repite."),
                crearEjercicio("Flexiones inclinadas", "30 seg", "Flexiones con pies elevados para mayor intensidad", 30,
                    "1. Coloca los pies en una superficie elevada (silla o banco).\n2. Colócate en posición de flexión con las manos en el suelo.\n3. Realiza flexiones manteniendo el cuerpo recto.\n4. Baja el pecho hacia el suelo flexionando los codos.\n5. Empuja hacia arriba con fuerza."),
                crearEjercicio("Plancha lateral", "30 seg cada lado", "Mantén la plancha de lado para trabajar oblicuos", 30,
                    "1. Acuéstate de lado apoyándote en un antebrazo y el borde del pie.\n2. Levanta la cadera formando una línea recta con el cuerpo.\n3. Mantén el brazo libre extendido hacia arriba.\n4. Contrae el abdomen y los oblicuos.\n5. Mantén la posición sin balancearte."),
                crearEjercicio("Superman", "30 seg", "Acostado boca abajo, levanta brazos y piernas", 30,
                    "1. Acuéstate boca abajo con los brazos extendidos hacia adelante.\n2. Levanta simultáneamente el pecho, brazos y piernas.\n3. Mantén la posición como si fueras Superman volando.\n4. Contrae los glúteos y la espalda baja.\n5. Baja controladamente y repite."),
                crearEjercicio("Mountain climbers", "30 seg", "Alterna las piernas como corriendo en posición de plancha", 30,
                    "1. Colócate en posición de plancha con las manos en el suelo.\n2. Alterna llevando las rodillas hacia el pecho.\n3. Mantén el cuerpo recto y el core contraído.\n4. Aumenta la velocidad gradualmente.\n5. Respira de forma constante."),
                crearEjercicio("Burpees", "30 seg", "Salto, flexión y vuelta a posición inicial", 30,
                    "1. Comienza de pie.\n2. Agáchate y coloca las manos en el suelo.\n3. Salta los pies hacia atrás a posición de plancha.\n4. Haz una flexión (opcional).\n5. Salta los pies hacia adelante y salta hacia arriba con los brazos extendidos."),
                crearEjercicio("Flexiones diamante", "30 seg", "Flexiones con manos en forma de diamante", 30,
                    "1. Colócate en posición de flexión.\n2. Coloca las manos juntas formando un diamante con los pulgares e índices.\n3. Baja el pecho hacia las manos.\n4. Mantén los codos cerca del cuerpo.\n5. Empuja hacia arriba con fuerza."),
                crearEjercicio("Abdominales bicicleta", "30 seg", "Simula pedaleo acostado para trabajar el core", 30,
                    "1. Acuéstate boca arriba con las manos detrás de la cabeza.\n2. Levanta las piernas y simula el movimiento de pedaleo.\n3. Alterna llevando el codo opuesto hacia la rodilla contraria.\n4. Mantén el cuello relajado.\n5. Continúa el movimiento fluido.")
            )
            "piernas" -> listOf(
                Ejercicio("Zancadas", "30 seg cada pierna", "Da un paso largo y baja la rodilla trasera", "", 30),
                Ejercicio("Elevaciones de talón", "30 seg", "Levántate sobre las puntas de los pies", "", 30),
                Ejercicio("Sentadilla sumo", "30 seg", "Sentadilla con piernas abiertas", "", 30),
                Ejercicio("Sentadillas con salto", "30 seg", "Sentadillas explosivas con salto", "", 30),
                Ejercicio("Elevaciones de pierna lateral", "30 seg", "Eleva la pierna lateralmente de pie", "", 30),
                Ejercicio("Wall sit", "45 seg", "Mantén posición de sentadilla contra la pared", "", 45),
                Ejercicio("Glute bridge", "30 seg", "Eleva la cadera acostado boca arriba", "", 30)
            )
            "fuerza" -> listOf(
                crearEjercicio("Peso muerto", "x10", "Levanta el peso manteniendo la espalda recta", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Agarra la barra con las manos separadas al ancho de los hombros.\n3. Flexiona las caderas y rodillas manteniendo la espalda recta.\n4. Levanta la barra extendiendo las caderas y rodillas.\n5. Mantén la barra cerca del cuerpo durante todo el movimiento."),
                crearEjercicio("Sentadillas con peso", "x12", "Sentadillas con barra o mancuernas", duracionSegundos = 0, instrucciones =
                    "1. Coloca la barra sobre los hombros o sostén mancuernas.\n2. Realiza una sentadilla normal.\n3. Baja hasta que los muslos queden paralelos al suelo.\n4. Mantén el torso recto y el core contraído.\n5. Empuja con los talones para subir."),
                crearEjercicio("Press militar", "x10", "Empuja el peso por encima de la cabeza", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén la barra o mancuernas a la altura de los hombros.\n3. Empuja el peso hacia arriba extendiendo los brazos.\n4. Mantén el core contraído y la espalda recta.\n5. Baja controladamente."),
                crearEjercicio("Remo con barra", "x12", "Tira la barra hacia el pecho", duracionSegundos = 0, instrucciones =
                    "1. Flexiona las caderas manteniendo la espalda recta.\n2. Agarra la barra con las manos separadas al ancho de los hombros.\n3. Tira la barra hacia el torso.\n4. Junta las escápulas al final del movimiento.\n5. Baja controladamente."),
                crearEjercicio("Press inclinado", "x12", "Press de banca en banco inclinado", duracionSegundos = 0, instrucciones =
                    "1. Ajusta el banco a un ángulo de 30-45 grados.\n2. Acuéstate y agarra la barra.\n3. Baja la barra hacia la parte superior del pecho.\n4. Empuja hacia arriba extendiendo los brazos.\n5. Mantén los pies firmes en el suelo."),
                crearEjercicio("Peso muerto rumano", "x10", "Variación de peso muerto con énfasis en isquiotibiales", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén la barra con las manos separadas al ancho de los hombros.\n3. Flexiona las caderas manteniendo las rodillas ligeramente flexionadas.\n4. Baja la barra a lo largo de las piernas.\n5. Extiende las caderas para volver a la posición inicial."),
                crearEjercicio("Sentadillas frontales", "x12", "Sentadillas con barra al frente", duracionSegundos = 0, instrucciones =
                    "1. Coloca la barra sobre la parte frontal de los hombros.\n2. Cruza los brazos para sostener la barra.\n3. Realiza una sentadilla manteniendo el torso recto.\n4. Mantén los codos elevados.\n5. Empuja con los talones para subir."),
                crearEjercicio("Remo T", "x12", "Remo con barra en posición T", duracionSegundos = 0, instrucciones =
                    "1. Flexiona las caderas manteniendo la espalda recta.\n2. Agarra la barra con las manos separadas.\n3. Tira la barra hacia el torso.\n4. Mantén el torso estable.\n5. Baja controladamente."),
                crearEjercicio("Curl de bíceps con barra", "x12", "Flexión de brazos con barra", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén la barra con las palmas hacia adelante.\n3. Flexiona los codos levantando la barra.\n4. Mantén los codos cerca del cuerpo.\n5. Baja controladamente."),
                crearEjercicio("Extensiones de tríceps", "x12", "Extiende los brazos trabajando tríceps", duracionSegundos = 0, instrucciones =
                    "1. Sostén una mancuerna o barra por encima de la cabeza.\n2. Flexiona los codos bajando el peso detrás de la cabeza.\n3. Extiende los brazos hacia arriba.\n4. Mantén los codos apuntando hacia adelante.\n5. Repite el movimiento."),
                crearEjercicio("Curl martillo", "x12", "Curl de bíceps con agarre neutro", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas con las palmas enfrentadas.\n3. Flexiona los codos levantando las mancuernas.\n4. Mantén los codos cerca del cuerpo.\n5. Baja controladamente.")
            )
            "cardio" -> listOf(
                crearEjercicio("Burpees", "30 seg", "Salto, flexión y vuelta a posición inicial", 30,
                    "1. Comienza de pie.\n2. Agáchate y coloca las manos en el suelo.\n3. Salta los pies hacia atrás a posición de plancha.\n4. Haz una flexión (opcional).\n5. Salta los pies hacia adelante y salta hacia arriba con los brazos extendidos."),
                crearEjercicio("Mountain climbers", "30 seg", "Alterna las piernas como corriendo en posición de plancha", 30,
                    "1. Colócate en posición de plancha con las manos en el suelo.\n2. Alterna llevando las rodillas hacia el pecho.\n3. Mantén el cuerpo recto y el core contraído.\n4. Aumenta la velocidad gradualmente.\n5. Respira de forma constante."),
                crearEjercicio("Jumping jacks", "30 seg", "Salta abriendo piernas y brazos", 30,
                    "1. Párate con los pies juntos y los brazos a los lados.\n2. Salta abriendo las piernas y levantando los brazos por encima de la cabeza.\n3. Salta de nuevo cerrando las piernas y bajando los brazos.\n4. Mantén el ritmo constante.\n5. Aterriza suavemente en cada salto."),
                crearEjercicio("High knees", "30 seg", "Corre en el lugar levantando las rodillas", 30,
                    "1. Párate derecho.\n2. Corre en el lugar levantando las rodillas alto.\n3. Alterna las piernas rápidamente.\n4. Balancea los brazos naturalmente.\n5. Mantén el torso recto y el core contraído."),
                crearEjercicio("Jump squats", "30 seg", "Salta desde posición de sentadilla", 30,
                    "1. Realiza una sentadilla normal.\n2. Al subir, salta explosivamente hacia arriba.\n3. Extiende los brazos hacia arriba al saltar.\n4. Aterriza suavemente en posición de sentadilla.\n5. Repite inmediatamente."),
                crearEjercicio("Escaladores", "30 seg", "Mountain climbers a alta velocidad", 30,
                    "1. Colócate en posición de plancha.\n2. Alterna las piernas rápidamente.\n3. Lleva las rodillas hacia el pecho lo más rápido posible.\n4. Mantén el core contraído.\n5. Aumenta la velocidad gradualmente."),
                crearEjercicio("Skipping", "30 seg", "Corre en el lugar con rodillas altas", 30,
                    "1. Párate derecho.\n2. Corre en el lugar levantando las rodillas alto.\n3. Alterna las piernas rápidamente.\n4. Mantén el torso recto.\n5. Balancea los brazos naturalmente.")
            )
            "sin equipamiento" -> listOf(
                crearEjercicio("Flexiones", "30 seg", "Ejercicio básico para pecho y brazos", 30,
                    "1. Colócate en posición de plancha con las manos separadas al ancho de los hombros.\n2. Mantén el cuerpo recto desde la cabeza hasta los talones.\n3. Baja el cuerpo flexionando los codos hasta casi tocar el suelo.\n4. Empuja hacia arriba hasta extender completamente los brazos.\n5. Repite manteniendo el ritmo constante."),
                crearEjercicio("Sentadillas", "30 seg", "Ejercicio fundamental para piernas", 30,
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Baja como si te fueras a sentar en una silla.\n3. Mantén las rodillas alineadas con los dedos de los pies.\n4. Baja hasta que los muslos queden paralelos al suelo.\n5. Empuja con los talones para volver a la posición inicial."),
                crearEjercicio("Plancha", "45 seg", "Fortalece el core", 45,
                    "1. Colócate boca abajo apoyándote en los antebrazos y las puntas de los pies.\n2. Mantén el cuerpo recto como una tabla, sin arquear la espalda.\n3. Contrae el abdomen y los glúteos.\n4. Respira de forma constante.\n5. Mantén la posición sin moverte."),
                crearEjercicio("Abdominales", "30 seg", "Fortalece el abdomen", 30,
                    "1. Acuéstate boca arriba con las rodillas flexionadas y los pies en el suelo.\n2. Coloca las manos detrás de la cabeza o cruzadas en el pecho.\n3. Levanta el torso contrayendo el abdomen.\n4. Lleva los codos hacia las rodillas sin forzar el cuello.\n5. Baja controladamente y repite."),
                crearEjercicio("Burpees", "30 seg", "Ejercicio completo de cuerpo", 30,
                    "1. Comienza de pie.\n2. Agáchate y coloca las manos en el suelo.\n3. Salta los pies hacia atrás a posición de plancha.\n4. Haz una flexión (opcional).\n5. Salta los pies hacia adelante y salta hacia arriba con los brazos extendidos."),
                crearEjercicio("Jumping jacks", "30 seg", "Salta abriendo piernas y brazos", 30,
                    "1. Párate con los pies juntos y los brazos a los lados.\n2. Salta abriendo las piernas y levantando los brazos por encima de la cabeza.\n3. Salta de nuevo cerrando las piernas y bajando los brazos.\n4. Mantén el ritmo constante.\n5. Aterriza suavemente en cada salto."),
                crearEjercicio("Zancadas", "30 seg", "Da un paso largo y baja la rodilla", 30,
                    "1. Da un paso largo hacia adelante.\n2. Baja la rodilla trasera hacia el suelo.\n3. Mantén el torso recto y el core contraído.\n4. La rodilla delantera no debe pasar la punta del pie.\n5. Empuja con el talón delantero para volver."),
                crearEjercicio("Plancha lateral", "30 seg cada lado", "Plancha de lado para oblicuos", 30,
                    "1. Acuéstate de lado apoyándote en un antebrazo y el borde del pie.\n2. Levanta la cadera formando una línea recta con el cuerpo.\n3. Mantén el brazo libre extendido hacia arriba.\n4. Contrae el abdomen y los oblicuos.\n5. Mantén la posición sin balancearte."),
                crearEjercicio("Flexiones diamante", "30 seg", "Flexiones con manos juntas", 30,
                    "1. Colócate en posición de flexión.\n2. Coloca las manos juntas formando un diamante con los pulgares e índices.\n3. Baja el pecho hacia las manos.\n4. Mantén los codos cerca del cuerpo.\n5. Empuja hacia arriba con fuerza."),
                crearEjercicio("Sentadillas con salto", "30 seg", "Sentadillas explosivas", 30,
                    "1. Realiza una sentadilla normal.\n2. Al subir, salta explosivamente hacia arriba.\n3. Extiende los brazos hacia arriba al saltar.\n4. Aterriza suavemente en posición de sentadilla.\n5. Repite inmediatamente."),
                crearEjercicio("Abdominales bicicleta", "30 seg", "Simula pedaleo acostado", 30,
                    "1. Acuéstate boca arriba con las manos detrás de la cabeza.\n2. Levanta las piernas y simula el movimiento de pedaleo.\n3. Alterna llevando el codo opuesto hacia la rodilla contraria.\n4. Mantén el cuello relajado.\n5. Continúa el movimiento fluido."),
                crearEjercicio("Superman", "30 seg", "Levanta brazos y piernas acostado boca abajo", 30,
                    "1. Acuéstate boca abajo con los brazos extendidos hacia adelante.\n2. Levanta simultáneamente el pecho, brazos y piernas.\n3. Mantén la posición como si fueras Superman volando.\n4. Contrae los glúteos y la espalda baja.\n5. Baja controladamente y repite."),
                crearEjercicio("High knees", "30 seg", "Corre en el lugar con rodillas altas", 30,
                    "1. Párate derecho.\n2. Corre en el lugar levantando las rodillas alto.\n3. Alterna las piernas rápidamente.\n4. Balancea los brazos naturalmente.\n5. Mantén el torso recto y el core contraído."),
                Ejercicio("Glute bridge", "30 seg", "Eleva la cadera acostado boca arriba", "", 30),
                crearEjercicio("Flexiones inclinadas", "30 seg", "Flexiones con pies elevados", 30,
                    "1. Coloca los pies en una superficie elevada (silla o banco).\n2. Colócate en posición de flexión con las manos en el suelo.\n3. Realiza flexiones manteniendo el cuerpo recto.\n4. Baja el pecho hacia el suelo flexionando los codos.\n5. Empuja hacia arriba con fuerza.")
            )
            "con pesas" -> listOf(
                crearEjercicio("Press de hombros", "x12", "Empuja las pesas por encima de la cabeza", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas a la altura de los hombros.\n3. Empuja las mancuernas hacia arriba.\n4. Mantén el core contraído.\n5. Baja controladamente."),
                crearEjercicio("Remo con pesas", "x12", "Tira las pesas hacia el torso", duracionSegundos = 0, instrucciones =
                    "1. Flexiona las caderas manteniendo la espalda recta.\n2. Sostén mancuernas con los brazos extendidos.\n3. Tira las mancuernas hacia el torso.\n4. Junta las escápulas al final.\n5. Baja controladamente."),
                crearEjercicio("Elevaciones laterales", "x12", "Levanta las pesas a los lados", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas a los lados.\n3. Eleva los brazos lateralmente hasta la altura de los hombros.\n4. Mantén una ligera flexión en los codos.\n5. Baja controladamente."),
                crearEjercicio("Press de banca con mancuernas", "x12", "Press de pecho con mancuernas", duracionSegundos = 0, instrucciones =
                    "1. Acuéstate en un banco con los pies en el suelo.\n2. Sostén mancuernas a la altura del pecho.\n3. Empuja las mancuernas hacia arriba.\n4. Mantén los hombros y glúteos en contacto con el banco.\n5. Baja controladamente."),
                crearEjercicio("Zancadas con pesas", "x12 cada pierna", "Zancadas sosteniendo pesas", duracionSegundos = 0, instrucciones =
                    "1. Sostén mancuernas a los lados.\n2. Da un paso largo hacia adelante.\n3. Baja la rodilla trasera.\n4. Mantén el torso recto.\n5. Empuja para volver y alterna."),
                crearEjercicio("Curl martillo", "x12", "Curl con agarre neutro", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas con las palmas enfrentadas.\n3. Flexiona los codos levantando las mancuernas.\n4. Mantén los codos cerca del cuerpo.\n5. Baja controladamente."),
                crearEjercicio("Peso muerto con mancuernas", "x10", "Peso muerto con mancuernas", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas con los brazos extendidos.\n3. Flexiona las caderas manteniendo la espalda recta.\n4. Baja las mancuernas a lo largo de las piernas.\n5. Extiende las caderas para volver."),
                crearEjercicio("Elevaciones frontales", "x12", "Eleva las pesas al frente", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas con los brazos extendidos.\n3. Eleva los brazos hacia adelante hasta la altura de los hombros.\n4. Mantén los brazos rectos.\n5. Baja controladamente."),
                crearEjercicio("Remo inclinado", "x12", "Remo inclinado con mancuernas", duracionSegundos = 0, instrucciones =
                    "1. Flexiona las caderas manteniendo la espalda recta.\n2. Sostén mancuernas con los brazos extendidos.\n3. Tira las mancuernas hacia el torso.\n4. Junta las escápulas.\n5. Baja controladamente."),
                crearEjercicio("Press francés", "x12", "Extensión de tríceps acostado", duracionSegundos = 0, instrucciones =
                    "1. Acuéstate en un banco con los brazos extendidos.\n2. Flexiona los codos bajando las mancuernas.\n3. Extiende los brazos hacia arriba.\n4. Mantén los codos apuntando hacia adelante.\n5. Repite el movimiento."),
                crearEjercicio("Sentadillas goblet", "x12", "Sentadillas con una pesa al pecho", duracionSegundos = 0, instrucciones =
                    "1. Sostén una mancuerna o pesa rusa al pecho.\n2. Realiza una sentadilla normal.\n3. Mantén la pesa cerca del pecho.\n4. Baja hasta que los muslos queden paralelos al suelo.\n5. Empuja con los talones para subir.")
            )
            "yoga" -> listOf(
                crearEjercicio("Postura de la montaña", "60 seg", "Postura básica de pie, alineando todo el cuerpo", 60,
                    "1. Párate con los pies juntos o ligeramente separados.\n2. Distribuye el peso uniformemente en ambos pies.\n3. Alinea la cabeza, hombros, caderas y tobillos.\n4. Relaja los hombros y deja los brazos a los lados.\n5. Respira profundamente y mantén la postura."),
                crearEjercicio("Saludo al sol", "90 seg", "Secuencia completa de saludo al sol", 90,
                    "1. Comienza en postura de la montaña.\n2. Inhala y levanta los brazos por encima de la cabeza.\n3. Exhala y flexiona hacia adelante.\n4. Salta o camina hacia atrás a plancha.\n5. Continúa la secuencia fluida de movimientos."),
                crearEjercicio("Postura del guerrero I", "45 seg cada lado", "Fortalece piernas y abre caderas", 45,
                    "1. Da un paso largo hacia adelante con una pierna.\n2. Gira el pie trasero en un ángulo de 45 grados.\n3. Flexiona la rodilla delantera formando un ángulo de 90 grados.\n4. Levanta los brazos por encima de la cabeza.\n5. Mantén la cadera cuadrada y respira."),
                crearEjercicio("Postura del guerrero II", "45 seg cada lado", "Mejora equilibrio y fuerza", 45,
                    "1. Separa los pies ampliamente.\n2. Gira el pie derecho hacia afuera y el izquierdo ligeramente.\n3. Flexiona la rodilla derecha sobre el tobillo.\n4. Extiende los brazos paralelos al suelo.\n5. Mira sobre la mano derecha y mantén la postura."),
                crearEjercicio("Postura del árbol", "60 seg cada lado", "Mejora equilibrio y concentración", 60,
                    "1. Párate sobre una pierna.\n2. Coloca el pie de la otra pierna en el muslo interno o pantorrilla.\n3. Evita colocarlo en la rodilla.\n4. Une las palmas de las manos frente al pecho o por encima de la cabeza.\n5. Enfócate en un punto fijo para mantener el equilibrio."),
                crearEjercicio("Postura del perro boca abajo", "60 seg", "Estira toda la parte posterior del cuerpo", 60,
                    "1. Comienza en posición de plancha.\n2. Levanta las caderas hacia arriba y atrás.\n3. Forma una V invertida con el cuerpo.\n4. Presiona las palmas de las manos en el suelo.\n5. Mantén las piernas rectas y respira profundamente."),
                crearEjercicio("Postura del niño", "60 seg", "Relaja y estira la espalda", 60,
                    "1. Arrodíllate en el suelo con los dedos gordos de los pies juntos.\n2. Siéntate sobre los talones.\n3. Exhala y baja el torso hacia los muslos.\n4. Extiende los brazos hacia adelante o a los lados.\n5. Relaja la frente en el suelo y respira."),
                crearEjercicio("Postura de la cobra", "45 seg", "Fortalece la espalda y abre el pecho", 45,
                    "1. Acuéstate boca abajo con las palmas de las manos bajo los hombros.\n2. Presiona las palmas y levanta el pecho.\n3. Mantén los codos ligeramente flexionados.\n4. Presiona la parte superior de los pies en el suelo.\n5. Respira y mantén la postura."),
                crearEjercicio("Postura del puente", "60 seg", "Fortalece glúteos y abre caderas", 60,
                    "1. Acuéstate boca arriba con las rodillas flexionadas.\n2. Coloca los pies separados al ancho de los hombros.\n3. Presiona los pies y levanta las caderas.\n4. Entrelaza los dedos debajo de la espalda si es posible.\n5. Mantén los muslos paralelos y respira."),
                crearEjercicio("Postura de la torsión sentada", "45 seg cada lado", "Mejora flexibilidad de columna", 45,
                    "1. Siéntate con las piernas extendidas.\n2. Flexiona la rodilla derecha y coloca el pie derecho fuera del muslo izquierdo.\n3. Gira el torso hacia la derecha.\n4. Coloca el brazo izquierdo en la rodilla derecha.\n5. Respira profundamente y mantén la torsión."),
                crearEjercicio("Postura del triángulo", "45 seg cada lado", "Estira laterales y fortalece piernas", 45,
                    "1. Separa los pies ampliamente.\n2. Gira el pie derecho hacia afuera.\n3. Extiende el brazo derecho hacia abajo tocando el tobillo o el suelo.\n4. Extiende el brazo izquierdo hacia arriba.\n5. Mira hacia arriba y mantén la postura."),
                crearEjercicio("Postura del cadáver", "120 seg", "Relajación final y meditación", 120,
                    "1. Acuéstate boca arriba con las piernas extendidas.\n2. Separa los brazos ligeramente del cuerpo con las palmas hacia arriba.\n3. Cierra los ojos y relaja todo el cuerpo.\n4. Respira naturalmente y deja ir cualquier tensión.\n5. Permanece en esta postura de relajación completa.")
            )
            else -> listOf(
                Ejercicio("Calentamiento", "5 min", "Calienta tus músculos antes de comenzar", "", 300),
                Ejercicio("Ejercicio principal", "30 seg", "Realiza el ejercicio principal de la rutina", "", 30),
                Ejercicio("Estiramiento", "5 min", "Estira los músculos trabajados", "", 300)
            )
        }
        
        // Filtrar solo los ejercicios que tienen imágenes (igual que en EjerciciosDetalleFragment)
        // IMPORTANTE: Mantener el orden original después del filtro
        val ejerciciosFiltrados = todosEjercicios.filter { ejercicio ->
            val tieneImagen = ejercicioTieneImagen(ejercicio.nombre, tipoRutina)
            if (ejercicio.nombre == "Peso muerto") {
                Log.d("EjerciciosRutina", "🔍 FILTRO: 'Peso muerto' - tieneImagen: $tieneImagen")
            }
            tieneImagen
        }
        
        // Log para depuración - verificar que el orden sea correcto
        if (ejerciciosFiltrados.isNotEmpty()) {
            Log.d("EjerciciosRutina", "=== FILTRO DE EJERCICIOS ===")
            Log.d("EjerciciosRutina", "Total ejercicios ANTES del filtro: ${todosEjercicios.size}")
            Log.d("EjerciciosRutina", "Total ejercicios DESPUÉS del filtro: ${ejerciciosFiltrados.size}")
            Log.d("EjerciciosRutina", "Primer ejercicio después del filtro: ${ejerciciosFiltrados.first().nombre}")
            Log.d("EjerciciosRutina", "Lista completa de ejercicios filtrados:")
            ejerciciosFiltrados.forEachIndexed { index, ejercicio ->
                Log.d("EjerciciosRutina", "  [$index] ${ejercicio.nombre}")
            }
        }
        
        return ejerciciosFiltrados
    }
    
    override fun onPause() {
        super.onPause()
        videoView?.pause()
        if (isEnDescanso) {
            detenerCronometroDescanso()
        } else {
            pausarCronometro()
        }
        // Pausar carga de GIFs para ahorrar memoria
        Glide.with(this).pauseRequests()
    }
    
    override fun onResume() {
        super.onResume()
        // Reanudar carga de GIFs
        Glide.with(this).resumeRequests()
        // Reanudar cronómetro según el estado
        if (isEnDescanso && tiempoDescansoRestante > 0) {
            iniciarCronometroDescanso()
        } else if (isPaused && tiempoRestante > 0) {
            reanudarCronometro()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        detenerCronometro()
        detenerCronometroDescanso()
        videoView?.stopPlayback()
        videoView = null
        
        // Limpiar GIFs para liberar memoria
        imageViewGif?.let { imageView ->
            Glide.with(this).clear(imageView)
        }
        imageViewGifBottom?.let { imageView ->
            Glide.with(this).clear(imageView)
        }
    }
}

