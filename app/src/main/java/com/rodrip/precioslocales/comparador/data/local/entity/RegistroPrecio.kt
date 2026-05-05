package com.rodrip.precioslocales.comparador.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_records",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocalComercial::class,
            parentColumns = ["id"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["storeId"])
    ]
)
data class RegistroPrecio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val storeId: Long,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)