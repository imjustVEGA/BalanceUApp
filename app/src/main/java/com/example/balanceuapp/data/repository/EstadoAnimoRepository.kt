package com.example.balanceuapp.data.repository

import com.example.balanceuapp.data.model.EstadoAnimo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class EstadoAnimoRepository {
    private val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            android.util.Log.e("EstadoAnimoRepository", "Error al inicializar Firestore: ${e.message}", e)
            e.printStackTrace()
            FirebaseFirestore.getInstance()
        }
    }

    suspend fun agregarEstadoAnimo(estadoAnimo: EstadoAnimo): Result<String> {
        return try {
            val docRef = firestore.collection("estadosAnimo").document()
            val estadoConId = estadoAnimo.copy(id = docRef.id)
            docRef.set(estadoConId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerEstadosAnimo(usuarioId: String): Result<List<EstadoAnimo>> {
        return try {
            val snapshot = firestore.collection("estadosAnimo")
                .whereEqualTo("usuarioId", usuarioId)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val estados = snapshot.documents.map { doc ->
                EstadoAnimo.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            }
            Result.success(estados)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerEstadoAnimoDelDia(usuarioId: String, fechaInicio: Long, fechaFin: Long): Result<EstadoAnimo?> {
        return try {
            val snapshot = firestore.collection("estadosAnimo")
                .whereEqualTo("usuarioId", usuarioId)
                .whereGreaterThanOrEqualTo("fecha", fechaInicio)
                .whereLessThanOrEqualTo("fecha", fechaFin)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            val estado = if (snapshot.documents.isNotEmpty()) {
                val doc = snapshot.documents[0]
                EstadoAnimo.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            } else {
                null
            }
            Result.success(estado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerEstadosAnimoPorRango(usuarioId: String, fechaInicio: Long, fechaFin: Long): Result<List<EstadoAnimo>> {
        return try {
            val snapshot = firestore.collection("estadosAnimo")
                .whereEqualTo("usuarioId", usuarioId)
                .whereGreaterThanOrEqualTo("fecha", fechaInicio)
                .whereLessThanOrEqualTo("fecha", fechaFin)
                .orderBy("fecha", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val estados = snapshot.documents.map { doc ->
                EstadoAnimo.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            }
            Result.success(estados)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarEstadosAnimoPorRango(
        usuarioId: String,
        fechaInicio: Long,
        fechaFin: Long,
        onUpdate: (List<EstadoAnimo>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val query = firestore.collection("estadosAnimo")
            .whereEqualTo("usuarioId", usuarioId)
            .whereGreaterThanOrEqualTo("fecha", fechaInicio)
            .whereLessThanOrEqualTo("fecha", fechaFin)
            .orderBy("fecha", Query.Direction.ASCENDING)

        return query.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onError(exception)
                return@addSnapshotListener
            }

            val estados = snapshot?.documents?.map { doc ->
                EstadoAnimo.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            } ?: emptyList()

            onUpdate(estados)
        }
    }

    suspend fun actualizarEstadoAnimo(estadoAnimo: EstadoAnimo): Result<Unit> {
        return try {
            firestore.collection("estadosAnimo")
                .document(estadoAnimo.id)
                .update(estadoAnimo.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarEstadoAnimo(estadoAnimoId: String): Result<Unit> {
        return try {
            firestore.collection("estadosAnimo")
                .document(estadoAnimoId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

