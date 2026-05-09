package com.rodrip.precioslocales.comparador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rodrip.precioslocales.comparador.data.local.AppTheme
import com.rodrip.precioslocales.comparador.ui.main.MainScreen
import com.rodrip.precioslocales.comparador.ui.settings.SettingsViewModel
import com.rodrip.precioslocales.comparador.ui.settings.SettingsViewModelFactory
import com.rodrip.precioslocales.comparador.ui.stores.StoreViewModel
import com.rodrip.precioslocales.comparador.ui.stores.StoreViewModelFactory
import com.rodrip.precioslocales.comparador.ui.theme.ComparaPreciosRodriPTheme

class MainActivity : ComponentActivity() {
    
    private val storeViewModel: StoreViewModel by viewModels {
        StoreViewModelFactory((application as MainApplication).repository)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as MainApplication
        SettingsViewModelFactory(
            app.repository,
            app.themePreferences,
            app.syncManager,
            app.syncPreferences
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState by settingsViewModel.themeState.collectAsState()
            
            ComparaPreciosRodriPTheme(appTheme = themeState) {
                MainScreen(storeViewModel = storeViewModel)
            }
        }
    }
}