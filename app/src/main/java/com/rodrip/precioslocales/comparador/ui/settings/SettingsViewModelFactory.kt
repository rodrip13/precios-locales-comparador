package com.rodrip.precioslocales.comparador.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rodrip.precioslocales.comparador.data.local.SyncPreferences
import com.rodrip.precioslocales.comparador.data.local.ThemePreferences
import com.rodrip.precioslocales.comparador.data.repository.MainRepository
import com.rodrip.precioslocales.comparador.data.sync.SyncManager

class SettingsViewModelFactory(
    private val repository: MainRepository,
    private val themePreferences: ThemePreferences,
    private val syncManager: SyncManager,
    private val syncPreferences: SyncPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, themePreferences, syncManager, syncPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
