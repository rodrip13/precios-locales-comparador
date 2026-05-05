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
    val longitude: Double
)