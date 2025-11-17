package com.example.balanceuapp.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
            }
        )

        binding.rvHabitos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHabitos.adapter = adapter

        binding.fabAgregarHabito.setOnClickListener {
            mostrarDialogoAgregarHabito()
        }

        setupObservers()

        val userId = authViewModel.obtenerUsuarioActual()
        if (userId != null) {
            habitoViewModel.cargarHabitos(userId)
        }
    }

    private fun setupObservers() {
        habitoViewModel.habitos.observe(viewLifecycleOwner) { habitos ->
            adapter.submitList(habitos)
            val isEmpty = habitos.isEmpty()
            binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvHabitos.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        habitoViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoAgregarHabito() {
        val dialogBinding = DialogAgregarHabitoBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Agregar Hábito")
            .setView(dialogBinding.root)
            .setPositiveButton("Agregar") { _, _ ->
                val nombre = dialogBinding.etNombreHabito.text.toString().trim()
                val descripcion = dialogBinding.etDescripcionHabito.text.toString().trim()

                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre es requerido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val userId = authViewModel.obtenerUsuarioActual()
                if (userId != null) {
                    val habito = Habito(
                        usuarioId = userId,
                        nombre = nombre,
                        descripcion = descripcion
                    )
                    habitoViewModel.agregarHabito(habito)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun mostrarDialogoEliminar(habito: Habito) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Hábito")
            .setMessage("¿Estás seguro de eliminar \"${habito.nombre}\"?")
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
        _binding = null
    }

    private class HabitosAdapter(
        private val onCompletadoChanged: (Habito, Boolean) -> Unit,
        private val onEliminar: (Habito) -> Unit
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
                binding.cbCompletado.isChecked = habito.completado

                binding.cbCompletado.setOnCheckedChangeListener { _, isChecked ->
                    onCompletadoChanged(habito, isChecked)
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

