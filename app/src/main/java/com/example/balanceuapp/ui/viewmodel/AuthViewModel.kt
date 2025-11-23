package com.example.balanceuapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.balanceuapp.data.repository.AuthRepository
import com.example.balanceuapp.util.Constants
import kotlinx.coroutines.launch

/**
 * ViewModel que maneja la lógica de autenticación de usuarios.
 * Expone LiveData para observar el estado de registro, login y usuario actual.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository()

    private val _registroResult = MutableLiveData<Result<String>>()
    /** LiveData que expone el resultado del registro de usuario */
    val registroResult: LiveData<Result<String>> = _registroResult

    private val _loginResult = MutableLiveData<Result<String>>()
    /** LiveData que expone el resultado del inicio de sesión */
    val loginResult: LiveData<Result<String>> = _loginResult

    private val _usuarioActual = MutableLiveData<String?>()
    /** LiveData que expone el ID del usuario actualmente autenticado */
    val usuarioActual: LiveData<String?> = _usuarioActual

    init {
        try {
            _usuarioActual.value = authRepository.obtenerUsuarioActual()
        } catch (e: Exception) {
            Log.e(Constants.LogTags.AUTH_VIEW_MODEL, "Error al obtener usuario actual en init: ${e.message}", e)
            _usuarioActual.value = null
        }
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @param nombre Nombre del usuario
     */
    fun registrarUsuario(email: String, password: String, nombre: String) {
        viewModelScope.launch {
            val result = authRepository.registrarUsuario(email, password, nombre)
            _registroResult.postValue(result)
            if (result.isSuccess) {
                _usuarioActual.postValue(result.getOrNull())
            }
        }
    }

    /**
     * Inicia sesión con un usuario existente.
     * 
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     */
    fun iniciarSesion(email: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.iniciarSesion(email, password)
            _loginResult.postValue(result)
            if (result.isSuccess) {
                _usuarioActual.postValue(result.getOrNull())
            }
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    fun cerrarSesion() {
        authRepository.cerrarSesion()
        _usuarioActual.postValue(null)
    }

    /**
     * Verifica si hay un usuario autenticado actualmente.
     * 
     * @return true si hay un usuario autenticado, false en caso contrario
     */
    fun verificarSesion(): Boolean {
        return authRepository.obtenerUsuarioActual() != null
    }

    /**
     * Obtiene el ID del usuario actualmente autenticado.
     * 
     * @return ID del usuario si está autenticado, null en caso contrario
     */
    fun obtenerUsuarioActual(): String? {
        return authRepository.obtenerUsuarioActual()
    }
}

