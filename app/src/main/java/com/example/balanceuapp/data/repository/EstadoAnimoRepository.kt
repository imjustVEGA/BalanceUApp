package com.example.balanceuapp.data.repository

import android.util.Log
import com.example.balanceuapp.data.model.EstadoAnimo
import com.example.balanceuapp.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repositorio que maneja todas las operaciones relacionadas con estados de ánimo en Firestore.
 * Proporciona métodos para CRUD de estados de ánimo y observación en tiempo real.
 */
class EstadoAnimoRepository {
    
    private val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(Constants.LogTags.ESTADO_ANIMO_REPOSITORY, "Error al inicializar Firestore: ${e.message}", e)
            e.printStackTrace()
            FirebaseFirestore.getInstance()
        }
    }

    /**
     * Agrega un nuevo estado de ánimo a Firestore.
     * 
     * @param estadoAnimo Objeto EstadoAnimo a guardar
     * @return Result con el ID del estado de ánimo creado si es exitoso, o un error si falla
     */
    suspend fun agregarEstadoAnimo(estadoAnimo: EstadoAnimo): Result<String> {
        return try {
            val docRef = firestore.collection(Constants.Firestore.COLLECTION_ESTADOS_ANIMO).document()
            val estadoConId = estadoAnimo.copy(id = docRef.id)
            docRef.set(estadoConId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todos los estados de ánimo de un usuario, ordenados por fecha descendente.
     * 
     * @param usuarioId ID del usuario
     * @return Result con la lista de estados de ánimo si es exitoso, o un error si falla
     */
    suspend fun obtenerEstadosAnimo(usuarioId: String): Result<List<EstadoAnimo>> {
        return try {
            val snapshot = firestore.collection(Constants.Firestore.COLLECTION_ESTADOS_ANIMO)
                .whereEqualTo(Constants.FirestoreFields.FIELD_USUARIO_ID, usuarioId)
                .orderBy(Constants.FirestoreFields.FIELD_FECHA, Query.Direction.DESCENDING)
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

    /**
     * Obtiene el estado de ánimo de un usuario para un día específico.
     * 
     * @param usuarioId ID del usuario
     * @param fechaInicio Timestamp de inicio del día (en milisegundos)
     * @param fechaFin Timestamp de fin del día (en milisegundos)
     * @return Result con el estado de ánimo del día si existe, null si no hay registro, o un error si falla
     */
    suspend fun obtenerEstadoAnimoDelDia(usuarioId: String, fechaInicio: Long, fechaFin: Long): Result<EstadoAnimo?> {
        return try {
            val snapshot = firestore.collection(Constants.Firestore.COLLECTION_ESTADOS_ANIMO)
                .whereEqualTo(Constants.FirestoreFields.FIELD_USUARIO_ID, usuarioId)
                .whereGreaterThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA, fechaInicio)
                .whereLessThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA, fechaFin)
                .orderBy(Constants.FirestoreFields.FIELD_FECHA, Query.Direction.DESCENDING)
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

    /**
     * Obtiene los estados de ánimo de un usuario en un rango de fechas.
     * 
     * @param usuarioId ID del usuario
     * @param fechaInicio Timestamp de inicio del rango (en milisegundos)
     * @param fechaFin Timestamp de fin del rango (en milisegundos)
     * @return Result con la lista de estados de ánimo si es exitoso, o un error si falla
     */
    suspend fun obtenerEstadosAnimoPorRango(usuarioId: String, fechaInicio: Long, fechaFin: Long): Result<List<EstadoAnimo>> {
        return try {
            val snapshot = firestore.collection(Constants.Firestore.COLLECTION_ESTADOS_ANIMO)
                .whereEqualTo(Constants.FirestoreFields.FIELD_USUARIO_ID, usuarioId)
                .whereGreaterThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA, fechaInicio)
                .whereLessThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA, fechaFin)
                .orderBy(Constants.FirestoreFields.FIELD_FECHA, Query.Direction.ASCENDING)
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

    /**
     * Observa cambios en tiempo real de los estados de ánimo de un usuario en un rango de fechas.
     * 
     * @param usuarioId ID del usuario
     * @param fechaInicio Timestamp de inicio del rango (en milisegundos)
     * @param fechaFin Timestamp de fin del rango (en milisegundos)
     * @param onUpdate Callback que se ejecuta cuando hay actualizaciones
     * @param onError Callback que se ejecuta cuando hay un error
     * @return ListenerRegistration que puede usarse para detener la observación
     */
    fun observarEstadosAnimoPorRango(
        usuarioId: String,
        fechaInicio: Long,
        fechaFin: Long,
        onUpdate: (List<EstadoAnimo>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val query = firestore.collection(Constants.Firestore.COLLECTION_ESTADOS_ANIMO)
            .whereEqualTo(Constants.FirestoreFields.FIELD_USUARIO_ID, usuarioId)
            .whereGreaterThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA, fechaInicio)
            .whereLessThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA, fechaFin)
            .orderBy(Constants.FirestoreFields.FIELD_FECHA, Query.Direction.ASCENDING)

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

    /**
     * Actualiza un estado de ánimo existente en Firestore.
     * 
     * @param estadoAnimo Objeto EstadoAnimo con los datos actualizados
     * @return Result exitoso o con error si falla
     */
    suspend fun actualizarEstadoAnimo(estadoAnimo: EstadoAnimo): Result<Unit> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_ESTADOS_ANIMO)
                .document(estadoAnimo.id)
                .update(estadoAnimo.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un estado de ánimo de Firestore.
     * 
     * @param estadoAnimoId ID del estado de ánimo a eliminar
     * @return Result exitoso o con error si falla
     */
    suspend fun eliminarEstadoAnimo(estadoAnimoId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_ESTADOS_ANIMO)
                .document(estadoAnimoId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

