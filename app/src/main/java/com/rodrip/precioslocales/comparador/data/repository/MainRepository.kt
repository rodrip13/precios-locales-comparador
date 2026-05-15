package com.rodrip.precioslocales.comparador.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.rodrip.precioslocales.comparador.data.local.CityGroupPreferences
import com.rodrip.precioslocales.comparador.data.local.dao.LocalComercialDao
import com.rodrip.precioslocales.comparador.data.local.dao.ProductoDao
import com.rodrip.precioslocales.comparador.data.local.dao.RegistroPrecioDao
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.entity.RegistroPrecio
import com.rodrip.precioslocales.comparador.data.local.model.ProductWithPrice
import com.rodrip.precioslocales.comparador.data.local.model.StoreWithPrice
import com.rodrip.precioslocales.comparador.data.remote.RemoteProductDataSource
import com.rodrip.precioslocales.comparador.data.remote.RemoteStoreDataSource
import com.rodrip.precioslocales.comparador.data.remote.RemoteStoreDto
import com.rodrip.precioslocales.comparador.data.util.ImageCompressor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/** Resultado de buscar un producto por barcode — indica el origen del dato. */
sealed class ProductLookupResult {
    data class Local(val product: Producto) : ProductLookupResult()
    data class Remote(val product: Producto) : ProductLookupResult()
    object NotFound : ProductLookupResult()
}

