package com.example.balanceuapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.balanceuapp.data.model.EstadoAnimo
import com.example.balanceuapp.data.repository.EstadoAnimoRepository
import kotlinx.coroutines.launch

class EstadoAnimoViewModel(application: Application) : AndroidViewModel(application) {
    private val estadoAnimoRepository = EstadoAnimoRepository()

    private val _estadoAnimoActual = MutableLiveData<EstadoAnimo?>()
    val estadoAnimoActual: LiveData<EstadoAnimo?> = _estadoAnimoActual

    private val _estadosAnimo = MutableLiveData<List<EstadoAnimo>>()
    val estadosAnimo: LiveData<List<EstadoAnimo>> = _estadosAnimo

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _operacionExitosa = MutableLiveData<Boolean?>()
    val operacionExitosa: LiveData<Boolean?> = _operacionExitosa

    fun registrarEstadoAnimo(estadoAnimo: EstadoAnimo) {
        viewModelScope.launch {
            val result = estadoAnimoRepository.agregarEstadoAnimo(estadoAnimo)
            result.onSuccess {
                _operacionExitosa.postValue(true)
                _error.postValue(null)
                obtenerEstadoAnimoDelDia(estadoAnimo.usuarioId, estadoAnimo.fecha)
            }.onFailure { exception ->
                _error.postValue(exception.message)
                _operacionExitosa.postValue(false)
            }
        }
    }

    fun limpiarOperacionExitosa() {
        _operacionExitosa.postValue(null)
    }

    fun obtenerEstadoAnimoDelDia(usuarioId: String, fecha: Long) {
        viewModelScope.launch {
            val inicioDia = fecha - (fecha % (24 * 60 * 60 * 1000))
            val finDia = inicioDia + (24 * 60 * 60 * 1000) - 1
            
            val result = estadoAnimoRepository.obtenerEstadoAnimoDelDia(usuarioId, inicioDia, finDia)
            result.onSuccess { estado ->
                _estadoAnimoActual.postValue(estado)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
            }
        }
    }

    fun cargarEstadosAnimo(usuarioId: String) {
        viewModelScope.launch {
            val result = estadoAnimoRepository.obtenerEstadosAnimo(usuarioId)
            result.onSuccess { lista ->
                _estadosAnimo.postValue(lista)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
            }
        }
    }

    fun obtenerEstadosAnimoPorRango(usuarioId: String, fechaInicio: Long, fechaFin: Long) {
        viewModelScope.launch {
            val result = estadoAnimoRepository.obtenerEstadosAnimoPorRango(usuarioId, fechaInicio, fechaFin)
            result.onSuccess { lista ->
                _estadosAnimo.postValue(lista)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
            }
        }
    }
}

