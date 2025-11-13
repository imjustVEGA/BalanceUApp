package com.example.balanceuapp.data.model

data class Usuario(
    val id: String = "",
    val email: String = "",
    val nombre: String = "",
    val fechaRegistro: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "email" to email,
            "nombre" to nombre,
            "fechaRegistro" to fechaRegistro
        )
    }

    companion object {
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

