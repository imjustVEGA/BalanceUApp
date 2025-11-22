package com.example.balanceuapp.data.repository

import android.util.Log
import com.example.balanceuapp.data.model.Usuario
import com.example.balanceuapp.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio que maneja todas las operaciones de autenticación con Firebase.
 * Proporciona métodos para registro, inicio de sesión, cierre de sesión y consulta de usuario actual.
 */
class AuthRepository {
    
    private val auth: FirebaseAuth by lazy {
        try {
            val instance = FirebaseAuth.getInstance()
            Log.d(Constants.LogTags.AUTH_REPOSITORY, "FirebaseAuth inicializado correctamente")
            instance
        } catch (e: Exception) {
            Log.e(Constants.LogTags.AUTH_REPOSITORY, "Error al inicializar FirebaseAuth: ${e.message}", e)
            e.printStackTrace()
            // No lanzar excepción, retornar instancia si está disponible
            FirebaseAuth.getInstance()
        }
    }
    
    private val firestore: FirebaseFirestore by lazy {
        try {
            val instance = FirebaseFirestore.getInstance()
            Log.d(Constants.LogTags.AUTH_REPOSITORY, "Firestore inicializado correctamente")
            instance
        } catch (e: Exception) {
            Log.e(Constants.LogTags.AUTH_REPOSITORY, "Error al inicializar Firestore: ${e.message}", e)
            e.printStackTrace()
            // No lanzar excepción, retornar instancia si está disponible
            FirebaseFirestore.getInstance()
        }
    }

    /**
     * Registra un nuevo usuario en Firebase Authentication.
     * 
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @param nombre Nombre del usuario
     * @return Result con el ID del usuario si es exitoso, o un error si falla
     */
    suspend fun registrarUsuario(email: String, password: String, nombre: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid 
                ?: return Result.failure(Exception(Constants.ErrorMessages.ERROR_OBTENER_ID_USUARIO))
            
            // Nota: La creación del documento de usuario en Firestore está comentada
            // Si se necesita, descomentar el siguiente código:
            // val usuario = Usuario(
            //     id = userId,
            //     email = email,
            //     nombre = nombre,
            //     fechaRegistro = System.currentTimeMillis()
            // )
            // firestore.collection(Constants.Firestore.COLLECTION_USUARIOS)
            //     .document(userId)
            //     .set(usuario.toMap())
            //     .await()
            
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inicia sesión con un usuario existente.
     * 
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @return Result con el ID del usuario si es exitoso, o un error si falla
     */
    suspend fun iniciarSesion(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid 
                ?: return Result.failure(Exception(Constants.ErrorMessages.ERROR_OBTENER_ID_USUARIO))
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    fun cerrarSesion() {
        auth.signOut()
    }

    /**
     * Obtiene el ID del usuario actualmente autenticado.
     * 
     * @return ID del usuario si está autenticado, null en caso contrario
     */
    fun obtenerUsuarioActual(): String? {
        return try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            Log.e(Constants.LogTags.AUTH_REPOSITORY, "Error al obtener usuario actual: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene los datos completos de un usuario desde Firestore.
     * 
     * @param userId ID del usuario a consultar
     * @return Result con el objeto Usuario si existe, o un error si no se encuentra
     */
    suspend fun obtenerUsuario(userId: String): Result<Usuario> {
        return try {
            val document = firestore.collection(Constants.Firestore.COLLECTION_USUARIOS)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val usuario = Usuario.fromMap(document.data ?: emptyMap()).copy(id = document.id)
                Result.success(usuario)
            } else {
                Result.failure(Exception(Constants.ErrorMessages.ERROR_USUARIO_NO_ENCONTRADO))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

