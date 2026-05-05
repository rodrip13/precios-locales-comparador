package com.rodrip.precioslocales.comparador.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalComercialDao {
    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getAllStores(): Flow<List<LocalComercial>>

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getStoreById(id: Long): LocalComercial?

    @Query("SELECT * FROM stores WHERE name LIKE '%' || :query || '%'")
    fun searchStores(query: String): Flow<List<LocalComercial>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: LocalComercial): Long

    @Update
    suspend fun updateStore(store: LocalComercial)

    @Delete
    suspend fun deleteStore(store: LocalComercial)
}