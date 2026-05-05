package com.rodrip.precioslocales.comparador.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.model.ProductWithPrice
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Producto>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Producto?

    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): Producto?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Producto>>

    @Query("""
        SELECT p.*, pr.price as price, pr.timestamp as lastUpdate
        FROM products p
        INNER JOIN (
            SELECT productId, price, timestamp
            FROM price_records 
            WHERE storeId = :storeId
            AND timestamp = (SELECT MAX(timestamp) FROM price_records WHERE productId = price_records.productId AND storeId = :storeId)
        ) pr ON p.id = pr.productId
        ORDER BY p.name ASC
    """)
    fun getProductsWithPricesByStore(storeId: Long): Flow<List<ProductWithPrice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Producto): Long

    @Update
    suspend fun updateProduct(product: Producto)

    @Delete
    suspend fun deleteProduct(product: Producto)
}