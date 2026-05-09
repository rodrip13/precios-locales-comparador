package com.rodrip.precioslocales.comparador.data.remote

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Gestiona la autenticación anónima de Firebase.
 * Cada instalación recibe un UID único y permanente sin requerir login del usuario.
 * Este UID se utiliza como "contributorId" para rastrear qué instalación subió cada producto.
 */
object AnonymousAuthManager {

    /**
     * Garantiza que la app tenga una sesión anónima activa.
     * Si ya existe una sesión, retorna el UID existente.
     * Si no, crea una nueva sesión anónima.
     */
    suspend fun ensureSignedIn(): String {
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            currentUser.uid
        } else {
            val result = auth.signInAnonymously().await()
            result.user?.uid ?: throw IllegalStateException("No se pudo autenticar de forma anónima")
        }
    }

    fun getCurrentUid(): String? = Firebase.auth.currentUser?.uid
}

