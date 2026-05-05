package com.rodrip.precioslocales.comparador.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.entity.RegistroPrecio
import com.rodrip.precioslocales.comparador.data.local.model.ProductWithPrice
import com.rodrip.precioslocales.comparador.data.local.model.StoreWithPrice
import com.rodrip.precioslocales.comparador.data.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: MainRepository) : ViewModel() {

    fun getProductsWithPricesByStore(storeId: Long): StateFlow<List<ProductWithPrice>> = 
        repository.getProductsWithPricesByStore(storeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getStoresWithPricesForProduct(productId: Long): StateFlow<List<StoreWithPrice>> =
        repository.getStoresWithPricesForProduct(productId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getPriceHistory(productId: Long, storeId: Long): StateFlow<List<RegistroPrecio>> =
        repository.getPriceHistoryForProductAtStore(productId, storeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun searchProducts(query: String): Flow<List<Producto>> = repository.searchProducts(query)

    private val _scannedProduct = MutableStateFlow<Producto?>(null)
    val scannedProduct: StateFlow<Producto?> = _scannedProduct

    fun searchProductByBarcode(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            _scannedProduct.value = product
        }
    }

    fun saveProductWithPrice(
        storeId: Long,
        productId: Long = 0,
        barcode: String?,
        name: String,
        weightQuantity: String,
        photoUri: String?,
        price: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val product = Producto(
                id = productId,
                barcode = barcode,
                name = name,
                weightQuantity = weightQuantity,
                photoUri = photoUri
            )
            
            val finalProductId = if (productId == 0L) {
                val existing = barcode?.let { repository.getProductByBarcode(it) }
                if (existing != null) {
                    repository.updateProduct(product.copy(id = existing.id))
                    existing.id
                } else {
                    repository.insertProduct(product)
                }
            } else {
                repository.updateProduct(product)
                productId
            }

            val priceRecord = RegistroPrecio(
                productId = finalProductId,
                storeId = storeId,
                price = price
            )
            repository.insertPriceRecord(priceRecord)
            onSuccess()
        }
    }

    suspend fun getProductById(id: Long) = repository.getProductById(id)
    
    suspend fun getStoreById(id: Long) = repository.getStoreById(id)

    fun clearScannedProduct() {
        _scannedProduct.value = null
    }

    fun deleteProduct(product: Producto) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }
}