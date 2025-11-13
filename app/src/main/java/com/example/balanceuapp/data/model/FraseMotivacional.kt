package com.example.balanceuapp.data.model

data class FraseMotivacional(
    val id: String = "",
    val frase: String = "",
    val autor: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "frase" to frase,
            "autor" to autor
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): FraseMotivacional {
            return FraseMotivacional(
                id = map["id"] as? String ?: "",
                frase = map["frase"] as? String ?: "",
                autor = map["autor"] as? String ?: ""
            )
        }
    }
}

