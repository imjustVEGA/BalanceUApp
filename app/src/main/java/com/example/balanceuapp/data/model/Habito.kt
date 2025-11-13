package com.example.balanceuapp.data.model

data class Habito(
    val id: String = "",
    val usuarioId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val completado: Boolean = false,
    val fechaCompletado: Long? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "usuarioId" to usuarioId,
            "nombre" to nombre,
            "descripcion" to descripcion,
            "fechaCreacion" to fechaCreacion,
            "completado" to completado,
            "fechaCompletado" to fechaCompletado
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Habito {
            return Habito(
                id = map["id"] as? String ?: "",
                usuarioId = map["usuarioId"] as? String ?: "",
                nombre = map["nombre"] as? String ?: "",
                descripcion = map["descripcion"] as? String ?: "",
                fechaCreacion = (map["fechaCreacion"] as? Long) ?: System.currentTimeMillis(),
                completado = map["completado"] as? Boolean ?: false,
                fechaCompletado = map["fechaCompletado"] as? Long?
            )
        }
    }
}

