package com.example.balanceuapp.data.model

/**
 * Modelo de datos que representa una frase motivacional.
 * 
 * @property id Identificador único de la frase (generado por Firestore)
 * @property frase Texto de la frase motivacional
 * @property autor Nombre del autor de la frase
 */
data class FraseMotivacional(
    val id: String = "",
    val frase: String = "",
    val autor: String = ""
) {
    /**
     * Convierte el objeto FraseMotivacional a un Map para guardarlo en Firestore.
     * 
     * @return Map con los campos de la frase
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "frase" to frase,
            "autor" to autor
        )
    }

    companion object {
        /**
         * Crea un objeto FraseMotivacional desde un Map (típicamente desde Firestore).
         * 
         * @param map Map con los datos de la frase
         * @return Objeto FraseMotivacional creado desde el Map
         */
        fun fromMap(map: Map<String, Any?>): FraseMotivacional {
            return FraseMotivacional(
                id = map["id"] as? String ?: "",
                frase = map["frase"] as? String ?: "",
                autor = map["autor"] as? String ?: ""
            )
        }
    }
}

