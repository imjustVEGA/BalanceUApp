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
    private var textExerciseName: TextView? = null
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
        imageViewGif = view.findViewById(R.id.imageViewGif)
        textExerciseName = view.findViewById(R.id.textExerciseName)
        textInstrucciones = view.findViewById(R.id.textInstrucciones)
        textTimer = view.findViewById(R.id.textTimer)
        textRepeticiones = view.findViewById(R.id.textRepeticiones)
        buttonAnterior = view.findViewById(R.id.buttonAnterior)
        buttonSiguiente = view.findViewById(R.id.buttonSiguiente)
        buttonPausa = view.findViewById(R.id.buttonPausa)
        buttonDone = view.findViewById(R.id.buttonDone)
        buttonBackTop = view.findViewById(R.id.buttonBackTop)
        containerTimer = view.findViewById(R.id.containerTimer)
        containerRepeticiones = view.findViewById(R.id.containerRepeticiones)
        
        // Asegurar que el ImageView esté visible desde el inicio
        imageViewGif?.let { imageView ->
            imageView.visibility = View.VISIBLE
            imageView.bringToFront()
        }
        // Ocultar placeholder y video
        view.findViewById<View>(R.id.illustrationPlaceholder)?.visibility = View.GONE
        videoView?.visibility = View.GONE
        
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
        
        // Limpiar GIF anterior para liberar memoria
        imageViewGif?.let { imageView ->
            Glide.with(this).clear(imageView)
        }
        
        val ejercicio = ejercicios[index]
        
        // Mostrar nombre en mayúsculas
        textExerciseName?.text = ejercicio.nombre.uppercase()
        
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
        
        // Cargar GIF del ejercicio
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
    
    private fun cargarGifEjercicio(nombreEjercicio: String) {
        imageViewGif?.let { imageView ->
            val placeholderView = view?.findViewById<View>(R.id.illustrationPlaceholder)
            
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
    }
    
    private fun cargarGifEnImageView(imageView: ImageView, placeholderView: View?, nombreEjercicio: String) {
            
            // Normalizar nombre del ejercicio para buscar el GIF
            var nombreNormalizado = nombreEjercicio.lowercase()
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("ñ", "n").replace("ü", "u")
                .replace(" ", "_")
                .replace("cada_lado", "")
                .replace("cada", "")
                .replace("lado", "")
                .replace("(", "").replace(")", "")
                .replace("__", "_")
                .replace("___", "_")
                .trim()
            
            // Lista de variaciones posibles del nombre para intentar
            val variaciones = mutableListOf(nombreNormalizado)
            
            // Agregar variaciones comunes
            if (nombreNormalizado.contains("_")) {
                variaciones.add(nombreNormalizado.replace("_", ""))
            }
            if (nombreNormalizado.endsWith("s")) {
                variaciones.add(nombreNormalizado.dropLast(1)) // Sin la 's' final
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
    
    private fun generarEjercicios(tipoRutina: String): List<Ejercicio> {
        return when (tipoRutina.lowercase()) {
            "torso" -> listOf(
                crearEjercicio("Flexiones", "30 seg", "Realiza flexiones manteniendo el cuerpo recto", 30, 
                    "1. Colócate en posición de plancha con las manos separadas al ancho de los hombros.\n2. Mantén el cuerpo recto desde la cabeza hasta los talones.\n3. Baja el cuerpo flexionando los codos hasta casi tocar el suelo.\n4. Empuja hacia arriba hasta extender completamente los brazos.\n5. Repite manteniendo el ritmo constante."),
                crearEjercicio("Plancha", "45 seg", "Mantén la posición de plancha con el cuerpo recto", 45,
                    "1. Colócate boca abajo apoyándote en los antebrazos y las puntas de los pies.\n2. Mantén el cuerpo recto como una tabla, sin arquear la espalda.\n3. Contrae el abdomen y los glúteos.\n4. Respira de forma constante.\n5. Mantén la posición sin moverte."),
                crearEjercicio("Fondos de tríceps", "30 seg", "Realiza fondos usando una silla o banco", 30,
                    "1. Siéntate en el borde de una silla o banco con las manos agarrando el borde.\n2. Desliza el cuerpo hacia adelante hasta que los glúteos queden fuera del asiento.\n3. Flexiona los codos bajando el cuerpo.\n4. Extiende los brazos para volver a la posición inicial.\n5. Mantén los pies firmes en el suelo."),
                crearEjercicio("Remo con peso corporal", "30 seg", "Tira hacia atrás manteniendo la espalda recta", 30,
                    "1. Acuéstate boca abajo con los brazos extendidos hacia adelante.\n2. Levanta el pecho y los brazos simultáneamente.\n3. Junta las escápulas mientras levantas.\n4. Mantén el cuello alineado con la columna.\n5. Baja controladamente y repite."),
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
                crearEjercicio("Plancha con elevación de pierna", "30 seg", "Plancha alternando elevación de piernas", 30,
                    "1. Colócate en posición de plancha.\n2. Mantén el cuerpo recto y estable.\n3. Levanta una pierna sin arquear la espalda.\n4. Baja la pierna y alterna con la otra.\n5. Mantén el core contraído durante todo el ejercicio."),
                crearEjercicio("Abdominales bicicleta", "30 seg", "Simula pedaleo acostado para trabajar el core", 30,
                    "1. Acuéstate boca arriba con las manos detrás de la cabeza.\n2. Levanta las piernas y simula el movimiento de pedaleo.\n3. Alterna llevando el codo opuesto hacia la rodilla contraria.\n4. Mantén el cuello relajado.\n5. Continúa el movimiento fluido."),
                crearEjercicio("Flexiones con palmada", "30 seg", "Flexiones explosivas con palmada en el aire", 30,
                    "1. Realiza una flexión explosiva empujando con fuerza.\n2. Al llegar arriba, separa las manos del suelo.\n3. Da una palmada en el aire.\n4. Aterriza suavemente en posición de flexión.\n5. Repite con control y potencia."),
                crearEjercicio("Plancha con toque de hombro", "30 seg", "Plancha alternando toque de hombros", 30,
                    "1. Colócate en posición de plancha alta.\n2. Mantén el cuerpo estable y recto.\n3. Toca un hombro con la mano opuesta.\n4. Vuelve la mano al suelo.\n5. Alterna con el otro hombro manteniendo el equilibrio.")
            )
            "piernas" -> listOf(
                crearEjercicio("Sentadillas", "30 seg", "Baja como si te sentaras en una silla", 30,
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Baja como si te fueras a sentar en una silla.\n3. Mantén las rodillas alineadas con los dedos de los pies.\n4. Baja hasta que los muslos queden paralelos al suelo.\n5. Empuja con los talones para volver a la posición inicial."),
                crearEjercicio("Zancadas", "30 seg cada pierna", "Da un paso largo y baja la rodilla trasera", 30,
                    "1. Da un paso largo hacia adelante.\n2. Baja la rodilla trasera hacia el suelo.\n3. Mantén el torso recto y el core contraído.\n4. La rodilla delantera no debe pasar la punta del pie.\n5. Empuja con el talón delantero para volver."),
                crearEjercicio("Elevaciones de talón", "30 seg", "Levántate sobre las puntas de los pies", 30,
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Levántate sobre las puntas de los pies.\n3. Mantén el equilibrio y contrae las pantorrillas.\n4. Baja controladamente.\n5. Repite el movimiento de forma constante."),
                crearEjercicio("Sentadilla sumo", "30 seg", "Sentadilla con piernas abiertas", 30,
                    "1. Separa los pies más allá del ancho de los hombros.\n2. Gira los pies hacia afuera en un ángulo de 45 grados.\n3. Baja manteniendo la espalda recta.\n4. Contrae los glúteos al bajar.\n5. Empuja con los talones para subir."),
                crearEjercicio("Estocadas laterales", "30 seg", "Da un paso lateral y flexiona la rodilla", 30,
                    "1. Párate con los pies juntos.\n2. Da un paso amplio hacia un lado.\n3. Flexiona la rodilla de la pierna que se movió.\n4. Mantén la otra pierna recta.\n5. Vuelve a la posición inicial y alterna."),
                crearEjercicio("Sentadillas con salto", "30 seg", "Sentadillas explosivas con salto", 30,
                    "1. Realiza una sentadilla normal.\n2. Al subir, salta explosivamente hacia arriba.\n3. Extiende los brazos hacia arriba al saltar.\n4. Aterriza suavemente en posición de sentadilla.\n5. Repite inmediatamente."),
                crearEjercicio("Zancadas alternas", "30 seg", "Alterna zancadas hacia adelante", 30,
                    "1. Da un paso largo hacia adelante con una pierna.\n2. Baja la rodilla trasera.\n3. Empuja para volver a la posición inicial.\n4. Alterna inmediatamente con la otra pierna.\n5. Mantén el ritmo constante."),
                crearEjercicio("Elevaciones de pierna lateral", "30 seg", "Eleva la pierna lateralmente de pie", 30,
                    "1. Párate derecho con una mano en la pared para equilibrio.\n2. Levanta una pierna lateralmente.\n3. Mantén el torso recto y no te inclines.\n4. Baja controladamente.\n5. Alterna con la otra pierna."),
                crearEjercicio("Pistol squat", "30 seg", "Sentadilla a una pierna (avanzado)", 30,
                    "1. Párate sobre una pierna.\n2. Extiende la otra pierna hacia adelante.\n3. Baja en sentadilla manteniendo el equilibrio.\n4. Mantén la pierna extendida recta.\n5. Empuja para volver a la posición inicial."),
                crearEjercicio("Wall sit", "45 seg", "Mantén posición de sentadilla contra la pared", 45,
                    "1. Apoya la espalda contra la pared.\n2. Desliza hacia abajo hasta formar un ángulo de 90 grados.\n3. Mantén las rodillas alineadas con los tobillos.\n4. Contrae los cuádriceps y glúteos.\n5. Respira y mantén la posición."),
                crearEjercicio("Step ups", "30 seg", "Sube y baja un escalón o banco", 30,
                    "1. Coloca un pie sobre un escalón o banco.\n2. Empuja con el talón para subir completamente.\n3. Baja controladamente.\n4. Alterna con la otra pierna.\n5. Mantén el torso recto durante todo el movimiento."),
                crearEjercicio("Glute bridge", "30 seg", "Eleva la cadera acostado boca arriba", 30,
                    "1. Acuéstate boca arriba con las rodillas flexionadas.\n2. Coloca los pies separados al ancho de los hombros.\n3. Eleva la cadera contrayendo los glúteos.\n4. Forma una línea recta desde las rodillas hasta los hombros.\n5. Baja controladamente y repite.")
            )
            "fuerza" -> listOf(
                crearEjercicio("Press de banca", "x12", "Empuja el peso alejándolo del pecho", duracionSegundos = 0, instrucciones = 
                    "1. Acuéstate en el banco con los pies firmes en el suelo.\n2. Agarra la barra con las manos separadas al ancho de los hombros.\n3. Baja la barra controladamente hacia el pecho.\n4. Empuja la barra hacia arriba extendiendo los brazos.\n5. Mantén los hombros y glúteos en contacto con el banco."),
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
                crearEjercicio("Press de hombros con barra", "x10", "Press militar con barra", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén la barra a la altura de los hombros.\n3. Empuja la barra hacia arriba.\n4. Mantén el core contraído.\n5. Baja controladamente."),
                crearEjercicio("Remo T", "x12", "Remo con barra en posición T", duracionSegundos = 0, instrucciones =
                    "1. Flexiona las caderas manteniendo la espalda recta.\n2. Agarra la barra con las manos separadas.\n3. Tira la barra hacia el torso.\n4. Mantén el torso estable.\n5. Baja controladamente."),
                crearEjercicio("Curl de bíceps con barra", "x12", "Flexión de brazos con barra", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén la barra con las palmas hacia adelante.\n3. Flexiona los codos levantando la barra.\n4. Mantén los codos cerca del cuerpo.\n5. Baja controladamente."),
                crearEjercicio("Extensiones de tríceps", "x12", "Extiende los brazos trabajando tríceps", duracionSegundos = 0, instrucciones =
                    "1. Sostén una mancuerna o barra por encima de la cabeza.\n2. Flexiona los codos bajando el peso detrás de la cabeza.\n3. Extiende los brazos hacia arriba.\n4. Mantén los codos apuntando hacia adelante.\n5. Repite el movimiento."),
                crearEjercicio("Press de piernas", "x12", "Press en máquina para piernas", duracionSegundos = 0, instrucciones =
                    "1. Siéntate en la máquina con la espalda apoyada.\n2. Coloca los pies en la plataforma separados al ancho de los hombros.\n3. Empuja la plataforma extendiendo las piernas.\n4. No bloquees las rodillas completamente.\n5. Baja controladamente."),
                crearEjercicio("Prensa de pecho", "x12", "Press en máquina para pecho", duracionSegundos = 0, instrucciones =
                    "1. Siéntate en la máquina con la espalda apoyada.\n2. Agarra las manijas a la altura del pecho.\n3. Empuja las manijas hacia adelante.\n4. Mantén los hombros relajados.\n5. Vuelve controladamente."),
                crearEjercicio("Jalones al pecho", "x12", "Jala el peso hacia el pecho", duracionSegundos = 0, instrucciones =
                    "1. Siéntate en la máquina con las rodillas fijadas.\n2. Agarra la barra con las manos separadas.\n3. Tira la barra hacia el pecho.\n4. Junta las escápulas al final.\n5. Vuelve controladamente."),
                crearEjercicio("Elevaciones laterales", "x12", "Eleva mancuernas lateralmente", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas a los lados con los brazos extendidos.\n3. Eleva los brazos lateralmente hasta la altura de los hombros.\n4. Mantén una ligera flexión en los codos.\n5. Baja controladamente."),
                crearEjercicio("Curl martillo", "x12", "Curl de bíceps con agarre neutro", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas con las palmas enfrentadas.\n3. Flexiona los codos levantando las mancuernas.\n4. Mantén los codos cerca del cuerpo.\n5. Baja controladamente."),
                crearEjercicio("Fondos en paralelas", "x10", "Fondos para tríceps y pecho", duracionSegundos = 0, instrucciones =
                    "1. Agarra las barras paralelas con las manos.\n2. Extiende los brazos completamente.\n3. Flexiona los codos bajando el cuerpo.\n4. Baja hasta que los brazos formen un ángulo de 90 grados.\n5. Empuja hacia arriba extendiendo los brazos.")
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
                crearEjercicio("Correr en el lugar", "30 seg", "Corre en el lugar a buen ritmo", 30,
                    "1. Párate derecho.\n2. Corre en el lugar levantando los pies alternativamente.\n3. Balancea los brazos como si estuvieras corriendo.\n4. Mantén un ritmo constante.\n5. Respira de forma regular."),
                crearEjercicio("Escaladores", "30 seg", "Mountain climbers a alta velocidad", 30,
                    "1. Colócate en posición de plancha.\n2. Alterna las piernas rápidamente.\n3. Lleva las rodillas hacia el pecho lo más rápido posible.\n4. Mantén el core contraído.\n5. Aumenta la velocidad gradualmente."),
                crearEjercicio("Saltos de tijera", "30 seg", "Salta alternando piernas adelante y atrás", 30,
                    "1. Párate con una pierna adelante y otra atrás.\n2. Salta cambiando la posición de las piernas.\n3. Alterna rápidamente.\n4. Balancea los brazos para mantener el equilibrio.\n5. Mantén el ritmo constante."),
                crearEjercicio("Burpees sin flexión", "30 seg", "Burpees sin hacer flexión completa", 30,
                    "1. Comienza de pie.\n2. Agáchate y coloca las manos en el suelo.\n3. Salta los pies hacia atrás a posición de plancha.\n4. Salta los pies hacia adelante sin hacer flexión.\n5. Salta hacia arriba con los brazos extendidos."),
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
                crearEjercicio("Mountain climbers", "30 seg", "Alterna piernas en posición de plancha", 30,
                    "1. Colócate en posición de plancha con las manos en el suelo.\n2. Alterna llevando las rodillas hacia el pecho.\n3. Mantén el cuerpo recto y el core contraído.\n4. Aumenta la velocidad gradualmente.\n5. Respira de forma constante."),
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
                crearEjercicio("Wall sit", "45 seg", "Sentadilla estática contra la pared", 45,
                    "1. Apoya la espalda contra la pared.\n2. Desliza hacia abajo hasta formar un ángulo de 90 grados.\n3. Mantén las rodillas alineadas con los tobillos.\n4. Contrae los cuádriceps y glúteos.\n5. Respira y mantén la posición."),
                crearEjercicio("High knees", "30 seg", "Corre en el lugar con rodillas altas", 30,
                    "1. Párate derecho.\n2. Corre en el lugar levantando las rodillas alto.\n3. Alterna las piernas rápidamente.\n4. Balancea los brazos naturalmente.\n5. Mantén el torso recto y el core contraído."),
                crearEjercicio("Glute bridge", "30 seg", "Eleva la cadera acostado", 30,
                    "1. Acuéstate boca arriba con las rodillas flexionadas.\n2. Coloca los pies separados al ancho de los hombros.\n3. Eleva la cadera contrayendo los glúteos.\n4. Forma una línea recta desde las rodillas hasta los hombros.\n5. Baja controladamente y repite."),
                crearEjercicio("Estocadas laterales", "30 seg", "Zancadas hacia los lados", 30,
                    "1. Párate con los pies juntos.\n2. Da un paso amplio hacia un lado.\n3. Flexiona la rodilla de la pierna que se movió.\n4. Mantén la otra pierna recta.\n5. Vuelve a la posición inicial y alterna."),
                crearEjercicio("Flexiones inclinadas", "30 seg", "Flexiones con pies elevados", 30,
                    "1. Coloca los pies en una superficie elevada (silla o banco).\n2. Colócate en posición de flexión con las manos en el suelo.\n3. Realiza flexiones manteniendo el cuerpo recto.\n4. Baja el pecho hacia el suelo flexionando los codos.\n5. Empuja hacia arriba con fuerza."),
                crearEjercicio("Jump squats", "30 seg", "Salta desde posición de sentadilla", 30,
                    "1. Realiza una sentadilla normal.\n2. Al subir, salta explosivamente hacia arriba.\n3. Extiende los brazos hacia arriba al saltar.\n4. Aterriza suavemente en posición de sentadilla.\n5. Repite inmediatamente."),
                crearEjercicio("Plancha con toque de hombro", "30 seg", "Plancha alternando toque de hombros", 30,
                    "1. Colócate en posición de plancha alta.\n2. Mantén el cuerpo estable y recto.\n3. Toca un hombro con la mano opuesta.\n4. Vuelve la mano al suelo.\n5. Alterna con el otro hombro manteniendo el equilibrio.")
            )
            "con pesas" -> listOf(
                crearEjercicio("Curl de bíceps", "x12", "Flexiona los brazos levantando las pesas", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas con las palmas hacia adelante.\n3. Flexiona los codos levantando las mancuernas.\n4. Mantén los codos cerca del cuerpo.\n5. Baja controladamente."),
                crearEjercicio("Press de hombros", "x12", "Empuja las pesas por encima de la cabeza", duracionSegundos = 0, instrucciones =
                    "1. Párate con los pies separados al ancho de los hombros.\n2. Sostén mancuernas a la altura de los hombros.\n3. Empuja las mancuernas hacia arriba.\n4. Mantén el core contraído.\n5. Baja controladamente."),
                crearEjercicio("Sentadillas con pesas", "x12", "Sentadillas sosteniendo pesas", duracionSegundos = 0, instrucciones =
                    "1. Sostén mancuernas a los lados.\n2. Realiza una sentadilla normal.\n3. Baja hasta que los muslos queden paralelos al suelo.\n4. Mantén el torso recto.\n5. Empuja con los talones para subir."),
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
                crearEjercicio("Extensiones de tríceps", "x12", "Extiende los brazos con pesa", duracionSegundos = 0, instrucciones =
                    "1. Sostén una mancuerna por encima de la cabeza.\n2. Flexiona los codos bajando el peso detrás de la cabeza.\n3. Extiende los brazos hacia arriba.\n4. Mantén los codos apuntando hacia adelante.\n5. Repite el movimiento."),
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
    }
    
    override fun onPause() {
        super.onPause()
        videoView?.pause()
        pausarCronometro()
        // Pausar carga de GIFs para ahorrar memoria
        Glide.with(this).pauseRequests()
    }
    
    override fun onResume() {
        super.onResume()
        // Reanudar carga de GIFs
        Glide.with(this).resumeRequests()
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
        
        // Limpiar GIF para liberar memoria
        imageViewGif?.let { imageView ->
            Glide.with(this).clear(imageView)
        }
    }
}

