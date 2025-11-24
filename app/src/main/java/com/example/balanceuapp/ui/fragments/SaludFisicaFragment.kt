package com.example.balanceuapp.ui.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.balanceuapp.R
import com.example.balanceuapp.databinding.FragmentSaludFisicaBinding
import com.google.android.material.tabs.TabLayoutMediator
import java.io.IOException

class SaludFisicaFragment : Fragment() {
    private var _binding: FragmentSaludFisicaBinding? = null
    private val binding get() = _binding!!
    
    // Métodos públicos para controlar la visibilidad
    fun showEjerciciosDetalle() {
        binding.viewPager.visibility = View.GONE
        binding.tabLayout.visibility = View.GONE
        binding.root.findViewById<View>(R.id.containerEjerciciosDetalle)?.visibility = View.VISIBLE
    }
    
    fun hideEjerciciosDetalle() {
        binding.viewPager.visibility = View.VISIBLE
        binding.tabLayout.visibility = View.VISIBLE
        binding.root.findViewById<View>(R.id.containerEjerciciosDetalle)?.visibility = View.GONE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaludFisicaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar el adapter del ViewPager2
        val adapter = SaludFisicaPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Asegurar que el deslizamiento esté habilitado
        binding.viewPager.isUserInputEnabled = true
        
        // Mejorar la experiencia de deslizamiento
        binding.viewPager.offscreenPageLimit = 1 // Mantener ambas páginas en memoria para transición suave

        // Conectar TabLayout con ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "💪 Salud Física"
                1 -> tab.text = "✨ Consejos"
            }
        }.attach()
        
        // Listener para detectar cuando se hace popBackStack y restaurar visibilidad
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                // Si no hay fragments en el back stack, restaurar la vista principal
                hideEjerciciosDetalle()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Adapter interno para manejar las páginas
    private class SaludFisicaPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> SaludFisicaContentFragment()
                1 -> ConsejosBienestarContentFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}

