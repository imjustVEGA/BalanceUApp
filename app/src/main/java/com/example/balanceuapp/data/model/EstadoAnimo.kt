package com.example.balanceuapp.data.model

enum class TipoEstadoAnimo {
    MUY_FELIZ,
    FELIZ,
    NEUTRAL,
    TRISTE,
    MUY_TRISTE
}

data class EstadoAnimo(
    val id: String = "",
    val usuarioId: String = "",
    val tipo: TipoEstadoAnimo = TipoEstadoAnimo.NEUTRAL,
    val nota: String = "",
    val fecha: Long = System.currentTimeMillis()
) {
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

