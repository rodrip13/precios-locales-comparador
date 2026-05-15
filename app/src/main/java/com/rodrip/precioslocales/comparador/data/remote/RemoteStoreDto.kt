package com.rodrip.precioslocales.comparador.data.remote

/**
 * DTO de un local comercial en Firestore.
 * Colección: "stores".
 */
data class RemoteStoreDto(
    val remoteId: String = "",
    val name: String = "",
    val address: String = "",
    val hours: String = "",
    val department: String = "",
    val localidad: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val updatedAt: Long = 0L,
    val uploadedBy: String = ""
)

