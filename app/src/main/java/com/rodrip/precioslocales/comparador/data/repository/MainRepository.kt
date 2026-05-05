package com.rodrip.precioslocales.comparador.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.rodrip.precioslocales.comparador.data.local.dao.LocalComercialDao
import com.rodrip.precioslocales.comparador.data.local.dao.ProductoDao
import com.rodrip.precioslocales.comparador.data.local.dao.RegistroPrecioDao
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.entity.RegistroPrecio
import com.rodrip.precioslocales.comparador.data.local.model.ProductWithPrice
import com.rodrip.precioslocales.comparador.data.local.model.StoreWithPrice
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainRepository(
    private val storeDao: LocalComercialDao,
    private val productDao: ProductoDao,
    private val priceDao: RegistroPrecioDao
) {
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
            val row = "${record.productName},${record.barcode ?: ""},${record.weightQuantity},${record.storeName},\"${record.storeAddress}\",${record.price},${dateFormat.format(Date(record.timestamp))}\n"
            sb.append(row)
        }

        return try {
            val fileName = "precios_locales_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { 
                it.write(sb.toString().toByteArray())
            }
            FileProvider.getUriForFile(
                context,
                "com.rodrip.precioslocales.comparador.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}