// Fragment para el contenido de Salud Física
class SaludFisicaContentFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            com.example.balanceuapp.R.layout.layout_salud_fisica_content,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cargar imágenes de las cards desde assets
        cargarImagenesCards(view)

        // Configurar botones de filtro de tipo de ejercicio
        configurarFiltrosEjercicios(view)

        // Configurar click listeners para cada card de ejercicio
        configurarCardsEjercicios(view)
    }
    
    private fun configurarFiltrosEjercicios(view: View) {
        // Mapeo de botones de filtro a categorías
        // "Para ti" muestra todas las cards (lista vacía = mostrar todas)
        val filtros = mapOf(
            "Para ti" to emptyList<String>(), // Mostrar todos los ejercicios
            "Cardio" to listOf("Cardio"),
            "Fuerza" to listOf("Fuerza", "Con Pesas"),
            "Yoga" to listOf("Yoga") // Mostrar solo Yoga
        )
        
        // Buscar contenedor para los botones de filtro
        val containerFiltros = buscarContenedorPrincipal(view)
        
        // Crear botones dinámicamente siempre (no dependemos de IDs del layout)
        if (containerFiltros != null) {
            crearBotonesFiltro(containerFiltros, filtros, view)
        }
    }
    
    private fun buscarContenedorPrincipal(view: View): ViewGroup? {
        // Buscar el contenedor principal (ScrollView, LinearLayout, etc.)
        if (view is ViewGroup) {
            // Buscar ScrollView o LinearLayout que contenga las cards
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is android.widget.ScrollView || 
                    (child is LinearLayout && child.orientation == LinearLayout.VERTICAL)) {
                    return child as? ViewGroup
                }
            }
            // Si no se encuentra, usar el view principal
            return view as? ViewGroup
        }
        return null
    }
    
    private fun crearBotonesFiltro(container: ViewGroup, filtros: Map<String, List<String>>, view: View) {
        // Crear un LinearLayout horizontal para los botones
        val layoutFiltros = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            tag = "filtros_buttons" // Marcar para poder encontrarlo después
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.MarginLayoutParams.MATCH_PARENT,
                ViewGroup.MarginLayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }
        
        val botonesTexto = listOf("Para ti", "Cardio", "Fuerza", "Yoga")
        var botonSeleccionado: TextView? = null
        
        botonesTexto.forEach { texto ->
            val boton = TextView(requireContext()).apply {
                this.text = texto
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(4, 0, 4, 0)
                }
                setPadding(16, 12, 16, 12)
                gravity = android.view.Gravity.CENTER
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.NORMAL)
                
                // Estilo moderno sin bordes
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(android.graphics.Color.parseColor("#666666")) // Gris por defecto
                
                setOnClickListener {
                    // Deseleccionar botón anterior
                    botonSeleccionado?.setTextColor(android.graphics.Color.parseColor("#666666"))
                    botonSeleccionado?.setTypeface(null, android.graphics.Typeface.NORMAL)
                    
                    // Seleccionar nuevo botón
                    setTextColor(android.graphics.Color.parseColor("#000000")) // Negro cuando está seleccionado
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    botonSeleccionado = this
                    
                    // Filtrar ejercicios
                    filtrarEjercicios(view, filtros[texto] ?: emptyList(), it)
                }
            }
            layoutFiltros.addView(boton)
            if (botonSeleccionado == null) {
                // Seleccionar "Para ti" por defecto
                boton.setTextColor(android.graphics.Color.parseColor("#000000"))
                boton.setTypeface(null, android.graphics.Typeface.BOLD)
                botonSeleccionado = boton
                // Mostrar todos los ejercicios por defecto
                filtrarEjercicios(view, filtros["Para ti"] ?: emptyList(), boton)
            }
        }
        
        // Insertar al inicio del contenedor
        if (container.childCount > 0) {
            container.addView(layoutFiltros, 0)
        } else {
            container.addView(layoutFiltros)
        }
    }
    
    private fun filtrarEjercicios(view: View, categorias: List<String>, botonSeleccionado: View) {
        // Mapeo de categorías a IDs de cards
        val mapeoCards = mapOf(
            "Torso" to com.example.balanceuapp.R.id.cardTorso,
            "Piernas" to com.example.balanceuapp.R.id.cardPiernas,
            "Fuerza" to com.example.balanceuapp.R.id.cardFuerza,
            "Cardio" to com.example.balanceuapp.R.id.cardCardio,
            "Sin Equipamiento" to com.example.balanceuapp.R.id.cardNoEquipment,
            "Con Pesas" to com.example.balanceuapp.R.id.cardPesas,
            "Yoga" to null // Yoga se maneja de forma especial (navegación directa)
        )
        
        // SIEMPRE ocultar ejercicios de yoga primero cuando se cambia de filtro
        ocultarEjerciciosYoga(view)
        
        // Si es Yoga, mostrar cards de ejercicios individuales de yoga
        if (categorias.contains("Yoga")) {
            // Ocultar todas las cards normales
            mapeoCards.values.forEach { cardId ->
                cardId?.let { view.findViewById<View>(it)?.visibility = View.GONE }
            }
            // Ocultar títulos de sección y header cuando se muestra Yoga
            ocultarTitulosYHeader(view)
            // Mostrar cards de ejercicios de yoga
            mostrarEjerciciosYoga(view)
            return
        }
        
        // Si hay categorías específicas (Fuerza, Cardio), ocultar títulos y header
        if (categorias.isNotEmpty()) {
            ocultarTitulosYHeader(view)
        } else {
            // Si es "Para ti", mostrar todo incluyendo títulos y header
            mostrarTitulosYHeader(view)
        }
        
        // Ocultar todas las cards primero
        mapeoCards.values.forEach { cardId ->
            cardId?.let { view.findViewById<View>(it)?.visibility = View.GONE }
        }
        
        // Si no hay categorías, mostrar todas (modo "Para ti")
        if (categorias.isEmpty()) {
            mapeoCards.values.forEach { cardId ->
                cardId?.let { view.findViewById<View>(it)?.visibility = View.VISIBLE }
            }
        } else {
            // Mostrar solo las cards de las categorías seleccionadas
            categorias.forEach { categoria ->
                val cardId = mapeoCards[categoria]
                cardId?.let {
                    view.findViewById<View>(it)?.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun ocultarTitulosYHeader(view: View) {
        // Buscar el contenedor principal (LinearLayout dentro del ScrollView)
        val container = buscarContenedorPrincipal(view)
        container?.let { cont ->
            // Recorrer todos los hijos del contenedor
            for (i in 0 until cont.childCount) {
                val child = cont.getChildAt(i)
                // Omitir los botones de filtro y los ejercicios de yoga
                if (child.tag == "filtros_buttons" || child.tag == "yoga_exercises") {
                    continue
                }
                
                // Si es un LinearLayout (header), verificar si contiene "Entrenamientos"
                if (child is LinearLayout && child.orientation == LinearLayout.VERTICAL) {
                    val tieneHeader = buscarTextViewConTexto(child, "Entrenamientos")
                    if (tieneHeader) {
                        child.visibility = View.GONE
                        continue
                    }
                }
                
                // Si es un TextView, verificar si es un título de sección
                if (child is TextView) {
                    val texto = child.text.toString()
                    // Títulos de sección: "Grupo Muscular", "Objetivo del Entrenamiento", "Equipamiento"
                    val esTituloSeccion = texto == "Grupo Muscular" || 
                                        texto == "Objetivo del Entrenamiento" || 
                                        texto == "Equipamiento" ||
                                        texto == "💪 Entrenamientos" ||
                                        texto == "Elige tu rutina de ejercicios"
                    
                    if (esTituloSeccion) {
                        // Verificar que no sea parte de una card
                        val esParteDeCard = esParteDeCard(child)
                        if (!esParteDeCard) {
                            child.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
    
    private fun mostrarTitulosYHeader(view: View) {
        // Buscar el contenedor principal
        val container = buscarContenedorPrincipal(view)
        container?.let { cont ->
            // Recorrer todos los hijos del contenedor
            for (i in 0 until cont.childCount) {
                val child = cont.getChildAt(i)
                // Omitir los botones de filtro y los ejercicios de yoga
                if (child.tag == "filtros_buttons" || child.tag == "yoga_exercises") {
                    continue
                }
                // Mostrar todos los demás elementos
                child.visibility = View.VISIBLE
            }
        }
    }
    
    private fun buscarTextViewConTexto(viewGroup: ViewGroup, texto: String): Boolean {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is TextView && child.text.contains(texto)) {
                return true
            }
            if (child is ViewGroup) {
                if (buscarTextViewConTexto(child, texto)) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun esParteDeCard(view: View): Boolean {
        // Verificar si el TextView está dentro de una MaterialCardView
        var parent = view.parent
        while (parent != null && parent is View) {
            if (parent is com.google.android.material.card.MaterialCardView) {
                return true
            }
            parent = parent.parent
        }
        return false
    }
    
    private fun configurarCardsEjercicios(view: View) {
        // Card Torso
        view.findViewById<View>(com.example.balanceuapp.R.id.cardTorso)?.setOnClickListener {
            navigateToEjerciciosDetalle(
                "Torso",
                "15 ejercicios • 30 min • Intermedio",
                "Rutina completa para fortalecer el torso, incluyendo ejercicios para pecho, espalda y core."
            )
        }

        // Card Piernas
        view.findViewById<View>(com.example.balanceuapp.R.id.cardPiernas)?.setOnClickListener {
            navigateToEjerciciosDetalle(
                "Piernas",
                "12 ejercicios • 25 min • Principiante",
                "Rutina enfocada en fortalecer y tonificar las piernas, incluyendo cuádriceps, glúteos y pantorrillas."
            )
        }

        // Card Fuerza
        view.findViewById<View>(com.example.balanceuapp.R.id.cardFuerza)?.setOnClickListener {
            navigateToEjerciciosDetalle(
                "Fuerza",
                "18 ejercicios • 45 min • Avanzado",
                "Entrenamiento intenso de fuerza para desarrollar masa muscular y potencia."
            )
        }

        // Card Cardio
        view.findViewById<View>(com.example.balanceuapp.R.id.cardCardio)?.setOnClickListener {
            navigateToEjerciciosDetalle(
                "Cardio",
                "10 ejercicios • 20 min • Principiante",
                "Rutina cardiovascular para mejorar la resistencia y quemar calorías."
            )
        }

        // Card Sin Equipamiento
        view.findViewById<View>(com.example.balanceuapp.R.id.cardNoEquipment)?.setOnClickListener {
            navigateToEjerciciosDetalle(
                "Sin Equipamiento",
                "20 ejercicios • 15-30 min • Todos los niveles",
                "Rutina completa que puedes hacer en casa sin necesidad de equipamiento especial."
            )
        }

        // Card Con Pesas
        view.findViewById<View>(com.example.balanceuapp.R.id.cardPesas)?.setOnClickListener {
            navigateToEjerciciosDetalle(
                "Con Pesas",
                "14 ejercicios • 35 min • Intermedio",
                "Entrenamiento con pesas para desarrollar fuerza y tonificar los músculos."
            )
        }
        
        // Card Yoga no existe en el layout, se omite
    }
    
    private fun cargarImagenesCards(view: View) {
        // Mapeo de cards a nombres de archivos en assets (con formato imagen_nombre.png)
        val cardsImagenes = mapOf(
            com.example.balanceuapp.R.id.cardTorso to "imagen_torso",
            com.example.balanceuapp.R.id.cardPiernas to "imagen_piernas",
            com.example.balanceuapp.R.id.cardFuerza to "imagen_fuerza",
            com.example.balanceuapp.R.id.cardCardio to "imagen_cardio",
            com.example.balanceuapp.R.id.cardNoEquipment to "imagen_sin_equipamiento",
            com.example.balanceuapp.R.id.cardPesas to "imagen_con_pesas"
        )
        
        cardsImagenes.forEach { (cardId, nombreImagen) ->
            val cardView = view.findViewById<View>(cardId)
            cardView?.let { card ->
                // Buscar ImageView dentro de la card
                val imageView = buscarImageViewEnCard(card)
                imageView?.let { img ->
                    try {
                        val bitmap = cargarImagenDesdeAssets(nombreImagen)
                        if (bitmap != null) {
                            img.setImageBitmap(bitmap)
                            img.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        // Si no se puede cargar la imagen, mantener el estado actual del ImageView
                    }
                }
            }
        }
    }
    
    private fun buscarImageViewEnCard(cardView: View): ImageView? {
        // Buscar recursivamente cualquier ImageView en la card
        if (cardView is ImageView) {
            return cardView
        }
        if (cardView is ViewGroup) {
            for (i in 0 until cardView.childCount) {
                val child = buscarImageViewEnCard(cardView.getChildAt(i))
                if (child != null) return child
            }
        }
        return null
    }
    
    private fun cargarImagenDesdeAssets(nombreImagen: String): android.graphics.Bitmap? {
        // Intentar cargar la imagen desde assets/imagenes_entrenamientos
        // Probar diferentes extensiones comunes
        val extensiones = listOf(".png", ".jpg", ".jpeg", ".webp")
        
        for (ext in extensiones) {
            try {
                val inputStream = requireContext().assets.open("imagenes_entrenamientos/$nombreImagen$ext")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                return bitmap
            } catch (e: IOException) {
                // Continuar con la siguiente extensión
            }
        }
        
        // Si no se encontró con ninguna extensión, intentar sin extensión
        try {
            val inputStream = requireContext().assets.open("imagenes_entrenamientos/$nombreImagen")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            return bitmap
        } catch (e: IOException) {
            return null
        }
    }

    private fun ocultarEjerciciosYoga(view: View) {
        // Buscar y eliminar el layout de ejercicios de yoga si existe
        val containerEjercicios = buscarContenedorParaYoga(view)
        containerEjercicios?.let { container ->
            // Buscar el layout de yoga y eliminarlo
            var layoutYogaExistente: ViewGroup? = null
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is ViewGroup && child.tag == "yoga_exercises") {
                    layoutYogaExistente = child
                    break
                }
            }
            layoutYogaExistente?.let { container.removeView(it) }
        }
    }
    
    private fun mostrarEjerciciosYoga(view: View) {
        // Lista de ejercicios de yoga individuales
        val ejerciciosYoga = listOf(
            "Postura de la montaña" to "60 seg",
            "Postura del guerrero I" to "45 seg cada lado",
            "Postura del guerrero II" to "45 seg cada lado",
            "Postura del árbol" to "60 seg cada lado",
            "Postura del perro boca abajo" to "60 seg",
            "Postura del niño" to "60 seg",
            "Postura de la cobra" to "45 seg",
            "Postura del puente" to "60 seg",
            "Postura de la torsión sentada" to "45 seg cada lado",
            "Postura del triángulo" to "45 seg cada lado",
            "Postura del cadáver" to "120 seg"
        )
        
        // Buscar contenedor donde mostrar los ejercicios de yoga (sin afectar los botones de filtro)
        val containerEjercicios = buscarContenedorParaYoga(view)
        
        containerEjercicios?.let { container ->
            // Buscar si ya existe un layout de yoga y eliminarlo
            var layoutYogaExistente: ViewGroup? = null
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is ViewGroup && child.tag == "yoga_exercises") {
                    layoutYogaExistente = child
                    break
                }
            }
            layoutYogaExistente?.let { container.removeView(it) }
            
            // Crear un layout para los ejercicios de yoga
            val layoutYoga = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                tag = "yoga_exercises" // Marcar para poder encontrarlo después
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.MarginLayoutParams.MATCH_PARENT,
                    ViewGroup.MarginLayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16)
                }
            }
            
            // Crear cards para cada ejercicio de yoga
            ejerciciosYoga.forEach { (nombre, duracion) ->
                val cardYoga = crearCardEjercicioYoga(nombre, duracion)
                layoutYoga.addView(cardYoga)
            }
            
            // Encontrar el índice donde insertar (después de los botones de filtro)
            var indiceInsercion = 0
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child.tag == "filtros_buttons") {
                    indiceInsercion = i + 1
                    break
                }
            }
            
            // Insertar el layout de yoga
            if (container.childCount > indiceInsercion) {
                container.addView(layoutYoga, indiceInsercion)
            } else {
                container.addView(layoutYoga)
            }
        }
    }
    
    private fun buscarContenedorParaYoga(view: View): ViewGroup? {
        // Buscar el contenedor principal donde están las cards (sin afectar los botones de filtro)
        if (view is ViewGroup) {
            // Buscar ScrollView o LinearLayout vertical
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is android.widget.ScrollView) {
                    val scrollContent = (child as android.widget.ScrollView).getChildAt(0)
                    if (scrollContent is ViewGroup) {
                        return scrollContent
                    }
                }
                if (child is LinearLayout && child.orientation == LinearLayout.VERTICAL) {
                    // Asegurarse de que no sea el contenedor de los botones de filtro
                    if (child.tag != "filtros_buttons") {
                        return child
                    }
                }
            }
            // Si no se encuentra un contenedor específico, buscar uno que no sea el de filtros
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is ViewGroup && child.tag != "filtros_buttons") {
                    return child
                }
            }
            return view
        }
        return null
    }
    
    private fun crearCardEjercicioYoga(nombre: String, duracion: String): View {
        // Mapeo de nombres de ejercicios a nombres de archivos de imágenes
        val mapeoImagenes = mapOf(
            "Postura de la montaña" to "yoga_montana",
            "Postura del guerrero I" to "yoga_guerrero_1",
            "Postura del guerrero II" to "yoga_guerrero_2",
            "Postura del árbol" to "yoga_arbol",
            "Postura del perro boca abajo" to "yoga_perro_boca_abajo",
            "Postura del niño" to "yoga_nino",
            "Postura de la cobra" to "yoga_cobra",
            "Postura del puente" to "yoga_puente",
            "Postura de la torsión sentada" to "yoga_torsion",
            "Postura del triángulo" to "yoga_triangulo",
            "Postura del cadáver" to "yoga_cadaver"
        )
        
        // Crear una card mejorada para cada ejercicio de yoga con imagen usando MaterialCardView
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            cardElevation = 4f
            radius = 12f * resources.displayMetrics.density // Convertir dp a px
            setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
            setOnClickListener {
                // Navegar al ejercicio individual de yoga
                navigateToEjercicioYogaIndividual(nombre, duracion)
            }
        }
        
        // Contenedor interno para el contenido de la card
        val cardContent = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }
        
        // ImageView para la imagen del ejercicio con bordes redondeados
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                (100 * resources.displayMetrics.density).toInt(), // 100dp
                (100 * resources.displayMetrics.density).toInt()  // 100dp
            ).apply {
                setMargins(0, 0, 16, 0)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            // Agregar bordes redondeados usando un drawable
            val radius = 8f * resources.displayMetrics.density
            val shape = android.graphics.drawable.GradientDrawable().apply {
                setCornerRadius(radius)
                setColor(android.graphics.Color.parseColor("#E0E0E0"))
            }
            background = shape
            clipToOutline = true
        }
        
        // Intentar cargar la imagen desde assets
        val nombreImagen = mapeoImagenes[nombre] ?: "yoga_default"
        try {
            val bitmap = cargarImagenYogaDesdeAssets(nombreImagen)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                // Si no hay imagen, usar un icono o color de fondo
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        } catch (e: Exception) {
            // Si falla, usar un icono por defecto
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        
        // Contenedor para el texto (vertical)
        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        // Texto del ejercicio
        val textNombre = TextView(requireContext()).apply {
            text = nombre
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#000000"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Texto de duración
        val textDuracion = TextView(requireContext()).apply {
            text = duracion
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 0)
            }
        }
        
        textContainer.addView(textNombre)
        textContainer.addView(textDuracion)
        
        cardContent.addView(imageView)
        cardContent.addView(textContainer)
        card.addView(cardContent)
        
        return card
    }
    
    private fun cargarImagenYogaDesdeAssets(nombreImagen: String): android.graphics.Bitmap? {
        // Intentar cargar la imagen desde assets/imagenes_yoga
        // Probar diferentes extensiones comunes
        val extensiones = listOf(".png", ".jpg", ".jpeg", ".webp")
        
        for (ext in extensiones) {
            try {
                val inputStream = requireContext().assets.open("imagenes_yoga/$nombreImagen$ext")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                return bitmap
            } catch (e: IOException) {
                // Continuar con la siguiente extensión
            }
        }
        
        // Si no se encontró en imagenes_yoga, intentar en imagenes_entrenamientos
        for (ext in extensiones) {
            try {
                val inputStream = requireContext().assets.open("imagenes_entrenamientos/$nombreImagen$ext")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                return bitmap
            } catch (e: IOException) {
                // Continuar con la siguiente extensión
            }
        }
        
        return null
    }
    
    private fun navigateToEjercicioYogaIndividual(nombreEjercicio: String, duracion: String) {
        // Crear una rutina con solo ese ejercicio de yoga
        val fragment = EjerciciosDetalleFragment.newInstance(
            nombreEjercicio,
            "1 ejercicio • ${duracion.replace(" seg", "").replace(" cada lado", "")} min • Yoga",
            "Ejercicio de yoga: $nombreEjercicio"
        )
        
        val parentFragment = parentFragment as? SaludFisicaFragment
        parentFragment?.let { parent ->
            parent.showEjerciciosDetalle()
            parent.childFragmentManager.beginTransaction()
                .replace(R.id.containerEjerciciosDetalle, fragment)
                .addToBackStack("ejercicio_yoga")
                .commit()
        }
    }
    
    private fun navigateToEjerciciosDetalle(title: String, info: String, description: String) {
        val fragment = EjerciciosDetalleFragment.newInstance(title, info, description)
        // Obtener el fragment padre (SaludFisicaFragment)
        val parentFragment = parentFragment as? SaludFisicaFragment
        parentFragment?.let { parent ->
            // Ocultar ViewPager y TabLayout, mostrar contenedor de detalle
            parent.showEjerciciosDetalle()
            
            // Agregar el fragment de detalle al contenedor
            parent.childFragmentManager.beginTransaction()
                .replace(R.id.containerEjerciciosDetalle, fragment)
                .addToBackStack("ejercicios_detalle")
                .commit()
        }
    }
}

// Fragment para el contenido de Consejos de Bienestar
class ConsejosBienestarContentFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            com.example.balanceuapp.R.layout.layout_consejos_bienestar_content,
            container,
            false
        )
    }
}
