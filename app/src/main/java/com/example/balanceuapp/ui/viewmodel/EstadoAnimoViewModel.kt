package com.example.balanceuapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.balanceuapp.data.model.EstadoAnimo
import com.example.balanceuapp.data.repository.EstadoAnimoRepository
import com.example.balanceuapp.util.Constants
import kotlinx.coroutines.launch

/**
 * ViewModel que maneja la lógica de gestión de estados de ánimo.
 * Expone LiveData para observar estados de ánimo, errores y operaciones exitosas.
 */
class EstadoAnimoViewModel(application: Application) : AndroidViewModel(application) {
    
    private val estadoAnimoRepository = EstadoAnimoRepository()

    private val _estadoAnimoActual = MutableLiveData<EstadoAnimo?>()
    /** LiveData que expone el estado de ánimo actual del día */
    val estadoAnimoActual: LiveData<EstadoAnimo?> = _estadoAnimoActual

    private val _estadosAnimo = MutableLiveData<List<EstadoAnimo>>()
    /** LiveData que expone la lista de estados de ánimo */
    val estadosAnimo: LiveData<List<EstadoAnimo>> = _estadosAnimo

    private val _error = MutableLiveData<String?>()
    /** LiveData que expone errores que puedan ocurrir */
    val error: LiveData<String?> = _error

    private val _operacionExitosa = MutableLiveData<Boolean?>()
    /** LiveData que expone si la última operación fue exitosa (null = sin operación) */
    val operacionExitosa: LiveData<Boolean?> = _operacionExitosa

    /**
     * Registra un nuevo estado de ánimo.
     * Después de registrar exitosamente, carga el estado de ánimo del día.
     * 
     * @param estadoAnimo Objeto EstadoAnimo a registrar
     */
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

    /**
     * Limpia el estado de operación exitosa, estableciéndolo en null.
     */
    fun limpiarOperacionExitosa() {
        _operacionExitosa.postValue(null)
    }

    /**
     * Obtiene el estado de ánimo de un usuario para un día específico.
     * 
     * @param usuarioId ID del usuario
     * @param fecha Timestamp del día (en milisegundos)
     */
    fun obtenerEstadoAnimoDelDia(usuarioId: String, fecha: Long) {
        viewModelScope.launch {
            val inicioDia = fecha - (fecha % Constants.Time.MILISEGUNDOS_EN_UN_DIA)
            val finDia = inicioDia + Constants.Time.MILISEGUNDOS_EN_UN_DIA - 1
            
            val result = estadoAnimoRepository.obtenerEstadoAnimoDelDia(usuarioId, inicioDia, finDia)
            result.onSuccess { estado ->
                _estadoAnimoActual.postValue(estado)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
            }
        }
    }

    /**
     * Carga todos los estados de ánimo de un usuario.
     * 
     * @param usuarioId ID del usuario
     */
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

    /**
     * Obtiene los estados de ánimo de un usuario en un rango de fechas.
     * 
     * @param usuarioId ID del usuario
     * @param fechaInicio Timestamp de inicio del rango
     * @param fechaFin Timestamp de fin del rango
     */
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

