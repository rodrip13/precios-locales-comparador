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

    /** Retorna solo los locales cuyo departamento esté en la lista dada. */
    @Query("SELECT * FROM stores WHERE department IN (:departments) ORDER BY name ASC")
    fun getStoresByDepartments(departments: List<String>): Flow<List<LocalComercial>>

    /**
     * Buscas candidatos duplicados en un bounding box ≈ 100 m (±0.001°) alrededor de (lat, lon)
     * con coincidencia parcial de nombre. Excluye el propio local cuando se edita (excludeId).
     */
    @Query("""
        SELECT * FROM stores
        WHERE LOWER(name) LIKE '%' || LOWER(:name) || '%'
          AND latitude  BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLon AND :maxLon
          AND id != :excludeId
    """)
    suspend fun findNearbyByName(
        name: String,
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
        excludeId: Long = 0L
    ): List<LocalComercial>

    /** Locales que aún no han sido subidos a Firestore. */
    @Query("SELECT * FROM stores WHERE remoteId IS NULL")
    suspend fun getUnsynced(): List<LocalComercial>

    /** Vincula un local con su documento Firestore después de un push exitoso. */
    @Query("UPDATE stores SET remoteId = :remoteId, updatedAt = :updatedAt WHERE id = :localId")
    suspend fun updateSyncInfo(
        localId: Long,
        remoteId: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: LocalComercial): Long

    @Update
    suspend fun updateStore(store: LocalComercial)

    @Delete
    suspend fun deleteStore(store: LocalComercial)
}