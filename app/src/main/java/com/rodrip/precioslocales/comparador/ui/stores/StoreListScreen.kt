package com.rodrip.precioslocales.comparador.ui.stores

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.model.UruguayGeo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreListScreen(
    viewModel: StoreViewModel,
    onAddStore: () -> Unit,
    onAddProduct: () -> Unit,
    onEditStore: (Long) -> Unit,
    onStoreClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val stores by viewModel.stores.collectAsState()
    val activeDepartments by viewModel.activeDepartments.collectAsState()
    val sortByDistance by viewModel.sortByDistance.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    var showFabMenu by remember { mutableStateOf(false) }
    var showDeptSheet by remember { mutableStateOf(false) }

    // Permiso de ubicación para "Cerca de mí"
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getCurrentLocationForList(context) { loc ->
                viewModel.setUserLocation(loc.latitude, loc.longitude)
            }
        }
    }

    // ── Sheet de gestión de departamentos ──────────────────────────────────────
    if (showDeptSheet) {
        DepartmentFilterSheet(
            activeDepartments = activeDepartments,
            onToggle = { dept -> viewModel.toggleDepartment(dept) },
            onDismiss = { showDeptSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Locales") },
                actions = {
                    // Badge con cantidad de departamentos activos
                    BadgedBox(
                        badge = {
                            if (activeDepartments.isNotEmpty())
                                Badge { Text("${activeDepartments.size}") }
                        }
                    ) {
                        IconButton(onClick = { showDeptSheet = true }) {
                            Icon(Icons.Rounded.FilterList, contentDescription = "Gestionar zonas")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Opciones")
                }
                DropdownMenu(expanded = showFabMenu, onDismissRequest = { showFabMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Nuevo Local") },
                        onClick = { showFabMenu = false; onAddStore() },
                        leadingIcon = { Icon(Icons.Rounded.Storefront, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Nuevo Producto") },
                        onClick = { showFabMenu = false; onAddProduct() },
                        leadingIcon = { Icon(Icons.Rounded.Inventory, null) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Chip "Cerca de mí" ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortByDistance,
                    onClick = {
                        if (sortByDistance) {
                            viewModel.clearDistanceSort()
                        } else {
                            locationPermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    label = { Text("Cerca de mí") },
                    leadingIcon = {
                        Icon(
                            if (sortByDistance) Icons.Rounded.LocationOn else Icons.Rounded.LocationSearching,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                if (activeDepartments.isNotEmpty()) {
                    FilterChip(
                        selected = true,
                        onClick = { showDeptSheet = true },
                        label = {
                            Text(
                                if (activeDepartments.size == 1) activeDepartments.first()
                                else "${activeDepartments.size} zonas"
                            )
                        },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Quitar filtro", modifier = Modifier.size(14.dp).clickable { /* clear from VM */ }) }
                    )
                }
            }

            if (stores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Storefront, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No hay locales registrados", style = MaterialTheme.typography.bodyLarge)
                        Text("Toca el botón + para agregar uno", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stores) { store ->
                        val distanceText = if (sortByDistance && userLocation != null) {
                            val dist = UruguayGeo.haversineMeters(
                                userLocation!!.first, userLocation!!.second,
                                store.latitude, store.longitude
                            )
                            UruguayGeo.formatDistance(dist)
                        } else null

                        StoreItem(
                            store = store,
                            distanceText = distanceText,
                            onClick = { onStoreClick(store.id) },
                            onEdit = { onEditStore(store.id) },
                            onDelete = { viewModel.deleteStore(store) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Item de tienda con distancia opcional ─────────────────────────────────────

@Composable
fun StoreItem(
    store: LocalComercial,
    distanceText: String? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(store.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (store.address.isNotEmpty())
                    Text(store.address, style = MaterialTheme.typography.bodyMedium)
                // Zona
                if (store.department.isNotEmpty() || store.localidad.isNotEmpty()) {
                    val zona = listOf(store.localidad, store.department).filter { it.isNotEmpty() }.joinToString(", ")
                    Text(
                        "📍 $zona",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (store.hours.isNotEmpty())
                    Text(store.hours, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                if (distanceText != null)
                    Text("🚶 $distanceText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            Row {
                TextButton(onClick = onEdit) { Text("Editar") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ─── Bottom Sheet de departamentos ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartmentFilterSheet(
    activeDepartments: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Zonas activas",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                if (activeDepartments.isEmpty()) "Mostrando todos los departamentos"
                else "Mostrando ${activeDepartments.size} departamento(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                UruguayGeo.getDepartmentNames().forEach { dept ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(dept) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = activeDepartments.isEmpty() || dept in activeDepartments,
                            onCheckedChange = { onToggle(dept) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(dept, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocationForList(context: Context, onLocation: (Location) -> Unit) {
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { loc: Location? -> loc?.let { onLocation(it) } }
}
