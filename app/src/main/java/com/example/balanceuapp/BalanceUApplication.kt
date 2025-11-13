package com.example.balanceuapp

import android.app.Application
import android.util.Log

class BalanceUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Firebase se inicializa automáticamente si google-services.json está presente
            Log.d("BalanceUApplication", "Application iniciada correctamente")
        } catch (e: Exception) {
            Log.e("BalanceUApplication", "Error al inicializar aplicación: ${e.message}", e)
        }
    }
}

