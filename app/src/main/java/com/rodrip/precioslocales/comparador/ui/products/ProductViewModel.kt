package com.rodrip.precioslocales.comparador.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.entity.RegistroPrecio
import com.rodrip.precioslocales.comparador.data.local.model.ProductWithPrice
import com.rodrip.precioslocales.comparador.data.local.model.StoreWithPrice
import com.rodrip.precioslocales.comparador.data.repository.MainRepository
import com.rodrip.precioslocales.comparador.data.repository.ProductLookupResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Origen del producto encontrado al escanear un barcode */
enum class ProductSource { LOCAL, REMOTE, NOT_FOUND }

data class ScannedProductState(
    val product: Producto? = null,
    val source: ProductSource = ProductSource.NOT_FOUND
)

class ProductViewModel(private val repository: MainRepository) : ViewModel() {

    val stores: StateFlow<List<LocalComercial>> = repository.getAllStores()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    // initialValue = null → distinguishes "loading" from "truly empty" (emptyList).
    // This prevents the false empty-state flash while Room emits the first result.
    fun getPriceHistory(productId: Long, storeId: Long): StateFlow<List<RegistroPrecio>?> =
        repository.getPriceHistoryForProductAtStore(productId, storeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun searchProducts(query: String): Flow<List<Producto>> = repository.searchProducts(query)

    // ── Búsqueda por nombre para autocompletado ───────────────────────────────

    private val _nameQuery = MutableStateFlow("")

    /** Actualiza la query de búsqueda por nombre. Emite sugerencias a partir del 4° carácter. */
    fun onNameQueryChanged(query: String) {
        _nameQuery.value = query
    }

    /** Producto seleccionado desde las sugerencias de nombre. Se limpia tras consumirlo. */
    private val _selectedNameSuggestion = MutableStateFlow<Producto?>(null)
    val selectedNameSuggestion: StateFlow<Producto?> = _selectedNameSuggestion

    fun selectNameSuggestion(product: Producto) {
        _selectedNameSuggestion.value = product
    }

    fun clearSelectedNameSuggestion() {
        _selectedNameSuggestion.value = null
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val nameSuggestions: StateFlow<List<Producto>> = _nameQuery
        .debounce(300L)
        .filter { it.length >= 4 }
        .flatMapLatest { query -> repository.searchProductsByName(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ── Escaneo de barcode ────────────────────────────────────────────────────

    private val _scannedProductState = MutableStateFlow(ScannedProductState())
    val scannedProductState: StateFlow<ScannedProductState> = _scannedProductState

    /** Compatibilidad con código existente */
    val scannedProduct: StateFlow<Producto?> get() = MutableStateFlow(_scannedProductState.value.product)

    fun searchProductByBarcode(barcode: String) {
        viewModelScope.launch {
            val result = repository.findProductByBarcode(barcode)
            _scannedProductState.value = when (result) {
                is ProductLookupResult.Local -> ScannedProductState(result.product, ProductSource.LOCAL)
                is ProductLookupResult.Remote -> ScannedProductState(result.product, ProductSource.REMOTE)
                is ProductLookupResult.NotFound -> ScannedProductState(null, ProductSource.NOT_FOUND)
            }
        }
    }

    fun clearScannedProduct() {
        _scannedProductState.value = ScannedProductState()
    }

    // ── Guardar producto con precio ───────────────────────────────────────────

    fun saveProductWithPrice(
        storeId: Long,
        productId: Long = 0,
        barcode: String?,
        name: String,
        weightQuantity: String,
        photoUri: String?,
        price: Double,
        remoteId: String? = null,
        remotePhotoUrl: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val product = Producto(
                id = productId,
                barcode = barcode,
                name = name,
                weightQuantity = weightQuantity,
                photoUri = photoUri,
                remoteId = remoteId,
                remotePhotoUrl = remotePhotoUrl
            )

            val finalProductId = if (productId == 0L) {
                val existing = barcode?.let { repository.getProductByBarcode(it) }
                if (existing != null) {
                    // Merge: preserva remoteId del existente, pero actualiza con datos de nube si hay nuevos
                    repository.updateProduct(
                        product.copy(
                            id = existing.id,
                            remoteId = remoteId ?: existing.remoteId,
                            remotePhotoUrl = remotePhotoUrl ?: existing.remotePhotoUrl
                        )
                    )
                    existing.id
                } else {
                    repository.insertProduct(product)
                }
            } else {
                repository.updateProduct(product)
                productId
            }

            repository.insertPriceRecord(RegistroPrecio(productId = finalProductId, storeId = storeId, price = price))
            onSuccess()
        }
    }

    suspend fun getProductById(id: Long) = repository.getProductById(id)
    suspend fun getStoreById(id: Long) = repository.getStoreById(id)

    fun deleteProduct(product: Producto) {
        viewModelScope.launch { repository.deleteProduct(product) }
    }

    fun deletePriceRecord(record: RegistroPrecio) {
        viewModelScope.launch { repository.deletePriceRecord(record) }
    }

    fun restorePriceRecord(record: RegistroPrecio) {
        viewModelScope.launch { repository.insertPriceRecord(record) }
    }
}
