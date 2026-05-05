package com.rodrip.precioslocales.comparador.data.local.model

import androidx.room.Embedded
import com.rodrip.precioslocales.comparador.data.local.entity.Producto

data class ProductWithPrice(
    @Embedded val product: Producto,
    val price: Double,
    val lastUpdate: Long
)