package com.rodrip.precioslocales.comparador.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.model.StoreWithPrice
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductComparisonScreen(
    viewModel: ProductViewModel,
    productId: Long,
    onNavigateBack: () -> Unit,
    onHistoryClick: (Long, Long) -> Unit
) {
    val storesWithPrices by viewModel.getStoresWithPricesForProduct(productId).collectAsState()
    var product by remember { mutableStateOf<Producto?>(null) }

    LaunchedEffect(productId) {
        product = viewModel.getProductById(productId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparar Precios") },
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
            product?.let { p ->
                ProductHeader(p)
            }

            if (storesWithPrices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay registros de precios para este producto.")
                }
            } else {
                Text(
                    text = "Disponibilidad en locales",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(storesWithPrices) { index, item ->
                        StoreComparisonItem(
                            item = item,
                            isCheapest = index == 0,
                            onClick = { onHistoryClick(productId, item.store.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductHeader(product: Producto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                AsyncImage(
                    model = product.photoUri ?: "https://via.placeholder.com/150",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.weightQuantity,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!product.barcode.isNullOrEmpty()) {
                    Text(
                        text = "Código: ${product.barcode}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

fun Double.toLocalPriceAndSimbol(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return formatter.format(this)
}

@Composable
fun StoreComparisonItem(
    item: StoreWithPrice,
    isCheapest: Boolean,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val backgroundColor = if (isCheapest) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
    val contentColor = if (isCheapest) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        border = if (isCheapest) BorderStroke(2.dp, Color(0xFF4CAF50)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCheapest) 4.dp else 1.dp)
    ) {
        Column {
            if (isCheapest) {
                Surface(
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "¡MEJOR PRECIO!",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 12.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.store.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.store.address,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Actualizado: ${dateFormat.format(Date(item.lastUpdate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCheapest) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = item.price.toLocalPriceAndSimbol(),//"\$ ${String.format(Locale.getDefault(), \"%.2f\", item.price)}"
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isCheapest) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isCheapest) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Ver historial",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCheapest) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
