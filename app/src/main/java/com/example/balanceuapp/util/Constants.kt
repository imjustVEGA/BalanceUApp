package com.example.balanceuapp.util

/**
 * Constantes utilizadas en toda la aplicación
 */
object Constants {
    
    // Firebase Collections
    object Firestore {
        const val COLLECTION_USUARIOS = "usuarios"
        const val COLLECTION_HABITOS = "habitos"
        const val COLLECTION_ESTADOS_ANIMO = "estadosAnimo"
        const val COLLECTION_FRASES_MOTIVACIONALES = "frasesMotivacionales"
    }
    
    // Firebase Fields
    object FirestoreFields {
        const val FIELD_USUARIO_ID = "usuarioId"
        const val FIELD_FECHA = "fecha"
        const val FIELD_FECHA_CREACION = "fechaCreacion"
        const val FIELD_COMPLETADO = "completado"
    }
    
    // Time Constants
    object Time {
        const val MILISEGUNDOS_EN_UN_DIA = 24 * 60 * 60 * 1000L
    }
    
    // Validation
    object Validation {
        const val PASSWORD_MIN_LENGTH = 6
    }
    
    // Log Tags
    object LogTags {
        const val AUTH_REPOSITORY = "AuthRepository"
        const val AUTH_VIEW_MODEL = "AuthViewModel"
        const val AUTH_ACTIVITY = "AuthActivity"
        const val MAIN_ACTIVITY = "MainActivity"
        const val HABITO_REPOSITORY = "HabitoRepository"
        const val HABITO_VIEW_MODEL = "HabitoViewModel"
        const val ESTADO_ANIMO_REPOSITORY = "EstadoAnimoRepository"
        const val ESTADO_ANIMO_VIEW_MODEL = "EstadoAnimoViewModel"
        const val FRASE_REPOSITORY = "FraseRepository"
        const val INICIO_VIEW_MODEL = "InicioViewModel"
        const val INICIO_FRAGMENT = "InicioFragment"
        const val HABITOS_FRAGMENT = "HabitosFragment"
        const val BALANCE_U_APPLICATION = "BalanceUApplication"
    }
    
    // Error Messages
    object ErrorMessages {
        const val ERROR_FIREBASE_INIT = "Error al inicializar Firebase"
        const val ERROR_USUARIO_NO_ENCONTRADO = "Usuario no encontrado"
        const val ERROR_USUARIO_NO_AUTENTICADO = "Usuario no autenticado"
        const val ERROR_OBTENER_ID_USUARIO = "Error al obtener ID de usuario"
        const val ERROR_CARGAR_DATOS = "Error al cargar datos"
        const val ERROR_INICIALIZAR_APLICACION = "Error al inicializar la aplicación. Verifica la configuración de Firebase."
        const val ERROR_NO_HAY_FRASES = "No hay frases disponibles"
    }
    
    // Success Messages
    object SuccessMessages {
        const val LOGIN_EXITOSO = "Inicio de sesión exitoso"
        const val REGISTRO_EXITOSO = "Registro exitoso"
        const val REGISTRO_GUARDADO = "Registro guardado"
    }
    
    // UI Messages
    object UIMessages {
        const val COMPLETAR_CAMPOS = "Por favor completa todos los campos"
        const val PASSWORD_CORTA = "La contraseña debe tener al menos 6 caracteres"
        const val SELECCIONAR_ESTADO_ANIMO = "Selecciona un estado de ánimo"
        const val NOMBRE_REQUERIDO = "El nombre es requerido"
        const val ELIMINAR_HABITO_CONFIRMACION = "¿Estás seguro de eliminar \"%s\"?"
        
        /**
         * Formatea el mensaje de confirmación de eliminación de hábito.
         */
        fun getEliminarHabitoConfirmacion(nombreHabito: String): String {
            return ELIMINAR_HABITO_CONFIRMACION.replace("%s", nombreHabito)
        }
    }
}

