package com.rodrip.precioslocales.comparador.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class LocalComercial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val hours: String,
    val latitude: Double,
    val longitude: Double,
    // Zona geográfica
    val department: String = "",
    val localidad: String = "",
    // Sync con Firestore
    val remoteId: String? = null,
    val uploadedBy: String = "",
    val updatedAt: Long = 0L
)