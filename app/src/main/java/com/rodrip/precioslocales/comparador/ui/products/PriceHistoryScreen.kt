package com.rodrip.precioslocales.comparador.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceHistoryScreen(
    viewModel: ProductViewModel,
    productId: Long,
    storeId: Long,
    onNavigateBack: () -> Unit
) {
    val history by viewModel.getPriceHistory(productId, storeId).collectAsState()
    var product by remember { mutableStateOf<Producto?>(null) }
    var store by remember { mutableStateOf<LocalComercial?>(null) }

    LaunchedEffect(productId, storeId) {
        product = viewModel.getProductById(productId)
        store = viewModel.getStoreById(storeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Precios") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (product != null && store != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = product?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "en ${store?.name ?: ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = store?.address ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("No hay historial disponible", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { record ->
                        HistoryItem(record.price, record.timestamp)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(price: Double, timestamp: Long) {
    val dateFormat = remember { SimpleDateFormat("dd 'de' MMMM, yyyy - HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = dateFormat.format(Date(timestamp)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "$ ${"%.2f".format(Locale.getDefault(), price)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}