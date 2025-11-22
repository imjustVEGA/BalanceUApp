package com.example.balanceuapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.balanceuapp.data.model.Habito
import com.example.balanceuapp.data.repository.HabitoRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

/**
 * ViewModel que maneja la lógica de gestión de hábitos.
 * Expone LiveData para observar la lista de hábitos, errores y operaciones exitosas.
 */
class HabitoViewModel(application: Application) : AndroidViewModel(application) {
    
    private val habitoRepository = HabitoRepository()
    private var habitosListener: ListenerRegistration? = null

    private val _habitos = MutableLiveData<List<Habito>>()
    /** LiveData que expone la lista de hábitos del usuario */
    val habitos: LiveData<List<Habito>> = _habitos

    private val _error = MutableLiveData<String?>()
    /** LiveData que expone errores que puedan ocurrir */
    val error: LiveData<String?> = _error

    private val _operacionExitosa = MutableLiveData<Boolean>()
    /** LiveData que expone si la última operación fue exitosa */
    val operacionExitosa: LiveData<Boolean> = _operacionExitosa

    /**
     * Carga todos los hábitos de un usuario desde Firestore.
     * 
     * @param usuarioId ID del usuario
     */
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

    /**
     * Agrega un nuevo hábito.
     * 
     * @param habito Objeto Habito a agregar
     */
    fun agregarHabito(habito: Habito) {
        viewModelScope.launch {
            val result = habitoRepository.agregarHabito(habito)
            result.onSuccess {
                _operacionExitosa.postValue(true)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
                _operacionExitosa.postValue(false)
            }
        }
    }

    /**
     * Actualiza un hábito existente.
     * 
     * @param habito Objeto Habito con los datos actualizados
     */
    fun actualizarHabito(habito: Habito) {
        viewModelScope.launch {
            val result = habitoRepository.actualizarHabito(habito)
            result.onSuccess {
                _operacionExitosa.postValue(true)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
                _operacionExitosa.postValue(false)
            }
        }
    }

    /**
     * Marca un hábito como completado o no completado.
     * 
     * @param habitoId ID del hábito
     * @param completado true para marcar como completado, false para desmarcar
     * @param usuarioId ID del usuario (parámetro no utilizado, mantenido por compatibilidad)
     */
    fun marcarCompletado(habitoId: String, completado: Boolean, usuarioId: String) {
        viewModelScope.launch {
            val result = habitoRepository.marcarCompletado(habitoId, completado)
            result.onSuccess {
                _operacionExitosa.postValue(true)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
                _operacionExitosa.postValue(false)
            }
        }
    }

    /**
     * Elimina un hábito.
     * 
     * @param habitoId ID del hábito a eliminar
     * @param usuarioId ID del usuario (parámetro no utilizado, mantenido por compatibilidad)
     */
    fun eliminarHabito(habitoId: String, usuarioId: String) {
        viewModelScope.launch {
            val result = habitoRepository.eliminarHabito(habitoId)
            result.onSuccess {
                _operacionExitosa.postValue(true)
                _error.postValue(null)
            }.onFailure { exception ->
                _error.postValue(exception.message)
                _operacionExitosa.postValue(false)
            }
        }
    }

    /**
     * Obtiene los hábitos de un usuario para un rango de fechas específico.
     * 
     * @param usuarioId ID del usuario
     * @param fechaInicio Timestamp de inicio del rango
     * @param fechaFin Timestamp de fin del rango
     */
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

    /**
     * Inicia la observación en tiempo real de todos los hábitos de un usuario.
     * 
     * @param usuarioId ID del usuario
     */
    fun startObservandoHabitos(usuarioId: String) {
        stopObservandoHabitos()
        habitosListener = habitoRepository.observarHabitos(
            usuarioId = usuarioId,
            onUpdate = { lista ->
                _habitos.postValue(lista)
                _error.postValue(null)
            },
            onError = { exception ->
                _error.postValue(exception.message)
            }
        )
    }

    /**
     * Detiene la observación de hábitos.
     */
    fun stopObservandoHabitos() {
        habitosListener?.remove()
        habitosListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopObservandoHabitos()
    }
}

