package com.example.balanceuapp.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.balanceuapp.data.model.EstadoAnimo
import com.example.balanceuapp.data.model.TipoEstadoAnimo
import com.example.balanceuapp.databinding.FragmentPerfilBinding
import com.example.balanceuapp.databinding.ItemEstadoAnimoBinding
import com.example.balanceuapp.ui.auth.AuthActivity
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.ui.viewmodel.EstadisticasViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var authViewModel: AuthViewModel
    private lateinit var estadisticasViewModel: EstadisticasViewModel
    private lateinit var estadosAdapter: EstadosAnimoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        estadisticasViewModel = ViewModelProvider(this)[EstadisticasViewModel::class.java]

        setupUserInfo()
        setupListeners()
        setupListaEstadosAnimo()
        setupObservers()
        
        // Cargar solo estados de ánimo
        val userId = authViewModel.obtenerUsuarioActual()
        if (userId != null) {
            val calendar = Calendar.getInstance()
            val fechaFin = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, -30)
            val fechaInicio = calendar.timeInMillis
            estadisticasViewModel.cargarDatosEstadisticos(userId, fechaInicio, fechaFin)
        }
    }

    private fun setupUserInfo() {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                binding.tvNombreUsuario.text = currentUser.displayName ?: "Usuario"
                binding.tvEmailUsuario.text = currentUser.email ?: "usuario@email.com"
            } else {
                binding.tvNombreUsuario.text = "Usuario"
                binding.tvEmailUsuario.text = "usuario@email.com"
            }
        } catch (e: Exception) {
            android.util.Log.e("PerfilFragment", "Error al cargar información del usuario: ${e.message}", e)
        }
    }

    private fun setupListeners() {
        binding.cardCerrarSesion.setOnClickListener {
            authViewModel.cerrarSesion()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun setupListaEstadosAnimo() {
        estadosAdapter = EstadosAnimoAdapter()
        binding.rvEstadosAnimo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEstadosAnimo.adapter = estadosAdapter
    }

    private fun setupObservers() {
        estadisticasViewModel.estadosAnimo.observe(viewLifecycleOwner) { estados ->
            actualizarListaEstadosAnimo(estados)
        }
    }

    private fun actualizarListaEstadosAnimo(estados: List<EstadoAnimo>) {
        val registrosOrdenados = estados.sortedByDescending { it.fecha }
        estadosAdapter.submitList(registrosOrdenados)
        val sinDatos = estados.isEmpty()
        binding.tvEstadosAnimoVacio.visibility = if (sinDatos) View.VISIBLE else View.GONE
        binding.rvEstadosAnimo.visibility = if (sinDatos) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class EstadosAnimoAdapter :
        ListAdapter<EstadoAnimo, EstadosAnimoAdapter.EstadoViewHolder>(EstadoDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EstadoViewHolder {
            val binding = ItemEstadoAnimoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return EstadoViewHolder(binding)
        }

        override fun onBindViewHolder(holder: EstadoViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class EstadoViewHolder(
            private val binding: ItemEstadoAnimoBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(estado: EstadoAnimo) {
                binding.tvTipoEstado.text = estado.tipo.displayName()
                binding.tvFechaEstado.text = DATE_FORMAT.format(Date(estado.fecha))
                binding.tvNotaEstado.text = estado.nota.ifBlank { "Sin nota registrada" }
            }
        }

        private class EstadoDiffCallback : DiffUtil.ItemCallback<EstadoAnimo>() {
            override fun areItemsTheSame(oldItem: EstadoAnimo, newItem: EstadoAnimo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: EstadoAnimo, newItem: EstadoAnimo): Boolean {
                return oldItem == newItem
            }
        }

        companion object {
            private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
        }
    }
}

private fun TipoEstadoAnimo.displayName(): String {
    return when (this) {
        TipoEstadoAnimo.ALEGRE -> "Muy feliz"
        TipoEstadoAnimo.FELIZ -> "Feliz"
        TipoEstadoAnimo.NEUTRAL -> "Neutral"
        TipoEstadoAnimo.TRISTE -> "Triste"
        TipoEstadoAnimo.TERRIBLE -> "Muy triste"
    }
}
