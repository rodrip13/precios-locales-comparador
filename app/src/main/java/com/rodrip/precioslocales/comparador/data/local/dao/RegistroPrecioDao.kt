package com.rodrip.precioslocales.comparador.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rodrip.precioslocales.comparador.data.local.entity.RegistroPrecio
import com.rodrip.precioslocales.comparador.data.local.model.CsvExportRow
import com.rodrip.precioslocales.comparador.data.local.model.StoreWithPrice
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroPrecioDao {
    @Query("SELECT * FROM price_records WHERE productId = :productId ORDER BY timestamp DESC")
    fun getPriceHistoryForProduct(productId: Long): Flow<List<RegistroPrecio>>

    @Query("SELECT * FROM price_records WHERE productId = :productId AND storeId = :storeId ORDER BY timestamp DESC")
    fun getPriceHistoryForProductAtStore(productId: Long, storeId: Long): Flow<List<RegistroPrecio>>

    @Query("SELECT * FROM price_records WHERE productId = :productId ORDER BY price ASC LIMIT 1")
    suspend fun getCheapestPriceForProduct(productId: Long): RegistroPrecio?

    @Query("""
        SELECT s.*, pr.price as price, pr.timestamp as lastUpdate
        FROM stores s
        INNER JOIN (
            SELECT pr1.storeId, pr1.price, pr1.timestamp
            FROM price_records pr1
            WHERE pr1.productId = :productId
            AND pr1.timestamp = (
                SELECT MAX(timestamp) 
                FROM price_records pr2 
                WHERE pr2.productId = :productId AND pr2.storeId = pr1.storeId
            )
        ) pr ON s.id = pr.storeId
        ORDER BY pr.price ASC
    """)
    fun getStoresWithPricesForProduct(productId: Long): Flow<List<StoreWithPrice>>

    @Query("""
        SELECT p.name as productName, p.barcode as barcode, p.weightQuantity as weightQuantity, 
               s.name as storeName, s.address as storeAddress, pr.price as price, pr.timestamp as timestamp
        FROM price_records pr
        INNER JOIN products p ON pr.productId = p.id
        INNER JOIN stores s ON pr.storeId = s.id
        ORDER BY pr.timestamp DESC
    """)
    suspend fun getAllRecordsForExport(): List<CsvExportRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecord(record: RegistroPrecio): Long

    @Update
    suspend fun updatePriceRecord(record: RegistroPrecio)

    @Delete
    suspend fun deletePriceRecord(record: RegistroPrecio)
}