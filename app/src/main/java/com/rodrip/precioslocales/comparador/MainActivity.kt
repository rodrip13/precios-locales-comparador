package com.rodrip.precioslocales.comparador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rodrip.precioslocales.comparador.ui.main.MainScreen
import com.rodrip.precioslocales.comparador.ui.stores.StoreViewModel
import com.rodrip.precioslocales.comparador.ui.stores.StoreViewModelFactory
import com.rodrip.precioslocales.comparador.ui.theme.ComparaPreciosRodriPTheme

class MainActivity : ComponentActivity() {
    
    private val storeViewModel: StoreViewModel by viewModels {
        StoreViewModelFactory((application as MainApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComparaPreciosRodriPTheme {
                MainScreen(storeViewModel = storeViewModel)
            }
        }
    }
}