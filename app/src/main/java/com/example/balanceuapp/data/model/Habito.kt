package com.example.balanceuapp.data.model

/**
 * Modelo de datos que representa un hábito del usuario.
 * 
 * @property id Identificador único del hábito (generado por Firestore)
 * @property usuarioId ID del usuario propietario del hábito
 * @property nombre Nombre del hábito
 * @property descripcion Descripción opcional del hábito
 * @property fechaCreacion Timestamp de cuando se creó el hábito (en milisegundos)
 * @property completado Indica si el hábito ha sido completado
 * @property fechaCompletado Timestamp de cuando se completó el hábito (null si no está completado)
 */
data class Habito(
    val id: String = "",
    val usuarioId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val completado: Boolean = false,
    val fechaCompletado: Long? = null
) {
    /**
     * Convierte el objeto Habito a un Map para guardarlo en Firestore.
     * 
     * @return Map con los campos del hábito
     */
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
        /**
         * Crea un objeto Habito desde un Map (típicamente desde Firestore).
         * 
         * @param map Map con los datos del hábito
         * @return Objeto Habito creado desde el Map
         */
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

