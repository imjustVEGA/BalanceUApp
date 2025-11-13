package com.example.balanceuapp.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.balanceuapp.data.model.TipoEstadoAnimo
import com.example.balanceuapp.databinding.FragmentEstadisticasBinding
import com.example.balanceuapp.ui.viewmodel.AuthViewModel
import com.example.balanceuapp.ui.viewmodel.EstadisticasViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.util.Calendar

class EstadisticasFragment : Fragment() {
    private var _binding: FragmentEstadisticasBinding? = null
    private val binding get() = _binding!!
    private lateinit var estadisticasViewModel: EstadisticasViewModel
    private lateinit var authViewModel: AuthViewModel

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

        setupObservers()
    }

    private fun setupObservers() {
        estadisticasViewModel.habitos.observe(viewLifecycleOwner) { habitos ->
            actualizarGraficaHabitos(habitos)
        }

        estadisticasViewModel.estadosAnimo.observe(viewLifecycleOwner) { estados ->
            actualizarGraficaEstadosAnimo(estados)
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
            dataSet.colors = listOf(Color.GREEN, Color.RED, Color.GRAY)
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.WHITE

            val pieData = PieData(dataSet)
            binding.pieChartHabitos.data = pieData
            binding.pieChartHabitos.description.isEnabled = false
            binding.pieChartHabitos.legend.isEnabled = true
            binding.pieChartHabitos.invalidate()
        } catch (e: Exception) {
            android.util.Log.e("EstadisticasFragment", "Error al actualizar gráfica de hábitos: ${e.message}", e)
        }
    }

    private fun actualizarGraficaEstadosAnimo(estados: List<com.example.balanceuapp.data.model.EstadoAnimo>) {
        try {
            val distribucion = estadisticasViewModel.obtenerDistribucionEstadosAnimo()

            val entries = mutableListOf<BarEntry>()
            val tipos = listOf(
                TipoEstadoAnimo.MUY_FELIZ,
                TipoEstadoAnimo.FELIZ,
                TipoEstadoAnimo.NEUTRAL,
                TipoEstadoAnimo.TRISTE,
                TipoEstadoAnimo.MUY_TRISTE
            )

            tipos.forEachIndexed { index, tipo ->
                val cantidad = distribucion[tipo]?.toFloat() ?: 0f
                entries.add(BarEntry(index.toFloat(), cantidad))
            }

            val dataSet = BarDataSet(entries, "Estados de Ánimo")
            dataSet.color = Color.BLUE
            dataSet.valueTextSize = 12f

            val barData = BarData(dataSet)
            binding.barChartEstadosAnimo.data = barData
            binding.barChartEstadosAnimo.description.isEnabled = false
            binding.barChartEstadosAnimo.legend.isEnabled = true
            binding.barChartEstadosAnimo.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return tipos.getOrNull(value.toInt())?.name ?: ""
                }
            }
            binding.barChartEstadosAnimo.invalidate()
        } catch (e: Exception) {
            android.util.Log.e("EstadisticasFragment", "Error al actualizar gráfica de estados de ánimo: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

