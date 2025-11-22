package com.example.balanceuapp.ui.viewmodel

import android.app.Application
import android.util.Log
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
import com.example.balanceuapp.util.Constants
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

/**
 * ViewModel que maneja la lógica de la pantalla de inicio.
 * Gestiona el resumen del día: hábitos, estado de ánimo y frase motivacional.
 */
class InicioViewModel(application: Application) : AndroidViewModel(application) {
    
    private val habitoRepository = HabitoRepository()
    private val estadoAnimoRepository = EstadoAnimoRepository()
    private val fraseRepository = FraseRepository(application)
    private var habitosDelDiaListener: ListenerRegistration? = null

    private val _habitosDelDia = MutableLiveData<List<Habito>>()
    /** LiveData que expone la lista de hábitos del día actual */
    val habitosDelDia: LiveData<List<Habito>> = _habitosDelDia

    private val _estadoAnimoDelDia = MutableLiveData<EstadoAnimo?>()
    /** LiveData que expone el estado de ánimo registrado para el día actual */
    val estadoAnimoDelDia: LiveData<EstadoAnimo?> = _estadoAnimoDelDia

    private val _fraseMotivacional = MutableLiveData<FraseMotivacional?>()
    /** LiveData que expone la frase motivacional del día */
    val fraseMotivacional: LiveData<FraseMotivacional?> = _fraseMotivacional

    private val _error = MutableLiveData<String?>()
    /** LiveData que expone errores que puedan ocurrir */
    val error: LiveData<String?> = _error

    /**
     * Carga el resumen completo del día: estado de ánimo y frase motivacional.
     * 
     * @param usuarioId ID del usuario para cargar sus datos
     */
    fun cargarResumenDelDia(usuarioId: String) {
        viewModelScope.launch {
            try {
                // Cargar estado de ánimo del día
                val (inicioDia, finDia) = calcularLimitesDelDia(System.currentTimeMillis())
                val resultEstado = estadoAnimoRepository.obtenerEstadoAnimoDelDia(usuarioId, inicioDia, finDia)
                resultEstado.onSuccess { estado ->
                    _estadoAnimoDelDia.postValue(estado)
                }.onFailure { exception ->
                    Log.e(Constants.LogTags.INICIO_VIEW_MODEL, "Error al cargar estado de ánimo: ${exception.message}", exception)
                    _error.postValue("Error al cargar estado de ánimo: ${exception.message}")
                    _estadoAnimoDelDia.postValue(null)
                }

                // Cargar frase motivacional
                val resultFrase = fraseRepository.obtenerFraseAleatoria()
                resultFrase.onSuccess { frase ->
                    _fraseMotivacional.postValue(frase)
                }.onFailure { exception ->
                    Log.e(Constants.LogTags.INICIO_VIEW_MODEL, "Error al cargar frase: ${exception.message}", exception)
                    _error.postValue("Error al cargar frase: ${exception.message}")
                    _fraseMotivacional.postValue(null)
                }
            } catch (e: Exception) {
                Log.e(Constants.LogTags.INICIO_VIEW_MODEL, "Error general en cargarResumenDelDia: ${e.message}", e)
                _error.postValue("${Constants.ErrorMessages.ERROR_CARGAR_DATOS}: ${e.message}")
            }
        }
    }

    /**
     * Obtiene el número de hábitos completados del día.
     * 
     * @return Número de hábitos completados
     */
    fun obtenerHabitossCompletados(): Int {
        return _habitosDelDia.value?.count { it.completado } ?: 0
    }

    /**
     * Obtiene el número total de hábitos del día.
     * 
     * @return Número total de hábitos
     */
    fun obtenerTotalHabitoss(): Int {
        return _habitosDelDia.value?.size ?: 0
    }

    /**
     * Inicia la observación en tiempo real de los hábitos del día.
     * 
     * @param usuarioId ID del usuario
     */
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

    /**
     * Detiene la observación de hábitos del día.
     */
    fun stopObservandoHabitosDelDia() {
        habitosDelDiaListener?.remove()
        habitosDelDiaListener = null
    }

    /**
     * Calcula los límites de tiempo (inicio y fin) para el día actual.
     * 
     * @param fechaActual Timestamp actual en milisegundos
     * @return Par con el timestamp de inicio y fin del día
     */
    private fun calcularLimitesDelDia(fechaActual: Long): Pair<Long, Long> {
        val inicioDia = fechaActual - (fechaActual % Constants.Time.MILISEGUNDOS_EN_UN_DIA)
        val finDia = inicioDia + Constants.Time.MILISEGUNDOS_EN_UN_DIA - 1
        return inicioDia to finDia
    }

    override fun onCleared() {
        super.onCleared()
        stopObservandoHabitosDelDia()
    }
}

