package com.example.balanceuapp.data.repository

import android.content.Context
import android.util.Log
import com.example.balanceuapp.data.model.FraseMotivacional
import com.example.balanceuapp.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Repositorio que maneja las frases motivacionales.
 * Intenta cargar desde Firebase primero, y si falla, carga desde un archivo JSON local.
 */
class FraseRepository(private val context: Context) {
    
    private val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(Constants.LogTags.FRASE_REPOSITORY, "Error al inicializar Firestore: ${e.message}", e)
            e.printStackTrace()
            FirebaseFirestore.getInstance()
        }
    }
    
    private val gson = Gson()

    /**
     * Obtiene una frase motivacional aleatoria.
     * Intenta cargar desde Firebase primero, y si falla o no hay datos, carga desde JSON local.
     * 
     * @return Result con una frase aleatoria si es exitoso, o un error si falla
     */
    suspend fun obtenerFraseAleatoria(): Result<FraseMotivacional> {
        return try {
            // Primero intentar obtener desde Firebase
            val snapshot = firestore.collection(Constants.Firestore.COLLECTION_FRASES_MOTIVACIONALES)
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

    /**
     * Carga frases motivacionales desde un archivo JSON local en assets.
     * 
     * @return Result con una frase aleatoria si es exitoso, o un error si falla
     */
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
                Result.failure(Exception(Constants.ErrorMessages.ERROR_NO_HAY_FRASES))
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todas las frases motivacionales desde Firebase.
     * 
     * @return Result con la lista de frases si es exitoso, o un error si falla
     */
    suspend fun obtenerTodasLasFrases(): Result<List<FraseMotivacional>> {
        return try {
            val snapshot = firestore.collection(Constants.Firestore.COLLECTION_FRASES_MOTIVACIONALES)
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

    /**
     * Agrega una nueva frase motivacional a Firestore.
     * 
     * @param frase Objeto FraseMotivacional a guardar
     * @return Result con el ID de la frase creada si es exitoso, o un error si falla
     */
    suspend fun agregarFrase(frase: FraseMotivacional): Result<String> {
        return try {
            val docRef = firestore.collection(Constants.Firestore.COLLECTION_FRASES_MOTIVACIONALES).document()
            val fraseConId = frase.copy(id = docRef.id)
            docRef.set(fraseConId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

