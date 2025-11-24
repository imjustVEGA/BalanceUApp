package com.example.balanceuapp.ui.fragments

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.balanceuapp.R
import com.example.balanceuapp.data.model.Habito
import com.example.balanceuapp.databinding.DialogAgregarHabitoBinding
import com.example.balanceuapp.databinding.FragmentHabitosBinding
import com.example.balanceuapp.databinding.ItemHabitoBinding
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.ui.viewmodel.HabitoViewModel
import com.example.balanceuapp.util.Constants

/**
 * Fragment que muestra y gestiona la lista de hábitos del usuario.
 * Permite agregar, editar, eliminar y marcar hábitos como completados.
 */
class HabitosFragment : Fragment() {
    
    private var _binding: FragmentHabitosBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var habitoViewModel: HabitoViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var adapter: HabitosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        habitoViewModel = ViewModelProvider(this)[HabitoViewModel::class.java]
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        adapter = HabitosAdapter(
            onCompletadoChanged = { habito, completado ->
                val userId = authViewModel.obtenerUsuarioActual()
                if (userId != null) {
                    habitoViewModel.marcarCompletado(habito.id, completado, userId)
                }
            },
            onEliminar = { habito ->
                mostrarDialogoEliminar(habito)
            },
            onEditar = { habito ->
                mostrarDialogoHabito(habito)
            }
        )

        binding.rvHabitos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHabitos.adapter = adapter

        binding.fabAgregarHabito.setOnClickListener {
            mostrarDialogoHabito()
        }

        setupObservers()

        val userId = authViewModel.obtenerUsuarioActual()
        if (userId != null) {
            habitoViewModel.startObservandoHabitos(userId)
        }
    }

    private fun setupObservers() {
        habitoViewModel.habitos.observe(viewLifecycleOwner) { habitos ->
            adapter.submitList(habitos)
            val isEmpty = habitos.isEmpty()
            binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvHabitos.visibility = if (isEmpty) View.GONE else View.VISIBLE
            
            // Actualizar progreso circular cuando cambian los hábitos
            actualizarProgresoCircular(habitos)
        }
        
        habitoViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun actualizarProgresoCircular(habitos: List<Habito>) {
        if (habitos.isEmpty()) {
            binding.progressCircularHabitos.progress = 0
            binding.tvPorcentajeProgreso.text = "0%"
            binding.tvCompletados.text = "0/0 completados"
            return
        }

        val total = habitos.size
        val completados = habitos.count { it.completado }
        val porcentaje = if (total > 0) {
            (completados * 100 / total).coerceIn(0, 100)
        } else {
            0
        }

        // Obtener el progreso actual
        val progresoActual = binding.progressCircularHabitos.progress
        
        // Animar el cambio de progreso
        val animacion = android.animation.ObjectAnimator.ofInt(
            binding.progressCircularHabitos,
            "progress",
            progresoActual,
            porcentaje
        )
        animacion.duration = 500 // 500ms de animación
        animacion.setInterpolator(android.view.animation.DecelerateInterpolator())
        animacion.start()
        
        // Actualizar textos
        binding.tvPorcentajeProgreso.text = "$porcentaje%"
        binding.tvCompletados.text = "$completados/$total completados"
    }

    private fun mostrarDialogoHabito(habito: Habito? = null) {
        val dialogBinding = DialogAgregarHabitoBinding.inflate(layoutInflater)
        dialogBinding.etNombreHabito.setText(habito?.nombre)
        dialogBinding.etDescripcionHabito.setText(habito?.descripcion)

        val titulo = if (habito == null) "Agregar Hábito" else "Editar Hábito"
        val positivo = if (habito == null) "Agregar" else "Actualizar"

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setView(dialogBinding.root)
            .setPositiveButton(positivo, null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val boton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            boton.setOnClickListener {
                val nombre = dialogBinding.etNombreHabito.text.toString().trim()
                val descripcion = dialogBinding.etDescripcionHabito.text.toString().trim()

                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), Constants.UIMessages.NOMBRE_REQUERIDO, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val userId = authViewModel.obtenerUsuarioActual()
                if (habito == null) {
                    if (userId == null) {
                        Toast.makeText(requireContext(), Constants.ErrorMessages.ERROR_USUARIO_NO_AUTENTICADO, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val nuevoHabito = Habito(
                        usuarioId = userId,
                        nombre = nombre,
                        descripcion = descripcion
                    )
                    habitoViewModel.agregarHabito(nuevoHabito)
                } else {
                    val habitoActualizado = habito.copy(
                        nombre = nombre,
                        descripcion = descripcion
                    )
                    habitoViewModel.actualizarHabito(habitoActualizado)
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    /**
     * Muestra un diálogo de confirmación para eliminar un hábito.
     * 
     * @param habito Hábito a eliminar
     */
    /**
     * Muestra un diálogo de confirmación para eliminar un hábito.
     * 
     * @param habito Hábito a eliminar
     */
    private fun mostrarDialogoEliminar(habito: Habito) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Hábito")
            .setMessage(Constants.UIMessages.getEliminarHabitoConfirmacion(habito.nombre))
            .setPositiveButton("Eliminar") { _, _ ->
                val userId = authViewModel.obtenerUsuarioActual()
                if (userId != null) {
                    habitoViewModel.eliminarHabito(habito.id, userId)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        habitoViewModel.stopObservandoHabitos()
        _binding = null
    }

    private class HabitosAdapter(
        private val onCompletadoChanged: (Habito, Boolean) -> Unit,
        private val onEliminar: (Habito) -> Unit,
        private val onEditar: (Habito) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<Habito, HabitosAdapter.HabitoViewHolder>(
        HabitoDiffCallback()
    ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitoViewHolder {
            val binding = ItemHabitoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return HabitoViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HabitoViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class HabitoViewHolder(
            private val binding: ItemHabitoBinding
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
            fun bind(habito: Habito) {
                binding.tvNombreHabito.text = habito.nombre
                binding.tvDescripcionHabito.text = habito.descripcion

                val context = binding.switchCompletado.context
                val thumbColor = ContextCompat.getColor(
                    context,
                    if (habito.completado) R.color.switch_thumb_done else R.color.switch_thumb_pending
                )
                val trackColor = ContextCompat.getColor(
                    context,
                    if (habito.completado) R.color.switch_track_done else R.color.switch_track_pending
                )
                binding.switchCompletado.thumbTintList = ColorStateList.valueOf(thumbColor)
                binding.switchCompletado.trackTintList = ColorStateList.valueOf(trackColor)

                binding.switchCompletado.setOnCheckedChangeListener(null)
                binding.switchCompletado.isChecked = habito.completado
                binding.switchCompletado.setOnCheckedChangeListener { _, isChecked ->
                    onCompletadoChanged(habito, isChecked)
                }

                binding.btnEditar.setOnClickListener {
                    onEditar(habito)
                }

                binding.btnEliminar.setOnClickListener {
                    onEliminar(habito)
                }
            }
        }
    }

    private class HabitoDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Habito>() {
        override fun areItemsTheSame(oldItem: Habito, newItem: Habito): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Habito, newItem: Habito): Boolean {
            return oldItem == newItem
        }
    }
}

