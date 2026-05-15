package com.rodrip.precioslocales.comparador.data.sync

import android.content.Context
import android.util.Log
import com.rodrip.precioslocales.comparador.data.local.CityGroupPreferences
import com.rodrip.precioslocales.comparador.data.local.SyncPreferences
import com.rodrip.precioslocales.comparador.data.local.dao.LocalComercialDao
import com.rodrip.precioslocales.comparador.data.local.dao.ProductoDao
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.remote.AnonymousAuthManager
import com.rodrip.precioslocales.comparador.data.remote.RemoteProductDataSource
import com.rodrip.precioslocales.comparador.data.remote.RemoteProductDataSource.PushResult
import com.rodrip.precioslocales.comparador.data.remote.RemoteStoreDataSource
import com.rodrip.precioslocales.comparador.data.remote.RemoteStoreDataSource.StorePushResult
import com.rodrip.precioslocales.comparador.data.util.ImageCompressor

/**
 * Orquestador del ciclo de sincronización manual.
 *
 * FASE 1   – PUSH productos nuevos.
 * FASE 1.5 – PUSH fotos de productos vinculados sin foto remota.
 * FASE 2   – PULL productos nuevos de la nube.
 * FASE 3   – REPARACIÓN de fotos locales faltantes.
 * FASE 4   – PUSH locales comerciales nuevos.
 * FASE 5   – PULL locales por departamentos activos.
 */
class SyncManager(
    private val context: Context,
    private val productoDao: ProductoDao,
    private val remoteDataSource: RemoteProductDataSource,
    private val syncPreferences: SyncPreferences,
    private val storeDao: LocalComercialDao,
    private val remoteStoreDataSource: RemoteStoreDataSource,
    private val cityGroupPreferences: CityGroupPreferences
) {
    private val TAG = "SyncManager"

    data class SyncResult(
        val pushed: Int = 0,
        val pulled: Int = 0,
        val localPhotosRepaired: Int = 0,
        val remotePhotosUploaded: Int = 0,
        val skippedDuplicates: Int = 0,
        val storesPushed: Int = 0,
        val storesPulled: Int = 0,
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
        var storesPushed = 0
        var storesPulled = 0
        val errors = mutableListOf<String>()

        // ── FASE 1: PUSH productos ────────────────────────────────────────────
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
                is PushResult.Error -> errors.add("${product.name}: ${result.message}")
            }
        }

        // ── FASE 1.5: PUSH fotos locales que faltan en la nube ────────────────
        val missingRemotePhotos = productoDao.getProductsMissingRemotePhoto()
        Log.d(TAG, "Productos con foto local pero sin foto en la nube: ${missingRemotePhotos.size}")
        for (product in missingRemotePhotos) {
            val remoteUrl = remoteDataSource.uploadMissingPhoto(product)
            if (remoteUrl != null) {
                productoDao.updateRemotePhotoUrl(product.id, remoteUrl)
                remotePhotosUploaded++
            }
        }

        // ── FASE 2: PULL productos ────────────────────────────────────────────
        val since = syncPreferences.getLastSyncedAtOnce()
        Log.d(TAG, "Buscando novedades en la nube desde: $since")
        val remoteProducts = remoteDataSource.pullProductsSince(since)
        Log.d(TAG, "Productos encontrados en la nube: ${remoteProducts.size}")
        for (dto in remoteProducts) {
            val existing = dto.barcode.takeIf { it.isNotBlank() }
                ?.let { productoDao.getProductByBarcode(it) }
            if (existing == null) {
                val localPhotoUri = dto.photoUrl?.let { ImageCompressor.downloadAndSave(context, it) }
                productoDao.insertProduct(
                    Producto(
                        barcode = dto.barcode,
                        name = dto.name,
                        weightQuantity = dto.weightQuantity,
                        photoUri = localPhotoUri,
                        remoteId = dto.remoteId,
                        remotePhotoUrl = dto.photoUrl
                    )
                )
                pulled++
            } else {
                if (existing.remoteId == null || existing.remotePhotoUrl == null) {
                    productoDao.updateSyncInfo(existing.id, dto.remoteId, dto.photoUrl ?: existing.remotePhotoUrl)
                }
            }
        }

        // ── FASE 3: REPARACIÓN fotos locales faltantes ───────────────────────
        val missingLocalPhotos = productoDao.getProductsMissingLocalPhoto()
        Log.d(TAG, "Reparando productos sin foto local: ${missingLocalPhotos.size}")
        for (product in missingLocalPhotos) {
            val url = product.remotePhotoUrl ?: continue
            val localUri = ImageCompressor.downloadAndSave(context, url)
            if (localUri != null) {
                productoDao.updateProduct(product.copy(photoUri = localUri))
                localPhotosRepaired++
            }
        }

        // ── FASE 4: PUSH locales comerciales nuevos ──────────────────────────
        val unsyncedStores = storeDao.getUnsynced()
        Log.d(TAG, "Locales sin sincronizar: ${unsyncedStores.size}")
        for (store in unsyncedStores) {
            when (val result = remoteStoreDataSource.pushStore(store, uid)) {
                is StorePushResult.Uploaded -> {
                    storeDao.updateSyncInfo(store.id, result.remoteId)
                    storesPushed++
                }
                is StorePushResult.AlreadyExists -> {
                    storeDao.updateSyncInfo(store.id, result.remoteId)
                    skipped++
                }
                is StorePushResult.Error -> errors.add("Local ${store.name}: ${result.message}")
            }
        }

        // ── FASE 5: PULL locales por departamentos activos ───────────────────
        val activeDepts = cityGroupPreferences.getActiveDepartmentsOnce()
        // Si está vacío → sincronizar los 19 departamentos
        val deptsToSync: Set<String> = activeDepts.ifEmpty {
            com.rodrip.precioslocales.comparador.data.model.UruguayGeo.getDepartmentNames().toSet()
        }
        Log.d(TAG, "Sincronizando locales de ${deptsToSync.size} departamento(s)...")
        for (dept in deptsToSync) {
            val remoteStores = remoteStoreDataSource.pullStoresByDepartment(dept, since)
            for (dto in remoteStores) {
                // Buscar si ya existe por remoteId o por nombre+coordenadas similares
                val existing = storeDao.findNearbyByName(
                    dto.name,
                    dto.latitude - 0.001, dto.latitude + 0.001,
                    dto.longitude - 0.001, dto.longitude + 0.001
                ).firstOrNull { it.remoteId == dto.remoteId || it.remoteId == null }

                if (existing == null) {
                    storeDao.insertStore(
                        LocalComercial(
                            name = dto.name,
                            address = dto.address,
                            hours = dto.hours,
                            department = dto.department,
                            localidad = dto.localidad,
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            remoteId = dto.remoteId,
                            uploadedBy = dto.uploadedBy,
                            updatedAt = dto.updatedAt
                        )
                    )
                    storesPulled++
                } else if (existing.remoteId == null) {
                    storeDao.updateSyncInfo(existing.id, dto.remoteId)
                }
            }
        }

        syncPreferences.saveLastSyncedAt(System.currentTimeMillis())
        Log.d(TAG, "Sincronización terminada. Products pushed=$pushed pulled=$pulled | Stores pushed=$storesPushed pulled=$storesPulled")

        return SyncResult(
            pushed = pushed,
            pulled = pulled,
            localPhotosRepaired = localPhotosRepaired,
            remotePhotosUploaded = remotePhotosUploaded,
            skippedDuplicates = skipped,
            storesPushed = storesPushed,
            storesPulled = storesPulled,
            errors = errors
        )
    }
}
