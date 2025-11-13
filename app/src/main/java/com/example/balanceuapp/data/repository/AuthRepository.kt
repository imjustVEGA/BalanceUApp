package com.example.balanceuapp.data.repository

import com.example.balanceuapp.data.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth by lazy {
        try {
            val instance = FirebaseAuth.getInstance()
            android.util.Log.d("AuthRepository", "FirebaseAuth inicializado correctamente")
            instance
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error al inicializar FirebaseAuth: ${e.message}", e)
            throw IllegalStateException("Firebase no está configurado correctamente. Verifica que google-services.json sea válido.", e)
        }
    }
    private val firestore: FirebaseFirestore by lazy {
        try {
            val instance = FirebaseFirestore.getInstance()
            android.util.Log.d("AuthRepository", "Firestore inicializado correctamente")
            instance
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error al inicializar Firestore: ${e.message}", e)
            throw IllegalStateException("Firebase no está configurado correctamente. Verifica que google-services.json sea válido.", e)
        }
    }

    suspend fun registrarUsuario(email: String, password: String, nombre: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("Error al obtener ID de usuario"))
            
            val usuario = Usuario(
                id = userId,
                email = email,
                nombre = nombre,
                fechaRegistro = System.currentTimeMillis()
            )
            
            firestore.collection("usuarios")
                .document(userId)
                .set(usuario.toMap())
                .await()
            
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun iniciarSesion(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("Error al obtener ID de usuario"))
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }

    fun obtenerUsuarioActual(): String? {
        return try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error al obtener usuario actual: ${e.message}", e)
            null
        }
    }

    suspend fun obtenerUsuario(userId: String): Result<Usuario> {
        return try {
            val document = firestore.collection("usuarios")
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val usuario = Usuario.fromMap(document.data ?: emptyMap()).copy(id = document.id)
                Result.success(usuario)
            } else {
                Result.failure(Exception("Usuario no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

