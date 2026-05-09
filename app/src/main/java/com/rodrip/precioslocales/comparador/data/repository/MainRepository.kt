package com.rodrip.precioslocales.comparador.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.rodrip.precioslocales.comparador.data.local.dao.LocalComercialDao
import com.rodrip.precioslocales.comparador.data.local.dao.ProductoDao
import com.rodrip.precioslocales.comparador.data.local.dao.RegistroPrecioDao
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.entity.RegistroPrecio
import com.rodrip.precioslocales.comparador.data.local.model.ProductWithPrice
import com.rodrip.precioslocales.comparador.data.local.model.StoreWithPrice
import com.rodrip.precioslocales.comparador.data.remote.RemoteProductDataSource
import com.rodrip.precioslocales.comparador.data.util.ImageCompressor
import kotlinx.coroutines.flow.Flow
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
    private val remoteDataSource: RemoteProductDataSource? = null
) {
    private val TAG = "MainRepository"

    // Store operations
    fun getAllStores(): Flow<List<LocalComercial>> = storeDao.getAllStores()
    suspend fun getStoreById(id: Long) = storeDao.getStoreById(id)
    fun searchStores(query: String) = storeDao.searchStores(query)
    suspend fun insertStore(store: LocalComercial) = storeDao.insertStore(store)
    suspend fun updateStore(store: LocalComercial) = storeDao.updateStore(store)
    suspend fun deleteStore(store: LocalComercial) = storeDao.deleteStore(store)

    // Product operations
    fun getAllProducts(): Flow<List<Producto>> = productDao.getAllProducts()
    suspend fun getProductById(id: Long) = productDao.getProductById(id)

    /**
     * Estrategia First-Local:
     * 1. Busca en Room → retorna Local
     * 2. Si no existe → consulta Firestore → descarga imagen → cachea en Room → retorna Remote
     */
    suspend fun findProductByBarcode(barcode: String): ProductLookupResult {
        val local = productDao.getProductByBarcode(barcode)
        if (local != null) return ProductLookupResult.Local(local)

        val remote = remoteDataSource?.fetchByBarcode(barcode) ?: return ProductLookupResult.NotFound

        // Descargar la imagen de Cloudinary para tenerla localmente (Local-First)
        val localPhotoUri = remote.photoUrl?.let { url ->
            Log.d(TAG, "Descargando foto para producto encontrado en la nube: $url")
            ImageCompressor.downloadAndSave(context, url)
        }

        // Cachear en Room con la URI local descargada
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

    // Price operations
    fun getPriceHistoryForProduct(productId: Long) = priceDao.getPriceHistoryForProduct(productId)
    fun getPriceHistoryForProductAtStore(productId: Long, storeId: Long) = 
        priceDao.getPriceHistoryForProductAtStore(productId, storeId)
    suspend fun getCheapestPriceForProduct(productId: Long) = priceDao.getCheapestPriceForProduct(productId)
    fun getStoresWithPricesForProduct(productId: Long): Flow<List<StoreWithPrice>> = 
        priceDao.getStoresWithPricesForProduct(productId)
    suspend fun insertPriceRecord(record: RegistroPrecio) = priceDao.insertPriceRecord(record)

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
