package com.example.balanceuapp.ui.fragments

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
import com.example.balanceuapp.databinding.FragmentEstadisticasBinding
import com.example.balanceuapp.databinding.ItemEstadoAnimoBinding
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.ui.viewmodel.EstadisticasViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EstadisticasFragment : Fragment() {
    private var _binding: FragmentEstadisticasBinding? = null
    private val binding get() = _binding!!
    private lateinit var estadisticasViewModel: EstadisticasViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var estadosAdapter: EstadosAnimoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstadisticasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        estadisticasViewModel = ViewModelProvider(this)[EstadisticasViewModel::class.java]
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        val userId = authViewModel.obtenerUsuarioActual()
        if (userId != null) {
            // Cargar datos de los últimos 30 días
            val calendar = Calendar.getInstance()
            val fechaFin = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, -30)
            val fechaInicio = calendar.timeInMillis

            estadisticasViewModel.cargarDatosEstadisticos(userId, fechaInicio, fechaFin)
        }

        setupListaEstadosAnimo()
        setupObservers()
    }

    private fun setupObservers() {
        estadisticasViewModel.habitos.observe(viewLifecycleOwner) { habitos ->
            actualizarGraficaHabitos(habitos)
        }

        estadisticasViewModel.estadosAnimo.observe(viewLifecycleOwner) { estados ->
            actualizarListaEstadosAnimo(estados)
        }
    }

    private fun actualizarGraficaHabitos(habitos: List<com.example.balanceuapp.data.model.Habito>) {
        try {
            val completados = habitos.count { it.completado }
            val noCompletados = habitos.size - completados

            val entries = mutableListOf<PieEntry>()
            if (completados > 0) {
                entries.add(PieEntry(completados.toFloat(), "Completados"))
            }
            if (noCompletados > 0) {
                entries.add(PieEntry(noCompletados.toFloat(), "Pendientes"))
            }

            if (entries.isEmpty()) {
                entries.add(PieEntry(1f, "Sin datos"))
            }

            val dataSet = PieDataSet(entries, "Hábitos")
            // Colores pastel suaves
            dataSet.colors = listOf(
                Color.parseColor("#A5D6A7"), // Verde pastel
                Color.parseColor("#FFB74D"), // Naranja pastel
                Color.parseColor("#BDBDBD")  // Gris pastel
            )
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.parseColor("#424242")

            val pieData = PieData(dataSet)
            binding.pieChartHabitos.data = pieData
            binding.pieChartHabitos.description.isEnabled = false
            binding.pieChartHabitos.legend.isEnabled = true
            binding.pieChartHabitos.invalidate()
        } catch (e: Exception) {
            android.util.Log.e("EstadisticasFragment", "Error al actualizar gráfica de hábitos: ${e.message}", e)
        }
    }

    private fun actualizarListaEstadosAnimo(estados: List<EstadoAnimo>) {
        val registrosOrdenados = estados.sortedByDescending { it.fecha }
        estadosAdapter.submitList(registrosOrdenados)
        val sinDatos = estados.isEmpty()
        binding.tvEstadosAnimoVacio.visibility = if (sinDatos) View.VISIBLE else View.GONE
        binding.rvEstadosAnimo.visibility = if (sinDatos) View.GONE else View.VISIBLE
    }

    private fun setupListaEstadosAnimo() {
        estadosAdapter = EstadosAnimoAdapter()
        binding.rvEstadosAnimo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEstadosAnimo.adapter = estadosAdapter
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

