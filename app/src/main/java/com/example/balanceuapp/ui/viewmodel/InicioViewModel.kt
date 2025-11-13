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
import kotlinx.coroutines.launch

class InicioViewModel(application: Application) : AndroidViewModel(application) {
    private val habitoRepository = HabitoRepository()
    private val estadoAnimoRepository = EstadoAnimoRepository()
    private val fraseRepository = FraseRepository(application)

    private val _habitosDelDia = MutableLiveData<List<Habito>>()
    val habitosDelDia: LiveData<List<Habito>> = _habitosDelDia

    private val _estadoAnimoDelDia = MutableLiveData<EstadoAnimo?>()
    val estadoAnimoDelDia: LiveData<EstadoAnimo?> = _estadoAnimoDelDia

    private val _fraseMotivacional = MutableLiveData<FraseMotivacional?>()
    val fraseMotivacional: LiveData<FraseMotivacional?> = _fraseMotivacional

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun cargarResumenDelDia(usuarioId: String) {
        val fechaActual = System.currentTimeMillis()
        val inicioDia = fechaActual - (fechaActual % (24 * 60 * 60 * 1000))
        val finDia = inicioDia + (24 * 60 * 60 * 1000) - 1

        viewModelScope.launch {
            // Cargar hábitos del día
            val resultHabitos = habitoRepository.obtenerHabitosDelDia(usuarioId, inicioDia, finDia)
            resultHabitos.onSuccess { lista ->
                _habitosDelDia.postValue(lista)
            }.onFailure { exception ->
                _error.postValue("Error al cargar hábitos: ${exception.message}")
            }

            // Cargar estado de ánimo del día
            val resultEstado = estadoAnimoRepository.obtenerEstadoAnimoDelDia(usuarioId, inicioDia, finDia)
            resultEstado.onSuccess { estado ->
                _estadoAnimoDelDia.postValue(estado)
            }.onFailure { exception ->
                _error.postValue("Error al cargar estado de ánimo: ${exception.message}")
            }

            // Cargar frase motivacional
            val resultFrase = fraseRepository.obtenerFraseAleatoria()
            resultFrase.onSuccess { frase ->
                _fraseMotivacional.postValue(frase)
            }.onFailure { exception ->
                _error.postValue("Error al cargar frase: ${exception.message}")
            }
        }
    }

    fun obtenerHabitossCompletados(): Int {
        return _habitosDelDia.value?.count { it.completado } ?: 0
    }

    fun obtenerTotalHabitoss(): Int {
        return _habitosDelDia.value?.size ?: 0
    }
}

