package com.rodrip.precioslocales.comparador.ui.stores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rodrip.precioslocales.comparador.data.repository.MainRepository

class StoreViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoreViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}