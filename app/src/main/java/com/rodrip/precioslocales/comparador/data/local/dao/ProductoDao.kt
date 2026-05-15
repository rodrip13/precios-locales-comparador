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

    @Query("SELECT * FROM products WHERE remoteId IS NULL")
    suspend fun getUnsynced(): List<Producto>

    @Query("SELECT * FROM products WHERE remoteId IS NOT NULL AND remotePhotoUrl IS NULL AND photoUri IS NOT NULL")
    suspend fun getProductsMissingRemotePhoto(): List<Producto>

    @Query("SELECT * FROM products WHERE remotePhotoUrl IS NOT NULL AND photoUri IS NULL")
    suspend fun getProductsMissingLocalPhoto(): List<Producto>

    @Query("UPDATE products SET remoteId = :remoteId, remotePhotoUrl = :remotePhotoUrl WHERE id = :localId")
    suspend fun updateSyncInfo(localId: Long, remoteId: String, remotePhotoUrl: String?)

    @Query("UPDATE products SET remotePhotoUrl = :remotePhotoUrl WHERE id = :localId")
    suspend fun updateRemotePhotoUrl(localId: Long, remotePhotoUrl: String)

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Producto>>

    /** Versión suspend (una sola emisión) para usar dentro de flow builders y coroutines. */
    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 8")
    suspend fun searchProductsOnce(query: String): List<Producto>

    @Query("""
        SELECT p.*, pr.price as price, pr.timestamp as lastUpdate
        FROM products p
        INNER JOIN (
            SELECT pr1.productId, pr1.price, pr1.timestamp
            FROM price_records pr1
            WHERE pr1.storeId = :storeId
            AND pr1.timestamp = (SELECT MAX(timestamp) FROM price_records pr2 WHERE pr2.productId = pr1.productId AND pr2.storeId = :storeId)
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
