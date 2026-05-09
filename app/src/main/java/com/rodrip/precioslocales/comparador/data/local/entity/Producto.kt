package com.rodrip.precioslocales.comparador.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Producto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String?,
    val name: String,
    val weightQuantity: String,
    val photoUri: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val remoteId: String? = null,
    val remotePhotoUrl: String? = null
)