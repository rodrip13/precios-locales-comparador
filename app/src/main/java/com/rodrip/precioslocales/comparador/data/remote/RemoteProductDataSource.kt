package com.rodrip.precioslocales.comparador.data.remote

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Acceso a datos remotos: Firestore para el catálogo, Cloudinary para las imágenes.
 * Principio First-Local: solo se consulta cuando es necesario.
 */
class RemoteProductDataSource(private val context: Context) {

    private val db = Firebase.firestore
    private val collection = db.collection("products")

    // ─── Búsqueda puntual por barcode (1 lectura Firestore) ───────────────────

    suspend fun fetchByBarcode(barcode: String): RemoteProductDto? {
        return try {
            val snapshot = collection
                .whereEqualTo("barcode", barcode)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toDto()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── PUSH: sube producto nuevo con deduplicación ──────────────────────────

    sealed class PushResult {
        data class Uploaded(val remoteId: String, val remotePhotoUrl: String?) : PushResult()
        data class AlreadyExists(val remoteId: String, val remotePhotoUrl: String?) : PushResult()
        data class Error(val message: String) : PushResult()
    }

    suspend fun pushProduct(product: Producto, uploaderUid: String): PushResult {
        if (product.barcode.isNullOrBlank()) return PushResult.Error("Sin código de barras")
        
        // Requisito: obligatorio tener imagen local para subir a la nube
        if (product.photoUri == null) {
            return PushResult.Error("La imagen es obligatoria para compartir el producto")
        }

        return try {
            // 1. Verificar duplicado en Firestore
            val existing = collection
                .whereEqualTo("barcode", product.barcode)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) {
                val doc = existing.documents.first()
                val dto = doc.toDto()
                
                // Si ya existe pero no tiene foto en la nube, y nosotros sí tenemos localmente,
                // aprovechamos para subirla.
                if (dto.photoUrl == null) {
                    val newPhotoUrl = uploadLocalPhoto(product)
                    if (newPhotoUrl != null) {
                        doc.reference.update("photoUrl", newPhotoUrl).await()
                        return PushResult.Uploaded(doc.id, newPhotoUrl)
                    }
                }
                
                return PushResult.AlreadyExists(dto.remoteId, dto.photoUrl)
            }

            // 2. Subir imagen a Cloudinary
            val remotePhotoUrl = uploadLocalPhoto(product) ?: return PushResult.Error("Error al subir la imagen")

            // 3. Escribir documento en Firestore
            val data = hashMapOf(
                "barcode" to product.barcode,
                "name" to product.name,
                "weightQuantity" to product.weightQuantity,
                "photoUrl" to remotePhotoUrl,
                "updatedAt" to System.currentTimeMillis(),
                "uploadedBy" to uploaderUid
            )
            val docRef = collection.add(data).await()
            PushResult.Uploaded(docRef.id, remotePhotoUrl)

        } catch (e: Exception) {
            e.printStackTrace()
            PushResult.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Sube solo la foto de un producto que ya tiene remoteId pero le falta la remotePhotoUrl.
     */
    suspend fun uploadMissingPhoto(product: Producto): String? {
        val remoteId = product.remoteId ?: return null
        val localUri = product.photoUri ?: return null
        
        return try {
            val remotePhotoUrl = uploadLocalPhoto(product)
            if (remotePhotoUrl != null) {
                collection.document(remoteId).update("photoUrl", remotePhotoUrl).await()
                remotePhotoUrl
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun uploadLocalPhoto(product: Producto): String? = withContext(Dispatchers.IO) {
        val uriStr = product.photoUri ?: return@withContext null
        val compressed = ImageCompressor.compress(context, Uri.parse(uriStr))
        compressed?.let {
            val url = CloudinaryUploader.upload(it, folder = "products/${product.barcode}")
            it.delete() // limpiar archivo temporal
            url
        }
    }

    // ─── PULL: trae novedades desde la última sync ────────────────────────────

    suspend fun pullProductsSince(since: Long): List<RemoteProductDto> {
        return try {
            val snapshot = collection
                .whereGreaterThan("updatedAt", since)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toDto() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun com.google.firebase.firestore.DocumentSnapshot.toDto() = RemoteProductDto(
        remoteId = id,
        barcode = getString("barcode") ?: "",
        name = getString("name") ?: "",
        weightQuantity = getString("weightQuantity") ?: "",
        photoUrl = getString("photoUrl"),
        updatedAt = getLong("updatedAt") ?: 0L,
        uploadedBy = getString("uploadedBy") ?: ""
    )
}
