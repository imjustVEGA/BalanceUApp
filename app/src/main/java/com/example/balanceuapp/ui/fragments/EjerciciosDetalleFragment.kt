package com.example.balanceuapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.balanceuapp.R

class EjerciciosDetalleFragment : Fragment() {
    
    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_INFO = "info"
        private const val ARG_DESCRIPTION = "description"
        
        fun newInstance(title: String, info: String, description: String): EjerciciosDetalleFragment {
            val fragment = EjerciciosDetalleFragment()
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
        return inflater.inflate(R.layout.fragment_ejercicios_detalle, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Configurar las vistas
        view.findViewById<TextView>(R.id.textTitle)?.text = title ?: "DÍA 1"
        
        // Extraer información del info string (ej: "15 ejercicios • 30 min • Intermedio")
        val infoText = info ?: ""
        val ejercicios = generarEjercicios(title ?: "")
        
        // Calcular duración total considerando ambos tipos de ejercicios
        val duracionTotal = calcularDuracionTotal(ejercicios)
        view.findViewById<TextView>(R.id.textDuracion)?.text = "$duracionTotal min"
        view.findViewById<TextView>(R.id.textNumEjercicios)?.text = "${ejercicios.size}"
        
        // Mostrar/ocultar textos de grupo muscular, objetivo y equipamiento según el tipo de ejercicio
        val tituloLower = (title ?: "").lowercase()
        if (tituloLower.contains("cardio") || tituloLower.contains("fuerza")) {
            // Ocultar estos elementos para Cardio y Fuerza
            ocultarElementosCardio(view)
        } else {
            // Mostrar estos elementos para otros ejercicios (si estaban ocultos)
            mostrarElementosEjercicio(view)
        }
        
        // Mostrar ejercicios
        mostrarEjercicios(view, ejercicios)
        
        // Botones
        view.findViewById<Button>(R.id.buttonIniciar)?.setOnClickListener {
            navigateToRutina()
        }
        
        view.findViewById<ImageButton>(R.id.buttonBack)?.setOnClickListener {
            regresar()
        }
    }
    
    private fun ocultarElementosCardio(view: View) {
        // Buscar y ocultar elementos relacionados con grupo muscular, objetivo y equipamiento
        // Intentar diferentes posibles IDs
        val posiblesIds = listOf(
            "textGrupoMuscular", "grupoMuscular", "textGrupo",
            "textObjetivo", "objetivo", "textObjetivoEntrenamiento",
            "textEquipamiento", "equipamiento", "textEquipo"
        )
        
        posiblesIds.forEach { nombreId ->
            try {
                val resId = resources.getIdentifier(nombreId, "id", requireContext().packageName)
                if (resId != 0) {
                    val elemento = view.findViewById<View>(resId)
                    elemento?.let {
                        it.visibility = View.GONE
                        // Guardar el estado original para poder restaurarlo
                        it.tag = "hidden_by_filter"
                    }
                }
            } catch (e: Exception) {
                // Continuar con el siguiente
            }
        }
        
        // También buscar por texto en TextViews
        buscarYocultarPorTexto(view, listOf("grupo muscular", "objetivo", "equipamiento"))
    }
    
    private fun buscarYocultarPorTexto(view: View, textos: List<String>) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is TextView) {
                    val textoCompleto = child.text.toString()
                    val textoLower = textoCompleto.lowercase()
                    
                    // Buscar textos específicos con mayor precisión
                    // Solo ocultar si el texto contiene exactamente estas frases
                    val debeOcultar = when {
                        // "Grupo muscular" o variaciones
                        textoLower.contains("grupo muscular") && 
                        (textoLower.contains("grupo") && textoLower.contains("muscular")) -> true
                        
                        // "Objetivo del entrenamiento" o solo "Objetivo" si es un título
                        (textoLower.contains("objetivo del entrenamiento") || 
                         (textoLower.trim() == "objetivo" && textoCompleto.length < 20)) -> true
                        
                        // "Equipamiento" pero no "Sin equipamiento"
                        textoLower.contains("equipamiento") && 
                        !textoLower.contains("sin equipamiento") &&
                        !textoLower.contains("con equipamiento") -> true
                        
                        else -> false
                    }
                    
                    if (debeOcultar) {
                        child.visibility = View.GONE
                        // Guardar el estado original para poder restaurarlo
                        child.tag = "hidden_by_filter"
                    }
                }
                if (child is ViewGroup) {
                    buscarYocultarPorTexto(child, textos)
                }
            }
        }
    }
    
    private fun mostrarElementosEjercicio(view: View) {
        // Restaurar elementos que fueron ocultados por el filtro
        restaurarElementosOcultos(view)
    }
    
    private fun restaurarElementosOcultos(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child.tag == "hidden_by_filter") {
                    child.visibility = View.VISIBLE
                    child.tag = null
                }
                if (child is ViewGroup) {
                    restaurarElementosOcultos(child)
                }
            }
        }
    }
    
    private fun regresar() {
        // Regresar al fragment anterior
        // El listener en SaludFisicaFragment restaurará la visibilidad automáticamente
        val fragmentManager = parentFragment?.childFragmentManager ?: parentFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else {
            // Si no hay fragment en el back stack, restaurar visibilidad manualmente
            val parentFragment = parentFragment as? SaludFisicaFragment
            parentFragment?.hideEjerciciosDetalle()
        }
    }
    
    private fun mostrarEjercicios(view: View, ejercicios: List<Ejercicio>) {
        val containerEjercicios = view.findViewById<LinearLayout>(R.id.containerEjercicios)
        containerEjercicios?.removeAllViews()
        
        val inflater = LayoutInflater.from(requireContext())
        ejercicios.forEachIndexed { index, ejercicio ->
            val ejercicioView = inflater.inflate(
                R.layout.item_ejercicio_lista,
                containerEjercicios,
                false
            )
            
            ejercicioView.findViewById<TextView>(R.id.textNombreEjercicio)?.text = ejercicio.nombre
            ejercicioView.findViewById<TextView>(R.id.textDuracionEjercicio)?.text = ejercicio.duracion
            
            containerEjercicios?.addView(ejercicioView)
        }
    }
    
    private fun calcularDuracionTotal(ejercicios: List<Ejercicio>): Int {
        var totalSegundos = 0
        ejercicios.forEach { ejercicio ->
            if (ejercicio.esPorRepeticiones) {
                // Estimar tiempo para ejercicios por repeticiones
                // Aproximadamente 5 segundos por repetición
                totalSegundos += ejercicio.repeticiones * 5
            } else {
                totalSegundos += ejercicio.duracionSegundos
            }
        }
        // Convertir a minutos y redondear
        return (totalSegundos / 60.0).toInt().coerceAtLeast(1)
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
                crearEjercicio("Flexiones", "30 seg", "Realiza flexiones manteniendo el cuerpo recto", 30),
                crearEjercicio("Plancha", "45 seg", "Mantén la posición de plancha con el cuerpo recto", 45),
                crearEjercicio("Fondos de tríceps", "30 seg", "Realiza fondos usando una silla o banco", 30),
                crearEjercicio("Remo con peso corporal", "30 seg", "Tira hacia atrás manteniendo la espalda recta", 30),
                crearEjercicio("Abdominales", "30 seg", "Contrae el abdomen levantando el torso", 30),
                crearEjercicio("Flexiones inclinadas", "30 seg", "Flexiones con pies elevados para mayor intensidad", 30),
                crearEjercicio("Plancha lateral", "30 seg cada lado", "Mantén la plancha de lado para trabajar oblicuos", 30),
                crearEjercicio("Superman", "30 seg", "Acostado boca abajo, levanta brazos y piernas", 30),
                crearEjercicio("Mountain climbers", "30 seg", "Alterna las piernas como corriendo en posición de plancha", 30),
                crearEjercicio("Burpees", "30 seg", "Salto, flexión y vuelta a posición inicial", 30),
                crearEjercicio("Flexiones diamante", "30 seg", "Flexiones con manos en forma de diamante", 30),
                crearEjercicio("Plancha con elevación de pierna", "30 seg", "Plancha alternando elevación de piernas", 30),
                crearEjercicio("Abdominales bicicleta", "30 seg", "Simula pedaleo acostado para trabajar el core", 30),
                crearEjercicio("Flexiones con palmada", "30 seg", "Flexiones explosivas con palmada en el aire", 30),
                crearEjercicio("Plancha con toque de hombro", "30 seg", "Plancha alternando toque de hombros", 30)
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
                crearEjercicio("Burpees", "30 seg", "Salto, flexión y vuelta a posición inicial", 30),
                crearEjercicio("Mountain climbers", "30 seg", "Alterna las piernas como corriendo en posición de plancha", 30),
                crearEjercicio("Jumping jacks", "30 seg", "Salta abriendo piernas y brazos", 30),
                crearEjercicio("High knees", "30 seg", "Corre en el lugar levantando las rodillas", 30),
                crearEjercicio("Jump squats", "30 seg", "Salta desde posición de sentadilla", 30),
                crearEjercicio("Correr en el lugar", "30 seg", "Corre en el lugar a buen ritmo", 30),
                crearEjercicio("Escaladores", "30 seg", "Mountain climbers a alta velocidad", 30),
                crearEjercicio("Saltos de tijera", "30 seg", "Salta alternando piernas adelante y atrás", 30),
                crearEjercicio("Burpees sin flexión", "30 seg", "Burpees sin hacer flexión completa", 30),
                crearEjercicio("Skipping", "30 seg", "Corre en el lugar con rodillas altas", 30)
            )
            "sin equipamiento" -> listOf(
                crearEjercicio("Flexiones", "30 seg", "Ejercicio básico para pecho y brazos", 30),
                crearEjercicio("Sentadillas", "30 seg", "Ejercicio fundamental para piernas", 30),
                crearEjercicio("Plancha", "45 seg", "Fortalece el core", 45),
                crearEjercicio("Abdominales", "30 seg", "Fortalece el abdomen", 30),
                crearEjercicio("Burpees", "30 seg", "Ejercicio completo de cuerpo", 30),
                crearEjercicio("Mountain climbers", "30 seg", "Alterna piernas en posición de plancha", 30),
                crearEjercicio("Jumping jacks", "30 seg", "Salta abriendo piernas y brazos", 30),
                crearEjercicio("Zancadas", "30 seg", "Da un paso largo y baja la rodilla", 30),
                crearEjercicio("Plancha lateral", "30 seg cada lado", "Plancha de lado para oblicuos", 30),
                crearEjercicio("Flexiones diamante", "30 seg", "Flexiones con manos juntas", 30),
                crearEjercicio("Sentadillas con salto", "30 seg", "Sentadillas explosivas", 30),
                crearEjercicio("Abdominales bicicleta", "30 seg", "Simula pedaleo acostado", 30),
                crearEjercicio("Superman", "30 seg", "Levanta brazos y piernas acostado boca abajo", 30),
                crearEjercicio("Wall sit", "45 seg", "Sentadilla estática contra la pared", 45),
                crearEjercicio("High knees", "30 seg", "Corre en el lugar con rodillas altas", 30),
                crearEjercicio("Glute bridge", "30 seg", "Eleva la cadera acostado", 30),
                crearEjercicio("Estocadas laterales", "30 seg", "Zancadas hacia los lados", 30),
                crearEjercicio("Flexiones inclinadas", "30 seg", "Flexiones con pies elevados", 30),
                crearEjercicio("Jump squats", "30 seg", "Salta desde posición de sentadilla", 30),
                crearEjercicio("Plancha con toque de hombro", "30 seg", "Plancha alternando toque de hombros", 30)
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
                crearEjercicio("Postura de la montaña", "60 seg", "Postura básica de pie, alineando todo el cuerpo", 60),
                crearEjercicio("Saludo al sol", "90 seg", "Secuencia completa de saludo al sol", 90),
                crearEjercicio("Postura del guerrero I", "45 seg cada lado", "Fortalece piernas y abre caderas", 45),
                crearEjercicio("Postura del guerrero II", "45 seg cada lado", "Mejora equilibrio y fuerza", 45),
                crearEjercicio("Postura del árbol", "60 seg cada lado", "Mejora equilibrio y concentración", 60),
                crearEjercicio("Postura del perro boca abajo", "60 seg", "Estira toda la parte posterior del cuerpo", 60),
                crearEjercicio("Postura del niño", "60 seg", "Relaja y estira la espalda", 60),
                crearEjercicio("Postura de la cobra", "45 seg", "Fortalece la espalda y abre el pecho", 45),
                crearEjercicio("Postura del puente", "60 seg", "Fortalece glúteos y abre caderas", 60),
                crearEjercicio("Postura de la torsión sentada", "45 seg cada lado", "Mejora flexibilidad de columna", 45),
                crearEjercicio("Postura del triángulo", "45 seg cada lado", "Estira laterales y fortalece piernas", 45),
                crearEjercicio("Postura del cadáver", "120 seg", "Relajación final y meditación", 120)
            )
            else -> {
                // Ejercicios individuales de yoga (cuando se selecciona uno específico)
                val tituloLower = tipoRutina.lowercase()
                when {
                    tituloLower.contains("postura de la montaña") -> listOf(
                        crearEjercicio("Postura de la montaña", "60 seg", "Postura básica de pie, alineando todo el cuerpo", 60)
                    )
                    tituloLower.contains("saludo al sol") -> listOf(
                        crearEjercicio("Saludo al sol", "90 seg", "Secuencia completa de saludo al sol", 90)
                    )
                    tituloLower.contains("guerrero i") -> listOf(
                        crearEjercicio("Postura del guerrero I", "45 seg cada lado", "Fortalece piernas y abre caderas", 45)
                    )
                    tituloLower.contains("guerrero ii") -> listOf(
                        crearEjercicio("Postura del guerrero II", "45 seg cada lado", "Mejora equilibrio y fuerza", 45)
                    )
                    tituloLower.contains("árbol") || tituloLower.contains("arbol") -> listOf(
                        crearEjercicio("Postura del árbol", "60 seg cada lado", "Mejora equilibrio y concentración", 60)
                    )
                    tituloLower.contains("perro boca abajo") -> listOf(
                        crearEjercicio("Postura del perro boca abajo", "60 seg", "Estira toda la parte posterior del cuerpo", 60)
                    )
                    tituloLower.contains("niño") -> listOf(
                        crearEjercicio("Postura del niño", "60 seg", "Relaja y estira la espalda", 60)
                    )
                    tituloLower.contains("cobra") -> listOf(
                        crearEjercicio("Postura de la cobra", "45 seg", "Fortalece la espalda y abre el pecho", 45)
                    )
                    tituloLower.contains("puente") -> listOf(
                        crearEjercicio("Postura del puente", "60 seg", "Fortalece glúteos y abre caderas", 60)
                    )
                    tituloLower.contains("torsión") || tituloLower.contains("torsion") -> listOf(
                        crearEjercicio("Postura de la torsión sentada", "45 seg cada lado", "Mejora flexibilidad de columna", 45)
                    )
                    tituloLower.contains("triángulo") || tituloLower.contains("triangulo") -> listOf(
                        crearEjercicio("Postura del triángulo", "45 seg cada lado", "Estira laterales y fortalece piernas", 45)
                    )
                    tituloLower.contains("cadáver") || tituloLower.contains("cadaver") -> listOf(
                        crearEjercicio("Postura del cadáver", "120 seg", "Relajación final y meditación", 120)
                    )
                    else -> listOf(
                        crearEjercicio("Calentamiento", "5 min", "Calienta tus músculos antes de comenzar", 300),
                        crearEjercicio("Ejercicio principal", "30 seg", "Realiza el ejercicio principal de la rutina", 30),
                        crearEjercicio("Estiramiento", "5 min", "Estira los músculos trabajados", 300)
                    )
                }
            }
        }
    }
    
    private fun navigateToRutina() {
        val rutinaFragment = EjerciciosRutinaFragment.newInstance(
            title ?: "",
            info ?: "",
            description ?: ""
        )
        
        // Navegar usando el childFragmentManager del padre (SaludFisicaFragment)
        parentFragment?.childFragmentManager?.beginTransaction()
            ?.replace(R.id.containerEjerciciosDetalle, rutinaFragment)
            ?.addToBackStack("ejercicios_rutina")
            ?.commit()
    }
}
