package com.example.balanceuapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.balanceuapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()

    private val _registroResult = MutableLiveData<Result<String>>()
    val registroResult: LiveData<Result<String>> = _registroResult

    private val _loginResult = MutableLiveData<Result<String>>()
    val loginResult: LiveData<Result<String>> = _loginResult

    private val _usuarioActual = MutableLiveData<String?>()
    val usuarioActual: LiveData<String?> = _usuarioActual

    init {
        try {
            _usuarioActual.value = authRepository.obtenerUsuarioActual()
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Error al obtener usuario actual en init: ${e.message}", e)
            _usuarioActual.value = null
        }
    }

    fun registrarUsuario(email: String, password: String, nombre: String) {
        viewModelScope.launch {
            val result = authRepository.registrarUsuario(email, password, nombre)
            _registroResult.postValue(result)
            if (result.isSuccess) {
                _usuarioActual.postValue(result.getOrNull())
            }
        }
    }

    fun iniciarSesion(email: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.iniciarSesion(email, password)
            _loginResult.postValue(result)
            if (result.isSuccess) {
                _usuarioActual.postValue(result.getOrNull())
            }
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
        _usuarioActual.postValue(null)
    }

    fun verificarSesion(): Boolean {
        return authRepository.obtenerUsuarioActual() != null
    }

    fun obtenerUsuarioActual(): String? {
        return authRepository.obtenerUsuarioActual()
    }
}

