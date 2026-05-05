package com.rodrip.precioslocales.comparador.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.model.ProductWithPrice
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    storeId: Long,
    onAddProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onProductClick: (Long) -> Unit
) {
    val productsWithPrices by viewModel.getProductsWithPricesByStore(storeId).collectAsState()
    var storeName by remember { mutableStateOf("") }

    LaunchedEffect(storeId) {
        viewModel.getStoreById(storeId)?.let {
            storeName = it.name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productos - $storeName") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Rounded.Add, contentDescription = "Agregar Producto")
            }
        }
    ) { padding ->
        if (productsWithPrices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay productos en este local",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Toca el botón + para agregar uno",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(productsWithPrices) { item ->
                    ProductItem(
                        product = item.product,
                        price = item.price,
                        onClick = { onProductClick(item.product.id) },
                        onEdit = { onEditProduct(item.product.id) },
                        onDelete = { viewModel.deleteProduct(item.product) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Producto,
    price: Double,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.small
            ) {
                AsyncImage(
                    model = product.photoUri ?: "https://via.placeholder.com/150",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.weightQuantity,
                    style = MaterialTheme.typography.bodySmall
                )
                if (!product.barcode.isNullOrEmpty()) {
                    Text(
                        text = "EAN: ${product.barcode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Text(
                text = "$ ${"%.2f".format(Locale.getDefault(), price)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Row {
                TextButton(onClick = onEdit) {
                    Text("Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}