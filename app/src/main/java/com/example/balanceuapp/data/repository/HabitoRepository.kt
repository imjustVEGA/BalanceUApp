package com.example.balanceuapp.data.repository

import android.util.Log
import com.example.balanceuapp.data.model.Habito
import com.example.balanceuapp.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repositorio que maneja todas las operaciones relacionadas con hábitos en Firestore.
 * Proporciona métodos para CRUD de hábitos y observación en tiempo real.
 */
class HabitoRepository {
    
    private val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(Constants.LogTags.HABITO_REPOSITORY, "Error al inicializar Firestore: ${e.message}", e)
            e.printStackTrace()
            FirebaseFirestore.getInstance()
        }
    }

    /**
     * Agrega un nuevo hábito a Firestore.
     * 
     * @param habito Objeto Habito a guardar
     * @return Result con el ID del hábito creado si es exitoso, o un error si falla
     */
    suspend fun agregarHabito(habito: Habito): Result<String> {
        return try {
            val docRef = firestore.collection(Constants.Firestore.COLLECTION_HABITOS).document()
            val habitoConId = habito.copy(id = docRef.id)
            docRef.set(habitoConId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todos los hábitos de un usuario, ordenados por fecha de creación descendente.
     * 
     * @param usuarioId ID del usuario
     * @return Result con la lista de hábitos si es exitoso, o un error si falla
     */
    suspend fun obtenerHabitos(usuarioId: String): Result<List<Habito>> {
        return try {
            val snapshot = firestore.collection(Constants.Firestore.COLLECTION_HABITOS)
                .whereEqualTo(Constants.FirestoreFields.FIELD_USUARIO_ID, usuarioId)
                .orderBy(Constants.FirestoreFields.FIELD_FECHA_CREACION, Query.Direction.DESCENDING)
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

    /**
     * Actualiza un hábito existente en Firestore.
     * 
     * @param habito Objeto Habito con los datos actualizados
     * @return Result exitoso o con error si falla
     */
    suspend fun actualizarHabito(habito: Habito): Result<Unit> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_HABITOS)
                .document(habito.id)
                .update(habito.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marca un hábito como completado o no completado.
     * 
     * @param habitoId ID del hábito a actualizar
     * @param completado true si se marca como completado, false en caso contrario
     * @return Result exitoso o con error si falla
     */
    suspend fun marcarCompletado(habitoId: String, completado: Boolean): Result<Unit> {
        return try {
            val updates = mapOf(
                Constants.FirestoreFields.FIELD_COMPLETADO to completado,
                "fechaCompletado" to if (completado) System.currentTimeMillis() else null
            )
            firestore.collection(Constants.Firestore.COLLECTION_HABITOS)
                .document(habitoId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un hábito de Firestore.
     * 
     * @param habitoId ID del hábito a eliminar
     * @return Result exitoso o con error si falla
     */
    suspend fun eliminarHabito(habitoId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_HABITOS)
                .document(habitoId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los hábitos de un usuario creados en un rango de fechas específico.
     * 
     * @param usuarioId ID del usuario
     * @param fechaInicio Timestamp de inicio del rango (en milisegundos)
     * @param fechaFin Timestamp de fin del rango (en milisegundos)
     * @return Result con la lista de hábitos si es exitoso, o un error si falla
     */
    suspend fun obtenerHabitosDelDia(usuarioId: String, fechaInicio: Long, fechaFin: Long): Result<List<Habito>> {
        return try {
            val snapshot = firestore.collection(Constants.Firestore.COLLECTION_HABITOS)
                .whereEqualTo(Constants.FirestoreFields.FIELD_USUARIO_ID, usuarioId)
                .whereGreaterThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA_CREACION, fechaInicio)
                .whereLessThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA_CREACION, fechaFin)
                .orderBy(Constants.FirestoreFields.FIELD_FECHA_CREACION, Query.Direction.DESCENDING)
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

    /**
     * Observa cambios en tiempo real de todos los hábitos de un usuario.
     * 
     * @param usuarioId ID del usuario
     * @param onUpdate Callback que se ejecuta cuando hay actualizaciones
     * @param onError Callback que se ejecuta cuando hay un error
     * @return ListenerRegistration que puede usarse para detener la observación
     */
    fun observarHabitos(
        usuarioId: String,
        onUpdate: (List<Habito>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val query = firestore.collection(Constants.Firestore.COLLECTION_HABITOS)
            .whereEqualTo(Constants.FirestoreFields.FIELD_USUARIO_ID, usuarioId)
            .orderBy(Constants.FirestoreFields.FIELD_FECHA_CREACION, Query.Direction.DESCENDING)

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

    /**
     * Observa cambios en tiempo real de los hábitos de un usuario en un rango de fechas.
     * 
     * @param usuarioId ID del usuario
     * @param fechaInicio Timestamp de inicio del rango (en milisegundos)
     * @param fechaFin Timestamp de fin del rango (en milisegundos)
     * @param onUpdate Callback que se ejecuta cuando hay actualizaciones
     * @param onError Callback que se ejecuta cuando hay un error
     * @return ListenerRegistration que puede usarse para detener la observación
     */
    fun observarHabitosDelDia(
        usuarioId: String,
        fechaInicio: Long,
        fechaFin: Long,
        onUpdate: (List<Habito>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val query = firestore.collection(Constants.Firestore.COLLECTION_HABITOS)
            .whereEqualTo(Constants.FirestoreFields.FIELD_USUARIO_ID, usuarioId)
            .whereGreaterThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA_CREACION, fechaInicio)
            .whereLessThanOrEqualTo(Constants.FirestoreFields.FIELD_FECHA_CREACION, fechaFin)
            .orderBy(Constants.FirestoreFields.FIELD_FECHA_CREACION, Query.Direction.DESCENDING)

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

