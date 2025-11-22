package com.example.balanceuapp

import android.app.Application
import android.util.Log
import com.example.balanceuapp.util.Constants

/**
 * Clase Application personalizada para Balance-U.
 * Se inicializa cuando la aplicación se inicia y configura Firebase automáticamente
 * si el archivo google-services.json está presente.
 */
class BalanceUApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        try {
            // Firebase se inicializa automáticamente si google-services.json está presente
            Log.d(Constants.LogTags.BALANCE_U_APPLICATION, "Application iniciada correctamente")
        } catch (e: Exception) {
            Log.e(Constants.LogTags.BALANCE_U_APPLICATION, "Error al inicializar aplicación: ${e.message}", e)
        }
    }
}

