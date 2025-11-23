package com.example.balanceuapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.balanceuapp.data.model.EstadoAnimo
import com.example.balanceuapp.data.model.Habito
import com.example.balanceuapp.data.model.TipoEstadoAnimo
import com.example.balanceuapp.data.repository.EstadoAnimoRepository
import com.example.balanceuapp.data.repository.HabitoRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class EstadisticasViewModel(application: Application) : AndroidViewModel(application) {
    private val habitoRepository = HabitoRepository()
    private val estadoAnimoRepository = EstadoAnimoRepository()
    private var estadosAnimoListener: ListenerRegistration? = null

    private val _habitos = MutableLiveData<List<Habito>>()
    val habitos: LiveData<List<Habito>> = _habitos

    private val _estadosAnimo = MutableLiveData<List<EstadoAnimo>>()
    val estadosAnimo: LiveData<List<EstadoAnimo>> = _estadosAnimo

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun cargarDatosEstadisticos(usuarioId: String, fechaInicio: Long, fechaFin: Long) {
        detenerObservadorEstadosAnimo()
        viewModelScope.launch {
            // Cargar hábitos
            val resultHabitos = habitoRepository.obtenerHabitosDelDia(usuarioId, fechaInicio, fechaFin)
            resultHabitos.onSuccess { lista ->
                _habitos.postValue(lista)
            }.onFailure { exception ->
                _error.postValue("Error al cargar hábitos: ${exception.message}")
            }

            // Cargar estados de ánimo
            val resultEstados = estadoAnimoRepository.obtenerEstadosAnimoPorRango(usuarioId, fechaInicio, fechaFin)
            resultEstados.onSuccess { lista ->
                _estadosAnimo.postValue(lista)
            }.onFailure { exception ->
                _error.postValue("Error al cargar estados de ánimo: ${exception.message}")
            }
        }
        iniciarObservadorEstadosAnimo(usuarioId, fechaInicio, fechaFin)
    }

    fun calcularPorcentajeCompletitud(): Float {
        val lista = _habitos.value ?: return 0f
        if (lista.isEmpty()) return 0f
        val completados = lista.count { it.completado }
        return (completados.toFloat() / lista.size) * 100f
    }

    fun obtenerDistribucionEstadosAnimo(): Map<TipoEstadoAnimo, Int> {
        val lista = _estadosAnimo.value ?: return emptyMap()
        return lista.groupingBy { it.tipo }.eachCount()
    }

    private fun iniciarObservadorEstadosAnimo(usuarioId: String, fechaInicio: Long, fechaFin: Long) {
        estadosAnimoListener?.remove()
        try {
            estadosAnimoListener = estadoAnimoRepository.observarEstadosAnimoPorRango(
                usuarioId = usuarioId,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                onUpdate = { lista ->
                    _estadosAnimo.postValue(lista)
                    _error.postValue(null)
                },
                onError = { exception ->
                    _error.postValue("Error al observar estados de ánimo: ${exception.message}")
                }
            )
        } catch (exception: Exception) {
            _error.postValue("Error al iniciar el observador de estados: ${exception.message}")
        }
    }

    private fun detenerObservadorEstadosAnimo() {
        estadosAnimoListener?.remove()
        estadosAnimoListener = null
    }

    override fun onCleared() {
        super.onCleared()
        detenerObservadorEstadosAnimo()
    }
}

