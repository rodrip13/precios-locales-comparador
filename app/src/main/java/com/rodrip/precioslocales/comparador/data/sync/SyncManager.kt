package com.rodrip.precioslocales.comparador.data.sync

import android.content.Context
import android.util.Log
import com.rodrip.precioslocales.comparador.data.local.SyncPreferences
import com.rodrip.precioslocales.comparador.data.local.dao.ProductoDao
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.remote.AnonymousAuthManager
import com.rodrip.precioslocales.comparador.data.remote.RemoteProductDataSource
import com.rodrip.precioslocales.comparador.data.remote.RemoteProductDataSource.PushResult
import com.rodrip.precioslocales.comparador.data.util.ImageCompressor

/**
 * Orquestador del ciclo de sincronización manual.
 * 
 * FASE 1 – PUSH: Sube productos locales nuevos.
 * FASE 1.5 – PUSH FOTOS: Sube fotos locales de productos ya vinculados.
 * FASE 2 – PULL: Baja productos nuevos de la nube.
 * FASE 3 – REPARACIÓN: Descarga fotos de la nube para productos que no las tienen localmente.
 */
class SyncManager(
    private val context: Context,
    private val productoDao: ProductoDao,
    private val remoteDataSource: RemoteProductDataSource,
    private val syncPreferences: SyncPreferences
) {
    private val TAG = "SyncManager"

    data class SyncResult(
        val pushed: Int = 0,
        val pulled: Int = 0,
        val localPhotosRepaired: Int = 0,
        val remotePhotosUploaded: Int = 0,
        val skippedDuplicates: Int = 0,
        val errors: List<String> = emptyList()
    )

    suspend fun sync(): SyncResult {
        Log.d(TAG, "Iniciando proceso de sincronización...")
        val uid = try {
            AnonymousAuthManager.ensureSignedIn()
        } catch (e: Exception) {
            Log.e(TAG, "Error de autenticación: ${e.message}")
            return SyncResult(errors = listOf("Sin conexión o error de autenticación: ${e.message}"))
        }

        var pushed = 0
        var remotePhotosUploaded = 0
        var pulled = 0
        var localPhotosRepaired = 0
        var skipped = 0
        val errors = mutableListOf<String>()

        // ── FASE 1: PUSH (Nuevos productos locales a la nube) ─────────────────
        val unsynced = productoDao.getUnsynced()
        Log.d(TAG, "Productos locales sin sincronizar: ${unsynced.size}")
        for (product in unsynced) {
            when (val result = remoteDataSource.pushProduct(product, uid)) {
                is PushResult.Uploaded -> {
                    productoDao.updateSyncInfo(product.id, result.remoteId, result.remotePhotoUrl)
                    pushed++
                }
                is PushResult.AlreadyExists -> {
                    productoDao.updateSyncInfo(product.id, result.remoteId, result.remotePhotoUrl)
                    skipped++
                }
                is PushResult.Error -> {
                    errors.add("${product.name}: ${result.message}")
                }
            }
        }

        // ── FASE 1.5: PUSH (Subir fotos locales que faltan en la nube) ────────
        val missingRemotePhotos = productoDao.getProductsMissingRemotePhoto()
        Log.d(TAG, "Productos con foto local pero sin foto en la nube: ${missingRemotePhotos.size}")
        for (product in missingRemotePhotos) {
            val remoteUrl = remoteDataSource.uploadMissingPhoto(product)
            if (remoteUrl != null) {
                productoDao.updateRemotePhotoUrl(product.id, remoteUrl)
                remotePhotosUploaded++
            }
        }

        // ── FASE 2: PULL (Bajar novedades de la nube) ────────────────────────
        val since = syncPreferences.getLastSyncedAtOnce()
        Log.d(TAG, "Buscando novedades en la nube desde: $since")
        val remoteProducts = remoteDataSource.pullProductsSince(since)
        Log.d(TAG, "Productos encontrados en la nube: ${remoteProducts.size}")

        for (dto in remoteProducts) {
            val existing = dto.barcode.takeIf { it.isNotBlank() }
                ?.let { productoDao.getProductByBarcode(it) }

            if (existing == null) {
                Log.d(TAG, "Descargando nuevo producto: ${dto.name}")
                val localPhotoUri = dto.photoUrl?.let { ImageCompressor.downloadAndSave(context, it) }
                
                val local = Producto(
                    barcode = dto.barcode,
                    name = dto.name,
                    weightQuantity = dto.weightQuantity,
                    photoUri = localPhotoUri,
                    remoteId = dto.remoteId,
                    remotePhotoUrl = dto.photoUrl
                )
                productoDao.insertProduct(local)
                pulled++
            } else {
                // Actualizar info remota si es necesario
                if (existing.remoteId == null || existing.remotePhotoUrl == null) {
                    productoDao.updateSyncInfo(
                        existing.id, 
                        dto.remoteId, 
                        dto.photoUrl ?: existing.remotePhotoUrl
                    )
                }
            }
        }

        // ── FASE 3: REPARACIÓN (Descargar fotos locales faltantes) ────────────
        // Esto arregla los productos que ya estaban pero no tenían la foto descargada
        val missingLocalPhotos = productoDao.getProductsMissingLocalPhoto()
        Log.d(TAG, "Reparando productos sin foto local: ${missingLocalPhotos.size}")
        for (product in missingLocalPhotos) {
            val url = product.remotePhotoUrl ?: continue
            Log.d(TAG, "Descargando foto para reparar: ${product.name}")
            val localUri = ImageCompressor.downloadAndSave(context, url)
            if (localUri != null) {
                productoDao.updateProduct(product.copy(photoUri = localUri))
                localPhotosRepaired++
            }
        }

        // Guardar timestamp de esta sincronización
        syncPreferences.saveLastSyncedAt(System.currentTimeMillis())
        Log.d(TAG, "Sincronización terminada. Pushed: $pushed, Pulled: $pulled, Repaired: $localPhotosRepaired")

        return SyncResult(
            pushed = pushed,
            pulled = pulled,
            localPhotosRepaired = localPhotosRepaired,
            remotePhotosUploaded = remotePhotosUploaded,
            skippedDuplicates = skipped,
            errors = errors
        )
    }
}
