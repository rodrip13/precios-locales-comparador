package com.rodrip.precioslocales.comparador.ui.stores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreListScreen(
    viewModel: StoreViewModel,
    onAddStore: () -> Unit,
    onAddProduct: () -> Unit,
    onEditStore: (Long) -> Unit,
    onStoreClick: (Long) -> Unit
) {
    val stores by viewModel.stores.collectAsState()
    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Locales") }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Opciones")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nuevo Local") },
                        onClick = {
                            showFabMenu = false
                            onAddStore()
                        },
                        leadingIcon = { Icon(Icons.Rounded.Storefront, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Nuevo Producto") },
                        onClick = {
                            showFabMenu = false
                            onAddProduct()
                        },
                        leadingIcon = { Icon(Icons.Rounded.Inventory, null) }
                    )
                }
            }
        }
    ) { padding ->
        if (stores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay locales registrados",
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
                items(stores) { store ->
                    StoreItem(
                        store = store,
                        onClick = { onStoreClick(store.id) },
                        onEdit = { onEditStore(store.id) },
                        onDelete = { viewModel.deleteStore(store) }
                    )
                }
            }
        }
    }
}

@Composable
fun StoreItem(
    store: LocalComercial,
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
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = store.address,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (store.hours.isNotEmpty()) {
                    Text(
                        text = store.hours,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
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
