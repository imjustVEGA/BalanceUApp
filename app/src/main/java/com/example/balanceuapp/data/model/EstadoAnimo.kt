package com.example.balanceuapp.data.model

/**
 * Enum que representa los diferentes tipos de estados de ánimo disponibles.
 */
enum class TipoEstadoAnimo {
    ALEGRE,
    FELIZ,
    NEUTRAL,
    TRISTE,
    TERRIBLE
}

/**
 * Modelo de datos que representa un registro de estado de ánimo del usuario.
 * 
 * @property id Identificador único del registro (generado por Firestore)
 * @property usuarioId ID del usuario que registró el estado de ánimo
 * @property tipo Tipo de estado de ánimo seleccionado
 * @property nota Nota opcional que el usuario puede agregar
 * @property fecha Timestamp de cuando se registró el estado de ánimo (en milisegundos)
 */
data class EstadoAnimo(
    val id: String = "",
    val usuarioId: String = "",
    val tipo: TipoEstadoAnimo = TipoEstadoAnimo.NEUTRAL,
    val nota: String = "",
    val fecha: Long = System.currentTimeMillis()
) {
    /**
     * Convierte el objeto EstadoAnimo a un Map para guardarlo en Firestore.
     * 
     * @return Map con los campos del estado de ánimo
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "usuarioId" to usuarioId,
            "tipo" to tipo.name,
            "nota" to nota,
            "fecha" to fecha
        )
    }

    companion object {
        /**
         * Crea un objeto EstadoAnimo desde un Map (típicamente desde Firestore).
         * Si el tipo no es válido, se usa NEUTRAL por defecto.
         * 
         * @param map Map con los datos del estado de ánimo
         * @return Objeto EstadoAnimo creado desde el Map
         */
        fun fromMap(map: Map<String, Any?>): EstadoAnimo {
            return EstadoAnimo(
                id = map["id"] as? String ?: "",
                usuarioId = map["usuarioId"] as? String ?: "",
                tipo = try {
                    TipoEstadoAnimo.valueOf(map["tipo"] as? String ?: "NEUTRAL")
                } catch (e: Exception) {
                    TipoEstadoAnimo.NEUTRAL
                },
                nota = map["nota"] as? String ?: "",
                fecha = (map["fecha"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

