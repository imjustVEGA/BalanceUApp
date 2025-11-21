package com.example.balanceuapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.balanceuapp.data.model.EstadoAnimo
import com.example.balanceuapp.data.model.FraseMotivacional
import com.example.balanceuapp.data.model.Habito
import com.example.balanceuapp.data.repository.EstadoAnimoRepository
import com.example.balanceuapp.data.repository.FraseRepository
import com.example.balanceuapp.data.repository.HabitoRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class InicioViewModel(application: Application) : AndroidViewModel(application) {
    private val habitoRepository = HabitoRepository()
    private val estadoAnimoRepository = EstadoAnimoRepository()
    private val fraseRepository = FraseRepository(application)
    private var habitosDelDiaListener: ListenerRegistration? = null

    private val _habitosDelDia = MutableLiveData<List<Habito>>()
    val habitosDelDia: LiveData<List<Habito>> = _habitosDelDia

    private val _estadoAnimoDelDia = MutableLiveData<EstadoAnimo?>()
    val estadoAnimoDelDia: LiveData<EstadoAnimo?> = _estadoAnimoDelDia

    private val _fraseMotivacional = MutableLiveData<FraseMotivacional?>()
    val fraseMotivacional: LiveData<FraseMotivacional?> = _fraseMotivacional

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun cargarResumenDelDia(usuarioId: String) {
        viewModelScope.launch {
            try {
                // Cargar estado de ánimo del día
                val (inicioDia, finDia) = calcularLimitesDelDia(System.currentTimeMillis())
                val resultEstado = estadoAnimoRepository.obtenerEstadoAnimoDelDia(usuarioId, inicioDia, finDia)
                resultEstado.onSuccess { estado ->
                    _estadoAnimoDelDia.postValue(estado)
                }.onFailure { exception ->
                    android.util.Log.e("InicioViewModel", "Error al cargar estado de ánimo: ${exception.message}", exception)
                    _error.postValue("Error al cargar estado de ánimo: ${exception.message}")
                    _estadoAnimoDelDia.postValue(null) // Inicializar con null
                }

                // Cargar frase motivacional
                val resultFrase = fraseRepository.obtenerFraseAleatoria()
                resultFrase.onSuccess { frase ->
                    _fraseMotivacional.postValue(frase)
                }.onFailure { exception ->
                    android.util.Log.e("InicioViewModel", "Error al cargar frase: ${exception.message}", exception)
                    _error.postValue("Error al cargar frase: ${exception.message}")
                    _fraseMotivacional.postValue(null) // Inicializar con null
                }
            } catch (e: Exception) {
                android.util.Log.e("InicioViewModel", "Error general en cargarResumenDelDia: ${e.message}", e)
                _error.postValue("Error al cargar datos: ${e.message}")
            }
        }
    }

    fun obtenerHabitossCompletados(): Int {
        return _habitosDelDia.value?.count { it.completado } ?: 0
    }

    fun obtenerTotalHabitoss(): Int {
        return _habitosDelDia.value?.size ?: 0
    }

    fun startObservandoHabitosDelDia(usuarioId: String) {
        stopObservandoHabitosDelDia()
        val (inicioDia, finDia) = calcularLimitesDelDia(System.currentTimeMillis())
        habitosDelDiaListener = habitoRepository.observarHabitosDelDia(
            usuarioId = usuarioId,
            fechaInicio = inicioDia,
            fechaFin = finDia,
            onUpdate = { lista ->
                _habitosDelDia.postValue(lista)
                _error.postValue(null)
            },
            onError = { exception ->
                _error.postValue(exception.message)
            }
        )
    }

    fun stopObservandoHabitosDelDia() {
        habitosDelDiaListener?.remove()
        habitosDelDiaListener = null
    }

    private fun calcularLimitesDelDia(fechaActual: Long): Pair<Long, Long> {
        val inicioDia = fechaActual - (fechaActual % MILISEGUNDOS_EN_UN_DIA)
        val finDia = inicioDia + MILISEGUNDOS_EN_UN_DIA - 1
        return inicioDia to finDia
    }

    override fun onCleared() {
        super.onCleared()
        stopObservandoHabitosDelDia()
    }

    private companion object {
        const val MILISEGUNDOS_EN_UN_DIA = 24 * 60 * 60 * 1000L
    }
}

