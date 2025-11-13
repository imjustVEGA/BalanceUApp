package com.example.balanceuapp.data.repository

import android.content.Context
import com.example.balanceuapp.data.model.FraseMotivacional
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import java.io.IOException

class FraseRepository(private val context: Context) {
    private val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            throw IllegalStateException("Firebase no está configurado correctamente. Verifica que google-services.json sea válido.", e)
        }
    }
    private val gson = Gson()

    suspend fun obtenerFraseAleatoria(): Result<FraseMotivacional> {
        return try {
            // Primero intentar obtener desde Firebase
            val snapshot = firestore.collection("frasesMotivacionales")
                .get()
                .await()
            
            if (snapshot.documents.isNotEmpty()) {
                val frases = snapshot.documents.map { doc ->
                    FraseMotivacional.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
                }
                val fraseAleatoria = frases.random()
                Result.success(fraseAleatoria)
            } else {
                // Si no hay en Firebase, cargar desde JSON local
                cargarDesdeJSON()
            }
        } catch (e: Exception) {
            // Si falla Firebase, cargar desde JSON local
            cargarDesdeJSON()
        }
    }

    private suspend fun cargarDesdeJSON(): Result<FraseMotivacional> {
        return try {
            val jsonString = context.assets.open("frases_motivacionales.json")
                .bufferedReader()
                .use { it.readText() }
            
            val listType = object : TypeToken<List<FraseMotivacional>>() {}.type
            val frases: List<FraseMotivacional> = gson.fromJson(jsonString, listType)
            
            if (frases.isNotEmpty()) {
                Result.success(frases.random())
            } else {
                Result.failure(Exception("No hay frases disponibles"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    suspend fun obtenerTodasLasFrases(): Result<List<FraseMotivacional>> {
        return try {
            val snapshot = firestore.collection("frasesMotivacionales")
                .get()
                .await()
            
            val frases = snapshot.documents.map { doc ->
                FraseMotivacional.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            }
            Result.success(frases)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun agregarFrase(frase: FraseMotivacional): Result<String> {
        return try {
            val docRef = firestore.collection("frasesMotivacionales").document()
            val fraseConId = frase.copy(id = docRef.id)
            docRef.set(fraseConId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

