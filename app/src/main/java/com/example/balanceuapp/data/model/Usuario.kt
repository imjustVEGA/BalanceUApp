package com.example.balanceuapp.data.model

/**
 * Modelo de datos que representa un usuario de la aplicación.
 * 
 * @property id Identificador único del usuario (generado por Firebase)
 * @property email Correo electrónico del usuario
 * @property nombre Nombre completo del usuario
 * @property fechaRegistro Timestamp de cuando se registró el usuario (en milisegundos)
 */
data class Usuario(
    val id: String = "",
    val email: String = "",
    val nombre: String = "",
    val fechaRegistro: Long = System.currentTimeMillis()
) {
    /**
     * Convierte el objeto Usuario a un Map para guardarlo en Firestore.
     * 
     * @return Map con los campos del usuario
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "email" to email,
            "nombre" to nombre,
            "fechaRegistro" to fechaRegistro
        )
    }

    companion object {
        /**
         * Crea un objeto Usuario desde un Map (típicamente desde Firestore).
         * 
         * @param map Map con los datos del usuario
         * @return Objeto Usuario creado desde el Map
         */
        fun fromMap(map: Map<String, Any?>): Usuario {
            return Usuario(
                id = map["id"] as? String ?: "",
                email = map["email"] as? String ?: "",
                nombre = map["nombre"] as? String ?: "",
                fechaRegistro = (map["fechaRegistro"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

