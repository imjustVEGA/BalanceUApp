package com.example.balanceuapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.balanceuapp.data.model.Habito
import com.example.balanceuapp.data.repository.HabitoRepository
import kotlinx.coroutines.launch

class HabitoViewModel(application: Application) : AndroidViewModel(application) {
    private val habitoRepository = HabitoRepository()

    private val _habitos = MutableLiveData<List<Habito>>()
    val habitos: LiveData<List<Habito>> = _habitos

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _operacionExitosa = MutableLiveData<Boolean>()
    val operacionExitosa: LiveData<Boolean> = _operacionExitosa

    fun cargarHabitos(usuarioId: String) {
        viewModelScope.launch {
            val result = habitoRepository.obtenerHabitos(usuarioId)
            result.onSuccess { lista ->
                _habitos.postValue(lista)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
            }
        }
    }

    fun agregarHabito(habito: Habito) {
        viewModelScope.launch {
            val result = habitoRepository.agregarHabito(habito)
            result.onSuccess {
                _operacionExitosa.postValue(true)
                cargarHabitos(habito.usuarioId)
            }.onFailure { exception ->
                _error.postValue(exception.message)
                _operacionExitosa.postValue(false)
            }
        }
    }

    fun marcarCompletado(habitoId: String, completado: Boolean, usuarioId: String) {
        viewModelScope.launch {
            val result = habitoRepository.marcarCompletado(habitoId, completado)
            result.onSuccess {
                _operacionExitosa.postValue(true)
                cargarHabitos(usuarioId)
            }.onFailure { exception ->
                _error.postValue(exception.message)
                _operacionExitosa.postValue(false)
            }
        }
    }

    fun eliminarHabito(habitoId: String, usuarioId: String) {
        viewModelScope.launch {
            val result = habitoRepository.eliminarHabito(habitoId)
            result.onSuccess {
                _operacionExitosa.postValue(true)
                cargarHabitos(usuarioId)
            }.onFailure { exception ->
                _error.postValue(exception.message)
                _operacionExitosa.postValue(false)
            }
        }
    }

    fun obtenerHabitosDelDia(usuarioId: String, fechaInicio: Long, fechaFin: Long) {
        viewModelScope.launch {
            val result = habitoRepository.obtenerHabitosDelDia(usuarioId, fechaInicio, fechaFin)
            result.onSuccess { lista ->
                _habitos.postValue(lista)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
            }
        }
    }
}

