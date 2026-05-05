package com.rodrip.precioslocales.comparador.ui.stores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoreViewModel(private val repository: MainRepository) : ViewModel() {

    val stores: StateFlow<List<LocalComercial>> = repository.getAllStores()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveStore(
        id: Long = 0,
        name: String,
        address: String,
        hours: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val store = LocalComercial(
                id = id,
                name = name,
                address = address,
                hours = hours,
                latitude = latitude,
                longitude = longitude
            )
            if (id == 0L) {
                repository.insertStore(store)
            } else {
                repository.updateStore(store)
            }
            onSuccess()
        }
    }

    suspend fun getStoreById(id: Long): LocalComercial? {
        return repository.getStoreById(id)
    }

    fun searchStores(query: String): Flow<List<LocalComercial>> = repository.searchStores(query)

    fun deleteStore(store: LocalComercial) {
        viewModelScope.launch {
            repository.deleteStore(store)
        }
    }
}