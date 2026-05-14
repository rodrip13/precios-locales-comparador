package com.rodrip.precioslocales.comparador.data.remote

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import kotlinx.coroutines.tasks.await

/**
 * Acceso a la colección "stores" en Firestore.
 *
 * Estrategia anti-duplicados: antes de hacer push se consulta un bounding-box
 * de ±0.001° (~100 m) alrededor de las coordenadas del local. Si ya existe un
 * documento con el mismo nombre se retorna [StorePushResult.AlreadyExists].
 *
 * NOTA: para el pull por departamento se requiere el índice compuesto
 * (department ASC, updatedAt ASC) en Firestore.
 */
class RemoteStoreDataSource {

    private val TAG = "RemoteStoreDataSource"
    private val db = Firebase.firestore
    private val collection = db.collection("stores")

    // Bounding box ~100 m
    private val GEO_DELTA = 0.001

    // ─── Resultado de un push ─────────────────────────────────────────────────

    sealed class StorePushResult {
        data class Uploaded(val remoteId: String) : StorePushResult()
        data class AlreadyExists(val remoteId: String) : StorePushResult()
        data class Error(val message: String) : StorePushResult()
    }

    // ─── Verificación de duplicado remoto ─────────────────────────────────────

    /**
     * Consulta Firestore por el bounding-box y nombre.
     * Devuelve el primer documento que coincide, o null si no hay duplicado.
     * Si no hay conexión devuelve null (se degrada silenciosamente).
     */
    suspend fun checkAndFetchDuplicate(
        name: String,
        lat: Double,
        lon: Double
    ): RemoteStoreDto? {
        if (lat == 0.0 && lon == 0.0) return null
        return try {
            val snapshot = collection
                .whereGreaterThan("latitude", lat - GEO_DELTA)
                .whereLessThan("latitude", lat + GEO_DELTA)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.toDto() }
                .firstOrNull { dto ->
                    kotlin.math.abs(dto.longitude - lon) <= GEO_DELTA &&
                            dto.name.trim().equals(name.trim(), ignoreCase = true)
                }
        } catch (e: Exception) {
            Log.w(TAG, "checkAndFetchDuplicate falló (sin conexión?): ${e.message}")
            null
        }
    }

    // ─── PUSH ─────────────────────────────────────────────────────────────────

    /**
     * Sube un local a Firestore.
     * - Primero verifica duplicado remoto.
     * - Si ya existe devuelve [StorePushResult.AlreadyExists] con su remoteId.
     * - Si no existe, crea el documento y devuelve [StorePushResult.Uploaded].
     */
    suspend fun pushStore(store: LocalComercial, uploaderUid: String): StorePushResult {
        return try {
            val existing = checkAndFetchDuplicate(store.name, store.latitude, store.longitude)
            if (existing != null) {
                Log.d(TAG, "Local ya existe en Firestore: ${existing.remoteId}")
                return StorePushResult.AlreadyExists(existing.remoteId)
            }

            val data = hashMapOf(
                "name" to store.name,
                "address" to store.address,
                "hours" to store.hours,
                "department" to store.department,
                "localidad" to store.localidad,
                "latitude" to store.latitude,
                "longitude" to store.longitude,
                "updatedAt" to System.currentTimeMillis(),
                "uploadedBy" to uploaderUid
            )
            val docRef = collection.add(data).await()
            Log.d(TAG, "Local subido a Firestore: ${docRef.id}")
            StorePushResult.Uploaded(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "pushStore error: ${e.message}")
            StorePushResult.Error(e.message ?: "Error desconocido")
        }
    }

    // ─── PULL ─────────────────────────────────────────────────────────────────

    /**
     * Trae locales de un departamento actualizados después de [since].
     * Requiere índice compuesto Firestore: department ASC + updatedAt ASC.
     */
    suspend fun pullStoresByDepartment(dept: String, since: Long): List<RemoteStoreDto> {
        return try {
            val snapshot = collection
                .whereEqualTo("department", dept)
                .whereGreaterThan("updatedAt", since)
                .orderBy("updatedAt")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toDto() }
        } catch (e: Exception) {
            Log.w(TAG, "pullStoresByDepartment($dept) error: ${e.message}")
            emptyList()
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun DocumentSnapshot.toDto(): RemoteStoreDto? = try {
        RemoteStoreDto(
            remoteId = id,
            name = getString("name") ?: "",
            address = getString("address") ?: "",
            hours = getString("hours") ?: "",
            department = getString("department") ?: "",
            localidad = getString("localidad") ?: "",
            latitude = getDouble("latitude") ?: 0.0,
            longitude = getDouble("longitude") ?: 0.0,
            updatedAt = getLong("updatedAt") ?: 0L,
            uploadedBy = getString("uploadedBy") ?: ""
        )
    } catch (e: Exception) {
        null
    }
}

