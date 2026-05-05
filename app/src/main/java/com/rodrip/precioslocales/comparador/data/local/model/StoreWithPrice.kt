package com.rodrip.precioslocales.comparador.data.local.model

import androidx.room.Embedded
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial

data class StoreWithPrice(
    @Embedded val store: LocalComercial,
    val price: Double,
    val lastUpdate: Long
)