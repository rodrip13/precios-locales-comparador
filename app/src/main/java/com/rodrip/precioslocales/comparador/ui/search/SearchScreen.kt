package com.rodrip.precioslocales.comparador.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.ui.products.ProductViewModel
import com.rodrip.precioslocales.comparador.ui.stores.StoreViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    productViewModel: ProductViewModel,
    storeViewModel: StoreViewModel,
    onProductClick: (Long) -> Unit,
    onStoreClick: (Long) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var stores by remember { mutableStateOf<List<LocalComercial>>(emptyList()) }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            productViewModel.searchProducts(query).collectLatest { products = it }
        } else {
            products = emptyList()
        }
    }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            storeViewModel.searchStores(query).collectLatest { stores = it }
        } else {
            stores = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Buscar productos o locales...") },
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                }
            )
        }
    ) { padding ->
        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("Ingresa un nombre o código de barras", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else if (products.isEmpty() && stores.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No se encontraron resultados para \"$query\"")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (products.isNotEmpty()) {
                    item {
                        Text("Productos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(products) { product ->
                        SearchItem(
                            title = product.name,
                            subtitle = product.barcode ?: product.weightQuantity,
                            icon = Icons.Rounded.Inventory2,
                            onClick = { onProductClick(product.id) }
                        )
                    }
                }

                if (stores.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Locales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(stores) { store ->
                        SearchItem(
                            title = store.name,
                            subtitle = store.address,
                            icon = Icons.Rounded.Storefront,
                            onClick = { onStoreClick(store.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}