class MainRepository(
    private val context: Context,
    private val storeDao: LocalComercialDao,
    private val productDao: ProductoDao,
    private val priceDao: RegistroPrecioDao,
    private val remoteDataSource: RemoteProductDataSource? = null,
    private val remoteStoreDataSource: RemoteStoreDataSource? = null,
    val cityGroupPreferences: CityGroupPreferences? = null
) {
    private val TAG = "MainRepository"

    // ── Store operations ──────────────────────────────────────────────────────

    fun getAllStores(): Flow<List<LocalComercial>> = storeDao.getAllStores()
    suspend fun getStoreById(id: Long) = storeDao.getStoreById(id)
    fun searchStores(query: String) = storeDao.searchStores(query)
    fun getStoresByDepartments(depts: List<String>): Flow<List<LocalComercial>> =
        if (depts.isEmpty()) storeDao.getAllStores() else storeDao.getStoresByDepartments(depts)
    suspend fun insertStore(store: LocalComercial) = storeDao.insertStore(store)
    suspend fun updateStore(store: LocalComercial) = storeDao.updateStore(store)
    suspend fun deleteStore(store: LocalComercial) = storeDao.deleteStore(store)

    /** Busca en Room candidatos duplicados dentro de un radio de ~100 m.  */
    suspend fun findNearbyStoresByName(
        name: String,
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
        excludeId: Long = 0L
    ): List<LocalComercial> =
        storeDao.findNearbyByName(name, minLat, maxLat, minLon, maxLon, excludeId)

    /**
     * Consulta Firestore para ver si ya existe un local con el mismo nombre
     * en un radio de ~100 m. Devuelve null si no hay conexión o no hay duplicado.
     */
    suspend fun checkRemoteStoreDuplicate(name: String, lat: Double, lon: Double): RemoteStoreDto? =
        remoteStoreDataSource?.checkAndFetchDuplicate(name, lat, lon)

    /** Flow de departamentos activos del usuario (vacío = todos). */
    fun getActiveDepartmentsFlow(): Flow<Set<String>> =
        cityGroupPreferences?.activeDepartmentsFlow ?: flowOf(emptySet())

    /** Activa/desactiva un departamento en las preferencias del usuario. */
    suspend fun toggleDepartment(dept: String) {
        cityGroupPreferences?.toggleDepartment(dept)
    }

    // ── Product operations ────────────────────────────────────────────────────

    fun getAllProducts(): Flow<List<Producto>> = productDao.getAllProducts()
    suspend fun getProductById(id: Long) = productDao.getProductById(id)

    /**
     * Busca productos por nombre combinando fuentes local y Firestore.
     * Estrategia de merge:
     *  - Los resultados remotos son la fuente de verdad para imagen y datos.
     *  - Si el barcode del resultado remoto ya existe en Room → se mergea con el id local.
     *  - Si no existe localmente → se presenta como entrada temporal (id=0, sin guardar aún).
     * Se requieren mínimo 4 caracteres para consultar Firebase.
     * Emite primero los resultados locales (rápido) y luego el merge completo.
     */
    fun searchProductsByName(query: String): Flow<List<Producto>> = flow {
        if (query.length < 4) {
            emit(emptyList())
            return@flow
        }

        // 1. Primero emite resultados locales de inmediato para baja latencia
        val localResults = productDao.searchProductsOnce(query)
        emit(localResults)

        // 2. Consulta Firestore en paralelo
        val remoteResults = remoteDataSource?.fetchByName(query) ?: emptyList()
        if (remoteResults.isEmpty()) return@flow

        // 3. Merge: para cada resultado remoto, ver si el barcode ya existe en Room
        val merged = mutableListOf<Producto>()
        val seenNames = mutableSetOf<String>()

        remoteResults.forEach { dto ->
            val localByBarcode = dto.barcode.takeIf { it.isNotBlank() }
                ?.let { productDao.getProductByBarcode(it) }

            // Descarga la imagen remota si no la tenemos aún
            val localPhotoUri = localByBarcode?.photoUri
                ?: dto.photoUrl?.let { ImageCompressor.downloadAndSave(context, it) }

            val merged_product = if (localByBarcode != null) {
                // Merge: usa id local pero datos de la nube (foto/nombre/peso)
                localByBarcode.copy(
                    name = dto.name,
                    weightQuantity = dto.weightQuantity,
                    remoteId = dto.remoteId,
                    remotePhotoUrl = dto.photoUrl,
                    photoUri = localPhotoUri ?: localByBarcode.photoUri
                )
            } else {
                // Sin barcode local: entrada temporal (id=0), no se guarda aún en Room
                Producto(
                    id = 0L,
                    barcode = dto.barcode.takeIf { it.isNotBlank() },
                    name = dto.name,
                    weightQuantity = dto.weightQuantity,
                    photoUri = localPhotoUri,
                    remoteId = dto.remoteId,
                    remotePhotoUrl = dto.photoUrl
                )
            }
            if (seenNames.add(dto.name.lowercase())) {
                merged.add(merged_product)
            }
        }

        // 4. Agrega resultados locales que no aparecieron en el remoto (sin remoteId)
        localResults.forEach { local ->
            val nameKey = local.name.lowercase()
            if (seenNames.add(nameKey)) {
                merged.add(local)
            }
        }

        emit(merged.sortedBy { it.name })
    }

    /**
     * Estrategia First-Local:
     * 1. Busca en Room → retorna Local
     * 2. Si no existe → consulta Firestore → descarga imagen → cachea en Room → retorna Remote
     */
    suspend fun findProductByBarcode(barcode: String): ProductLookupResult {
        val local = productDao.getProductByBarcode(barcode)
        if (local != null) return ProductLookupResult.Local(local)

        val remote = remoteDataSource?.fetchByBarcode(barcode) ?: return ProductLookupResult.NotFound

        val localPhotoUri = remote.photoUrl?.let { url ->
            Log.d(TAG, "Descargando foto para producto encontrado en la nube: $url")
            ImageCompressor.downloadAndSave(context, url)
        }
        val cached = Producto(
            barcode = remote.barcode,
            name = remote.name,
            weightQuantity = remote.weightQuantity,
            photoUri = localPhotoUri,
            remoteId = remote.remoteId,
            remotePhotoUrl = remote.photoUrl
        )
        val newId = productDao.insertProduct(cached)
        return ProductLookupResult.Remote(cached.copy(id = newId))
    }

    suspend fun getProductByBarcode(barcode: String) = productDao.getProductByBarcode(barcode)
    fun searchProducts(query: String) = productDao.searchProducts(query)
    fun getProductsWithPricesByStore(storeId: Long): Flow<List<ProductWithPrice>> =
        productDao.getProductsWithPricesByStore(storeId)
    suspend fun insertProduct(product: Producto) = productDao.insertProduct(product)
    suspend fun updateProduct(product: Producto) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Producto) = productDao.deleteProduct(product)

    // ── Price operations ──────────────────────────────────────────────────────

    fun getPriceHistoryForProduct(productId: Long) = priceDao.getPriceHistoryForProduct(productId)
    fun getPriceHistoryForProductAtStore(productId: Long, storeId: Long) =
        priceDao.getPriceHistoryForProductAtStore(productId, storeId)
    suspend fun getCheapestPriceForProduct(productId: Long) = priceDao.getCheapestPriceForProduct(productId)
    fun getStoresWithPricesForProduct(productId: Long): Flow<List<StoreWithPrice>> =
        priceDao.getStoresWithPricesForProduct(productId)
    suspend fun insertPriceRecord(record: RegistroPrecio) = priceDao.insertPriceRecord(record)
    suspend fun deletePriceRecord(record: RegistroPrecio) = priceDao.deletePriceRecord(record)

    suspend fun exportDataToCsv(context: Context): Uri? {
        val records = priceDao.getAllRecordsForExport()
        if (records.isEmpty()) return null
        val csvHeader = "Producto,CódigoBarras,PesoCantidad,Local,Direccion,Precio,FechaRegistro\n"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append(csvHeader)
        for (record in records) {
            sb.append("${record.productName},${record.barcode ?: ""},${record.weightQuantity},${record.storeName},\"${record.storeAddress}\",${record.price},${dateFormat.format(Date(record.timestamp))}\n")
        }
        return try {
            val file = File(context.cacheDir, "precios_locales_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).use { it.write(sb.toString().toByteArray()) }
            FileProvider.getUriForFile(context, "com.rodrip.precioslocales.comparador.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
