package com.rodrip.precioslocales.comparador.data.local.model

data class CsvExportRow(
    val productName: String,
    val barcode: String?,
    val weightQuantity: String,
    val storeName: String,
    val storeAddress: String,
    val price: Double,
    val timestamp: Long
)