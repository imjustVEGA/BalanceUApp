package com.example.balanceuapp.data.repository

import com.example.balanceuapp.data.model.Habito
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class HabitoRepository {
    private val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            android.util.Log.e("HabitoRepository", "Error al inicializar Firestore: ${e.message}", e)
            e.printStackTrace()
            FirebaseFirestore.getInstance()
        }
    }

    suspend fun agregarHabito(habito: Habito): Result<String> {
        return try {
            val docRef = firestore.collection("habitos").document()
            val habitoConId = habito.copy(id = docRef.id)
            docRef.set(habitoConId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerHabitos(usuarioId: String): Result<List<Habito>> {
        return try {
            val snapshot = firestore.collection("habitos")
                .whereEqualTo("usuarioId", usuarioId)
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val habitos = snapshot.documents.map { doc ->
                Habito.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            }
            Result.success(habitos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarHabito(habito: Habito): Result<Unit> {
        return try {
            firestore.collection("habitos")
                .document(habito.id)
                .update(habito.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun marcarCompletado(habitoId: String, completado: Boolean): Result<Unit> {
        return try {
            val updates = mapOf(
                "completado" to completado,
                "fechaCompletado" to if (completado) System.currentTimeMillis() else null
            )
            firestore.collection("habitos")
                .document(habitoId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarHabito(habitoId: String): Result<Unit> {
        return try {
            firestore.collection("habitos")
                .document(habitoId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerHabitosDelDia(usuarioId: String, fechaInicio: Long, fechaFin: Long): Result<List<Habito>> {
        return try {
            val snapshot = firestore.collection("habitos")
                .whereEqualTo("usuarioId", usuarioId)
                .whereGreaterThanOrEqualTo("fechaCreacion", fechaInicio)
                .whereLessThanOrEqualTo("fechaCreacion", fechaFin)
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val habitos = snapshot.documents.map { doc ->
                Habito.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            }
            Result.success(habitos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarHabitos(
        usuarioId: String,
        onUpdate: (List<Habito>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val query = firestore.collection("habitos")
            .whereEqualTo("usuarioId", usuarioId)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)

        return query.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onError(exception)
                return@addSnapshotListener
            }
            val habitos = snapshot?.documents?.map { doc ->
                Habito.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            } ?: emptyList()
            onUpdate(habitos)
        }
    }

    fun observarHabitosDelDia(
        usuarioId: String,
        fechaInicio: Long,
        fechaFin: Long,
        onUpdate: (List<Habito>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val query = firestore.collection("habitos")
            .whereEqualTo("usuarioId", usuarioId)
            .whereGreaterThanOrEqualTo("fechaCreacion", fechaInicio)
            .whereLessThanOrEqualTo("fechaCreacion", fechaFin)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)

        return query.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onError(exception)
                return@addSnapshotListener
            }
            val habitos = snapshot?.documents?.map { doc ->
                Habito.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            } ?: emptyList()
            onUpdate(habitos)
        }
    }
}

