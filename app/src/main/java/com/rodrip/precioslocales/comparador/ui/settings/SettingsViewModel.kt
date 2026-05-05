package com.rodrip.precioslocales.comparador.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrip.precioslocales.comparador.data.repository.MainRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: MainRepository) : ViewModel() {

    fun exportDataToCsv(context: Context, onResult: (android.net.Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = repository.exportDataToCsv(context)
            onResult(uri)
        }
    }
}
