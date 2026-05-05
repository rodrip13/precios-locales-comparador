package com.rodrip.precioslocales.comparador.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rodrip.precioslocales.comparador.data.repository.MainRepository

class ProductViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}