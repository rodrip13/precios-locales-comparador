package com.rodrip.precioslocales.comparador.data.remote

/**
 * Representación de un producto en Firestore.
 * NO incluye precio ni local comercial — esos datos son estrictamente locales.
 */
data class RemoteProductDto(
    val remoteId: String = "",
    val barcode: String = "",
    val name: String = "",
    val weightQuantity: String = "",
    val photoUrl: String? = null,
    val updatedAt: Long = 0L,
    val uploadedBy: String = ""   // UID anónimo del contribuidor
)

